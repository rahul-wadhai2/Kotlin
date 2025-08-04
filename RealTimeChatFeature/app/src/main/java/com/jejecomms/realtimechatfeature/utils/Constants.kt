package com.jejecomms.realtimechatfeature.utils

/**
 *  Constant class containing various constants used throughout the application.
 */
object Constants {
    /**
     * Maximum character limit for a message
     */
    const val MESSAGE_CHAR_LIMIT = 300

    /**
     * Sender Name
     */
    const val SENDER_NAME = "Guest"

    /**
     * User joined the chat room message.
     */
    const val USER_JOINED_THE_CHAT_ROOM = "has joined the chat room"

    /**
     * You have joined the chat room message.
     */
    const val YOU_HAVE_JOINED_THE_CHAT_ROOM = "You have joined the chat room"

    /*************************SharedPreferences*********************************/
    /**
     * Sender Name store in SharedPreferences.
     */
    const val SENDER_NAME_PREF = "sender_name"

    /**
     * Sender ID store in SharedPreferences.
     */
    const val KEY_SENDER_ID = "current_sender_id"

    /*************************Table Name*********************************/
    const val CHAT_ROOM = "chat_room"
    const val CHAT_ROOM_MEMBERS = "chat_room_members"
    const val MESSAGES = "messages"

    /*************************Firebase Collection*********************************/
    const val CHAT_ROOMS = "chatrooms"
    const val IMAGES = "images"
    const val IMAGE_EXTENSION = ".jpg"
}