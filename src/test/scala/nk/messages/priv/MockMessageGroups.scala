package nk.messages.priv

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup

import scala.collection.mutable.{ArrayBuffer, ListBuffer}


class MockMessageGroups extends PrivateMessageGroups {

  val groups = ListBuffer[MessageGroup]()

  val findForUsersWasCalledWith = ArrayBuffer[(Seq[UUID])]()

  def withGroupForTesting(g: MessageGroup): MockMessageGroups = {
    groups += g
    this
  }

  override def find(messageGroupId: UUID): IO[Option[Groups.MessageGroup]] = {
    IO.pure(
      groups.find(_.id == messageGroupId)
    )
  }

  override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
    val g = MessageGroup(UUID.randomUUID(), users, None)
    groups += g
    IO.pure(g)
  }

  override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = {
    findForUsersWasCalledWith += users
    IO.pure(groups.find(_.users.sorted == users.sorted))
  }

  override def findAllGroupsOfUser(user: UUID): IO[Seq[MessageGroup]] = {
    IO.pure(groups.filter(_.users.contains(user)).toList)
  }

  override def setLastMessage(messageGroup: MessageGroup, time: Instant) = {
    groups.filterInPlace(g => g != messageGroup)
    val g = messageGroup.copy(
      lastMessage = Some(time)
    )
    groups += g
    IO.pure(g)
  }
}


