package nk.messages.integration

import java.time.Instant
import java.util.UUID

import cats.effect.IO
import nk.messages.priv.{Groups, PrivateMessageGroups}

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

class InMemoryPrivateMessageGroups extends PrivateMessageGroups {


  val groups = new ArrayBuffer[Groups.MessageGroup]()


  override def find(messageGroupId: UUID): IO[Option[Groups.MessageGroup]] = {
    IO.pure(groups.find(_.id == messageGroupId))
  }

  override def create(users: Seq[UUID]): IO[Groups.MessageGroup] = {
    val group = Groups.MessageGroup(UUID.randomUUID(), users.sorted, None)
    groups += group
    IO.pure(group)
  }


  override def findForUsers(users: Seq[UUID]): IO[Option[Groups.MessageGroup]] = {
    val us = users.sorted
    IO.pure(groups.find(p => p.users == us))
  }

  override def findAllGroupsOfUser(user: UUID): IO[Seq[Groups.MessageGroup]] = {
    IO.pure(
      groups.filter(_.users.contains(user)).toList
    )
  }


  import nk.messages.MutableListExtensions._

  override def setLastMessage(messageGroup: Groups.MessageGroup, time: Instant) = {
    groups.filterInPlace(g => g != messageGroup)
    val g = messageGroup.copy(
      lastMessage = Some(time)
    )
    groups += g
    IO.pure(g)
  }
}
