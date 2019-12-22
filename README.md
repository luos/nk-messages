[![Build Status](https://travis-ci.org/luos/nk-messages.svg?branch=master)](https://travis-ci.org/luos/nk-messages)

# nk-messages

Messaging system for websites. Currently supports sending private messages.

## Private Messages

The system is built for a classified ads website. It is built around  `MessageGroups`, if later it's needed to introduce multi user groups. Message can be sent to a message group or a user, if the recipient is a user then a message group is looked up for it, if it does not exist it is created.

### Implementation

The entry point for implementors is the `PrivateMessageComponent` class, it takes a few objects in its constructor which need to be implemented by the user of this library.

#### License

MIT