package nk.messages.priv

import java.util.UUID

import cats.effect.IO
import nk.messages.priv.Groups.MessageGroup

import scala.collection.mutable.ListBuffer


class MockMessageGroups extends PrivateMessageGroups {

  val groups = ListBuffer[MessageGroup]()

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
    val g = MessageGroup(UUID.randomUUID(), users)
    groups += g
    IO.pure(g)
  }

  override def findForUsers(users: Seq[UUID]): IO[Option[MessageGroup]] = {
    IO.pure(groups.find(_.users.sorted == users.sorted))
  }
}


