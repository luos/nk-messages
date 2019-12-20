package nk.messages

import nk.messages.priv.permissions.MessageGroupPermissionsImplementation
import nk.messages.priv.{GetConversationsForUser, PrivateMessageGroups, PrivateMessageStore, PrivateUserStorage, ReadConversation, SendPrivateMessage, UserBlockList}

class PrivateMessagesComponent(
                                privateMessageGroups: PrivateMessageGroups,
                                privateMessageStore: PrivateMessageStore,
                                userBlockList: UserBlockList,
                                privateUserStorage: PrivateUserStorage
                              ) {

  private val groupPermissions = new MessageGroupPermissionsImplementation(userBlockList)

  val privateMessageSender = new SendPrivateMessage(
    privateMessageGroups = privateMessageGroups,
    privateMessageStore = privateMessageStore,
    messageGroupPermissions = groupPermissions,
    privateUserStorage = privateUserStorage
  )

  object messages {
    def send = privateMessageSender.execute _
  }


  object conversations {
    private val conversationLists = new GetConversationsForUser(privateMessageGroups, privateUserStorage)
    private val conversationReader = new ReadConversation(privateMessageGroups, privateMessageStore, groupPermissions)

    def list = conversationLists.list _

    def read = conversationReader.execute _

  }

}
