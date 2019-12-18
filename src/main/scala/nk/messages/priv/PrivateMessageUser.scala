package nk.messages.priv

import java.util.UUID

case class PrivateMessageUser(userId: UUID, name: String) extends Ordered[PrivateMessageUser] {
  override def compare(that: PrivateMessageUser): Int = this.userId.compareTo(userId)
}
