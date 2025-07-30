package com.jejecomms.realtimechatfeature.utils

import java.util.UUID

/**
 * Utility object for generating universally unique identifiers (UUIDs).
 * This centralizes the UUID generation logic for common use throughout the application.
 */
object UuidGenerator {

    /**
     * Generates a new random UUID and returns its string representation.
     *
     * @return A unique string identifier.
     */
    fun generateUniqueId(): String {
        return UUID.randomUUID().toString()
    }
}