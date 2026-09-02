package com.example.data.firebase

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DashboardSummary(
    val totalMembers: Int = 0,
    val activeMemberships: Int = 0,
    val expiredMemberships: Int = 0,
    val expiringSoon: Int = 0,
    val pendingPaymentsAmount: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    val todayCollections: Double = 0.0,
    val totalExpenses: Double = 0.0
)

/**
 * Service calculating aggregate metrics and statistics from Firestore collections.
 */
class FirebaseDashboardService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getDashboardSummary(gymId: String): Result<DashboardSummary> = withContext(Dispatchers.IO) {
        try {
            // 1. Customers
            val custSnap = firestore.collection("customers")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()
            val totalMembers = custSnap.size()

            // 2. Memberships
            val memSnap = firestore.collection("memberships")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()

            val now = System.currentTimeMillis()
            val sevenDaysFromNow = now + (7L * 24 * 60 * 60 * 1000)

            var activeCount = 0
            var expiredCount = 0
            var expiringSoonCount = 0
            var pendingAmountSum = 0.0

            for (doc in memSnap.documents) {
                val endMillis = FirebaseDateUtils.fromIso(doc.getString("endDate"))
                val pending = doc.getDouble("pendingAmount") ?: 0.0
                pendingAmountSum += pending

                val status = doc.getString("membershipStatus")?.lowercase() ?: "active"
                if (status == "cancelled" || status == "expired" || endMillis < now) {
                    expiredCount++
                } else if (endMillis in now..sevenDaysFromNow) {
                    expiringSoonCount++
                    activeCount++
                } else {
                    activeCount++
                }
            }

            // 3. Payments
            val paySnap = firestore.collection("payments")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthStart = cal.timeInMillis

            var todayCollections = 0.0
            var monthlyRevenue = 0.0

            for (doc in paySnap.documents) {
                val payDateMillis = FirebaseDateUtils.fromIso(doc.getString("paymentDate"))
                val amt = doc.getDouble("amount") ?: 0.0

                if (payDateMillis >= todayStart) {
                    todayCollections += amt
                }
                if (payDateMillis >= monthStart) {
                    monthlyRevenue += amt
                }
            }

            // 4. Expenses
            val expSnap = firestore.collection("expenses")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()

            var totalExpenses = 0.0
            for (doc in expSnap.documents) {
                totalExpenses += (doc.getDouble("amount") ?: 0.0)
            }

            val summary = DashboardSummary(
                totalMembers = totalMembers,
                activeMemberships = activeCount,
                expiredMemberships = expiredCount,
                expiringSoon = expiringSoonCount,
                pendingPaymentsAmount = pendingAmountSum,
                monthlyRevenue = monthlyRevenue,
                todayCollections = todayCollections,
                totalExpenses = totalExpenses
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
