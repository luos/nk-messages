package nk.messages

import java.util.UUID

import nk.messages.priv.{GetConversationsForUser, MessageGroupPermissionsImplementation, PrivateMessageGroups, PrivateMessageStore, PrivateMessageUser, PrivateUserStorage, ReadConversation, SendPrivateMessage, UserBlockList}

class PrivateMessagesComponent(
                                privateMessageGroups: PrivateMessageGroups,
                                privateMessageStore: PrivateMessageStore,
                                userBlockList: UserBlockList,
                                privateUserStorage: PrivateUserStorage
                              ) {

  private val groupPermissions = new MessageGroupPermissionsImplementation(userBlockList)
  val send = new SendPrivateMessage(
    privateMessageGroups = privateMessageGroups,
    privateMessageStore = privateMessageStore,
    messageGroupPermissions = groupPermissions
  )


  object conversations {
    private val conversationLists = new GetConversationsForUser(privateMessageGroups, privateUserStorage)
    private val conversationReader = new ReadConversation(privateMessageGroups, privateMessageStore, groupPermissions)

    def list = conversationLists.list _

    def read = conversationReader.execute _

  }

}
