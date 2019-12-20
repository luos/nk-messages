package nk.messages.priv.permissions

import cats.effect.IO
import nk.messages.CurrentUserId
import nk.messages.priv.Groups
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.permissions.MessageGroupPermissions.{Allowed, Blocked, Permission}


class SiteSpecificPermissions(checkers: Seq[MessageGroupPermissions]) extends MessageGroupPermissions {
  private def process(currentUserId: CurrentUserId,
                      messageGroup: MessageGroup,
                      fn: (MessageGroupPermissions) => IO[Permission]
                     ) = {
    val checks: Seq[IO[Permission]] = checkers.map(fn)
    checks.fold(IO.pure(Allowed()))({
      case (elem: IO[Permission], acc: IO[Permission]) => {
        acc.flatMap(
          {
            case Allowed() => {
              elem
            }
            case _ =>
              acc
          }
        )
      }
    })
  }

  override def canPostTo(sendingUser: CurrentUserId, messageGroup: Groups.MessageGroup): IO[MessageGroupPermissions.Permission] = {
    process(sendingUser, messageGroup, (c) => c.canPostTo(sendingUser, messageGroup))
  }

  override def canRead(sendingUser: CurrentUserId, messageGroup: Groups.MessageGroup): IO[MessageGroupPermissions.Permission] = {
    process(sendingUser, messageGroup, (c) => c.canRead(sendingUser, messageGroup))
  }
}
