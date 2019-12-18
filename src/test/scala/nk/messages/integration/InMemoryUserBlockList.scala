package nk.messages.integration

import java.util.UUID

import cats.effect.IO
import nk.messages.priv.{SendPrivateMessage, UserBlockList}
import nk.messages.CurrentUserId

class InMemoryUserBlockList extends UserBlockList {
  override def isBlocked(user: CurrentUserId, byUser: UUID): IO[Boolean] = {
    IO.pure(false)
  }
}