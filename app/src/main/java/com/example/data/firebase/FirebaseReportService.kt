package com.example.data.firebase

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FinancialReport(
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val paymentCount: Int = 0,
    val expenseCount: Int = 0,
    val payments: List<FirebasePayment> = emptyList(),
    val expenses: List<FirebaseExpense> = emptyList()
)

/**
 * Service querying and compiling financial & operational reports from Firestore.
 */
class FirebaseReportService(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy { FirebaseConfig.getFirestore(context) }

    suspend fun getFinancialReport(
        gymId: String,
        startDateMillis: Long,
        endDateMillis: Long
    ): Result<FinancialReport> = withContext(Dispatchers.IO) {
        try {
            val paySnap = firestore.collection("payments")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()

            val payments = paySnap.documents.mapNotNull { doc ->
                doc.toObject(FirebasePayment::class.java)?.apply { id = doc.id }
            }.filter { p ->
                val pDate = FirebaseDateUtils.fromIso(p.paymentDate)
                pDate in startDateMillis..endDateMillis
            }

            val expSnap = firestore.collection("expenses")
                .whereEqualTo("gymId", gymId)
                .get()
                .await()

            val expenses = expSnap.documents.mapNotNull { doc ->
                doc.toObject(FirebaseExpense::class.java)?.apply { id = doc.id }
            }.filter { e ->
                val eDate = FirebaseDateUtils.fromIso(e.expenseDate)
                eDate in startDateMillis..endDateMillis
            }

            val totalRev = payments.sumOf { it.amount }
            val totalExp = expenses.sumOf { it.amount }

            val report = FinancialReport(
                totalRevenue = totalRev,
                totalExpenses = totalExp,
                netProfit = totalRev - totalExp,
                paymentCount = payments.size,
                expenseCount = expenses.size,
                payments = payments,
                expenses = expenses
            )
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
