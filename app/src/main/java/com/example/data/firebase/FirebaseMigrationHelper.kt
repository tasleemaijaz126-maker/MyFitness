package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MigrationResult(
    val totalRecords: Int = 0,
    val migratedRecords: Int = 0,
    val skippedRecords: Int = 0,
    val failedRecords: Int = 0,
    val details: String = ""
)

/**
 * Migration helper to safely migrate all local database and existing entities into Firestore collections.
 * Idempotent: Can safely be rerun without duplicating records.
 */
class FirebaseMigrationHelper(
    private val context: Context,
    private val database: AppDatabase,
    private val firebaseManager: FirebaseGymManager
) {
    private val tag = "FirebaseMigration"

    suspend fun runMigration(gymId: String): MigrationResult = withContext(Dispatchers.IO) {
        var total = 0
        var migrated = 0
        var skipped = 0
        var failed = 0
        val logBuilder = StringBuilder()

        try {
            // 1. Migrate Gym Profile & Settings
            val settings = database.gymSettingDao().getSettingsSnapshot()
            if (settings != null) {
                total++
                try {
                    val gymDto = settings.toFirebaseGym()
                    firebaseManager.profile.saveGymProfile(gymId, gymDto)

                    val appSettingsDto = settings.toFirebaseAppSettings(gymId)
                    firebaseManager.settings.saveSettings(gymId, appSettingsDto)
                    migrated++
                    logBuilder.appendLine("Gym settings & profile migrated.")
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate gym settings: ${e.message}")
                }
            }

            // 2. Migrate Membership Plans
            val plans = database.membershipPlanDao().getAllPlansSnapshot()
            total += plans.size
            for (plan in plans) {
                try {
                    val dto = plan.toFirebaseDto(gymId)
                    val saved = firebaseManager.plans.savePlan(gymId, dto)
                    if (saved.isSuccess) {
                        val savedId = saved.getOrNull()?.id
                        if (!savedId.isNullOrBlank() && plan.firestoreId != savedId) {
                            database.membershipPlanDao().update(plan.copy(firestoreId = savedId))
                        }
                        migrated++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate plan ${plan.name}: ${e.message}")
                }
            }

            // 3. Migrate Customers
            val customers = database.customerDao().getAllCustomersSnapshot()
            total += customers.size
            for (cust in customers) {
                try {
                    val dto = cust.toFirebaseDto(gymId)
                    val saved = firebaseManager.customers.saveCustomer(gymId, dto)
                    if (saved.isSuccess) {
                        val savedId = saved.getOrNull()?.id
                        if (!savedId.isNullOrBlank() && cust.firestoreId != savedId) {
                            database.customerDao().update(cust.copy(firestoreId = savedId))
                        }
                        migrated++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate customer ${cust.name}: ${e.message}")
                }
            }

            // 4. Migrate Memberships
            val memberships = database.membershipDao().getAllMembershipsSnapshot()
            total += memberships.size
            for (mem in memberships) {
                try {
                    val cust = database.customerDao().getCustomerById(mem.customerId)
                    val plan = database.membershipPlanDao().getPlanById(mem.planId)
                    val custFirestoreId = cust?.firestoreId?.ifBlank { "cust_${mem.customerId}" } ?: "cust_${mem.customerId}"
                    val planFirestoreId = plan?.firestoreId?.ifBlank { "plan_${mem.planId}" }

                    val dto = mem.toFirebaseDto(gymId, custFirestoreId, planFirestoreId)
                    val saved = firebaseManager.memberships.saveMembership(gymId, dto)
                    if (saved.isSuccess) {
                        val savedId = saved.getOrNull()?.id
                        if (!savedId.isNullOrBlank() && mem.firestoreId != savedId) {
                            database.membershipDao().update(mem.copy(firestoreId = savedId))
                        }
                        migrated++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate membership ${mem.id}: ${e.message}")
                }
            }

            // 5. Migrate Payments
            val payments = database.paymentDao().getAllPaymentsSnapshot()
            total += payments.size
            for (pay in payments) {
                try {
                    val cust = database.customerDao().getCustomerById(pay.customerId)
                    val mem = database.membershipDao().getMembershipById(pay.membershipId)
                    val custFirestoreId = cust?.firestoreId?.ifBlank { "cust_${pay.customerId}" } ?: "cust_${pay.customerId}"
                    val memFirestoreId = mem?.firestoreId?.ifBlank { "mem_${pay.membershipId}" }

                    val dto = pay.toFirebaseDto(gymId, custFirestoreId, memFirestoreId)
                    val saved = firebaseManager.payments.savePayment(gymId, dto)
                    if (saved.isSuccess) {
                        val savedId = saved.getOrNull()?.id
                        if (!savedId.isNullOrBlank() && pay.firestoreId != savedId) {
                            database.paymentDao().update(pay.copy(firestoreId = savedId))
                        }
                        migrated++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate payment ${pay.id}: ${e.message}")
                }
            }

            // 6. Migrate Invoices
            val invoices = database.invoiceDao().getAllInvoicesSnapshot()
            total += invoices.size
            for (inv in invoices) {
                try {
                    val cust = database.customerDao().getCustomerById(inv.customerId)
                    val mem = database.membershipDao().getMembershipById(inv.membershipId)
                    val custFirestoreId = cust?.firestoreId?.ifBlank { "cust_${inv.customerId}" } ?: "cust_${inv.customerId}"
                    val memFirestoreId = mem?.firestoreId?.ifBlank { "mem_${inv.membershipId}" }

                    val dto = inv.toFirebaseDto(gymId, custFirestoreId, memFirestoreId)
                    val saved = firebaseManager.invoices.saveInvoice(gymId, dto)
                    if (saved.isSuccess) {
                        val savedId = saved.getOrNull()?.id
                        if (!savedId.isNullOrBlank() && inv.firestoreId != savedId) {
                            database.invoiceDao().update(inv.copy(firestoreId = savedId))
                        }
                        migrated++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate invoice ${inv.invoiceNumber}: ${e.message}")
                }
            }

            // 7. Migrate Expenses
            val expenses = database.expenseDao().getAllExpensesSnapshot()
            total += expenses.size
            for (exp in expenses) {
                try {
                    val dto = exp.toFirebaseDto(gymId)
                    val saved = firebaseManager.expenses.saveExpense(gymId, dto)
                    if (saved.isSuccess) {
                        val savedId = saved.getOrNull()?.id
                        if (!savedId.isNullOrBlank() && exp.firestoreId != savedId) {
                            database.expenseDao().update(exp.copy(firestoreId = savedId))
                        }
                        migrated++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate expense ${exp.title}: ${e.message}")
                }
            }

            // 8. Migrate Attendance
            val attendanceList = database.attendanceDao().getAllAttendanceSnapshot()
            total += attendanceList.size
            for (att in attendanceList) {
                try {
                    val cust = database.customerDao().getCustomerById(att.customerId)
                    val custFirestoreId = cust?.firestoreId?.ifBlank { "cust_${att.customerId}" } ?: "cust_${att.customerId}"

                    val dto = att.toFirebaseDto(gymId, custFirestoreId)
                    val saved = firebaseManager.attendance.saveAttendance(gymId, dto)
                    if (saved.isSuccess) {
                        val savedId = saved.getOrNull()?.id
                        if (!savedId.isNullOrBlank() && att.firestoreId != savedId) {
                            database.attendanceDao().update(att.copy(firestoreId = savedId))
                        }
                        migrated++
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                    Log.e(tag, "Failed to migrate attendance ${att.id}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Migration general error: ${e.message}")
        }

        MigrationResult(
            totalRecords = total,
            migratedRecords = migrated,
            skippedRecords = skipped,
            failedRecords = failed,
            details = "Total: $total | Migrated to Firestore: $migrated | Skipped: $skipped | Failed: $failed"
        )
    }
}
