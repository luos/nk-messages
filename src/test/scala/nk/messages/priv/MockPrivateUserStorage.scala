package nk.messages.priv

import java.util.UUID

import cats.effect.IO

import scala.collection.mutable.ArrayBuffer

class MockPrivateUserStorage(users: Seq[PrivateMessageUser]) extends PrivateUserStorage {

  val findUsersCaledWith = ArrayBuffer[(Seq[UUID])]()

  override def findUsers(userIds: Seq[UUID]): Seq[PrivateMessageUser] = {
    findUsersCaledWith += (userIds)
    users.filter(u => {
      userIds.contains(u.userId)
    })
  }

  override def findUser(userIds: UUID): IO[Option[PrivateMessageUser]] = {
    IO.pure(findUsers(Seq(userIds)).headOption)
  }
}
