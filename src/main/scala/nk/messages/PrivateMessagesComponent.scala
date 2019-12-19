package nk.messages

import nk.messages.priv.{
  GetConversationsForUser,
  MessageGroupPermissionsImplementation,
  PrivateMessageGroups,
  PrivateMessageStore,
  PrivateUserStorage,
  ReadConversation,
  SendPrivateMessage,
  UserBlockList
}

class PrivateMessagesComponent(
                                privateMessageGroups: PrivateMessageGroups,
                                privateMessageStore: PrivateMessageStore,
                                userBlockList: UserBlockList,
                                privateUserStorage: PrivateUserStorage
                              ) {

  private val groupPermissions = new MessageGroupPermissionsImplementation(userBlockList)

  private val _send = new SendPrivateMessage(
    privateMessageGroups = privateMessageGroups,
    privateMessageStore = privateMessageStore,
    messageGroupPermissions = groupPermissions
  )

  object messages {
    def send = _send.execute _
  }


  object conversations {
    private val conversationLists = new GetConversationsForUser(privateMessageGroups, privateUserStorage)
    private val conversationReader = new ReadConversation(privateMessageGroups, privateMessageStore, groupPermissions)

    def list = conversationLists.list _

    def read = conversationReader.execute _

  }

}
