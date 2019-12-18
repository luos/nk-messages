package nk.messages.priv

import java.util.UUID



trait PrivateUserStorage {
  def findUsers(userIds: Seq[UUID]) : Seq[PrivateMessageUser]
}
