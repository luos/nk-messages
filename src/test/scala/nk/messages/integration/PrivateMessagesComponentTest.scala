package nk.messages.integration

import java.util.UUID

import nk.messages.{CurrentUserId, PrivateMessagesComponent, TestUtils}
import cats.effect.IO
import nk.messages.priv.ReadConversation.Messages
import nk.messages.priv.SendPrivateMessage.{NewMessage, TargetUserId}
import nk.messages.priv.metadata.MockMetadataRenderer
import nk.messages.priv.{Groups, PrivateMessageGroups, PrivateMessageStore, PrivateMessageUser, ReadConversation, SendPrivateMessage, UserBlockList}
import org.scalatest.FunSuite

class PrivateMessagesComponentTest extends FunSuite {
  test("that a message can be sent and read back") {
    val currentUserId = CurrentUserId(UUID.randomUUID())
    val targetUserId = TargetUserId(UUID.randomUUID())
    val pms = new PrivateMessagesComponent(
      new InMemoryPrivateMessageGroups,
      new InMemoryPrivateMessageStore,
      new InMemoryUserBlockList,
      new InMemoryPrivateUserStorage(Seq(
        PrivateMessageUser(currentUserId.userId, "Cacao"),
        PrivateMessageUser(targetUserId.userId, "Grapefruit"),
      )),
      new MockMetadataRenderer
    )

    val sendResult = pms.sendMessage.execute(
      sendingUser = currentUserId,
      messageTarget = Right(targetUserId),
      NewMessage("Hi! This is my message", None)
    ).unsafeRunSync()

    assert(sendResult.isRight)

    val conversations = pms.conversations.list(currentUserId).unsafeRunSync()
    assert(conversations.size == 1)
    val messages = pms.conversations
      .read(currentUserId, conversations.head.messageGroupId)
      .unsafeRunSync()
      .asInstanceOf[Messages]
    TestUtils.assertHappenedNowInstant(conversations.head.lastMessage)
    assert(conversations.head.users.map(_.userId).sorted == Seq(currentUserId.userId, targetUserId.userId).sorted)
    assert(messages.messages.head.message == "Hi! This is my message")
  }

  test("that multiple message can be sent and read back") {
    val currentUserId = CurrentUserId(UUID.randomUUID())
    val targetUserId = TargetUserId(UUID.randomUUID())
    val pms = new PrivateMessagesComponent(
      new InMemoryPrivateMessageGroups,
      new InMemoryPrivateMessageStore,
      new InMemoryUserBlockList,
      new InMemoryPrivateUserStorage(Seq(
        PrivateMessageUser(currentUserId.userId, "Cacao"),
        PrivateMessageUser(targetUserId.userId, "Grapefruit"),
      )),
      new MockMetadataRenderer
    )

    (1 to 95).map(n => {
      val sendResult = pms.sendMessage.execute(
        sendingUser = currentUserId,
        messageTarget = Right(targetUserId),
        NewMessage(s"message $n", None)
      ).unsafeRunSync()

      assert(sendResult.isRight)
    })


    val conversations = pms.conversations.list(currentUserId).unsafeRunSync()
    assert(conversations.size == 1)
    val messages = pms.conversations
      .read(currentUserId, conversations.head.messageGroupId)
      .unsafeRunSync()
      .asInstanceOf[Messages]
    TestUtils.assertHappenedNowInstant(conversations.head.lastMessage)
    assert(conversations.head.users.map(_.userId).sorted == Seq(currentUserId.userId, targetUserId.userId).sorted)
    assert(messages.messages.size == ReadConversation.PAGE_SIZE)
    assert(messages.totalPages == 4)
    assert(messages.currentPage == 4)
    assert(messages.messages.head.message == "message 71")
    assert(messages.messages.last.message == "message 95")
  }

}
