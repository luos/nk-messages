package nk.messages.priv

import java.util.UUID

trait PrivateMessagesTestHelpers {
  def createUser(): PrivateMessageUser = {
    val uuid = UUID.randomUUID()
    createUser(uuid)
  }

  def createUser(uuid: UUID): PrivateMessageUser = {
    PrivateMessageUser(uuid, s"User $uuid")
  }
}
