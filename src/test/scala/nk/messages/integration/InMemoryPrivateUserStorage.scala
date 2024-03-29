package nk.messages.integration

import java.util.UUID

import cats.effect.IO
import nk.messages.priv.{PrivateMessageUser, PrivateUserStorage}

import scala.collection.mutable.ArrayBuffer

class InMemoryPrivateUserStorage(_users: Seq[PrivateMessageUser]) extends PrivateUserStorage {
  val users = ArrayBuffer[PrivateMessageUser](_users: _*)

  override def findUsers(userIds: Seq[UUID]): Seq[PrivateMessageUser] = {
    users.filter(
      u =>
        userIds.contains(u.userId)
    ).toSeq
  }

  override def findUser(userId: UUID): IO[Option[PrivateMessageUser]] = {
    IO.pure(users.find(_.userId == userId))
  }
}