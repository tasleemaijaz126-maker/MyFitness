package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.GymRepository
import com.example.data.firebase.FirebaseGymManager
import com.example.data.local.entity.Customer
import com.example.data.local.entity.MembershipPlan
import com.example.data.model.InvoiceTemplates
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My Fitness", appName)
  }

  @Test
  fun `verify database operations and invoice templates`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)
    val firebaseManager = FirebaseGymManager.getInstance(context)
    val repo = GymRepository(db, firebaseManager)

    val plan = MembershipPlan(
        name = "Gold Monthly",
        durationMonths = 1,
        durationDays = 30,
        price = 1500.0,
        description = "Full gym and cardio access",
        isActive = true
    )
    repo.insertPlan(plan, "gym_test_123")

    val plans = repo.allPlans.first()
    assertTrue(plans.isNotEmpty())
    assertEquals("Gold Monthly", plans.first().name)

    val templates = InvoiceTemplates.availableTemplates
    assertEquals(4, templates.size)
  }

  @Test
  fun `verify biometric status check and security settings persistence`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val status = com.example.util.BiometricAuthManager.checkBiometricStatus(context)
    assertNotNull(status)

    val db = AppDatabase.getInstance(context)
    val firebaseManager = FirebaseGymManager.getInstance(context)
    val repo = GymRepository(db, firebaseManager)

    val currentSettings = repo.settings.first() ?: com.example.data.local.entity.GymSetting()
    val updatedSettings = currentSettings.copy(
        isBiometricEnabled = true,
        biometricAutoLockMinutes = 5,
        securityPin = "9876",
        ownerSignatureName = "Sarhan Bayousuf",
        ownerSignatureStyleId = "signature_15"
    )
    repo.updateSettings(updatedSettings, "test_owner_uid")

    val retrieved = repo.settings.first()
    assertNotNull(retrieved)
    assertTrue(retrieved!!.isBiometricEnabled)
    assertEquals(5, retrieved.biometricAutoLockMinutes)
    assertEquals("9876", retrieved.securityPin)
    assertEquals("Sarhan Bayousuf", retrieved.ownerSignatureName)
    assertEquals("signature_15", retrieved.ownerSignatureStyleId)
  }

  @Test
  fun `verify signature helper uses local asset fonts`() {
    val styles = com.example.util.SignatureHelper.styles
    assertTrue("Signature styles count should be at least 27 from local font assets", styles.size >= 27)

    // Verify all styles have valid local font file names and asset paths
    styles.forEach { style ->
      assertTrue("Font file name should end with ttf or otf: ${style.fontFileName}",
        style.fontFileName.endsWith(".ttf", ignoreCase = true) || style.fontFileName.endsWith(".otf", ignoreCase = true))
      assertTrue("Asset path should contain font file name", style.assetPath.contains(style.fontFileName))
      assertTrue("Display name should be non-blank", style.name.isNotBlank())
      assertTrue("Category should be non-blank", style.category.isNotBlank())
    }

    val categories = com.example.util.SignatureHelper.categories
    assertTrue("Should include All category", categories.contains("All"))
    assertTrue("Should include multiple font categories", categories.size >= 5)

    // Verify resolving by style id works smoothly
    val style15 = com.example.util.SignatureHelper.getStyleById("signature_15")
    assertNotNull(style15)

    // Verify display name formatting
    val formatted = com.example.util.SignatureHelper.formatDisplayNameFromFileName("TheCheckmate-Regular.ttf")
    assertEquals("The Checkmate", formatted)
  }

  @Test
  fun `verify app theme modes exist with distinct visual configurations`() {
    val modes = com.example.data.model.AppThemeMode.entries
    assertEquals(4, modes.size)
    val names = modes.map { it.name }
    assertTrue(names.contains("CLASSIC"))
    assertTrue(names.contains("MODERN"))
    assertTrue(names.contains("PREMIUM"))
    assertTrue(names.contains("MINIMAL"))
  }
}
