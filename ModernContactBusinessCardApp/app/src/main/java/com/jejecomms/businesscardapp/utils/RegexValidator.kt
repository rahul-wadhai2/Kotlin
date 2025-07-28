package com.jejecomms.businesscardapp.utils

/**
 * Regex validator class.
 */
object RegexValidator {
    val EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    val PHONE_NUMBER_PATTERN = "^\\d{10}$".toRegex() // Simple 10-digit phone number

    fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matches(email)
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        return PHONE_NUMBER_PATTERN.matches(phone)
    }
}