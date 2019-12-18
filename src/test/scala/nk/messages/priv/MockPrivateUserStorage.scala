package nk.messages.priv

import java.util.UUID

import scala.collection.mutable.ArrayBuffer

class MockPrivateUserStorage(users: Seq[PrivateMessageUser]) extends PrivateUserStorage {

  val findUsersCaledWith = ArrayBuffer[(Seq[UUID])]()

  override def findUsers(userIds: Seq[UUID]): Seq[PrivateMessageUser] = {
    findUsersCaledWith += (userIds)
    users.filter(u => {
      userIds.contains(u.userId)
    })
  }
}
