package nk.messages.priv

import java.time.Instant
import java.util.UUID

import cats._
import cats.implicits._
import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.SendPrivateMessage.{ErrorResult, NewMessage, SuccessResult, TargetMessageGroup, TargetUserId}
import nk.messages.{CurrentUserId, TestUtils}
import org.scalatest.FunSuite

class SendPrivateMessageTest extends FunSuite {

  val allowAllPermissions = new MessageGroupPermissions {
    override def canPostTo(sendingUser: CurrentUserId, messageGroup: MessageGroup): IO[MessageGroupPermissions.Permission] = {
      IO.pure(MessageGroupPermissions.Allowed())
    }
  }

  test("sending a message where there was none before") {
    val messages = new MockPrivateMessageStore()
    val targetGroupId = UUID.randomUUID()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val messageGroups = new MockMessageGroups()
    val privateMessageSender = new SendPrivateMessage(messageGroups, messages, allowAllPermissions)

    val Right(result) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUserId(receivingUser).asRight
    ).unsafeRunSync()
    assert(result.success)
    assert(messageGroups.groups.head.id == result.messageGroup.id)
    assert(messages.messages.size == 1)
    val (groupId, msgs) = messages.messages.head
    assert(result.messageGroup.id == groupId)
    assert(msgs.head.message == "hello")
  }

  test("sending a message where there was some before") {
    val messages = new MockPrivateMessageStore()
    val targetGroupId = UUID.randomUUID()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val privateMessageSender = new SendPrivateMessage(
      new PrivateMessageGroups {
        override def find(messageGroupId: UUID): IO[Option[Groups.MessageGroup]] = {
          IO.pure(Some(
            MessageGroup(targetGroupId, Seq(sendingUser, receivingUser), None)
          ))
        }

        override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
          throw new Exception("should not be called")
        }

        override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = ???

        override def findAllGroupsOfUser(user: UUID): IO[Seq[MessageGroup]] = ???

        override def setLastMessage(messageGroup: MessageGroup, time: Instant) = {
          IO.pure(messageGroup)
        }
      }
      , messages, allowAllPermissions)

    val Right(result) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetMessageGroup(targetGroupId).asLeft
    ).unsafeRunSync()
    assert(result.success)
    assert(result.messageGroup.id == targetGroupId)
    val (groupId, msgs) = messages.messages.head
    assert(result.messageGroup.id == targetGroupId)
    assert(groupId == targetGroupId)
    assert(msgs.head.message == "hello")
  }

  test("sending a message containing html") {
    val messages = new MockPrivateMessageStore()
    val targetGroupId = UUID.randomUUID()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val messageGroups = new MockMessageGroups().withGroupForTesting(
      MessageGroup(targetGroupId, Seq(sendingUser, receivingUser), None)
    )


    val privateMessageSender = new SendPrivateMessage(messageGroups, messages, allowAllPermissions)

    val Right(result) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("<script>hello</script><iframe>hello \n"),
      messageTarget = TargetMessageGroup(targetGroupId).asLeft
    ).unsafeRunSync()
    assert(result.success)
    assert(result.messageGroup.id == targetGroupId)
    val (groupId, msgs) = messages.messages.head
    assert(result.messageGroup.id == targetGroupId)
    assert(groupId == targetGroupId)
    assert(msgs.head.message == "hello")
    TestUtils.assertHappenedNowInstant(msgs.head.createdAt)
  }

  test("sending a message to a non-existing group") {
    val messages = new MockPrivateMessageStore()
    val targetGroupId = UUID.randomUUID()
    val sendingUser = UUID.randomUUID()
    val messageGroups = new MockMessageGroups()
    val privateMessageSender = new SendPrivateMessage(messageGroups, messages, allowAllPermissions)

    val Left(result: ErrorResult) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetMessageGroup(targetGroupId).asLeft
    ).unsafeRunSync()

    assert(!result.success)
    assert(messageGroups.groups.isEmpty)
    assert(result.messages.exists(_.contains("Private message conversation does not exist.")))

  }

  test("sending a message to a user which we already have a group with") {
    val messages = new MockPrivateMessageStore()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val privateMessageSender = new SendPrivateMessage(new MockMessageGroups(), messages, allowAllPermissions)

    val Right(result: SuccessResult) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUserId(receivingUser).asRight
    ).unsafeRunSync()
    val Right(result2: SuccessResult) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUserId(receivingUser).asRight
    ).unsafeRunSync()

    assert(result.messageGroup.id == result2.messageGroup.id)
  }

  test("given the user has no permission can not post to group") {
    val messages = new MockPrivateMessageStore()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val blockPostPermissions = new MessageGroupPermissions {
      override def canPostTo(sendingUser: CurrentUserId, messageGroup: MessageGroup): IO[MessageGroupPermissions.Permission] = {
        val p = MessageGroupPermissions.Blocked()
        IO.pure(p)
      }
    }
    val privateMessageSender = new SendPrivateMessage(new MockMessageGroups(), messages, blockPostPermissions)

    val Left(result: ErrorResult) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUserId(receivingUser).asRight
    ).unsafeRunSync()

    assert(result.messages.contains("You have no access to post to this group."))
  }

  test("sending a message to a non existing user") {
    val messages = new MockPrivateMessageStore()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val blockPostPermissions = new MessageGroupPermissions {
      override def canPostTo(sendingUser: CurrentUserId, messageGroup: MessageGroup): IO[MessageGroupPermissions.Permission] = {
        val p = MessageGroupPermissions.Blocked()
        IO.pure(p)
      }
    }
    val privateMessageSender = new SendPrivateMessage(
      new MockMessageGroups(),
      messages,
      blockPostPermissions)

    val Left(result: ErrorResult) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUserId(receivingUser).asRight
    ).unsafeRunSync()

    assert(result.messages.contains("You have no access to post to this group."))
  }

  test("sending a message should update groups last message timestamp") {
    val messages = new MockPrivateMessageStore()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val privateMessageSender = new SendPrivateMessage(new MockMessageGroups(), messages, allowAllPermissions)

    val Right(result: SuccessResult) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUserId(receivingUser).asRight
    ).unsafeRunSync()
    val Right(result2: SuccessResult) = privateMessageSender.execute(
      sendingUser = CurrentUserId(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUserId(receivingUser).asRight
    ).unsafeRunSync()

    assert(result.messageGroup.lastMessage.isDefined)
    TestUtils.assertHappenedNowInstant(result.messageGroup.lastMessage)
    assert((for {
      a <- result.messageGroup.lastMessage
      b <- result2.messageGroup.lastMessage
    } yield a.isBefore(b)).get)
  }

}
