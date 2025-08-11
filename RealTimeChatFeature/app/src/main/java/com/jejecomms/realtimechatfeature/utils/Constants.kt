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

    /**
     * The key used to pass the chat room ID as an extra in an Intent.
     * This is primarily used for deep linking from a notification to the correct chat room.
     */
    const val EXTRA_ROOM_ID = "extra_room_id"

    /**
     * Package name for the application.
     */
    const val PACKAGE = "package"

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
    const val DOCUMENTS = "documents"
    const val IMAGES = "images"
    const val IMAGE_EXTENSION = ".jpg"
    const val DOCUMENT_EXTENSION = ".pdf"
    const val AUDIO_EXTENSION = ".mp4"
    /*************************Cache Folder Name*********************************/
    const val CACHE_FOLDER_MAIN = "RealChatData"
    const val CACHE_FOLDER_IMAGES = "Images"
    const val CACHE_FOLDER_DOCUMENTS = "Documents"
    const val CACHE_FOLDER_AUDIO = "Audio"
    const val CACHE_FOLDER_EXPORT_CHAT = "ExportChat"
}