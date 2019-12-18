package nk.messages.priv

import java.util.UUID

import cats.effect.IO
import nk.messages.CurrentUserId

object UserBlockList {

}

trait UserBlockList {
  def isBlocked(user: CurrentUserId, byUser: UUID): IO[Boolean]
}
