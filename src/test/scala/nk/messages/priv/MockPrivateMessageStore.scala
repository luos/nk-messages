package nk.messages.priv

import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore.NewStoredMessage

import scala.collection.mutable

class MockPrivateMessageStore extends PrivateMessageStore {

  val messages: mutable.ListBuffer[(MessageGroup, mutable.Iterable[NewStoredMessage])] = mutable.ListBuffer()

  override def insert(messageGroup: Groups.MessageGroup, message: PrivateMessageStore.NewStoredMessage): Unit = {
    val item = (messageGroup, mutable.ListBuffer(message))
    messages += item
  }
}
