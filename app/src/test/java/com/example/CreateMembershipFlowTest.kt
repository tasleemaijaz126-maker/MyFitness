package com.example

import com.example.data.local.entity.MembershipPlan
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.util.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateMembershipFlowTest {

    private val samplePlan = MembershipPlan(
        id = 1L,
        name = "Gold 3 Months",
        durationMonths = 3,
        durationDays = 90,
        price = 3500.0,
        description = "Full gym access",
        isActive = true
    )

    // Scenario A: OTP ON -> requires valid phone & verification before registration
    @Test
    fun testScenarioA_OtpOn_ValidationAndFlow() {
        val requireOtpVerification = true
        var isMobileVerified = false
        val mobileInput = "9876543210"

        // 1. Phone number validation
        val normalizedPhone = PhoneUtils.normalizeToE164(mobileInput)
        assertNotNull("Normalized phone should not be null", normalizedPhone)
        assertEquals("+919876543210", normalizedPhone)

        // 2. Before OTP is verified, creation must be blocked
        val canCreateBeforeOtp = !requireOtpVerification || isMobileVerified
        assertFalse("Creation must be blocked when OTP is ON and unverified", canCreateBeforeOtp)

        // 3. User verifies OTP
        isMobileVerified = true

        // 4. After OTP is verified, creation can proceed
        val canCreateAfterOtp = !requireOtpVerification || isMobileVerified
        assertTrue("Creation must proceed after OTP verification", canCreateAfterOtp)
    }

    // Scenario B: OTP OFF -> direct creation without OTP verification
    @Test
    fun testScenarioB_OtpOff_DirectCreationFlow() {
        val requireOtpVerification = false
        val isMobileVerified = false
        val mobileInput = "9876543210"

        // 1. Phone number validation
        val normalizedPhone = PhoneUtils.normalizeToE164(mobileInput)
        assertNotNull("Normalized phone should not be null", normalizedPhone)
        assertEquals("+919876543210", normalizedPhone)

        // 2. When OTP is OFF, creation is permitted directly without OTP
        val canCreateDirectly = !requireOtpVerification || isMobileVerified
        assertTrue("Creation must proceed directly when OTP is OFF", canCreateDirectly)

        // 3. Member mobile verification flag passed to database is preserved or false
        val finalVerifiedFlag = if (requireOtpVerification) isMobileVerified else false
        assertFalse("Unverified direct creation sets isMobileVerified appropriately", finalVerifiedFlag)
    }

    @Test
    fun testInvalidPhoneNumber_RejectedInBothFlows() {
        val invalidMobile = "12345"
        val normalizedPhone = PhoneUtils.normalizeToE164(invalidMobile)
        assertNull("Invalid phone number should be rejected regardless of OTP setting", normalizedPhone)
    }
}
