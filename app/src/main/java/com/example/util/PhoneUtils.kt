package com.example.util

/**
 * Standard Phone Number Utility for E.164 normalization, Indian mobile validation, and UI formatting.
 */
object PhoneUtils {

    /**
     * Normalizes a raw phone number input into strict E.164 format.
     *
     * Supports:
     * - Standard 10-digit Indian numbers starting with 6, 7, 8, 9 (e.g., "9876543210" -> "+919876543210")
     * - 11-digit numbers with leading 0 (e.g., "09876543210" -> "+919876543210")
     * - 12-digit numbers with country code 91 (e.g., "919876543210" -> "+919876543210")
     * - Numbers with existing "+91" prefix and spaces/dashes (e.g., "+91 98765 43210" -> "+919876543210")
     * - Standard international numbers in E.164 format (e.g., "+14155552671")
     *
     * Returns null if the phone number is invalid or cannot be normalized.
     */
    fun normalizeToE164(rawNumber: String?): String? {
        if (rawNumber.isNullOrBlank()) return null

        // Strip spaces, dashes, parentheses, dots, tabs
        val clean = rawNumber.trim().replace(" ", "").replace("-", "").replace("(", "")
            .replace(")", "").replace(".", "").replace("\t", "")

        return when {
            // Already standard +91 followed by 10 digits
            clean.startsWith("+91") && clean.length == 13 -> {
                val digits = clean.substring(3)
                if (digits.all { it.isDigit() } && digits.first() in '6'..'9') clean else null
            }
            // Starts with 91 followed by 10 digits (total length 12)
            clean.startsWith("91") && clean.length == 12 && clean.all { it.isDigit() } -> {
                val digits = clean.substring(2)
                if (digits.first() in '6'..'9') "+$clean" else null
            }
            // Starts with 0 followed by 10 digits (total length 11)
            clean.startsWith("0") && clean.length == 11 && clean.all { it.isDigit() } -> {
                val digits = clean.substring(1)
                if (digits.first() in '6'..'9') "+91$digits" else null
            }
            // Exactly 10 digits (standard Indian mobile)
            clean.length == 10 && clean.all { it.isDigit() } -> {
                if (clean.first() in '6'..'9') "+91$clean" else null
            }
            // Standard general international format E.164 (+ followed by 10 to 15 digits)
            clean.startsWith("+") && clean.length in 11..16 && clean.substring(1).all { it.isDigit() } -> {
                clean
            }
            else -> null
        }
    }

    /**
     * Checks if the phone number is a valid mobile number (Indian or E.164).
     */
    fun isValidPhoneNumber(rawNumber: String?): Boolean {
        return normalizeToE164(rawNumber) != null
    }

    /**
     * Formats phone number for clean UI presentation (e.g., "+91 98765 43210").
     */
    fun formatDisplayNumber(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val normalized = normalizeToE164(rawNumber) ?: rawNumber.trim()
        return if (normalized.startsWith("+91") && normalized.length == 13) {
            "+91 ${normalized.substring(3, 8)} ${normalized.substring(8)}"
        } else {
            normalized
        }
    }

    /**
     * Extracts only digits, stripping prefixes for local 10-digit display.
     */
    fun extract10Digits(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val normalized = normalizeToE164(rawNumber)
        return if (normalized != null && normalized.startsWith("+91") && normalized.length == 13) {
            normalized.substring(3)
        } else {
            rawNumber.filter { it.isDigit() }.takeLast(10)
        }
    }
}
