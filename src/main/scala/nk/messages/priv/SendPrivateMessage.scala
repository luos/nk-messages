package nk.messages.priv

import java.util.UUID

import cats.implicits._
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
                         privateMessageStore: PrivateMessageStore,
                         messageGroupPermissions: MessageGroupPermissions) {

  def execute(sendingUser: SendingUser,
              messageTarget: Either[TargetMessageGroup, TargetUser],
              message: NewMessage): IO[Either[ErrorResult, SuccessResult]] = {
    val targetGroupIo: IO[Option[MessageGroup]] = messageTarget match {
      case Left(group) => {
        privateMessageGroups.find(group.id)
      }
      case Right(user) => {
        val users = Seq(user.userId, sendingUser.userId)
        privateMessageGroups.findForUsers(users).flatMap({
          case Some(existingGroup) => IO.pure(Some(existingGroup))
          case None => {
            privateMessageGroups.create(users).map(g => g.some)
          }
        })
      }
    }
    targetGroupIo.flatMap({
      case None =>
        IO.pure(Left(ErrorResult(
          messages = Seq(
            "Private message conversation does not exist."
          )
        )))
      case Some(g) => {
        messageGroupPermissions.canPostTo(sendingUser, g).map(
          {
            case MessageGroupPermissions.Allowed() => {
              val messageContent = Escaper.removeHtml(message.message)
              privateMessageStore.insert(g, NewStoredMessage(sendingUser.userId, messageContent.text))
              Right(SuccessResult(
                success = true,
                messageGroup = g
              ))
            }
            case _ => {
              Left(ErrorResult(
                messages = Seq(
                  "You have no access to post to this group."
                )
              ))
            }
          }
        )

      }
    })

  }

}
