package com.example.data.local

import com.example.data.firebase.FirebaseGymManager
import com.example.data.firebase.toEntity
import com.example.data.firebase.toFirebaseAppSettings
import com.example.data.firebase.toFirebaseDto
import com.example.data.firebase.toFirebaseGym
import com.example.data.local.entity.AttendanceRecord
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Expense
import com.example.data.local.entity.GymSetting
import com.example.data.local.entity.Invoice
import com.example.data.local.entity.Membership
import com.example.data.local.entity.MembershipPlan
import com.example.data.local.entity.NotificationItem
import com.example.data.local.entity.Payment
import com.example.data.local.entity.SyncStatus
import com.example.data.model.MembershipStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Repository mediating between local Room Database cache and Firebase Cloud Firestore.
 * Implements an Offline-First, Multi-Tenant Synchronized architecture.
 */
class GymRepository(
    private val database: AppDatabase,
    val firebaseManager: FirebaseGymManager
) {

    // Local DAOs
    private val customerDao = database.customerDao()
    private val planDao = database.membershipPlanDao()
    private val membershipDao = database.membershipDao()
    private val paymentDao = database.paymentDao()
    private val invoiceDao = database.invoiceDao()
    private val expenseDao = database.expenseDao()
    private val settingsDao = database.gymSettingDao()
    private val notificationDao = database.notificationDao()
    private val attendanceDao = database.attendanceDao()

    private var syncJob: Job? = null
    private var currentListeningGymId: String? = null

    // Real-time Database Flows for UI
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allPlans: Flow<List<MembershipPlan>> = planDao.getAllPlans()
    val activePlans: Flow<List<MembershipPlan>> = planDao.getActivePlans()
    val allMemberships: Flow<List<Membership>> = membershipDao.getAllMemberships()
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val settings: Flow<GymSetting?> = settingsDao.getSettings()
    val notifications: Flow<List<NotificationItem>> = notificationDao.getAllNotifications()
    val allAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendance()

    // -------------------------------------------------------------
    // Real-Time / Periodic Sync Orchestration
    // -------------------------------------------------------------

    /**
     * Starts periodic syncing with Firebase Firestore for the given gym tenant.
     */
    fun startRealtimeSync(gymId: String) {
        if (gymId.isBlank() || currentListeningGymId == gymId) return
        stopRealtimeSync()
        currentListeningGymId = gymId

        val scope = CoroutineScope(Dispatchers.IO)
        syncJob = scope.launch {
            // Initial sync
            syncWithFirebase(gymId)

            // Periodic sync loop every 60 seconds
            while (isActive) {
                delay(60_000L)
                try {
                    syncWithFirebase(gymId)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Stops active sync loop.
     */
    fun stopRealtimeSync() {
        syncJob?.cancel()
        syncJob = null
        currentListeningGymId = null
    }

    // -------------------------------------------------------------
    // Full Bidirectional Cloud Synchronization
    // -------------------------------------------------------------

    /**
     * Pulls latest data from Firebase Firestore to Room and syncs any pending local writes to Firestore.
     */
    suspend fun syncWithFirebase(gymId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // First push pending local changes
            syncPendingChanges(gymId)

            // Then pull remote changes
            withTimeoutOrNull(10000L) {
                // 1. Settings & Profile
                try {
                    val settingsResult = withTimeoutOrNull(2000L) { firebaseManager.settings.getSettings(gymId) }
                    val gymProfileResult = withTimeoutOrNull(2000L) { firebaseManager.profile.getGymProfile(gymId) }

                    val remoteSettings = settingsResult?.getOrNull()
                    val remoteProfile = gymProfileResult?.getOrNull()

                    if (remoteSettings != null || remoteProfile != null) {
                        val local = settingsDao.getSettingsSnapshot() ?: GymSetting()
                        val updated = local.copy(
                            gymName = remoteProfile?.name ?: local.gymName,
                            gymTagline = remoteProfile?.tagline ?: local.gymTagline,
                            gymAddress = remoteProfile?.address ?: local.gymAddress,
                            gymCity = remoteProfile?.city ?: local.gymCity,
                            gymPhone = remoteProfile?.phone ?: local.gymPhone,
                            gymEmail = remoteProfile?.email ?: local.gymEmail,
                            gymGstin = remoteProfile?.gstin ?: local.gymGstin,
                            currencySymbol = remoteProfile?.currencySymbol ?: local.currencySymbol,
                            ownerSignatureName = remoteProfile?.ownerName ?: local.ownerSignatureName,
                            ownerSignatureStyleId = remoteProfile?.signatureStyleId ?: local.ownerSignatureStyleId,
                            gymLocationUrl = remoteProfile?.gymLocationUrl ?: local.gymLocationUrl,
                            appTheme = remoteSettings?.theme ?: local.appTheme,
                            appLanguage = remoteSettings?.language ?: local.appLanguage,
                            activeInvoiceTemplateId = remoteSettings?.selectedInvoiceTemplate ?: local.activeInvoiceTemplateId,
                            isBiometricEnabled = remoteSettings?.biometricEnabled ?: local.isBiometricEnabled,
                            biometricAutoLockMinutes = remoteSettings?.biometricTimeout ?: local.biometricAutoLockMinutes,
                            syncStatus = SyncStatus.SYNCED
                        )
                        settingsDao.insertOrUpdate(updated)
                    }
                } catch (_: Exception) {}

                // 2. Plans
                try {
                    val plansResult = withTimeoutOrNull(2000L) { firebaseManager.plans.getPlans(gymId) }
                    if (plansResult?.isSuccess == true && !plansResult.getOrNull().isNullOrEmpty()) {
                        plansResult.getOrNull()!!.forEach { planDto ->
                            val local = if (!planDto.id.isNullOrBlank()) planDao.getPlanByFirestoreId(planDto.id!!) else null
                            planDao.insertPlan(planDto.toEntity(existingId = local?.id ?: 0L))
                        }
                    }
                } catch (_: Exception) {}

                // 3. Customers
                try {
                    val custResult = withTimeoutOrNull(2000L) { firebaseManager.customers.getCustomers(gymId) }
                    if (custResult?.isSuccess == true && !custResult.getOrNull().isNullOrEmpty()) {
                        custResult.getOrNull()!!.forEach { custDto ->
                            val docId = custDto.id.orEmpty()
                            val local = if (docId.isNotBlank()) customerDao.getCustomerByFirestoreId(docId) else null
                            customerDao.insertCustomer(custDto.toEntity(existingId = local?.id ?: 0L))
                        }
                    }
                } catch (_: Exception) {}

                // 4. Memberships
                try {
                    val memResult = withTimeoutOrNull(2000L) { firebaseManager.memberships.getMemberships(gymId) }
                    if (memResult?.isSuccess == true && !memResult.getOrNull().isNullOrEmpty()) {
                        memResult.getOrNull()!!.forEach { memDto ->
                            val local = if (!memDto.id.isNullOrBlank()) membershipDao.getMembershipByFirestoreId(memDto.id!!) else null
                            val localCust = customerDao.getCustomerByFirestoreId(memDto.customerId)
                            val localPlan = if (!memDto.planId.isNullOrBlank()) planDao.getPlanByFirestoreId(memDto.planId!!) else null
                            membershipDao.insertMembership(
                                memDto.toEntity(
                                    existingId = local?.id ?: 0L,
                                    localCustomerId = localCust?.id ?: local?.customerId ?: 0L,
                                    localPlanId = localPlan?.id ?: local?.planId ?: 0L
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                // 5. Payments
                try {
                    val payResult = withTimeoutOrNull(2000L) { firebaseManager.payments.getPayments(gymId) }
                    if (payResult?.isSuccess == true && !payResult.getOrNull().isNullOrEmpty()) {
                        payResult.getOrNull()!!.forEach { payDto ->
                            val local = if (!payDto.id.isNullOrBlank()) paymentDao.getPaymentByFirestoreId(payDto.id!!) else null
                            val localCust = customerDao.getCustomerByFirestoreId(payDto.customerId)
                            val localMem = if (!payDto.membershipId.isNullOrBlank()) membershipDao.getMembershipByFirestoreId(payDto.membershipId!!) else null
                            paymentDao.insertPayment(
                                payDto.toEntity(
                                    existingId = local?.id ?: 0L,
                                    localCustomerId = localCust?.id ?: local?.customerId ?: 0L,
                                    localMembershipId = localMem?.id ?: local?.membershipId ?: 0L
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                // 6. Invoices
                try {
                    val invResult = withTimeoutOrNull(2000L) { firebaseManager.invoices.getInvoices(gymId) }
                    if (invResult?.isSuccess == true && !invResult.getOrNull().isNullOrEmpty()) {
                        invResult.getOrNull()!!.forEach { invDto ->
                            val local = if (!invDto.id.isNullOrBlank()) invoiceDao.getInvoiceByFirestoreId(invDto.id!!) else null
                            val localCust = customerDao.getCustomerByFirestoreId(invDto.customerId)
                            val localMem = if (!invDto.membershipId.isNullOrBlank()) membershipDao.getMembershipByFirestoreId(invDto.membershipId!!) else null
                            invoiceDao.insertInvoice(
                                invDto.toEntity(
                                    existingId = local?.id ?: 0L,
                                    localCustomerId = localCust?.id ?: local?.customerId ?: 0L,
                                    localMembershipId = localMem?.id ?: local?.membershipId ?: 0L
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                // 7. Expenses
                try {
                    val expResult = withTimeoutOrNull(2000L) { firebaseManager.expenses.getExpenses(gymId) }
                    if (expResult?.isSuccess == true && !expResult.getOrNull().isNullOrEmpty()) {
                        expResult.getOrNull()!!.forEach { expDto ->
                            val local = if (!expDto.id.isNullOrBlank()) expenseDao.getExpenseByFirestoreId(expDto.id!!) else null
                            expenseDao.insertExpense(expDto.toEntity(existingId = local?.id ?: 0L))
                        }
                    }
                } catch (_: Exception) {}

                // 8. Attendance
                try {
                    val attResult = withTimeoutOrNull(2000L) { firebaseManager.attendance.getAttendance(gymId) }
                    if (attResult?.isSuccess == true && !attResult.getOrNull().isNullOrEmpty()) {
                        attResult.getOrNull()!!.forEach { attDto ->
                            val local = if (!attDto.id.isNullOrBlank()) attendanceDao.getAttendanceByFirestoreId(attDto.id!!) else null
                            val localCust = customerDao.getCustomerByFirestoreId(attDto.customerId)
                            attendanceDao.insertAttendance(
                                attDto.toEntity(
                                    existingId = local?.id ?: 0L,
                                    localCustomerId = localCust?.id ?: local?.customerId ?: 0L
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                // 9. Notifications
                try {
                    val notifResult = withTimeoutOrNull(2000L) { firebaseManager.notifications.getNotifications(gymId) }
                    if (notifResult?.isSuccess == true && !notifResult.getOrNull().isNullOrEmpty()) {
                        notifResult.getOrNull()!!.forEach { notifDto ->
                            val local = if (!notifDto.id.isNullOrBlank()) notificationDao.getNotificationByFirestoreId(notifDto.id!!) else null
                            notificationDao.insertNotification(notifDto.toEntity(existingId = local?.id ?: 0L))
                        }
                    }
                } catch (_: Exception) {}
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads all pending local changes (inserts/updates) and completes pending deletions in Firestore.
     */
    suspend fun syncPendingChanges(gymId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // A. PENDING INSERTS & UPDATES
            // 1. Settings
            val pendingSettings = settingsDao.getPendingSyncSettings()
            if (pendingSettings != null) {
                try {
                    firebaseManager.settings.saveSettings(gymId, pendingSettings.toFirebaseAppSettings(gymId))
                    firebaseManager.profile.saveGymProfile(gymId, pendingSettings.toFirebaseGym())
                    settingsDao.updateSyncStatus(SyncStatus.SYNCED)
                } catch (e: Exception) {
                    settingsDao.updateSyncStatus(SyncStatus.FAILED, error = e.message)
                }
            }

            // 2. Plans
            val pendingPlans = planDao.getPendingSyncPlans()
            pendingPlans.forEach { plan ->
                try {
                    val fPlan = plan.toFirebaseDto(gymId)
                    val res = firebaseManager.plans.savePlan(gymId, fPlan)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (plan.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            planDao.updatePlan(plan.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            planDao.updateSyncStatus(plan.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    planDao.updateSyncStatus(plan.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // 3. Customers
            val pendingCustomers = customerDao.getPendingSyncCustomers()
            pendingCustomers.forEach { cust ->
                try {
                    val fCust = cust.toFirebaseDto(gymId)
                    val res = firebaseManager.customers.saveCustomer(gymId, fCust)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (cust.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            customerDao.updateCustomer(cust.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            customerDao.updateSyncStatus(cust.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    customerDao.updateSyncStatus(cust.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // 4. Memberships
            val pendingMemberships = membershipDao.getPendingSyncMemberships()
            pendingMemberships.forEach { mem ->
                try {
                    val cust = customerDao.getCustomerById(mem.customerId)
                    val plan = planDao.getPlanById(mem.planId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: mem.customerId.toString()
                    val planRemoteId = plan?.firestoreId?.ifBlank { plan.id.toString() }
                    val fMem = mem.toFirebaseDto(gymId, custRemoteId, planRemoteId)
                    val res = firebaseManager.memberships.saveMembership(gymId, fMem)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (mem.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            membershipDao.updateMembership(mem.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            membershipDao.updateSyncStatus(mem.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    membershipDao.updateSyncStatus(mem.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // 5. Payments
            val pendingPayments = paymentDao.getPendingSyncPayments()
            pendingPayments.forEach { pay ->
                try {
                    val cust = customerDao.getCustomerById(pay.customerId)
                    val mem = membershipDao.getMembershipById(pay.membershipId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: pay.customerId.toString()
                    val memRemoteId = mem?.firestoreId?.ifBlank { mem.id.toString() }
                    val fPay = pay.toFirebaseDto(gymId, custRemoteId, memRemoteId)
                    val res = firebaseManager.payments.savePayment(gymId, fPay)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (pay.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            paymentDao.updatePayment(pay.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            paymentDao.updateSyncStatus(pay.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    paymentDao.updateSyncStatus(pay.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // 6. Invoices
            val pendingInvoices = invoiceDao.getPendingSyncInvoices()
            pendingInvoices.forEach { inv ->
                try {
                    val cust = customerDao.getCustomerById(inv.customerId)
                    val mem = membershipDao.getMembershipById(inv.membershipId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: inv.customerId.toString()
                    val memRemoteId = mem?.firestoreId?.ifBlank { mem.id.toString() }
                    val fInv = inv.toFirebaseDto(gymId, custRemoteId, memRemoteId)
                    val res = firebaseManager.invoices.saveInvoice(gymId, fInv)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (inv.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            invoiceDao.updateInvoice(inv.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            invoiceDao.updateSyncStatus(inv.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    invoiceDao.updateSyncStatus(inv.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // 7. Expenses
            val pendingExpenses = expenseDao.getPendingSyncExpenses()
            pendingExpenses.forEach { exp ->
                try {
                    val fExp = exp.toFirebaseDto(gymId)
                    val res = firebaseManager.expenses.saveExpense(gymId, fExp)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (exp.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            expenseDao.updateExpense(exp.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            expenseDao.updateSyncStatus(exp.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    expenseDao.updateSyncStatus(exp.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // 8. Attendance
            val pendingAttendance = attendanceDao.getPendingSyncAttendance()
            pendingAttendance.forEach { att ->
                try {
                    val cust = customerDao.getCustomerById(att.customerId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: att.customerId.toString()
                    val fAtt = att.toFirebaseDto(gymId, custRemoteId)
                    val res = firebaseManager.attendance.saveAttendance(gymId, fAtt)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (att.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            attendanceDao.updateAttendance(att.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            attendanceDao.updateSyncStatus(att.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    attendanceDao.updateSyncStatus(att.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // 9. Notifications
            val pendingNotifs = notificationDao.getPendingSyncNotifications()
            pendingNotifs.forEach { notif ->
                try {
                    val fNotif = notif.toFirebaseDto(gymId)
                    val res = firebaseManager.notifications.saveNotification(gymId, fNotif)
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (notif.firestoreId.isBlank() && !saved.id.isNullOrBlank()) {
                            notificationDao.updateNotification(notif.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            notificationDao.updateSyncStatus(notif.id, SyncStatus.SYNCED)
                        }
                    }
                } catch (e: Exception) {
                    notificationDao.updateSyncStatus(notif.id, SyncStatus.FAILED, error = e.message)
                }
            }

            // B. PENDING DELETIONS
            // 1. Customers
            val pendingDeleteCustomers = customerDao.getPendingDeleteCustomers()
            pendingDeleteCustomers.forEach { cust ->
                val docId = if (cust.firestoreId.isNotBlank()) cust.firestoreId else cust.id.toString()
                try {
                    firebaseManager.customers.deleteCustomer(gymId, docId)
                    customerDao.deleteCustomer(cust)
                } catch (_: Exception) {}
            }

            // 2. Plans
            val pendingDeletePlans = planDao.getPendingDeletePlans()
            pendingDeletePlans.forEach { plan ->
                val docId = if (plan.firestoreId.isNotBlank()) plan.firestoreId else plan.id.toString()
                try {
                    firebaseManager.plans.deletePlan(gymId, docId)
                    planDao.deletePlan(plan)
                } catch (_: Exception) {}
            }

            // 3. Memberships
            val pendingDeleteMemberships = membershipDao.getPendingDeleteMemberships()
            pendingDeleteMemberships.forEach { mem ->
                val docId = if (mem.firestoreId.isNotBlank()) mem.firestoreId else mem.id.toString()
                try {
                    firebaseManager.memberships.deleteMembership(gymId, docId)
                    membershipDao.deleteMembership(mem)
                } catch (_: Exception) {}
            }

            // 4. Payments
            val pendingDeletePayments = paymentDao.getPendingDeletePayments()
            pendingDeletePayments.forEach { pay ->
                val docId = if (pay.firestoreId.isNotBlank()) pay.firestoreId else pay.id.toString()
                try {
                    firebaseManager.payments.deletePayment(gymId, docId)
                    paymentDao.deletePayment(pay)
                } catch (_: Exception) {}
            }

            // 5. Invoices
            val pendingDeleteInvoices = invoiceDao.getPendingDeleteInvoices()
            pendingDeleteInvoices.forEach { inv ->
                val docId = if (inv.firestoreId.isNotBlank()) inv.firestoreId else inv.id.toString()
                try {
                    firebaseManager.invoices.deleteInvoice(gymId, docId)
                    invoiceDao.deleteInvoice(inv)
                } catch (_: Exception) {}
            }

            // 6. Expenses
            val pendingDeleteExpenses = expenseDao.getPendingDeleteExpenses()
            pendingDeleteExpenses.forEach { exp ->
                val docId = if (exp.firestoreId.isNotBlank()) exp.firestoreId else exp.id.toString()
                try {
                    firebaseManager.expenses.deleteExpense(gymId, docId)
                    expenseDao.deleteExpense(exp)
                } catch (_: Exception) {}
            }

            // 7. Attendance
            val pendingDeleteAttendance = attendanceDao.getPendingDeleteAttendance()
            pendingDeleteAttendance.forEach { att ->
                val docId = if (att.firestoreId.isNotBlank()) att.firestoreId else att.id.toString()
                try {
                    firebaseManager.attendance.deleteAttendance(gymId, docId)
                    attendanceDao.deleteAttendance(att)
                } catch (_: Exception) {}
            }

            // 8. Notifications
            val pendingDeleteNotifications = notificationDao.getPendingDeleteNotifications()
            pendingDeleteNotifications.forEach { notif ->
                val docId = if (notif.firestoreId.isNotBlank()) notif.firestoreId else notif.id.toString()
                try {
                    firebaseManager.notifications.deleteNotification(gymId, docId)
                    notificationDao.deleteNotification(notif)
                } catch (_: Exception) {}
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Customer Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)

    suspend fun insertCustomer(customer: Customer, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = customer.copy(
            createdAt = if (customer.createdAt > 0) customer.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = customerDao.insertCustomer(toInsert)
        val fullCustomer = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())
        customerDao.updateCustomer(fullCustomer)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val res = firebaseManager.customers.saveCustomer(gymId, fullCustomer.toFirebaseDto(gymId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            customerDao.updateCustomer(fullCustomer.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            customerDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                customerDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun updateCustomer(customer: Customer, gymId: String) {
        val now = System.currentTimeMillis()
        val updated = customer.copy(
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        customerDao.updateCustomer(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    firebaseManager.customers.saveCustomer(gymId, updated.toFirebaseDto(gymId))
                    customerDao.updateSyncStatus(updated.id, SyncStatus.SYNCED)
                }
            } catch (e: Exception) {
                customerDao.updateSyncStatus(updated.id, SyncStatus.FAILED, error = e.message)
            }
        }
    }

    suspend fun deleteCustomer(customer: Customer, gymId: String) {
        val now = System.currentTimeMillis()
        customerDao.markCustomerForDeletion(customer.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val docId = if (customer.firestoreId.isNotBlank()) customer.firestoreId else customer.id.toString()
                    firebaseManager.customers.deleteCustomer(gymId, docId)
                    customerDao.deleteCustomer(customer)
                }
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Membership Plan Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun getPlanById(id: Long): MembershipPlan? = planDao.getPlanById(id)

    suspend fun insertPlan(plan: MembershipPlan, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = plan.copy(
            createdAt = if (plan.createdAt > 0) plan.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = planDao.insertPlan(toInsert)
        val fullPlan = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())
        planDao.updatePlan(fullPlan)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val res = firebaseManager.plans.savePlan(gymId, fullPlan.toFirebaseDto(gymId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            planDao.updatePlan(fullPlan.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            planDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                planDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun updatePlan(plan: MembershipPlan, gymId: String) {
        val now = System.currentTimeMillis()
        val updated = plan.copy(
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        planDao.updatePlan(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    firebaseManager.plans.savePlan(gymId, updated.toFirebaseDto(gymId))
                    planDao.updateSyncStatus(updated.id, SyncStatus.SYNCED)
                }
            } catch (e: Exception) {
                planDao.updateSyncStatus(updated.id, SyncStatus.FAILED, error = e.message)
            }
        }
    }

    suspend fun deletePlan(plan: MembershipPlan, gymId: String) {
        val now = System.currentTimeMillis()
        planDao.markPlanForDeletion(plan.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val docId = if (plan.firestoreId.isNotBlank()) plan.firestoreId else plan.id.toString()
                    firebaseManager.plans.deletePlan(gymId, docId)
                    planDao.deletePlan(plan)
                }
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Customer Membership Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun getMembershipById(id: Long): Membership? = membershipDao.getMembershipById(id)

    fun getMembershipsByCustomer(customerId: Long): Flow<List<Membership>> =
        membershipDao.getMembershipsByCustomer(customerId)

    fun getActiveMemberships(): Flow<List<Membership>> = membershipDao.getActiveMemberships()

    fun getExpiringSoonMemberships(now: Long, threeDaysLater: Long): Flow<List<Membership>> =
        membershipDao.getExpiringSoonMemberships(now, threeDaysLater)

    fun getExpiredMemberships(now: Long): Flow<List<Membership>> =
        membershipDao.getExpiredMemberships(now)

    fun getPendingPaymentMemberships(): Flow<List<Membership>> =
        membershipDao.getPendingPaymentMemberships()

    suspend fun insertMembership(membership: Membership, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = membership.copy(
            createdAt = if (membership.createdAt > 0) membership.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = membershipDao.insertMembership(toInsert)
        val fullMem = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())
        membershipDao.updateMembership(fullMem)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(fullMem.customerId)
                    val plan = planDao.getPlanById(fullMem.planId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: fullMem.customerId.toString()
                    val planRemoteId = plan?.firestoreId?.ifBlank { plan.id.toString() }
                    val res = firebaseManager.memberships.saveMembership(gymId, fullMem.toFirebaseDto(gymId, custRemoteId, planRemoteId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            membershipDao.updateMembership(fullMem.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            membershipDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                membershipDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun updateMembership(membership: Membership, gymId: String) {
        val now = System.currentTimeMillis()
        val updated = membership.copy(
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        membershipDao.updateMembership(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(updated.customerId)
                    val plan = planDao.getPlanById(updated.planId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: updated.customerId.toString()
                    val planRemoteId = plan?.firestoreId?.ifBlank { plan.id.toString() }
                    firebaseManager.memberships.saveMembership(gymId, updated.toFirebaseDto(gymId, custRemoteId, planRemoteId))
                    membershipDao.updateSyncStatus(updated.id, SyncStatus.SYNCED)
                }
            } catch (e: Exception) {
                membershipDao.updateSyncStatus(updated.id, SyncStatus.FAILED, error = e.message)
            }
        }
    }

    suspend fun deleteMembership(membership: Membership, gymId: String) {
        val now = System.currentTimeMillis()
        membershipDao.markMembershipForDeletion(membership.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val docId = if (membership.firestoreId.isNotBlank()) membership.firestoreId else membership.id.toString()
                    firebaseManager.memberships.deleteMembership(gymId, docId)
                    membershipDao.deleteMembership(membership)
                }
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Payment Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun getPaymentById(id: Long): Payment? = paymentDao.getPaymentById(id)

    fun getPaymentsByCustomer(customerId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsByCustomer(customerId)

    fun getPaymentsByMembership(membershipId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsByMembership(membershipId)

    fun getPaymentsBetween(start: Long, end: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsBetween(start, end)

    suspend fun insertPayment(payment: Payment, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = payment.copy(
            createdAt = if (payment.createdAt > 0) payment.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = paymentDao.insertPayment(toInsert)
        val fullPay = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())
        paymentDao.updatePayment(fullPay)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(fullPay.customerId)
                    val mem = membershipDao.getMembershipById(fullPay.membershipId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: fullPay.customerId.toString()
                    val memRemoteId = mem?.firestoreId?.ifBlank { mem.id.toString() }
                    val res = firebaseManager.payments.savePayment(gymId, fullPay.toFirebaseDto(gymId, custRemoteId, memRemoteId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            paymentDao.updatePayment(fullPay.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            paymentDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                paymentDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun updatePayment(payment: Payment, gymId: String) {
        val now = System.currentTimeMillis()
        val updated = payment.copy(
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        paymentDao.updatePayment(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(updated.customerId)
                    val mem = membershipDao.getMembershipById(updated.membershipId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: updated.customerId.toString()
                    val memRemoteId = mem?.firestoreId?.ifBlank { mem.id.toString() }
                    firebaseManager.payments.savePayment(gymId, updated.toFirebaseDto(gymId, custRemoteId, memRemoteId))
                    paymentDao.updateSyncStatus(updated.id, SyncStatus.SYNCED)
                }
            } catch (e: Exception) {
                paymentDao.updateSyncStatus(updated.id, SyncStatus.FAILED, error = e.message)
            }
        }
    }

    suspend fun deletePayment(payment: Payment, gymId: String) {
        val now = System.currentTimeMillis()
        paymentDao.markPaymentForDeletion(payment.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val docId = if (payment.firestoreId.isNotBlank()) payment.firestoreId else payment.id.toString()
                    firebaseManager.payments.deletePayment(gymId, docId)
                    paymentDao.deletePayment(payment)
                }
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Invoice Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun getInvoiceById(id: Long): Invoice? = invoiceDao.getInvoiceById(id)

    suspend fun getInvoiceByNumber(number: String): Invoice? = invoiceDao.getInvoiceByNumber(number)

    fun getInvoicesByCustomer(customerId: Long): Flow<List<Invoice>> =
        invoiceDao.getInvoicesByCustomer(customerId)

    suspend fun getInvoiceByMembership(membershipId: Long): Invoice? =
        invoiceDao.getInvoiceByMembership(membershipId)

    suspend fun insertInvoice(invoice: Invoice, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = invoice.copy(
            createdAt = if (invoice.createdAt > 0) invoice.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = invoiceDao.insertInvoice(toInsert)
        val fullInv = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())
        invoiceDao.updateInvoice(fullInv)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(fullInv.customerId)
                    val mem = membershipDao.getMembershipById(fullInv.membershipId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: fullInv.customerId.toString()
                    val memRemoteId = mem?.firestoreId?.ifBlank { mem.id.toString() }
                    val res = firebaseManager.invoices.saveInvoice(gymId, fullInv.toFirebaseDto(gymId, custRemoteId, memRemoteId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            invoiceDao.updateInvoice(fullInv.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            invoiceDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                invoiceDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun updateInvoice(invoice: Invoice, gymId: String) {
        val now = System.currentTimeMillis()
        val updated = invoice.copy(
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        invoiceDao.updateInvoice(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(updated.customerId)
                    val mem = membershipDao.getMembershipById(updated.membershipId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: updated.customerId.toString()
                    val memRemoteId = mem?.firestoreId?.ifBlank { mem.id.toString() }
                    firebaseManager.invoices.saveInvoice(gymId, updated.toFirebaseDto(gymId, custRemoteId, memRemoteId))
                    invoiceDao.updateSyncStatus(updated.id, SyncStatus.SYNCED)
                }
            } catch (e: Exception) {
                invoiceDao.updateSyncStatus(updated.id, SyncStatus.FAILED, error = e.message)
            }
        }
    }

    suspend fun deleteInvoice(invoice: Invoice, gymId: String) {
        val now = System.currentTimeMillis()
        invoiceDao.markInvoiceForDeletion(invoice.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val docId = if (invoice.firestoreId.isNotBlank()) invoice.firestoreId else invoice.id.toString()
                    firebaseManager.invoices.deleteInvoice(gymId, docId)
                    invoiceDao.deleteInvoice(invoice)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun markInvoiceSent(invoiceId: Long, gymId: String) {
        val invoice = invoiceDao.getInvoiceById(invoiceId) ?: return
        val updated = invoice.copy(
            isSentToCustomer = true,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_SYNC
        )
        invoiceDao.updateInvoice(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(updated.customerId)
                    val mem = membershipDao.getMembershipById(updated.membershipId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: updated.customerId.toString()
                    val memRemoteId = mem?.firestoreId?.ifBlank { mem.id.toString() }
                    firebaseManager.invoices.saveInvoice(gymId, updated.toFirebaseDto(gymId, custRemoteId, memRemoteId))
                    invoiceDao.updateSyncStatus(invoiceId, SyncStatus.SYNCED)
                }
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Expense Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    fun getExpensesBetween(start: Long, end: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetween(start, end)

    suspend fun insertExpense(expense: Expense, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = expense.copy(
            createdAt = if (expense.createdAt > 0) expense.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = expenseDao.insertExpense(toInsert)
        val fullExp = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())
        expenseDao.updateExpense(fullExp)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val res = firebaseManager.expenses.saveExpense(gymId, fullExp.toFirebaseDto(gymId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            expenseDao.updateExpense(fullExp.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            expenseDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                expenseDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun updateExpense(expense: Expense, gymId: String) {
        val now = System.currentTimeMillis()
        val updated = expense.copy(
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        expenseDao.updateExpense(updated)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    firebaseManager.expenses.saveExpense(gymId, updated.toFirebaseDto(gymId))
                    expenseDao.updateSyncStatus(updated.id, SyncStatus.SYNCED)
                }
            } catch (e: Exception) {
                expenseDao.updateSyncStatus(updated.id, SyncStatus.FAILED, error = e.message)
            }
        }
    }

    suspend fun deleteExpense(expense: Expense, gymId: String) {
        val now = System.currentTimeMillis()
        expenseDao.markExpenseForDeletion(expense.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val docId = if (expense.firestoreId.isNotBlank()) expense.firestoreId else expense.id.toString()
                    firebaseManager.expenses.deleteExpense(gymId, docId)
                    expenseDao.deleteExpense(expense)
                }
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Attendance Operations (Offline-First)
    // -------------------------------------------------------------

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForDate(date)

    fun getTodayPresentCount(date: String): Flow<Int> =
        attendanceDao.getTodayPresentCount(date)

    fun getAttendanceByCustomer(customerId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceByCustomer(customerId)

    suspend fun recordAttendance(gymId: String, customerId: Long, customerName: String, status: String = "present"): Long {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val record = AttendanceRecord(
            customerId = customerId,
            customerName = customerName,
            checkInTime = System.currentTimeMillis(),
            date = todayStr,
            status = status
        )
        return insertAttendance(record, gymId)
    }

    suspend fun insertAttendance(attendance: AttendanceRecord, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = attendance.copy(
            createdAt = if (attendance.createdAt > 0) attendance.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = attendanceDao.insertAttendance(toInsert)
        val fullAtt = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())
        attendanceDao.updateAttendance(fullAtt)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val cust = customerDao.getCustomerById(fullAtt.customerId)
                    val custRemoteId = cust?.firestoreId?.ifBlank { cust.id.toString() } ?: fullAtt.customerId.toString()
                    val res = firebaseManager.attendance.saveAttendance(gymId, fullAtt.toFirebaseDto(gymId, custRemoteId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            attendanceDao.updateAttendance(fullAtt.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            attendanceDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                attendanceDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun deleteAttendance(attendance: AttendanceRecord, gymId: String) {
        val now = System.currentTimeMillis()
        attendanceDao.markAttendanceForDeletion(attendance.id, now)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val docId = if (attendance.firestoreId.isNotBlank()) attendance.firestoreId else attendance.id.toString()
                    firebaseManager.attendance.deleteAttendance(gymId, docId)
                    attendanceDao.deleteAttendance(attendance)
                }
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Gym Settings & Profile Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun updateSettings(settings: GymSetting, gymId: String) {
        val now = System.currentTimeMillis()
        val toSave = settings.copy(
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        settingsDao.insertOrUpdate(toSave)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    firebaseManager.settings.saveSettings(gymId, toSave.toFirebaseAppSettings(gymId))
                    firebaseManager.profile.saveGymProfile(gymId, toSave.toFirebaseGym())
                    settingsDao.updateSyncStatus(SyncStatus.SYNCED)
                }
            } catch (e: Exception) {
                settingsDao.updateSyncStatus(SyncStatus.FAILED, error = e.message)
            }
        }
    }

    // -------------------------------------------------------------
    // Notification Operations (Offline-First)
    // -------------------------------------------------------------

    suspend fun insertNotification(notification: NotificationItem, gymId: String): Long {
        val now = System.currentTimeMillis()
        val toInsert = notification.copy(
            createdAt = if (notification.createdAt > 0) notification.createdAt else now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        val id = notificationDao.insertNotification(toInsert)
        val fullNotif = toInsert.copy(id = id, firestoreId = if (toInsert.firestoreId.isNotBlank()) toInsert.firestoreId else id.toString())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank()) {
                    val res = firebaseManager.notifications.saveNotification(gymId, fullNotif.toFirebaseDto(gymId))
                    if (res.isSuccess) {
                        val saved = res.getOrThrow()
                        if (!saved.id.isNullOrBlank()) {
                            notificationDao.updateNotification(fullNotif.copy(firestoreId = saved.id!!, syncStatus = SyncStatus.SYNCED))
                        } else {
                            notificationDao.updateSyncStatus(id, SyncStatus.SYNCED)
                        }
                    }
                }
            } catch (e: Exception) {
                notificationDao.updateSyncStatus(id, SyncStatus.FAILED, error = e.message)
            }
        }
        return id
    }

    suspend fun markNotificationRead(id: Long, gymId: String) {
        val notif = notificationDao.getNotificationById(id)
        notificationDao.markAsRead(id)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (gymId.isNotBlank() && notif != null) {
                    val docId = if (notif.firestoreId.isNotBlank()) notif.firestoreId else notif.id.toString()
                    val fNotif = notif.toFirebaseDto(gymId)
                    fNotif.id = docId
                    fNotif.isRead = true
                    firebaseManager.notifications.saveNotification(gymId, fNotif)
                    notificationDao.updateSyncStatus(id, SyncStatus.SYNCED)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun markAllNotificationsRead(gymId: String) {
        notificationDao.markAllAsRead()
    }

    suspend fun clearNotifications(gymId: String) {
        notificationDao.clearAll()
    }

    // -------------------------------------------------------------
    // Full Business Logic Composite Workflows
    // -------------------------------------------------------------

    /**
     * Registers a new member with their initial membership, payment, and invoice.
     */
    suspend fun registerMemberWithMembership(
        gymId: String,
        customerName: String,
        mobileNumber: String,
        email: String,
        gender: String,
        address: String,
        emergencyContact: String,
        isMobileVerified: Boolean,
        plan: MembershipPlan,
        startDate: Long,
        expiryDate: Long,
        totalAmount: Double,
        paidAmount: Double,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        transactionRef: String,
        templateId: String
    ): Triple<Long, Long, String> = withContext(Dispatchers.IO) {
        val customer = Customer(
            name = customerName,
            mobileNumber = mobileNumber,
            email = email,
            gender = gender,
            address = address,
            emergencyContact = emergencyContact,
            isMobileVerified = isMobileVerified,
            registeredDate = System.currentTimeMillis()
        )
        val custId = insertCustomer(customer, gymId)

        val pendingAmount = (totalAmount - paidAmount).coerceAtLeast(0.0)
        val membership = Membership(
            customerId = custId,
            planId = plan.id,
            planName = plan.name,
            startDate = startDate,
            expiryDate = expiryDate,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            pendingAmount = pendingAmount,
            paymentStatus = paymentStatus,
            status = MembershipStatus.ACTIVE
        )
        val memId = insertMembership(membership, gymId)

        if (paidAmount > 0) {
            val payment = Payment(
                membershipId = memId,
                customerId = custId,
                amount = paidAmount,
                paymentDate = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                transactionRef = transactionRef,
                notes = "Initial membership fee"
            )
            insertPayment(payment, gymId)
        }

        val settingsSnap = settingsDao.getSettingsSnapshot()
        val nextSeq = 101
        val prefix = "INV-"
        val year = SimpleDateFormat("yy", Locale.getDefault()).format(Date())
        val invoiceNumber = "$prefix$year-${String.format(Locale.getDefault(), "%04d", nextSeq)}"

        val durationText = if (plan.durationMonths > 0) "${plan.durationMonths} Month${if (plan.durationMonths > 1) "s" else ""}" else "${plan.durationDays} Days"

        val invoice = Invoice(
            invoiceNumber = invoiceNumber,
            membershipId = memId,
            customerId = custId,
            customerName = customerName,
            customerMobile = mobileNumber,
            planName = plan.name,
            durationText = durationText,
            startDate = startDate,
            expiryDate = expiryDate,
            subtotal = totalAmount,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            pendingAmount = pendingAmount,
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod,
            templateId = templateId.ifBlank { settingsSnap?.activeInvoiceTemplateId ?: "modern_clean" }
        )
        insertInvoice(invoice, gymId)

        insertNotification(
            NotificationItem(
                title = "New Member Enrolled",
                message = "$customerName registered under ${plan.name}.",
                type = "NEW_MEMBER"
            ),
            gymId
        )

        Triple(custId, memId, invoiceNumber)
    }

    /**
     * Renews an existing customer's membership with a new plan duration.
     */
    suspend fun renewMembership(
        gymId: String,
        customerId: Long,
        customerName: String,
        customerMobile: String,
        plan: MembershipPlan,
        startDate: Long,
        expiryDate: Long,
        totalAmount: Double,
        paidAmount: Double,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        transactionRef: String,
        templateId: String
    ): Pair<Long, String> = withContext(Dispatchers.IO) {
        val pendingAmount = (totalAmount - paidAmount).coerceAtLeast(0.0)
        val membership = Membership(
            customerId = customerId,
            planId = plan.id,
            planName = plan.name,
            startDate = startDate,
            expiryDate = expiryDate,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            pendingAmount = pendingAmount,
            paymentStatus = paymentStatus,
            status = MembershipStatus.ACTIVE
        )
        val memId = insertMembership(membership, gymId)

        if (paidAmount > 0) {
            val payment = Payment(
                membershipId = memId,
                customerId = customerId,
                amount = paidAmount,
                paymentDate = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                transactionRef = transactionRef,
                notes = "Membership Renewal Payment"
            )
            insertPayment(payment, gymId)
        }

        val settingsSnap = settingsDao.getSettingsSnapshot()
        val nextSeq = 102
        val prefix = "INV-"
        val year = SimpleDateFormat("yy", Locale.getDefault()).format(Date())
        val invoiceNumber = "$prefix$year-${String.format(Locale.getDefault(), "%04d", nextSeq)}"

        val durationText = if (plan.durationMonths > 0) "${plan.durationMonths} Month${if (plan.durationMonths > 1) "s" else ""}" else "${plan.durationDays} Days"

        val invoice = Invoice(
            invoiceNumber = invoiceNumber,
            membershipId = memId,
            customerId = customerId,
            customerName = customerName,
            customerMobile = customerMobile,
            planName = plan.name,
            durationText = durationText,
            startDate = startDate,
            expiryDate = expiryDate,
            subtotal = totalAmount,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            pendingAmount = pendingAmount,
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod,
            templateId = templateId.ifBlank { settingsSnap?.activeInvoiceTemplateId ?: "modern_clean" }
        )
        insertInvoice(invoice, gymId)

        insertNotification(
            NotificationItem(
                title = "Membership Renewed",
                message = "$customerName renewed ${plan.name}.",
                type = "RENEWAL"
            ),
            gymId
        )

        Pair(memId, invoiceNumber)
    }

    /**
     * Records a dues / partial payment for an existing membership.
     */
    suspend fun recordPendingPayment(
        gymId: String,
        membershipId: Long,
        customerId: Long,
        amount: Double,
        paymentMethod: PaymentMethod,
        transactionRef: String,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val membership = membershipDao.getMembershipById(membershipId) ?: return@withContext 0L
        val customer = customerDao.getCustomerById(customerId)

        val newPaidAmount = membership.paidAmount + amount
        val newPendingAmount = (membership.totalAmount - newPaidAmount).coerceAtLeast(0.0)

        val newPaymentStatus = when {
            newPendingAmount <= 0.0 -> PaymentStatus.PAID
            newPaidAmount > 0.0 -> PaymentStatus.PARTIALLY_PAID
            else -> PaymentStatus.PENDING
        }

        val updatedMembership = membership.copy(
            paidAmount = newPaidAmount,
            pendingAmount = newPendingAmount,
            paymentStatus = newPaymentStatus
        )
        updateMembership(updatedMembership, gymId)

        val payment = Payment(
            membershipId = membershipId,
            customerId = customerId,
            amount = amount,
            paymentDate = System.currentTimeMillis(),
            paymentMethod = paymentMethod,
            paymentStatus = newPaymentStatus,
            transactionRef = transactionRef,
            notes = notes.ifEmpty { "Due payment collected" }
        )
        val pId = insertPayment(payment, gymId)

        val existingInvoice = invoiceDao.getInvoiceByMembership(membershipId)
        if (existingInvoice != null) {
            val updatedInvoice = existingInvoice.copy(
                paidAmount = newPaidAmount,
                pendingAmount = newPendingAmount,
                paymentStatus = newPaymentStatus,
                paymentMethod = paymentMethod
            )
            updateInvoice(updatedInvoice, gymId)
        }

        insertNotification(
            NotificationItem(
                title = "Payment Received",
                message = "Collected ₹${String.format(Locale.getDefault(), "%.0f", amount)} from ${customer?.name ?: "Member"}.",
                type = "PENDING_PAYMENT"
            ),
            gymId
        )

        pId
    }

    /**
     * Hard wipes all local records to establish a clean zero-state.
     */
    suspend fun resetAllGymData(gymId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            val defaultSettings = GymSetting(
                gymName = "My Fitness Club",
                gymPhone = "+91 98765 43210",
                currencySymbol = "₹",
                activeInvoiceTemplateId = "modern_clean"
            )
            settingsDao.insertOrUpdate(defaultSettings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
