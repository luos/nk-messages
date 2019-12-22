package nk.messages.priv

import java.time.Instant
import java.util.UUID

import nk.messages.CurrentUserId
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore.{Message, NewStoredMessage}
import nk.messages.priv.ReadConversation.{GroupNotFound, Messages, NoPermission, Request, Response}
import nk.messages.priv.permissions.MessageGroupPermissions
import org.scalatest.FunSuite

class ReadConversationTest extends FunSuite {

  private val allowGroup = new MockGroupPermissions().returning(
    MessageGroupPermissions.Allowed()
  )

  test("given a conversation id, returns not found if group is not found") {
    val groups = new MockMessageGroups()
    val rc = new ReadConversation(groups, new MockPrivateMessageStore, allowGroup)
    val result = rc.execute(
      CurrentUserId(UUID.randomUUID()),
      UUID.randomUUID()
    ).unsafeRunSync()

    assert(result == GroupNotFound())
  }

  test("given a conversation id, returns messages from message store") {
    val groupId = UUID.randomUUID()
    val groups = new MockMessageGroups().withGroupForTesting(
      MessageGroup(groupId, Seq(), None)
    )
    val messages = new MockPrivateMessageStore
    val userId = UUID.randomUUID()
    val createdAt = Instant.now()
    messages.setMessagesForGroup(groupId, Seq(NewStoredMessage(userId, "this is a message", createdAt, None)))

    val rc = new ReadConversation(groups, messages, allowGroup)
    val result = rc.execute(
      CurrentUserId(UUID.randomUUID()),
      groupId
    ).unsafeRunSync()

    assert(result == Messages(Seq(
      Message("this is a message", userId = userId, createdAt = createdAt)
    ), 1, 1, 1))
  }

  test("given a conversation id, returns messages from message store if multiple batches are available") {
    val groupId = UUID.randomUUID()
    val groups = new MockMessageGroups().withGroupForTesting(
      MessageGroup(groupId, Seq(), None)
    )
    val messages = new MockPrivateMessageStore
    val userId = UUID.randomUUID()
    val createdAt = Instant.now()

    messages.setMessagesForGroup(groupId,
      (1 to 100).map(n => NewStoredMessage(userId, s"this is a message $n", createdAt, None))
    )

    val rc = new ReadConversation(groups, messages, allowGroup)
    val result = rc.execute(
      CurrentUserId(UUID.randomUUID()),
      groupId
    ).unsafeRunSync().asInstanceOf[Messages]

    assert(messages.getMessagesWasCalledWith.toList == Seq(
      (groupId, None, ReadConversation.PAGE_SIZE)
    ))

    assert(result.currentPage == 4)
    assert(result.totalPages == 4)
    assert(result.totalMessages == 100)
  }

  test("given a conversation id, reading a conversation checks for access") {
    val groupId = UUID.randomUUID()
    val permissions = new MockGroupPermissions().returning(
      MessageGroupPermissions.Blocked()
    )
    val group = MessageGroup(groupId, Seq(), None)
    val groups = new MockMessageGroups().withGroupForTesting(
      group
    )
    val messages = new MockPrivateMessageStore
    val userId = UUID.randomUUID()
    val createdAt = Instant.now()

    messages.setMessagesForGroup(groupId,
      (1 to 100).map(n => NewStoredMessage(userId, s"this is a message $n", createdAt, None))
    )

    val rc = new ReadConversation(groups, messages, permissions)
    val currentUserId = CurrentUserId(UUID.randomUUID())
    val result = rc.execute(
      currentUserId,
      groupId
    ).unsafeRunSync().asInstanceOf[NoPermission]

    assert(permissions.canReadCalledWith.head == (currentUserId, group))
  }


}
