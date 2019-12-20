package nk.messages.priv

import java.util.UUID

import cats.effect.IO


trait PrivateUserStorage {
  def findUsers(userIds: Seq[UUID]): Seq[PrivateMessageUser]

  def findUser(userIds: UUID): IO[Option[PrivateMessageUser]]
}
