package nk.messages.priv

import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup

object Groups {

  case class MessageGroup(id: UUID, users: Seq[UUID])

}

trait PrivateMessageGroups {
  def find(messageGroupId: UUID): IO[Option[MessageGroup]]

  def create(users: Seq[UUID]): IO[MessageGroup]
}
