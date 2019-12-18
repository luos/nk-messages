package nk.messages.priv

import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore.{Message, MessageResult, NewStoredMessage}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class MockPrivateMessageStore extends PrivateMessageStore {

  val messages: mutable.ListBuffer[(UUID, mutable.Iterable[NewStoredMessage])] = mutable.ListBuffer()

  def setMessagesForGroup(groupId: UUID, msgs: Seq[NewStoredMessage]): Unit = {
    val contents: (UUID, ListBuffer[NewStoredMessage]) = (groupId, mutable.ListBuffer(msgs: _*))
    messages += contents
  }

  override def insert(messageGroup: Groups.MessageGroup, message: PrivateMessageStore.NewStoredMessage): Unit = {
    val item = (messageGroup.id, mutable.ListBuffer(message))
    messages += item
  }

  val getMessagesWasCalledWith: mutable.ListBuffer[(UUID, Option[Int], Int)] = mutable.ListBuffer()

  override def getMessagesForGroup(groupId: UUID, offset: Option[Int], limit: Int): IO[PrivateMessageStore.MessageResult] = {
    val args = (groupId, offset, limit)
    getMessagesWasCalledWith += args
    IO.pure(
      messages.find(mg => mg._1 == groupId).map({
        case (mg, ms) => {
          val messages = ms.map(m => {
            Message(m.message, m.createdBy, m.createdAt)
          }).toList
          MessageResult(messages, messages.size)
        }
      }).getOrElse(
        MessageResult(Seq(), 0)
      )
    )

  }
}
