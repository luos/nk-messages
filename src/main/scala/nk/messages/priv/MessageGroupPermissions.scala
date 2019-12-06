package nk.messages.priv

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.SendPrivateMessage.SendingUser


object MessageGroupPermissions {

  class Permission()

  case class Allowed() extends Permission()

  case class Blocked() extends Permission()

  case class NotParticipant() extends Permission()

}

trait MessageGroupPermissions {

  import MessageGroupPermissions._

  def canPostTo(sendingUser: SendingUser, messageGroup: MessageGroup): IO[Permission]

  def canRead(sendingUser: SendingUser, messageGroup: MessageGroup): IO[Permission] = {
    canPostTo(sendingUser, messageGroup)
  }
}
