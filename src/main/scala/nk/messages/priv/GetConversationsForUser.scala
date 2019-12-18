package nk.messages.priv

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import nk.messages.{CurrentUserId, priv}

object ConversationType extends Enumeration {
  type ConversationType = String
  val Private: priv.ConversationType.Value = Value("private")
}

case class Conversation(
                         messageGroupId: UUID,
                         users: Seq[PrivateMessageUser],
                         lastMessage: Option[Instant],
                         lastRead: Option[LocalDateTime],
                         conversationType: ConversationType.Value
                       )

class GetConversationsForUser(privateMessageGroups: PrivateMessageGroups, privateUserStorage: PrivateUserStorage) {
  def list(currentUser: CurrentUserId): IO[Seq[Conversation]] = {
    val gsIo = privateMessageGroups.findAllGroupsOfUser(currentUser.userId)
    gsIo.map(
      gs => {
        gs.map(g => {
          val users = privateUserStorage.findUsers(g.users)
          Conversation(
            g.id,
            users,
            g.lastMessage,
            None,
            ConversationType.Private
          )
        })
      }
    )
  }
}
