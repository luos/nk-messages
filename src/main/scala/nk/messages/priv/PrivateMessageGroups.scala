package nk.messages.priv

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup

object Groups {

  case class MessageGroup(id: UUID, users: Seq[UUID], lastMessage: Option[Instant])

}

trait PrivateMessageGroups {
  def find(messageGroupId: UUID): IO[Option[MessageGroup]]

  def create(users: Seq[UUID]): IO[MessageGroup]

  /**
   * Get all groups the user is in
   *
   * @param user
   * @return
   */
  def findAllGroupsOfUser(user: UUID): IO[Seq[MessageGroup]]

  /**
   * Find one group belonging to exactlz these users
   *
   * @param users
   * @return
   */
  def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]]

  def setLastMessage(messageGroup: MessageGroup, time: Instant): IO[MessageGroup]
}
