package nk.messages.priv

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import nk.messages.Escaper
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.PrivateMessageStore.NewStoredMessage
import nk.messages.priv.SendPrivateMessage._
import nk.messages.CurrentUserId

object SendPrivateMessage {

  case class TargetMessageGroup(id: UUID)

  case class TargetUserId(userId: UUID)

  case class NewMessage(message: String)


  case class SuccessResult(success: Boolean, messageGroup: MessageGroup)

  case class ErrorResult(success: Boolean = false, messages: Seq[String])


}

class SendPrivateMessage(privateMessageGroups: PrivateMessageGroups,
                         privateMessageStore: PrivateMessageStore,
                         messageGroupPermissions: MessageGroupPermissions) {

  def execute(sendingUser: CurrentUserId,
              messageTarget: Either[TargetMessageGroup, TargetUserId],
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
        messageGroupPermissions.canPostTo(sendingUser, g).flatMap(
          {
            case MessageGroupPermissions.Allowed() => {
              val createdAt = Instant.now()
              val messageContent = Escaper.removeHtml(message.message)
              privateMessageStore.insert(g, NewStoredMessage(sendingUser.userId, messageContent.text, createdAt))
              for {
                g2 <- privateMessageGroups.setLastMessage(g, createdAt)
              } yield Right(SuccessResult(
                success = true,
                messageGroup = g2
              ))
            }
            case _ => {
              IO.pure(Left(ErrorResult(
                messages = Seq(
                  "You have no access to post to this group."
                )
              )))
            }
          }
        )

      }
    })

  }

}
