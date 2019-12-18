package nk.messages.priv

import cats.effect.IO
import nk.messages.CurrentUserId
import nk.messages.priv.Groups.MessageGroup

import scala.collection.mutable.ListBuffer

class MockGroupPermissions extends MessageGroupPermissions {

  val canPostCalledWith: ListBuffer[(CurrentUserId, MessageGroup)] = ListBuffer()
  val canReadCalledWith: ListBuffer[(CurrentUserId, MessageGroup)] = ListBuffer()

  var _returning: MessageGroupPermissions.Permission = null

  def returning(messageGroupPermissions: MessageGroupPermissions.Permission) = {
    _returning = messageGroupPermissions
    this
  }

  override def canRead(sendingUser: CurrentUserId,
                       messageGroup: Groups.MessageGroup): IO[MessageGroupPermissions.Permission] = {
    val tuple = (sendingUser, messageGroup)
    canReadCalledWith += tuple
    IO.pure(_returning)
  }

  override def canPostTo(sendingUser: CurrentUserId,
                         messageGroup: Groups.MessageGroup): IO[MessageGroupPermissions.Permission] = {
    IO.pure(_returning)
  }
}
