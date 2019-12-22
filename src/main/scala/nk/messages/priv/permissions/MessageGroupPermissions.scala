package nk.messages.priv.permissions

import java.util.UUID

import cats.effect.IO
import nk.messages.CurrentUserId
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.UserBlockList
import nk.messages.priv.permissions.MessageGroupPermissions.{Allowed, Blocked, NotParticipant}


object MessageGroupPermissions {

  class Permission()

  case class Allowed() extends Permission()

  case class Blocked() extends Permission()

  case class NotParticipant() extends Permission()

  case class BlockedWithReason(reason: String) extends Permission()

}

trait MessageGroupPermissions {

  import MessageGroupPermissions._

  def canPostTo(sendingUser: CurrentUserId, messageGroup: MessageGroup): IO[Permission]

  def canRead(sendingUser: CurrentUserId, messageGroup: MessageGroup): IO[Permission] = {
    canPostTo(sendingUser, messageGroup)
  }
}

class InternalMessageGroupPermissions(blockList: UserBlockList)
  extends MessageGroupPermissions {

  override
  def canPostTo(sendingUser: CurrentUserId, messageGroup: MessageGroup): IO[MessageGroupPermissions.Permission] = {
    val isUserInGroup = messageGroup.users.contains(sendingUser.userId)
    if (isUserInGroup) {
      val Seq(otherUser: UUID) = messageGroup.users.filter(_ != sendingUser.userId)
      blockList.isBlocked(sendingUser, byUser = otherUser).map(
        blocked => {
          if (blocked) {
            Blocked()
          } else {
            Allowed()
          }
        }
      )
    } else {
      IO.pure(NotParticipant())
    }
  }
}
