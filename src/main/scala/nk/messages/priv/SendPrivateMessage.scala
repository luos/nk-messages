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
import nk.messages.priv.permissions.MessageGroupPermissions
import nk.messages.priv.permissions.MessageGroupPermissions.Permission

object SendPrivateMessage {

  case class TargetMessageGroup(id: UUID)

  case class TargetUserId(userId: UUID)

  case class NewMessage(message: String)


  case class SuccessResult(success: Boolean, messageGroup: MessageGroup)

  case class ErrorResult(success: Boolean = false, messages: Seq[String])


}

class SendPrivateMessage(privateMessageGroups: PrivateMessageGroups,
                         privateMessageStore: PrivateMessageStore,
                         messageGroupPermissions: MessageGroupPermissions,
                         privateUserStorage: PrivateUserStorage
                        ) {

  def execute(sendingUser: CurrentUserId,
              messageTarget: Either[TargetMessageGroup, TargetUserId],
              message: NewMessage): IO[Either[ErrorResult, SuccessResult]] = {
    val targetGroupIo = getOrCreateTargetMessageGroup(sendingUser, messageTarget)
    targetGroupIo.flatMap(_.fold(
      error => IO.pure(Left(error)),
      ({
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
                createMessage(sendingUser, g, message)
              }
              case perm : Permission => {
                IO.pure(Left(ErrorResult(
                  messages = Seq(
                    s"You have no access to post to this group. (${perm})"
                  )
                )))
              }
            }
          )

        }
      })
    ))


  }

  private
  def getOrCreateTargetMessageGroup(sendingUser: CurrentUserId,
                                    messageTarget: Either[TargetMessageGroup, TargetUserId]): IO[Either[ErrorResult, Option[MessageGroup]]] = {
    messageTarget match {
      case Left(group) => {
        privateMessageGroups.find(group.id).map(g => Right(g))
      }
      case Right(user) => {
        val userInfos: IO[(Option[PrivateMessageUser], Option[PrivateMessageUser])] = getUsers(sendingUser, user)

        userInfos.flatMap({
          case (_, None) => {
            IO.pure(Left(ErrorResult(messages = Seq("Recipient user does not exist."))))
          }
          case (None, _) => {
            IO.pure(Left(ErrorResult(messages = Seq("Sender user does not exist."))))
          }
          case (Some(targetUserInfo), Some(senderUserInfo)) => {
            val users = Seq(targetUserInfo.userId, senderUserInfo.userId)
            privateMessageGroups.findForUsers(users).flatMap({
              case Some(existingGroup) => IO.pure(Right(Some(existingGroup)))
              case None => {
                privateMessageGroups.create(users).map(g => g.some).map(Right(_))
              }
            })
          }
        })

      }
    }
  }

  private
  def getUsers(sendingUser: CurrentUserId, user: TargetUserId) = {
    val target = privateUserStorage.findUser(user.userId)
    val sender = privateUserStorage.findUser(sendingUser.userId)
    val userInfos = for {
      targetUserInfo <- target
      senderUserInfo <- sender
    } yield (senderUserInfo, targetUserInfo)
    userInfos
  }

  private
  def createMessage(sendingUser: CurrentUserId, group: MessageGroup, message: NewMessage) = {
    val createdAt = Instant.now()
    val messageContent = Escaper.removeHtml(message.message)
    privateMessageStore.insert(group, NewStoredMessage(sendingUser.userId, messageContent.text, createdAt))
    for {
      g2 <- privateMessageGroups.setLastMessage(group, createdAt)
    } yield Right(SuccessResult(
      success = true,
      messageGroup = g2
    ))
  }
}
