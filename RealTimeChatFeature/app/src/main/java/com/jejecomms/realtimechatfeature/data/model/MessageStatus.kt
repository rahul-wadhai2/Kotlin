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
    FAILED,

    /**
     * Message has been delivered to the recipient.
     */
    DELIVERED,

    /**
     * Message has been pending for delivery.
     */
    PENDING,

    /**
     * Message has been read by the recipient.
     */
    READ,

    /**
     * Read receipt failed to send.
     */
    READ_FAILED
}