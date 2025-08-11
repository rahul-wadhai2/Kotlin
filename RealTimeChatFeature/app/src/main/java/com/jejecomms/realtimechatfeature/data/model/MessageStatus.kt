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
     * Message has been delivered to the recipient.
     */
    DELIVERED,

    /**
     * Message has been read by the recipient.
     */
    READ,

    /**
     * Message sending failed.
     */
    FAILED,

    /**
     * Message has been pending for delivery.
     */
    PENDING,

    /**
     * Read receipt failed to send.
     */
    READ_FAILED
}