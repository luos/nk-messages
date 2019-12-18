package nk.messages.priv

import java.time.LocalDateTime
import java.util.UUID

import cats.effect.IO
import org.scalatest.FunSuite

class GetConversationsForUserTest extends FunSuite {
  test("getting conversation for a user") {
    val groupId = UUID.randomUUID()
    val users = Seq(
      PrivateMessageUser(UUID.randomUUID(), "Coconut"),
      PrivateMessageUser(UUID.randomUUID(), "Orange")).sorted
    val currentUserId = nk.messages.CurrentUserId(users.head.userId)
    val pmg = new MockMessageGroups().withGroupForTesting(
      Groups.MessageGroup(groupId, users.map(_.userId), None)
    )

    val userStorage = new MockPrivateUserStorage(users)

    val getConversations = new GetConversationsForUser(pmg, userStorage)
    val convo = getConversations.list(currentUserId).unsafeRunSync().head

    assert(convo.conversationType == ConversationType.Private)
    assert(userStorage.findUsersCaledWith.head == users.map(_.userId))
    assert(convo.users.sorted == users)
    assert(pmg.groups.head.id == convo.messageGroupId)
    assert(convo.lastMessage == None)
  }



}
