package nk.messages.priv

import java.util.UUID

import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore.NewStoredMessage

object PrivateMessageStore {

  case class NewStoredMessage(createdBy: UUID, message: String)

}

trait PrivateMessageStore {
  def insert(messageGroup: MessageGroup, message: NewStoredMessage)
}
