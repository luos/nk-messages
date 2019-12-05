package nk.messages.priv

import java.util.UUID

import cats._
import cats.implicits._
import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.SendPrivateMessage.{ErrorResult, NewMessage, SendingUser, SuccessResult, TargetMessageGroup, TargetUser}
import org.scalatest.FunSuite

import scala.collection.mutable.ListBuffer

class SendPrivateMessageTest extends FunSuite {

  test("sending a message where there was none before") {
    val messages = new MockPrivateMessageStore()
    val targetGroupId = UUID.randomUUID()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val privateMessageSender = new SendPrivateMessage(
      new PrivateMessageGroups {
        override def find(messageGroupId: UUID): IO[Option[Groups.MessageGroup]] = {
          IO.pure(None)
        }

        override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
          IO.pure(
            MessageGroup(targetGroupId, Seq(sendingUser, receivingUser))
          )
        }

        override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = IO.pure(None)
      }
      , messages)

    val Right(result) = privateMessageSender.execute(
      sendingUser = SendingUser(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUser(receivingUser).asRight
    ).unsafeRunSync()
    assert(result.success)
    assert(result.messageGroup.id == targetGroupId)
    assert(messages.messages.size == 1)
    val (group, msgs) = messages.messages.head
    assert(result.messageGroup == group)
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
            MessageGroup(targetGroupId, Seq(sendingUser, receivingUser))
          ))
        }

        override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
          throw new Exception("should not be called")
        }

        override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = ???
      }
      , messages)

    val Right(result) = privateMessageSender.execute(
      sendingUser = SendingUser(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetMessageGroup(targetGroupId).asLeft
    ).unsafeRunSync()
    assert(result.success)
    assert(result.messageGroup.id == targetGroupId)
    val (group, msgs) = messages.messages.head
    assert(result.messageGroup.id == targetGroupId)
    assert(group.id == targetGroupId)
    assert(msgs.head.message == "hello")
  }

  test("sending a message containing html") {
    val messages = new MockPrivateMessageStore()
    val targetGroupId = UUID.randomUUID()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val privateMessageSender = new SendPrivateMessage(
      new PrivateMessageGroups {
        override def find(messageGroupId: UUID): IO[Option[Groups.MessageGroup]] = {
          IO.pure(Some(
            MessageGroup(targetGroupId, Seq(sendingUser, receivingUser))
          ))
        }

        override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
          throw new Exception("should not be called")
        }

        override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = ???
      }
      , messages)

    val Right(result) = privateMessageSender.execute(
      sendingUser = SendingUser(sendingUser),
      message = NewMessage("<script>hello</script><iframe>hello \n"),
      messageTarget = TargetMessageGroup(targetGroupId).asLeft
    ).unsafeRunSync()
    assert(result.success)
    assert(result.messageGroup.id == targetGroupId)
    val (group, msgs) = messages.messages.head
    assert(result.messageGroup.id == targetGroupId)
    assert(group.id == targetGroupId)
    assert(msgs.head.message == "hello")
  }

  test("sending a message to a non-existing group") {
    val messages = new MockPrivateMessageStore()
    val targetGroupId = UUID.randomUUID()
    val sendingUser = UUID.randomUUID()
    val privateMessageSender = new SendPrivateMessage(
      new PrivateMessageGroups {
        override def find(messageGroupId: UUID): IO[Option[Groups.MessageGroup]] = {
          IO.pure(None)
        }

        override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
          throw new Exception("should not be called")
        }

        override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = ???
      }
      , messages)

    val Left(result: ErrorResult) = privateMessageSender.execute(
      sendingUser = SendingUser(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetMessageGroup(targetGroupId).asLeft
    ).unsafeRunSync()
    assert(!result.success)
    assert(result.messages.exists(_.contains("Private message conversation does not exist.")))
  }

  test("sending a message to a user which we already have a group with") {
    val messages = new MockPrivateMessageStore()
    val sendingUser = UUID.randomUUID()
    val receivingUser = UUID.randomUUID()
    val privateMessageSender = new SendPrivateMessage(
      new PrivateMessageGroups {

        val groups = ListBuffer[MessageGroup]()

        override def find(messageGroupId: UUID): IO[Option[Groups.MessageGroup]] = {
          IO.pure(
            groups.find(_.id == messageGroupId).headOption
          )
        }

        override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
          val g = MessageGroup(UUID.randomUUID(), Seq(sendingUser, receivingUser))
          groups += g
          IO.pure(g)
        }

        override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = IO.pure(groups.headOption)
      }
      , messages)

    val Right(result: SuccessResult) = privateMessageSender.execute(
      sendingUser = SendingUser(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUser(receivingUser).asRight
    ).unsafeRunSync()
    val Right(result2: SuccessResult) = privateMessageSender.execute(
      sendingUser = SendingUser(sendingUser),
      message = NewMessage("hello"),
      messageTarget = TargetUser(receivingUser).asRight
    ).unsafeRunSync()

    assert(result.messageGroup == result2.messageGroup)
  }


}
