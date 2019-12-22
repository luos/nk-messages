package nk.messages

import java.util.UUID

import nk.messages.priv.messages.MetadataRenderer
import nk.messages.priv.permissions.InternalMessageGroupPermissions
import nk.messages.priv.{GetConversationsForUser, PrivateMessageGroups, PrivateMessageStore, PrivateUserStorage, ReadConversation, SendPrivateMessage, UserBlockList}

class PrivateMessagesComponent(
                                privateMessageGroups: PrivateMessageGroups,
                                privateMessageStore: PrivateMessageStore,
                                userBlockList: UserBlockList,
                                privateUserStorage: PrivateUserStorage,
                                metadataRenderer: MetadataRenderer
                              ) {

  private val groupPermissions = new InternalMessageGroupPermissions(userBlockList)

  val sendMessage = new SendPrivateMessage(privateMessageGroups = privateMessageGroups,
    privateMessageStore = privateMessageStore,
    messageGroupPermissions = groupPermissions,
    privateUserStorage = privateUserStorage,
    metadataRenderer = metadataRenderer
  )


  object conversations {
    private val conversationLists = new GetConversationsForUser(privateMessageGroups, privateUserStorage)
    private val conversationReader = new ReadConversation(privateMessageGroups, privateMessageStore, groupPermissions)

    def list(currentUserId: CurrentUserId) = conversationLists.list(currentUserId)

    def read(currentUserId: CurrentUserId, messageGroupId: UUID) = {
      conversationReader.execute(currentUserId, messageGroupId)
    }

  }

}
