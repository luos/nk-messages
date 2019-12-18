package nk.messages.integration

import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore
import nk.messages.priv.PrivateMessageStore.{Message, MessageResult}

import scala.collection.mutable.ListBuffer

class InMemoryPrivateMessageStore extends PrivateMessageStore {
  val groupsMessages: ListBuffer[(MessageGroup, ListBuffer[Message])] = ListBuffer()

  override def insert(messageGroup: MessageGroup, message: PrivateMessageStore.NewStoredMessage): Unit = {
    groupsMessages.find(_._1.id == messageGroup.id) match {
      case None => {
        val elem = (messageGroup, ListBuffer[Message]())
        groupsMessages += elem
        ()
      }
      case Some(_) => ()
    }
    groupsMessages.find(_._1.id == messageGroup.id).head._2 +=
      Message(message.message, message.createdBy, message.createdAt)
  }

  override def getMessagesForGroup(groupId: UUID, offset: Option[Int], limit: Int): IO[PrivateMessageStore.MessageResult] = {
    val r = groupsMessages.find(_._1.id == groupId).map(
      { case (group, messages) => {
        val count = messages.size
        MessageResult(messages.toSeq.drop(offset.getOrElse(count - limit)), count)
      }
      }
    ).getOrElse({
      MessageResult(Seq(), 0)
    })
    IO.pure(r)
  }
}
