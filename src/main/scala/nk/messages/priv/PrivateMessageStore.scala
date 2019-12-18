package nk.messages.priv

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore.{Message, MessageResult, NewStoredMessage}

object PrivateMessageStore {

  case class NewStoredMessage(createdBy: UUID, message: String, createdAt: Instant)

  /**
   * This will be displayed to the user
   */
  case class Message(message: String,
                     userId: UUID,
                     createdAt: Instant)

  case class MessageResult(messages: Seq[Message], count: Int)

}

trait PrivateMessageStore {
  def getMessagesForGroup(groupId: UUID, offset: Option[Int], limit: Int): IO[MessageResult]

  def insert(messageGroup: MessageGroup, message: NewStoredMessage)
}
