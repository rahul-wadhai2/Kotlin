package com.jejecomms.realtimechatfeature.data.model

/**
 * Enum to represent the status of a chat message.
 */
enum class MessageStatus {
    /**
     * Message is currently being sent.
     */
    SENDING,

    /**
     * Message has been successfully sent and persisted.
     */
    SENT,

    /**
     * Message sending failed.
     */
    FAILED
}