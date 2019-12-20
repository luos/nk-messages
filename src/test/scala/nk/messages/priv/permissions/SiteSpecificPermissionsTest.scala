package nk.messages.priv.permissions

import java.util.UUID

import nk.messages.CurrentUserId
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.MockGroupPermissions
import nk.messages.priv.permissions.MessageGroupPermissions.{Allowed, Blocked, BlockedWithReason, NotParticipant}
import org.scalatest.FunSuite

import scala.util.Random

class SiteSpecificPermissionsTest extends FunSuite {

  import nk.messages.TestUtils._

  private val currentUser = CurrentUserId(UUID.randomUUID())
  private val messageGroup = MessageGroup(UUID.randomUUID(), Seq(), None)

  test("given no site specific checkers, returns alllowed") {
    val checker = new SiteSpecificPermissions(Seq())
    assert(checker.canPostTo(currentUser, messageGroup).run == Allowed())
    assert(checker.canRead(currentUser, messageGroup).run == Allowed())
  }

  test("given a site specific checker, if blocked, returns blocked") {
    val permissions = new MockGroupPermissions().returning(Blocked())
    val checker = new SiteSpecificPermissions(Seq(permissions))
    assert(checker.canPostTo(currentUser, messageGroup).run == Blocked())
    assert(checker.canRead(currentUser, messageGroup).run == Blocked())
    assert(permissions.canPostCalledWith.head == (currentUser, messageGroup))
    assert(permissions.canReadCalledWith.head == (currentUser, messageGroup))
  }

  test("given a site specific checker, if any of them returns blocked, returns blocked") {
    val permissions = Random.shuffle(Seq(
      new MockGroupPermissions().returning(Allowed()),
      new MockGroupPermissions().returning(Blocked()),
      new MockGroupPermissions().returning((NotParticipant())),
      new MockGroupPermissions().returning(BlockedWithReason("hello"))
    ))

    val checker = new SiteSpecificPermissions(permissions)
    assert(checker.canPostTo(currentUser, messageGroup).run != Allowed())
    assert(checker.canRead(currentUser, messageGroup).run != Allowed())
  }

}
