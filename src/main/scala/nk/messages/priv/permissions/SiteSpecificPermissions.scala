package nk.messages.priv.permissions

import cats.effect.IO
import nk.messages.CurrentUserId
import nk.messages.priv.Groups
import nk.messages.priv.Groups.MessageGroup
import nk.messages.priv.permissions.MessageGroupPermissions.{Allowed, Blocked, Permission}

/**
 *
 * @doc A way to implement site specific permission checks, for example rate limiting,
 *      fully banned users or any other logic to allow / disallow sending messages
 * @param checkers A list of MessageGroupPermissions 's to check, all of them will
 *                 be checked in order until a blocked one is found.
 *                 If any of them return not Allowed() the message will be blocked
 */
class SiteSpecificPermissions(checkers: Seq[MessageGroupPermissions]) extends MessageGroupPermissions {
  private def process(currentUserId: CurrentUserId,
                      messageGroup: MessageGroup,
                      fn: (MessageGroupPermissions) => IO[Permission]
                     ) = {
    val checks: Seq[IO[Permission]] = checkers.map(fn)
    checks.fold(IO.pure(Allowed()))({
      case (checkResult: IO[Permission], finalResult: IO[Permission]) => {
        finalResult.flatMap(
          {
            case Allowed() => {
              checkResult
            }
            case _ =>
              finalResult
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
