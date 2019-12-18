package nk.messages.priv

import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup
import nk.messages.CurrentUserId
import org.scalatest.FunSuite
import cats.implicits._

class MessageGroupPermissionsImplementationTest extends FunSuite {

  val notBlocked = new UserBlockList {
    override def isBlocked(user: CurrentUserId, byUser: UUID): IO[Boolean] = IO.pure(false)
  }

  val blocked = new UserBlockList {
    override def isBlocked(user: CurrentUserId, byUser: UUID): IO[Boolean] = IO.pure(true)
  }

  private def groupWithUsers(users: Seq[UUID]) = {
    MessageGroup(UUID.randomUUID(), users, None)
  }

  test("Given a user is a member of the group, can post") {
    val permissions = new MessageGroupPermissionsImplementation(notBlocked)
    val user = CurrentUserId(UUID.randomUUID())
    val targetUser = UUID.randomUUID()
    val canPost = permissions.canPostTo(
      user, groupWithUsers(Seq(user.userId, targetUser)))
    assert(canPost.unsafeRunSync() == MessageGroupPermissions.Allowed())
  }

  test("Given a user is not a member of the group, can not post") {
    val permissions = new MessageGroupPermissionsImplementation(notBlocked)
    val user = CurrentUserId(UUID.randomUUID())
    val canPost = permissions.canPostTo(
      user, groupWithUsers(Seq(UUID.randomUUID(), UUID.randomUUID())))
    assert(canPost.unsafeRunSync() == MessageGroupPermissions.NotParticipant())
  }

  test("Given the sending user is blocked by the target user") {
    val permissions = new MessageGroupPermissionsImplementation(blocked)
    val user = CurrentUserId(UUID.randomUUID())
    val targetUser = UUID.randomUUID()
    val canPost = permissions.canPostTo(
      user, groupWithUsers(Seq(targetUser, user.userId)))
    assert(canPost.unsafeRunSync() == MessageGroupPermissions.Blocked())
  }
}
