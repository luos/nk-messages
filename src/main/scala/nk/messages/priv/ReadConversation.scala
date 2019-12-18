package nk.messages.priv

import java.util.UUID

import cats.effect.IO
import nk.messages.CurrentUserId
import nk.messages.priv.MessageGroupPermissions.{Allowed, Blocked}
import nk.messages.priv.PrivateMessageStore.Message
import nk.messages.priv.ReadConversation.{GroupNotFound, Messages, NoPermission, Request, Response}

object ReadConversation {

  case class Request(
                      currentUserId: CurrentUserId,
                      groupId: UUID,
                      page: Option[Int])

  class Response() {

  }

  case class GroupNotFound() extends Response

  case class NoPermission() extends Response

  case class Messages(
                       messages: Seq[Message],
                       currentPage: Int,
                       totalPages: Int
                     ) extends Response

  val PAGE_SIZE = 25

}

class ReadConversation(privateMessageGroups: PrivateMessageGroups,
                       privateMessageStore: PrivateMessageStore,
                       messageGroupPermissions: MessageGroupPermissions
                      ) {

  def execute(currentUserId: CurrentUserId, groupId: UUID): IO[Response] = {
    privateMessageGroups.find(groupId).flatMap({
      case Some(g) => {
        val permission = messageGroupPermissions.canRead(currentUserId, g)
        permission.flatMap({
          case Allowed() => {
            privateMessageStore.getMessagesForGroup(g.id, None, ReadConversation.PAGE_SIZE).map(
              ms => {
                val page = (ms.count.toFloat / ReadConversation.PAGE_SIZE.toFloat).ceil.toInt
                Messages(ms.messages, page, page)
              }
            )
          }
          case Blocked() =>
            IO.pure(NoPermission())
        })
      }
      case None => {
        IO.pure(GroupNotFound())
      }
    })
  }
}
