package com.example.data.firebase

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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Timestamp utilities for Firestore documents and Room entity interop.
 */
object FirebaseDateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun toIso(timestamp: Long): String {
        return isoFormat.format(Date(timestamp))
    }

    fun fromIso(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            isoFormat.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            try {
                fallbackFormat.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    fun toDateString(timestamp: Long): String {
        return dateOnlyFormat.format(Date(timestamp))
    }

    fun toTimestamp(millis: Long): Timestamp {
        return Timestamp(Date(millis))
    }

    fun fromTimestamp(ts: Timestamp?): Long {
        return ts?.toDate()?.time ?: System.currentTimeMillis()
    }
}

// --------------------------------------------------------------------
// 1. Firebase Gym DTO (gyms/{gymId} and gymProfiles/{gymId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseGym(
    @DocumentId var id: String? = null,
    @PropertyName("name") var name: String = "My Fitness Club",
    @PropertyName("ownerName") var ownerName: String = "Gym Owner",
    @PropertyName("phone") var phone: String? = "",
    @PropertyName("email") var email: String? = "",
    @PropertyName("address") var address: String? = "",
    @PropertyName("city") var city: String? = "",
    @PropertyName("state") var state: String? = "",
    @PropertyName("country") var country: String? = "India",
    @PropertyName("logoUrl") var logoUrl: String? = "",
    @PropertyName("signatureData") var signatureData: String? = "",
    @PropertyName("signatureStyleId") var signatureStyleId: String? = "signature_1",
    @PropertyName("locationQrUrl") var locationQrUrl: String? = "",
    @PropertyName("gymLocationUrl") var gymLocationUrl: String? = "",
    @PropertyName("currencySymbol") var currencySymbol: String? = "₹",
    @PropertyName("tagline") var tagline: String? = "Building Champions Every Day",
    @PropertyName("gstin") var gstin: String? = "",
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 2. Firebase Profile DTO (users/{userId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseProfile(
    @DocumentId var id: String? = null,
    @PropertyName("userId") var userId: String? = null,
    @PropertyName("gymId") var gymId: String? = null,
    @PropertyName("fullName") var fullName: String = "",
    @PropertyName("phone") var phone: String? = "",
    @PropertyName("email") var email: String? = "",
    @PropertyName("profilePhotoUrl") var profilePhotoUrl: String? = "",
    @PropertyName("role") var role: String = "OWNER",
    @PropertyName("status") var status: String = "ACTIVE",
    @PropertyName("isActive") var isActive: Boolean = true,
    @ServerTimestamp @PropertyName("lastLoginAt") var lastLoginAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 3. Firebase App Settings DTO (appSettings/{gymId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseAppSettings(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("biometricEnabled") var biometricEnabled: Boolean = false,
    @PropertyName("biometricTimeout") var biometricTimeout: Int = 0,
    @PropertyName("theme") var theme: String = "SYSTEM",
    @PropertyName("language") var language: String = "ENGLISH",
    @PropertyName("notificationEnabled") var notificationEnabled: Boolean = true,
    @PropertyName("autoBackupEnabled") var autoBackupEnabled: Boolean = true,
    @PropertyName("soundEffectsEnabled") var soundEffectsEnabled: Boolean = true,
    @PropertyName("hapticFeedbackEnabled") var hapticFeedbackEnabled: Boolean = true,
    @PropertyName("selectedInvoiceTemplate") var selectedInvoiceTemplate: String = "modern_clean",
    @PropertyName("signatureSvgPath") var signatureSvgPath: String? = "",
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 4. Firebase Customer DTO (customers/{customerId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseCustomer(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("fullName") var fullName: String = "",
    @PropertyName("phone") var phone: String = "",
    @PropertyName("email") var email: String? = "",
    @PropertyName("dateOfBirth") var dateOfBirth: String? = "",
    @PropertyName("gender") var gender: String? = "Male",
    @PropertyName("address") var address: String? = "",
    @PropertyName("emergencyContact") var emergencyContact: String? = "",
    @PropertyName("profilePhotoUrl") var profilePhotoUrl: String? = "",
    @PropertyName("status") var status: String = "active",
    @PropertyName("joiningDate") var joiningDate: String? = null,
    @PropertyName("notes") var notes: String? = "",
    @PropertyName("isMobileVerified") var isMobileVerified: Boolean = false,
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 5. Firebase Membership Plan DTO (membershipPlans/{planId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseMembershipPlan(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("name") var name: String = "",
    @PropertyName("description") var description: String? = "",
    @PropertyName("duration") var duration: Int = 1,
    @PropertyName("durationUnit") var durationUnit: String = "Months",
    @PropertyName("price") var price: Double = 0.0,
    @PropertyName("discount") var discount: Double = 0.0,
    @PropertyName("finalPrice") var finalPrice: Double = 0.0,
    @PropertyName("status") var status: String = "active",
    @PropertyName("isActive") var isActive: Boolean = true,
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 6. Firebase Membership DTO (memberships/{membershipId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseMembership(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("customerId") var customerId: String = "",
    @PropertyName("planId") var planId: String? = null,
    @PropertyName("customerName") var customerName: String = "",
    @PropertyName("planName") var planName: String = "",
    @PropertyName("startDate") var startDate: String = "",
    @PropertyName("endDate") var endDate: String = "",
    @PropertyName("duration") var duration: Int = 1,
    @PropertyName("amount") var amount: Double = 0.0,
    @PropertyName("paidAmount") var paidAmount: Double = 0.0,
    @PropertyName("pendingAmount") var pendingAmount: Double = 0.0,
    @PropertyName("discount") var discount: Double = 0.0,
    @PropertyName("finalAmount") var finalAmount: Double = 0.0,
    @PropertyName("paymentStatus") var paymentStatus: String = "paid",
    @PropertyName("membershipStatus") var membershipStatus: String = "active",
    @PropertyName("invoiceId") var invoiceId: String? = "",
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 7. Firebase Payment DTO (payments/{paymentId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebasePayment(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("customerId") var customerId: String = "",
    @PropertyName("membershipId") var membershipId: String? = null,
    @PropertyName("customerName") var customerName: String = "",
    @PropertyName("amount") var amount: Double = 0.0,
    @PropertyName("paymentMethod") var paymentMethod: String = "cash",
    @PropertyName("paymentStatus") var paymentStatus: String = "paid",
    @PropertyName("transactionReference") var transactionReference: String? = "",
    @PropertyName("invoiceId") var invoiceId: String? = "",
    @PropertyName("paymentDate") var paymentDate: String? = null,
    @PropertyName("dueDate") var dueDate: String? = null,
    @PropertyName("notes") var notes: String? = "",
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 8. Firebase Invoice DTO (invoices/{invoiceId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseInvoice(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("customerId") var customerId: String = "",
    @PropertyName("membershipId") var membershipId: String? = null,
    @PropertyName("invoiceNumber") var invoiceNumber: String = "",
    @PropertyName("customerName") var customerName: String = "",
    @PropertyName("mobile") var mobile: String? = "",
    @PropertyName("planName") var planName: String? = "",
    @PropertyName("invoiceDate") var invoiceDate: String? = null,
    @PropertyName("dueDate") var dueDate: String? = null,
    @PropertyName("amount") var amount: Double = 0.0,
    @PropertyName("paidAmount") var paidAmount: Double = 0.0,
    @PropertyName("pendingAmount") var pendingAmount: Double = 0.0,
    @PropertyName("discount") var discount: Double = 0.0,
    @PropertyName("subtotal") var subtotal: Double = 0.0,
    @PropertyName("totalAmount") var totalAmount: Double = 0.0,
    @PropertyName("paymentStatus") var paymentStatus: String = "paid",
    @PropertyName("paymentMethod") var paymentMethod: String = "cash",
    @PropertyName("invoiceTemplate") var invoiceTemplate: String = "modern_clean",
    @PropertyName("signatureStyleId") var signatureStyleId: String = "signature_1",
    @PropertyName("pdfUrl") var pdfUrl: String? = "",
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 9. Firebase Attendance DTO (attendance/{attendanceId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseAttendance(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("customerId") var customerId: String = "",
    @PropertyName("customerName") var customerName: String = "",
    @PropertyName("attendanceDate") var attendanceDate: String = "",
    @PropertyName("checkInTime") var checkInTime: String? = null,
    @PropertyName("checkOutTime") var checkOutTime: String? = null,
    @PropertyName("status") var status: String = "present",
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 10. Firebase Expense DTO (expenses/{expenseId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseExpense(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("title") var title: String = "",
    @PropertyName("category") var category: String = "General",
    @PropertyName("description") var description: String? = "",
    @PropertyName("amount") var amount: Double = 0.0,
    @PropertyName("paymentMethod") var paymentMethod: String = "cash",
    @PropertyName("expenseDate") var expenseDate: String? = null,
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// --------------------------------------------------------------------
// 11. Firebase Notification DTO (notifications/{notificationId})
// --------------------------------------------------------------------
@IgnoreExtraProperties
data class FirebaseNotification(
    @DocumentId var id: String? = null,
    @PropertyName("gymId") var gymId: String = "",
    @PropertyName("title") var title: String = "",
    @PropertyName("message") var message: String = "",
    @PropertyName("type") var type: String = "SYSTEM",
    @PropertyName("isRead") var isRead: Boolean = false,
    @PropertyName("actionUrl") var actionUrl: String? = "",
    @ServerTimestamp @PropertyName("createdAt") var createdAt: Timestamp? = null,
    @ServerTimestamp @PropertyName("updatedAt") var updatedAt: Timestamp? = null
)

// ====================================================================
// CONVERSION EXTENSION FUNCTIONS (ROOM <-> FIREBASE)
// ====================================================================

fun Customer.toFirebaseDto(gymId: String): FirebaseCustomer {
    return FirebaseCustomer(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        fullName = name,
        phone = mobileNumber,
        email = email,
        profilePhotoUrl = profileImageUri,
        gender = gender,
        emergencyContact = emergencyContact,
        address = address,
        isMobileVerified = isMobileVerified,
        notes = notes,
        joiningDate = FirebaseDateUtils.toIso(registeredDate)
    )
}

fun FirebaseCustomer.toEntity(existingId: Long = 0): Customer {
    return Customer(
        id = existingId,
        firestoreId = id ?: "",
        name = fullName,
        mobileNumber = phone,
        email = email.orEmpty(),
        profileImageUri = profilePhotoUrl.orEmpty(),
        gender = gender ?: "Male",
        emergencyContact = emergencyContact.orEmpty(),
        address = address.orEmpty(),
        isMobileVerified = isMobileVerified,
        notes = notes.orEmpty(),
        registeredDate = FirebaseDateUtils.fromIso(joiningDate),
        createdAt = FirebaseDateUtils.fromTimestamp(createdAt),
        updatedAt = FirebaseDateUtils.fromTimestamp(updatedAt),
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun MembershipPlan.toFirebaseDto(gymId: String): FirebaseMembershipPlan {
    return FirebaseMembershipPlan(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        name = name,
        description = description,
        duration = durationMonths,
        durationUnit = "Months",
        price = price,
        discount = 0.0,
        finalPrice = price,
        status = if (isActive) "active" else "inactive",
        isActive = isActive
    )
}

fun FirebaseMembershipPlan.toEntity(existingId: Long = 0): MembershipPlan {
    return MembershipPlan(
        id = existingId,
        firestoreId = id ?: "",
        name = name,
        durationMonths = duration,
        durationDays = duration * 30,
        price = if (finalPrice > 0) finalPrice else price,
        description = description.orEmpty(),
        isActive = isActive || status.equals("active", ignoreCase = true),
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun Membership.toFirebaseDto(gymId: String, customerFirestoreId: String, planFirestoreId: String?): FirebaseMembership {
    return FirebaseMembership(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        customerId = customerFirestoreId,
        planId = planFirestoreId,
        customerName = "",
        planName = planName,
        startDate = FirebaseDateUtils.toIso(startDate),
        endDate = FirebaseDateUtils.toIso(expiryDate),
        duration = 1,
        amount = totalAmount,
        paidAmount = paidAmount,
        pendingAmount = pendingAmount,
        discount = 0.0,
        finalAmount = totalAmount,
        paymentStatus = paymentStatus.name.lowercase(),
        membershipStatus = status.name.lowercase(),
        invoiceId = ""
    )
}

fun FirebaseMembership.toEntity(
    existingId: Long = 0,
    localCustomerId: Long,
    localPlanId: Long
): Membership {
    val statusEnum = when (membershipStatus.lowercase()) {
        "active" -> MembershipStatus.ACTIVE
        "expired" -> MembershipStatus.EXPIRED
        "expiring_soon", "expiringsoon" -> MembershipStatus.EXPIRING_SOON
        "inactive" -> MembershipStatus.INACTIVE
        "pending_payment" -> MembershipStatus.PENDING_PAYMENT
        else -> MembershipStatus.ACTIVE
    }

    val payStatusEnum = when (paymentStatus.lowercase()) {
        "paid" -> PaymentStatus.PAID
        "partial", "partially_paid" -> PaymentStatus.PARTIALLY_PAID
        "pending", "unpaid" -> PaymentStatus.PENDING
        "overdue" -> PaymentStatus.OVERDUE
        else -> PaymentStatus.PAID
    }

    return Membership(
        id = existingId,
        firestoreId = id ?: "",
        customerId = localCustomerId,
        planId = localPlanId,
        planName = planName,
        startDate = FirebaseDateUtils.fromIso(startDate),
        expiryDate = FirebaseDateUtils.fromIso(endDate),
        totalAmount = if (finalAmount > 0) finalAmount else amount,
        paidAmount = paidAmount,
        pendingAmount = pendingAmount,
        paymentStatus = payStatusEnum,
        status = statusEnum,
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun Payment.toFirebaseDto(gymId: String, customerFirestoreId: String, membershipFirestoreId: String?): FirebasePayment {
    return FirebasePayment(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        customerId = customerFirestoreId,
        membershipId = membershipFirestoreId,
        customerName = "",
        amount = amount,
        paymentMethod = paymentMethod.name.lowercase(),
        paymentStatus = paymentStatus.name.lowercase(),
        transactionReference = transactionRef,
        invoiceId = "",
        paymentDate = FirebaseDateUtils.toIso(paymentDate),
        dueDate = null,
        notes = notes
    )
}

fun FirebasePayment.toEntity(
    existingId: Long = 0,
    localCustomerId: Long,
    localMembershipId: Long
): Payment {
    val methodEnum = when (paymentMethod.lowercase()) {
        "cash" -> PaymentMethod.CASH
        "upi" -> PaymentMethod.UPI
        "card", "credit_card", "debit_card", "credit_debit_card" -> PaymentMethod.CREDIT_DEBIT_CARD
        "bank_transfer", "bank", "net_banking" -> PaymentMethod.NET_BANKING
        "cheque" -> PaymentMethod.CHEQUE
        else -> PaymentMethod.CASH
    }

    val statusEnum = when (paymentStatus.lowercase()) {
        "paid" -> PaymentStatus.PAID
        "partial", "partially_paid" -> PaymentStatus.PARTIALLY_PAID
        "pending", "unpaid" -> PaymentStatus.PENDING
        "overdue" -> PaymentStatus.OVERDUE
        else -> PaymentStatus.PAID
    }

    return Payment(
        id = existingId,
        firestoreId = id ?: "",
        customerId = localCustomerId,
        membershipId = localMembershipId,
        amount = amount,
        paymentDate = FirebaseDateUtils.fromIso(paymentDate),
        paymentMethod = methodEnum,
        paymentStatus = statusEnum,
        transactionRef = transactionReference.orEmpty(),
        notes = notes.orEmpty(),
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun Invoice.toFirebaseDto(gymId: String, customerFirestoreId: String, membershipFirestoreId: String?): FirebaseInvoice {
    return FirebaseInvoice(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        customerId = customerFirestoreId,
        membershipId = membershipFirestoreId,
        invoiceNumber = invoiceNumber,
        customerName = customerName,
        mobile = customerMobile,
        planName = planName,
        invoiceDate = FirebaseDateUtils.toIso(generatedDate),
        dueDate = FirebaseDateUtils.toIso(expiryDate),
        amount = subtotal,
        paidAmount = paidAmount,
        pendingAmount = pendingAmount,
        discount = 0.0,
        subtotal = subtotal,
        totalAmount = totalAmount,
        paymentStatus = paymentStatus.name.lowercase(),
        paymentMethod = paymentMethod.name.lowercase(),
        invoiceTemplate = templateId,
        signatureStyleId = "signature_1",
        pdfUrl = ""
    )
}

fun FirebaseInvoice.toEntity(
    existingId: Long = 0,
    localCustomerId: Long,
    localMembershipId: Long
): Invoice {
    val methodEnum = when (paymentMethod.lowercase()) {
        "cash" -> PaymentMethod.CASH
        "upi" -> PaymentMethod.UPI
        "card", "credit_card", "debit_card", "credit_debit_card" -> PaymentMethod.CREDIT_DEBIT_CARD
        "bank_transfer", "bank", "net_banking" -> PaymentMethod.NET_BANKING
        "cheque" -> PaymentMethod.CHEQUE
        else -> PaymentMethod.CASH
    }

    val statusEnum = when (paymentStatus.lowercase()) {
        "paid" -> PaymentStatus.PAID
        "partial", "partially_paid" -> PaymentStatus.PARTIALLY_PAID
        "pending", "unpaid" -> PaymentStatus.PENDING
        "overdue" -> PaymentStatus.OVERDUE
        else -> PaymentStatus.PAID
    }

    return Invoice(
        id = existingId,
        firestoreId = id ?: "",
        invoiceNumber = invoiceNumber,
        customerId = localCustomerId,
        membershipId = localMembershipId,
        customerName = customerName,
        customerMobile = mobile.orEmpty(),
        planName = planName.orEmpty(),
        durationText = "1 Month",
        startDate = FirebaseDateUtils.fromIso(invoiceDate),
        expiryDate = dueDate?.let { FirebaseDateUtils.fromIso(it) } ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000),
        subtotal = if (subtotal > 0) subtotal else amount,
        taxGst = 0.0,
        totalAmount = if (totalAmount > 0) totalAmount else amount,
        paidAmount = paidAmount,
        pendingAmount = pendingAmount,
        paymentMethod = methodEnum,
        paymentStatus = statusEnum,
        templateId = invoiceTemplate.ifBlank { "modern_clean" },
        generatedDate = FirebaseDateUtils.fromIso(invoiceDate),
        isSentToCustomer = false,
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun Expense.toFirebaseDto(gymId: String): FirebaseExpense {
    return FirebaseExpense(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        title = title,
        category = category,
        description = notes,
        amount = amount,
        paymentMethod = paymentMethod.name.lowercase(),
        expenseDate = FirebaseDateUtils.toIso(expenseDate)
    )
}

fun FirebaseExpense.toEntity(existingId: Long = 0): Expense {
    val methodEnum = when (paymentMethod.lowercase()) {
        "cash" -> PaymentMethod.CASH
        "upi" -> PaymentMethod.UPI
        "card", "credit_card", "debit_card", "credit_debit_card" -> PaymentMethod.CREDIT_DEBIT_CARD
        "bank_transfer", "bank", "net_banking" -> PaymentMethod.NET_BANKING
        "cheque" -> PaymentMethod.CHEQUE
        else -> PaymentMethod.CASH
    }

    return Expense(
        id = existingId,
        firestoreId = id ?: "",
        title = title,
        amount = amount,
        category = category,
        expenseDate = FirebaseDateUtils.fromIso(expenseDate),
        paymentMethod = methodEnum,
        notes = description.orEmpty(),
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun AttendanceRecord.toFirebaseDto(gymId: String, customerFirestoreId: String): FirebaseAttendance {
    return FirebaseAttendance(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        customerId = customerFirestoreId,
        customerName = customerName,
        attendanceDate = date,
        checkInTime = FirebaseDateUtils.toIso(checkInTime),
        checkOutTime = checkOutTime?.let { FirebaseDateUtils.toIso(it) },
        status = status
    )
}

fun FirebaseAttendance.toEntity(existingId: Long = 0, localCustomerId: Long): AttendanceRecord {
    return AttendanceRecord(
        id = existingId,
        firestoreId = id ?: "",
        customerId = localCustomerId,
        customerName = customerName,
        date = attendanceDate.ifBlank { FirebaseDateUtils.toDateString(System.currentTimeMillis()) },
        checkInTime = checkInTime?.let { FirebaseDateUtils.fromIso(it) } ?: System.currentTimeMillis(),
        checkOutTime = checkOutTime?.let { FirebaseDateUtils.fromIso(it) },
        status = status,
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun NotificationItem.toFirebaseDto(gymId: String): FirebaseNotification {
    return FirebaseNotification(
        id = if (firestoreId.isNotBlank()) firestoreId else null,
        gymId = gymId,
        title = title,
        message = message,
        type = type,
        isRead = isRead,
        actionUrl = ""
    )
}

fun FirebaseNotification.toEntity(existingId: Long = 0): NotificationItem {
    return NotificationItem(
        id = existingId,
        firestoreId = id ?: "",
        title = title,
        message = message,
        type = type,
        timestamp = FirebaseDateUtils.fromTimestamp(createdAt),
        isRead = isRead,
        syncStatus = SyncStatus.SYNCED,
        lastSyncedAt = System.currentTimeMillis()
    )
}

fun GymSetting.toFirebaseGym(): FirebaseGym {
    return FirebaseGym(
        name = gymName,
        ownerName = ownerSignatureName,
        phone = gymPhone,
        email = gymEmail,
        address = gymAddress,
        city = gymCity,
        state = "",
        country = "India",
        logoUrl = "",
        signatureData = "",
        signatureStyleId = ownerSignatureStyleId,
        locationQrUrl = "",
        gymLocationUrl = gymLocationUrl,
        currencySymbol = currencySymbol,
        tagline = gymTagline,
        gstin = gymGstin
    )
}

fun GymSetting.toFirebaseAppSettings(gymId: String): FirebaseAppSettings {
    return FirebaseAppSettings(
        gymId = gymId,
        biometricEnabled = isBiometricEnabled,
        biometricTimeout = biometricAutoLockMinutes,
        theme = appTheme,
        language = appLanguage,
        notificationEnabled = true,
        autoBackupEnabled = true,
        soundEffectsEnabled = true,
        hapticFeedbackEnabled = true,
        selectedInvoiceTemplate = activeInvoiceTemplateId,
        signatureSvgPath = ""
    )
}
