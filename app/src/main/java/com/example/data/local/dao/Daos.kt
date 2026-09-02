package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE syncStatus != 'PENDING_DELETE' ORDER BY registeredDate DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE syncStatus != 'PENDING_DELETE' ORDER BY registeredDate DESC")
    suspend fun getAllCustomersSnapshot(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getCustomerByFirestoreId(firestoreId: String): Customer?

    @Query("SELECT * FROM customers WHERE mobileNumber = :mobile AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getCustomerByMobile(mobile: String): Customer?

    @Query("SELECT * FROM customers WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    fun observeCustomerById(id: Long): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Update
    suspend fun update(customer: Customer)

    @Query("UPDATE customers SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markCustomerForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: Long)

    @Query("SELECT COUNT(*) FROM customers WHERE syncStatus != 'PENDING_DELETE'")
    fun getCustomerCount(): Flow<Int>

    @Query("SELECT * FROM customers WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncCustomers(): List<Customer>

    @Query("SELECT * FROM customers WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteCustomers(): List<Customer>

    @Query("UPDATE customers SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface MembershipPlanDao {
    @Query("SELECT * FROM membership_plans WHERE syncStatus != 'PENDING_DELETE' ORDER BY price ASC")
    fun getAllPlans(): Flow<List<MembershipPlan>>

    @Query("SELECT * FROM membership_plans WHERE syncStatus != 'PENDING_DELETE' ORDER BY price ASC")
    suspend fun getAllPlansSnapshot(): List<MembershipPlan>

    @Query("SELECT * FROM membership_plans WHERE isActive = 1 AND syncStatus != 'PENDING_DELETE' ORDER BY price ASC")
    fun getActivePlans(): Flow<List<MembershipPlan>>

    @Query("SELECT * FROM membership_plans WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getPlanById(id: Long): MembershipPlan?

    @Query("SELECT * FROM membership_plans WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getPlanByFirestoreId(firestoreId: String): MembershipPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MembershipPlan): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<MembershipPlan>)

    @Update
    suspend fun updatePlan(plan: MembershipPlan)

    @Update
    suspend fun update(plan: MembershipPlan)

    @Query("UPDATE membership_plans SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markPlanForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deletePlan(plan: MembershipPlan)

    @Query("DELETE FROM membership_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    @Query("SELECT * FROM membership_plans WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncPlans(): List<MembershipPlan>

    @Query("SELECT * FROM membership_plans WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletePlans(): List<MembershipPlan>

    @Query("UPDATE membership_plans SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface MembershipDao {
    @Query("SELECT * FROM memberships WHERE syncStatus != 'PENDING_DELETE' ORDER BY createdDate DESC")
    fun getAllMemberships(): Flow<List<Membership>>

    @Query("SELECT * FROM memberships WHERE syncStatus != 'PENDING_DELETE' ORDER BY createdDate DESC")
    suspend fun getAllMembershipsSnapshot(): List<Membership>

    @Query("SELECT * FROM memberships WHERE customerId = :customerId AND syncStatus != 'PENDING_DELETE' ORDER BY createdDate DESC")
    fun getMembershipsByCustomer(customerId: Long): Flow<List<Membership>>

    @Query("SELECT * FROM memberships WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getMembershipById(id: Long): Membership?

    @Query("SELECT * FROM memberships WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getMembershipByFirestoreId(firestoreId: String): Membership?

    @Query("SELECT * FROM memberships WHERE status = 'ACTIVE' AND syncStatus != 'PENDING_DELETE' ORDER BY expiryDate ASC")
    fun getActiveMemberships(): Flow<List<Membership>>

    @Query("SELECT * FROM memberships WHERE expiryDate >= :now AND expiryDate <= :threeDaysLater AND syncStatus != 'PENDING_DELETE' ORDER BY expiryDate ASC")
    fun getExpiringSoonMemberships(now: Long, threeDaysLater: Long): Flow<List<Membership>>

    @Query("SELECT * FROM memberships WHERE expiryDate < :now AND syncStatus != 'PENDING_DELETE' ORDER BY expiryDate DESC")
    fun getExpiredMemberships(now: Long): Flow<List<Membership>>

    @Query("SELECT * FROM memberships WHERE pendingAmount > 0 AND syncStatus != 'PENDING_DELETE' ORDER BY createdDate DESC")
    fun getPendingPaymentMemberships(): Flow<List<Membership>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: Membership): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberships(memberships: List<Membership>)

    @Update
    suspend fun updateMembership(membership: Membership)

    @Update
    suspend fun update(membership: Membership)

    @Query("UPDATE memberships SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markMembershipForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteMembership(membership: Membership)

    @Query("DELETE FROM memberships WHERE id = :id")
    suspend fun deleteMembershipById(id: Long)

    @Query("SELECT * FROM memberships WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncMemberships(): List<Membership>

    @Query("SELECT * FROM memberships WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteMemberships(): List<Membership>

    @Query("UPDATE memberships SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE syncStatus != 'PENDING_DELETE' ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE syncStatus != 'PENDING_DELETE' ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsSnapshot(): List<Payment>

    @Query("SELECT * FROM payments WHERE customerId = :customerId AND syncStatus != 'PENDING_DELETE' ORDER BY paymentDate DESC")
    fun getPaymentsByCustomer(customerId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE membershipId = :membershipId AND syncStatus != 'PENDING_DELETE' ORDER BY paymentDate DESC")
    fun getPaymentsByMembership(membershipId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getPaymentById(id: Long): Payment?

    @Query("SELECT * FROM payments WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getPaymentByFirestoreId(firestoreId: String): Payment?

    @Query("SELECT * FROM payments WHERE paymentDate >= :startTimestamp AND paymentDate <= :endTimestamp AND syncStatus != 'PENDING_DELETE' ORDER BY paymentDate DESC")
    fun getPaymentsBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)

    @Update
    suspend fun updatePayment(payment: Payment)

    @Update
    suspend fun update(payment: Payment)

    @Query("UPDATE payments SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markPaymentForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("SELECT * FROM payments WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncPayments(): List<Payment>

    @Query("SELECT * FROM payments WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeletePayments(): List<Payment>

    @Query("UPDATE payments SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE syncStatus != 'PENDING_DELETE' ORDER BY generatedDate DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE syncStatus != 'PENDING_DELETE' ORDER BY generatedDate DESC")
    suspend fun getAllInvoicesSnapshot(): List<Invoice>

    @Query("SELECT * FROM invoices WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getInvoiceById(id: Long): Invoice?

    @Query("SELECT * FROM invoices WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getInvoiceByFirestoreId(firestoreId: String): Invoice?

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): Invoice?

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND syncStatus != 'PENDING_DELETE' ORDER BY generatedDate DESC")
    fun getInvoicesByCustomer(customerId: Long): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE membershipId = :membershipId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getInvoiceByMembership(membershipId: Long): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<Invoice>)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Update
    suspend fun update(invoice: Invoice)

    @Query("UPDATE invoices SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markInvoiceForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    @Query("SELECT * FROM invoices WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncInvoices(): List<Invoice>

    @Query("SELECT * FROM invoices WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteInvoices(): List<Invoice>

    @Query("UPDATE invoices SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE syncStatus != 'PENDING_DELETE' ORDER BY expenseDate DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE syncStatus != 'PENDING_DELETE' ORDER BY expenseDate DESC")
    suspend fun getAllExpensesSnapshot(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getExpenseByFirestoreId(firestoreId: String): Expense?

    @Query("SELECT * FROM expenses WHERE expenseDate >= :startTimestamp AND expenseDate <= :endTimestamp AND syncStatus != 'PENDING_DELETE' ORDER BY expenseDate DESC")
    fun getExpensesBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Query("UPDATE expenses SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markExpenseForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT * FROM expenses WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncExpenses(): List<Expense>

    @Query("SELECT * FROM expenses WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteExpenses(): List<Expense>

    @Query("UPDATE expenses SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface GymSettingDao {
    @Query("SELECT * FROM gym_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<GymSetting?>

    @Query("SELECT * FROM gym_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSnapshot(): GymSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: GymSetting)

    @Query("SELECT * FROM gym_settings WHERE syncStatus IN ('PENDING_SYNC', 'FAILED') LIMIT 1")
    suspend fun getPendingSyncSettings(): GymSetting?

    @Query("UPDATE gym_settings SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = 1")
    suspend fun updateSyncStatus(status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE syncStatus != 'PENDING_DELETE' ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE syncStatus != 'PENDING_DELETE' ORDER BY timestamp DESC")
    suspend fun getAllNotificationsSnapshot(): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getNotificationById(id: Long): NotificationItem?

    @Query("SELECT * FROM notifications WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getNotificationByFirestoreId(firestoreId: String): NotificationItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationItem>)

    @Update
    suspend fun updateNotification(notification: NotificationItem)

    @Update
    suspend fun update(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1, updatedAt = :updatedAt, syncStatus = 'PENDING_SYNC' WHERE id = :id")
    suspend fun markAsRead(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isRead = 1, updatedAt = :updatedAt, syncStatus = 'PENDING_SYNC'")
    suspend fun markAllAsRead(updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markNotificationForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Delete
    suspend fun deleteNotification(notification: NotificationItem)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("SELECT * FROM notifications WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncNotifications(): List<NotificationItem>

    @Query("SELECT * FROM notifications WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteNotifications(): List<NotificationItem>

    @Query("UPDATE notifications SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE syncStatus != 'PENDING_DELETE' ORDER BY checkInTime DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE syncStatus != 'PENDING_DELETE' ORDER BY checkInTime DESC")
    suspend fun getAllAttendanceSnapshot(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance WHERE date = :date AND syncStatus != 'PENDING_DELETE' ORDER BY checkInTime DESC")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT COUNT(*) FROM attendance WHERE date = :date AND status = 'present' AND syncStatus != 'PENDING_DELETE'")
    fun getTodayPresentCount(date: String): Flow<Int>

    @Query("SELECT * FROM attendance WHERE customerId = :customerId AND syncStatus != 'PENDING_DELETE' ORDER BY checkInTime DESC")
    fun getAttendanceByCustomer(customerId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE id = :id AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getAttendanceById(id: Long): AttendanceRecord?

    @Query("SELECT * FROM attendance WHERE firestoreId = :firestoreId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    suspend fun getAttendanceByFirestoreId(firestoreId: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(attendanceList: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceRecord)

    @Update
    suspend fun update(attendance: AttendanceRecord)

    @Query("UPDATE attendance SET syncStatus = 'PENDING_DELETE', updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAttendanceForDeletion(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceRecord)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: Long)

    @Query("SELECT * FROM attendance WHERE syncStatus IN ('PENDING_SYNC', 'FAILED')")
    suspend fun getPendingSyncAttendance(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance WHERE syncStatus = 'PENDING_DELETE'")
    suspend fun getPendingDeleteAttendance(): List<AttendanceRecord>

    @Query("UPDATE attendance SET syncStatus = :status, lastSyncedAt = :syncedAt, lastSyncError = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long = System.currentTimeMillis(), error: String? = null)
}
