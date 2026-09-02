package com.example.data.firebase

import android.content.Context

/**
 * Unified Firebase Gateway Manager for MyFitness Gym ERP.
 * Coordinates all remote services, Firestore multi-tenant isolation, Firebase Authentication, and Cloud Storage.
 */
class FirebaseGymManager(private val context: Context) {

    val auth: FirebaseAuthService = FirebaseAuthService(context)
    val customers: FirebaseCustomerService = FirebaseCustomerService(context)
    val plans: FirebaseMembershipPlanService = FirebaseMembershipPlanService(context)
    val memberships: FirebaseMembershipService = FirebaseMembershipService(context)
    val payments: FirebasePaymentService = FirebasePaymentService(context)
    val invoices: FirebaseInvoiceService = FirebaseInvoiceService(context)
    val expenses: FirebaseExpenseService = FirebaseExpenseService(context)
    val attendance: FirebaseAttendanceService = FirebaseAttendanceService(context)
    val notifications: FirebaseNotificationService = FirebaseNotificationService(context)
    val profile: FirebaseGymProfileService = FirebaseGymProfileService(context)
    val settings: FirebaseSettingsService = FirebaseSettingsService(context)
    val dashboard: FirebaseDashboardService = FirebaseDashboardService(context)
    val reports: FirebaseReportService = FirebaseReportService(context)
    val storage: FirebaseStorageService = FirebaseStorageService(context)
    val users: FirebaseUserService = FirebaseUserService(context)

    fun getCurrentGymId(): String {
        return FirebaseConfig.getGymId(context) ?: auth.getCurrentUser()?.gymId ?: "default_gym"
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseGymManager? = null

        fun getInstance(context: Context): FirebaseGymManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseGymManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
