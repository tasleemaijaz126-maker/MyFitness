package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.MembershipStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus

object SyncStatus {
    const val SYNCED = "SYNCED"
    const val PENDING_SYNC = "PENDING_SYNC"
    const val PENDING_DELETE = "PENDING_DELETE"
    const val FAILED = "FAILED"
}

@Entity(
    tableName = "customers",
    indices = [Index("mobileNumber"), Index("syncStatus")]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val name: String,
    val mobileNumber: String,
    val email: String = "",
    val profileImageUri: String = "",
    val gender: String = "Male", // "Male", "Female", "Other"
    val emergencyContact: String = "",
    val address: String = "",
    val isMobileVerified: Boolean = false,
    val notes: String = "",
    val registeredDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(
    tableName = "membership_plans",
    indices = [Index("syncStatus")]
)
data class MembershipPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val name: String,
    val durationMonths: Int,
    val durationDays: Int = 0,
    val price: Double,
    val description: String = "",
    val category: String = "General Fitness", // General Fitness, Strength, Cardio, VIP, Personal Training
    val isActive: Boolean = true,
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(
    tableName = "memberships",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MembershipPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("customerId"), Index("planId"), Index("syncStatus")]
)
data class Membership(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val customerId: Long,
    val planId: Long,
    val planName: String,
    val startDate: Long,
    val expiryDate: Long,
    val totalAmount: Double,
    val paidAmount: Double,
    val pendingAmount: Double,
    val paymentStatus: PaymentStatus,
    val status: MembershipStatus,
    val createdDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Membership::class,
            parentColumns = ["id"],
            childColumns = ["membershipId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("membershipId"), Index("customerId"), Index("syncStatus")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val membershipId: Long,
    val customerId: Long,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val transactionRef: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = Membership::class,
            parentColumns = ["id"],
            childColumns = ["membershipId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("membershipId"), Index("customerId"), Index("invoiceNumber", unique = true), Index("syncStatus")]
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val invoiceNumber: String,
    val membershipId: Long,
    val customerId: Long,
    val customerName: String,
    val customerMobile: String,
    val planName: String,
    val durationText: String,
    val startDate: Long,
    val expiryDate: Long,
    val subtotal: Double,
    val taxGst: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val pendingAmount: Double,
    val paymentStatus: PaymentStatus,
    val paymentMethod: PaymentMethod,
    val templateId: String = "modern_clean",
    val generatedDate: Long = System.currentTimeMillis(),
    val isSentToCustomer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(
    tableName = "expenses",
    indices = [Index("syncStatus"), Index("expenseDate")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val title: String,
    val category: String = "Maintenance", // Maintenance, Equipment, Electricity/Water, Rent, Salary, Supplements, Marketing, Other
    val amount: Double,
    val expenseDate: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val vendor: String = "",
    val receiptImageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(tableName = "gym_settings")
data class GymSetting(
    @PrimaryKey val id: Int = 1,
    val firestoreId: String = "settings",
    val gymName: String = "IronForge Fitness & ERP",
    val gymTagline: String = "Building Champions Every Day",
    val gymAddress: String = "Plot 42, Metro Commercial Arcade, 2nd Floor",
    val gymCity: String = "Mumbai, Maharashtra 400050",
    val gymPhone: String = "+91 98765 43210",
    val gymEmail: String = "contact@ironforgefitness.com",
    val gymGstin: String = "27AABCI1234F1Z8",
    val currencySymbol: String = "₹",
    val activeInvoiceTemplateId: String = "modern_clean",
    val appTheme: String = "DARK", // DARK, LIGHT, SYSTEM
    val appLanguage: String = "ENGLISH", // ENGLISH, HINDI, MARATHI, URDU
    val requireOtpForMemberCreation: Boolean = false,
    val requireOtpForInvoiceSend: Boolean = false,
    val invoiceTerms: String = "1. Membership is non-refundable & non-transferable.\n2. Gym rules must be respected at all times.\n3. Please keep this digital invoice for access.",
    val ownerSignatureName: String = "Zameer Khan",
    val ownerSignatureStyleId: String = "signature_1",
    val gymLocationUrl: String = "",
    val lastInvoiceSequence: Int = 100,
    val isBiometricEnabled: Boolean = false,
    val biometricAutoLockMinutes: Int = 0, // 0 = Immediately on background, 1, 5, 15
    val securityPin: String = "", // Optional 4-digit backup PIN
    val requireBiometricForExport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(
    tableName = "notifications",
    indices = [Index("syncStatus")]
)
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val title: String,
    val message: String,
    val type: String, // "EXPIRY", "PENDING_PAYMENT", "RENEWAL", "NEW_MEMBER", "SYSTEM"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val referenceId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)

@Entity(
    tableName = "attendance",
    indices = [Index("date"), Index("customerId"), Index("syncStatus")]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String = "",
    val customerId: Long,
    val customerName: String,
    val checkInTime: Long = System.currentTimeMillis(),
    val checkOutTime: Long? = null,
    val date: String, // YYYY-MM-DD
    val status: String = "present", // "present", "absent"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = SyncStatus.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastSyncError: String? = null
)
