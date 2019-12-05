package nk.messages.priv

import java.util.UUID

import cats.effect.IO
import nk.messages.Escaper
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore.NewStoredMessage
import nk.messages.priv.SendPrivateMessage.{ErrorResult, NewMessage, SendingUser, SuccessResult, TargetMessageGroup, TargetUser}

object SendPrivateMessage {

  case class TargetMessageGroup(id: UUID)

  case class TargetUser(userId: UUID)

  case class NewMessage(message: String)

  case class SendingUser(userId: UUID)

  case class SuccessResult(success: Boolean, messageGroup: MessageGroup)

  case class ErrorResult(success: Boolean = false, messages: Seq[String])

}

class SendPrivateMessage(privateMessageGroups: PrivateMessageGroups,
                         privateMessageStore: PrivateMessageStore) {

  def execute(sendingUser: SendingUser,
              messageTarget: Either[TargetMessageGroup, TargetUser],
              message: NewMessage): IO[Either[ErrorResult, SuccessResult]] = {
    val targetGroupIo = messageTarget match {
      case Left(group) => {
        privateMessageGroups.find(group.id)
      }
      case Right(user) => {
        privateMessageGroups.create(Seq(user.userId, sendingUser.userId)).map(g => Some(g))
      }
    }
    targetGroupIo.map({
      case None =>
        Left(ErrorResult(
          messages = Seq(
            "Private message conversation does not exist."
          )
        ))
      case Some(g) => {
        val messageContent = Escaper.removeHtml(message.message)
        privateMessageStore.insert(g, NewStoredMessage(sendingUser.userId, messageContent.text))
        Right(SuccessResult(
          success = true,
          messageGroup = g
        ))
      }
    })

  }

}
