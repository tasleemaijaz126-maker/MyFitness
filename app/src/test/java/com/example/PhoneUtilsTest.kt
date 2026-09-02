package com.example

import com.example.util.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun normalizeToE164_standardIndian10Digits_returnsE164() {
        assertEquals("+919876543210", PhoneUtils.normalizeToE164("9876543210"))
        assertEquals("+918888888888", PhoneUtils.normalizeToE164("8888888888"))
        assertEquals("+917000000000", PhoneUtils.normalizeToE164("7000000000"))
        assertEquals("+916999999999", PhoneUtils.normalizeToE164("6999999999"))
    }

    @Test
    fun normalizeToE164_withLeadingZero_returnsE164() {
        assertEquals("+919876543210", PhoneUtils.normalizeToE164("09876543210"))
    }

    @Test
    fun normalizeToE164_withCountryCode91_returnsE164() {
        assertEquals("+919876543210", PhoneUtils.normalizeToE164("919876543210"))
    }

    @Test
    fun normalizeToE164_withPlus91AndFormatting_returnsE164() {
        assertEquals("+919876543210", PhoneUtils.normalizeToE164("+91 98765 43210"))
        assertEquals("+919876543210", PhoneUtils.normalizeToE164("+91-9876543210"))
        assertEquals("+919876543210", PhoneUtils.normalizeToE164("+91 (98765) 43210"))
    }

    @Test
    fun normalizeToE164_internationalNumbers_returnsCleanE164() {
        assertEquals("+14155552671", PhoneUtils.normalizeToE164("+1 415 555 2671"))
        assertEquals("+447911123456", PhoneUtils.normalizeToE164("+44-7911-123456"))
    }

    @Test
    fun normalizeToE164_invalidInputs_returnsNull() {
        assertNull(PhoneUtils.normalizeToE164(null))
        assertNull(PhoneUtils.normalizeToE164(""))
        assertNull(PhoneUtils.normalizeToE164("   "))
        assertNull(PhoneUtils.normalizeToE164("12345"))
        assertNull(PhoneUtils.normalizeToE164("1876543210")) // Invalid Indian leading digit 1
        assertNull(PhoneUtils.normalizeToE164("5876543210")) // Invalid Indian leading digit 5
        assertNull(PhoneUtils.normalizeToE164("abcdefghij"))
    }

    @Test
    fun isValidPhoneNumber_validationCheck() {
        assertTrue(PhoneUtils.isValidPhoneNumber("9876543210"))
        assertTrue(PhoneUtils.isValidPhoneNumber("+919876543210"))
        assertFalse(PhoneUtils.isValidPhoneNumber("1234"))
        assertFalse(PhoneUtils.isValidPhoneNumber(null))
    }

    @Test
    fun formatDisplayNumber_formattingCheck() {
        assertEquals("+91 98765 43210", PhoneUtils.formatDisplayNumber("9876543210"))
        assertEquals("+91 98765 43210", PhoneUtils.formatDisplayNumber("+919876543210"))
    }
}
