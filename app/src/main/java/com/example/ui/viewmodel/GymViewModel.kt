package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.GymRepository
import com.example.data.local.entity.AttendanceRecord
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Expense
import com.example.data.local.entity.GymSetting
import com.example.data.local.entity.Invoice
import com.example.data.local.entity.Membership
import com.example.data.local.entity.MembershipPlan
import com.example.data.local.entity.NotificationItem
import com.example.data.local.entity.Payment
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.InvoiceTemplateConfig
import com.example.data.model.InvoiceTemplates
import com.example.data.model.MembershipStatus
import com.example.data.model.MonthlyRevenuePoint
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PlanRevenueStat
import com.example.data.model.ReportPeriod
import com.example.data.firebase.AuthState
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirebaseConfig
import com.example.data.firebase.FirebaseGymManager
import com.example.data.firebase.UserSession
import com.example.util.AppSecurityManager
import com.example.util.InvoiceImageExporter
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CustomerWithMembership(
    val customer: Customer,
    val activeMembership: Membership?,
    val latestMembership: Membership?,
    val allMemberships: List<Membership> = emptyList(),
    val totalPaid: Double = 0.0,
    val totalPending: Double = 0.0
)

data class DashboardKpi(
    val totalMembers: Int = 0,
    val activeMembers: Int = 0,
    val inactiveMembers: Int = 0,
    val expiringSoonCount: Int = 0,
    val pendingPaymentCount: Int = 0,
    val todayCollections: Double = 0.0,
    val currentMonthRevenue: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val totalPendingDues: Double = 0.0
)

data class ReportAnalyticsState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val customStartDate: Long = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
    val customEndDate: Long = System.currentTimeMillis(),
    val periodRevenue: Double = 0.0,
    val periodPaidAmount: Double = 0.0,
    val periodPendingAmount: Double = 0.0,
    val newMembersCount: Int = 0,
    val renewedMembersCount: Int = 0,
    val planBreakdown: List<PlanRevenueStat> = emptyList(),
    val monthlyTrend: List<MonthlyRevenuePoint> = emptyList()
)

class GymViewModel(application: Application) : AndroidViewModel(application) {

    val firebaseManager: FirebaseGymManager = FirebaseGymManager.getInstance(application)
    val authManager: FirebaseAuthService = firebaseManager.auth
    val networkMonitor = NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val repository: GymRepository = GymRepository(
        AppDatabase.getInstance(application),
        firebaseManager
    )

    // Auth State
    val authState: StateFlow<AuthState> = authManager.authState

    // Cloud Sync State
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("Firebase Connected")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Direct database flows
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlans: StateFlow<List<MembershipPlan>> = repository.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlans: StateFlow<List<MembershipPlan>> = repository.activePlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemberships: StateFlow<List<Membership>> = repository.allMemberships
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gymSettings: StateFlow<GymSetting> = repository.settings
        .combine(MutableStateFlow(GymSetting())) { setting, _ -> setting ?: GymSetting() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GymSetting())

    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Filter & Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("ALL") // ALL, ACTIVE, INACTIVE, EXPIRING, EXPIRED, PENDING
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _sortBy = MutableStateFlow("DATE_DESC") // DATE_DESC, EXPIRY_ASC, NAME_ASC, AMOUNT_DESC
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    // App Preferences State (Theme & Language)
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _appTheme = MutableStateFlow(AppThemeMode.CLASSIC)
    val appTheme: StateFlow<AppThemeMode> = _appTheme.asStateFlow()

    // Reports Filter State
    private val _reportAnalytics = MutableStateFlow(ReportAnalyticsState())
    val reportAnalytics: StateFlow<ReportAnalyticsState> = _reportAnalytics.asStateFlow()

    // App Lock & Biometric Security Manager
    val securityManager = AppSecurityManager.getInstance(application)

    // Synchronously initialized on cold-start based on persisted security settings
    private val _isAppLocked = MutableStateFlow(securityManager.isLockRequiredOnLaunch())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Transient UI Message / Toast
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Observe auth state: when authenticated, trigger cloud sync
        viewModelScope.launch {
            authManager.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    syncWithCloud(state.session.gymId)
                }
            }
        }

        // Observe network state: when coming online, trigger pending sync
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    val currentGymId = getCurrentGymId()
                    if (currentGymId.isNotBlank()) {
                        syncWithCloud(currentGymId)
                    }
                }
            }
        }

        // Observe settings to update active UI theme, language preferences and biometric sync
        viewModelScope.launch {
            gymSettings.collect { settings ->
                _appTheme.value = AppThemeMode.fromString(settings.appTheme)
                try {
                    _appLanguage.value = AppLanguage.valueOf(settings.appLanguage)
                } catch (_: Exception) {
                    _appLanguage.value = AppLanguage.ENGLISH
                }

                securityManager.syncFromGymSetting(settings)
                if (securityManager.isLockRequiredOnLaunch() && !securityManager.isSessionAuthenticated()) {
                    _isAppLocked.value = true
                }
            }
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun getCurrentUserId(): String {
        return authManager.getCurrentUserId() ?: "usr_owner_default"
    }

    fun getCurrentGymId(): String {
        return firebaseManager.getCurrentGymId()
    }

    fun getCurrentUserSession(): UserSession? {
        return authManager.getCurrentUser()
    }

    // -------------------------------------------------------------
    // FIREBASE AUTHENTICATION ACTIONS
    // -------------------------------------------------------------
    fun signIn(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authManager.signInWithEmail(email, pass)
            if (res.isSuccess) {
                val session = res.getOrNull()!!
                showToast("Welcome back, ${session.name}!")
                syncWithCloud(session.gymId)
                onResult(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Login failed"
                showToast(err)
                onResult(false, err)
            }
        }
    }

    fun signUp(
        email: String,
        pass: String,
        ownerName: String,
        gymName: String,
        phone: String,
        gymAddress: String = "",
        city: String = "",
        state: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = authManager.signUpWithEmail(
                email = email,
                password = pass,
                ownerName = ownerName,
                gymName = gymName,
                phone = phone,
                address = gymAddress,
                city = city
            )
            if (res.isSuccess) {
                val session = res.getOrNull()!!
                val current = gymSettings.value
                repository.updateSettings(
                    current.copy(
                        gymName = session.gymName,
                        gymEmail = session.email,
                        gymPhone = phone.trim(),
                        gymAddress = gymAddress.trim(),
                        gymCity = city.trim()
                    ),
                    session.gymId
                )
                showToast("Account created successfully! Welcome to My Fitness ERP")
                syncWithCloud(session.gymId)
                onResult(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Registration failed"
                showToast(err)
                onResult(false, err)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            showToast("Signed out of My Fitness ERP")
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = authManager.resetPasswordForEmail(email)
            if (res.isSuccess) {
                showToast("Password reset instructions sent to $email")
                onResult(true, null)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Failed to send reset email"
                showToast(err)
                onResult(false, err)
            }
        }
    }

    // -------------------------------------------------------------
    // CLOUD SYNC & SCOPED OWNER LIFECYCLE
    // -------------------------------------------------------------
    fun onOwnerAuthenticated(gymId: String) {
        if (gymId.isNotBlank()) {
            syncWithCloud(gymId)
        }
    }

    fun syncWithCloud(gymId: String = getCurrentGymId()) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Syncing with Firebase Cloud..."
            val result = repository.syncWithFirebase(gymId)
            _isSyncing.value = false
            if (result.isSuccess) {
                _syncStatus.value = "Synced with Firebase Cloud"
            } else {
                _syncStatus.value = "Working with Local Cache (Offline)"
            }
        }
    }

    // -------------------------------------------------------------
    // RESET ALL DATA: PERMANENT ZERO-STATE WIPE
    // -------------------------------------------------------------
    fun resetAllData(confirmationText: String, onComplete: (Boolean) -> Unit) {
        if (confirmationText.trim() != "RESET") {
            showToast("Please type 'RESET' in capital letters to confirm.")
            onComplete(false)
            return
        }

        viewModelScope.launch {
            val gymId = getCurrentGymId()
            _isSyncing.value = true
            val result = repository.resetAllGymData(gymId)
            _isSyncing.value = false
            if (result.isSuccess) {
                showToast("All gym data reset to zero. Ready for real member entry.")
                onComplete(true)
            } else {
                showToast("Reset failed: ${result.exceptionOrNull()?.message}")
                onComplete(false)
            }
        }
    }

    // -------------------------------------------------------------
    // DASHBOARD KPIS
    // -------------------------------------------------------------
    val dashboardKpi: StateFlow<DashboardKpi> = combine(
        allCustomers,
        allMemberships,
        allPayments
    ) { customers, memberships, payments ->
        val now = System.currentTimeMillis()
        val threeDaysLater = now + (3L * 24 * 60 * 60 * 1000)

        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        var activeCount = 0
        var inactiveCount = 0
        var expiringCount = 0
        var pendingPaymentCount = 0
        var totalPendingDues = 0.0

        val latestByCustomer = memberships.groupBy { it.customerId }
            .mapValues { (_, list) -> list.maxByOrNull { it.startDate } }

        for (cust in customers) {
            val latest = latestByCustomer[cust.id]
            if (latest != null && latest.status == MembershipStatus.ACTIVE && latest.expiryDate >= now) {
                activeCount++
                if (latest.expiryDate <= threeDaysLater) {
                    expiringCount++
                }
            } else {
                inactiveCount++
            }
        }

        for (m in memberships) {
            if (m.pendingAmount > 0.0) {
                pendingPaymentCount++
                totalPendingDues += m.pendingAmount
            }
        }

        var todayCollections = 0.0
        var monthRevenue = 0.0
        var totalRevenue = 0.0

        for (p in payments) {
            totalRevenue += p.amount
            if (p.paymentDate >= todayStart) {
                todayCollections += p.amount
            }
            if (p.paymentDate >= monthStart) {
                monthRevenue += p.amount
            }
        }

        DashboardKpi(
            totalMembers = customers.size,
            activeMembers = activeCount,
            inactiveMembers = inactiveCount,
            expiringSoonCount = expiringCount,
            pendingPaymentCount = pendingPaymentCount,
            todayCollections = todayCollections,
            currentMonthRevenue = monthRevenue,
            totalRevenue = totalRevenue,
            totalPendingDues = totalPendingDues
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardKpi())

    // Combined Customer with Membership State
    val customerDirectory: StateFlow<List<CustomerWithMembership>> = combine(
        allCustomers,
        allMemberships,
        allPayments,
        _searchQuery,
        _selectedStatusFilter,
        _sortBy
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val customers = args[0] as List<Customer>
        @Suppress("UNCHECKED_CAST")
        val memberships = args[1] as List<Membership>
        @Suppress("UNCHECKED_CAST")
        val payments = args[2] as List<Payment>
        val query = args[3] as String
        val filter = args[4] as String
        val sort = args[5] as String

        val now = System.currentTimeMillis()
        val threeDaysLater = now + (3L * 24 * 60 * 60 * 1000)

        val memByCustomer = memberships.groupBy { it.customerId }
        val payByCustomer = payments.groupBy { it.customerId }

        val items = customers.map { customer ->
            val custMemberships = memByCustomer[customer.id] ?: emptyList()
            val custPayments = payByCustomer[customer.id] ?: emptyList()

            val activeMembership = custMemberships
                .filter { it.status == MembershipStatus.ACTIVE && it.expiryDate >= now }
                .maxByOrNull { it.expiryDate }

            val latestMembership = custMemberships.maxByOrNull { it.startDate }

            val totalPaid = custPayments.sumOf { it.amount }
            val totalPending = custMemberships.sumOf { it.pendingAmount }

            CustomerWithMembership(
                customer = customer,
                activeMembership = activeMembership,
                latestMembership = latestMembership,
                allMemberships = custMemberships.sortedByDescending { it.startDate },
                totalPaid = totalPaid,
                totalPending = totalPending
            )
        }

        // Apply Search Filter
        val searched = if (query.isBlank()) {
            items
        } else {
            val q = query.trim().lowercase()
            items.filter {
                it.customer.name.lowercase().contains(q) ||
                it.customer.mobileNumber.contains(q) ||
                it.customer.email.lowercase().contains(q) ||
                (it.latestMembership?.planName?.lowercase()?.contains(q) == true)
            }
        }

        // Apply Status Filter
        val filtered = when (filter) {
            "ACTIVE" -> searched.filter { it.activeMembership != null }
            "INACTIVE" -> searched.filter { it.activeMembership == null }
            "EXPIRING" -> searched.filter {
                val exp = it.activeMembership?.expiryDate ?: 0L
                exp in now..threeDaysLater
            }
            "EXPIRED" -> searched.filter {
                val exp = it.latestMembership?.expiryDate ?: 0L
                exp > 0 && exp < now
            }
            "PENDING" -> searched.filter { it.totalPending > 0.0 }
            else -> searched
        }

        // Apply Sorting
        when (sort) {
            "NAME_ASC" -> filtered.sortedBy { it.customer.name.lowercase() }
            "EXPIRY_ASC" -> filtered.sortedBy { it.latestMembership?.expiryDate ?: Long.MAX_VALUE }
            "AMOUNT_DESC" -> filtered.sortedByDescending { it.totalPaid }
            else -> filtered.sortedByDescending { it.customer.registeredDate }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithMembership: StateFlow<List<CustomerWithMembership>> get() = customerDirectory

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _selectedStatusFilter.value = filter
    }

    fun setSelectedStatusFilter(filter: String) {
        _selectedStatusFilter.value = filter
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun setAppTheme(theme: AppThemeMode) {
        _appTheme.value = theme
        viewModelScope.launch {
            val current = gymSettings.value
            val gymId = getCurrentGymId()
            repository.updateSettings(current.copy(appTheme = theme.name), gymId)
        }
    }

    fun setTheme(theme: AppThemeMode) = setAppTheme(theme)

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        viewModelScope.launch {
            val current = gymSettings.value
            val gymId = getCurrentGymId()
            repository.updateSettings(current.copy(appLanguage = language.name), gymId)
        }
    }

    fun setLanguage(language: AppLanguage) = setAppLanguage(language)

    // -------------------------------------------------------------
    // MEMBER ACTIONS (REAL OPERATIONS)
    // -------------------------------------------------------------
    fun registerNewMember(
        name: String,
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
        templateId: String,
        onSuccess: (Long, Long, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val gymId = getCurrentGymId()
                val (custId, memId, invNum) = repository.registerMemberWithMembership(
                    gymId = gymId,
                    customerName = name,
                    mobileNumber = mobileNumber,
                    email = email,
                    gender = gender,
                    address = address,
                    emergencyContact = emergencyContact,
                    isMobileVerified = isMobileVerified,
                    plan = plan,
                    startDate = startDate,
                    expiryDate = expiryDate,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    transactionRef = transactionRef,
                    templateId = templateId
                )
                showToast("Member registered! Invoice #$invNum created")
                onSuccess(custId, memId, invNum)
            } catch (e: Exception) {
                showToast("Registration failed: ${e.localizedMessage}")
            }
        }
    }

    // Renew Membership
    fun renewMember(
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
        templateId: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val gymId = getCurrentGymId()
                val (_, invNum) = repository.renewMembership(
                    gymId = gymId,
                    customerId = customerId,
                    customerName = customerName,
                    customerMobile = customerMobile,
                    plan = plan,
                    startDate = startDate,
                    expiryDate = expiryDate,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    transactionRef = transactionRef,
                    templateId = templateId
                )
                showToast("Membership renewed! Invoice #$invNum created")
                onSuccess(invNum)
            } catch (e: Exception) {
                showToast("Renewal failed: ${e.localizedMessage}")
            }
        }
    }

    // Record Pending Due Collection
    fun recordPendingPayment(
        membershipId: Long,
        customerId: Long,
        amount: Double,
        paymentMethod: PaymentMethod,
        transactionRef: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val gymId = getCurrentGymId()
                repository.recordPendingPayment(
                    gymId = gymId,
                    membershipId = membershipId,
                    customerId = customerId,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    transactionRef = transactionRef,
                    notes = notes
                )
                showToast("Payment recorded successfully")
                onSuccess()
            } catch (e: Exception) {
                showToast("Failed to record payment: ${e.localizedMessage}")
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.updateCustomer(customer, gymId)
            showToast("Customer updated")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.deleteCustomer(customer, gymId)
            showToast("Member removed")
        }
    }

    fun addMembershipPlan(plan: MembershipPlan) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.insertPlan(plan, gymId)
            showToast("Plan added: ${plan.name}")
        }
    }

    fun updateMembershipPlan(plan: MembershipPlan) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.updatePlan(plan, gymId)
            showToast("Plan updated")
        }
    }

    fun deleteMembershipPlan(plan: MembershipPlan) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.deletePlan(plan, gymId)
            showToast("Plan deleted")
        }
    }

    fun addExpense(
        title: String,
        category: String,
        amount: Double,
        expenseDate: Long,
        paymentMethod: PaymentMethod,
        notes: String
    ) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            val expense = Expense(
                title = title,
                category = category,
                amount = amount,
                expenseDate = expenseDate,
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.insertExpense(expense, gymId)
            showToast("Expense added")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.deleteExpense(expense, gymId)
            showToast("Expense deleted")
        }
    }

    fun recordAttendance(customerId: Long, customerName: String) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.recordAttendance(gymId, customerId, customerName)
            showToast("Check-in recorded for $customerName")
        }
    }

    fun deleteAttendance(attendance: AttendanceRecord) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.deleteAttendance(attendance, gymId)
            showToast("Attendance record removed")
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.markNotificationRead(id, gymId)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.markAllNotificationsRead(gymId)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.clearNotifications(gymId)
        }
    }

    fun updateGymSettings(settings: GymSetting) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.updateSettings(settings, gymId)
            showToast("Settings saved successfully")
        }
    }

    fun setActiveInvoiceTemplate(templateId: String) {
        viewModelScope.launch {
            val current = gymSettings.value
            val gymId = getCurrentGymId()
            repository.updateSettings(current.copy(activeInvoiceTemplateId = templateId), gymId)
            showToast("Invoice template updated")
        }
    }

    fun selectActiveInvoiceTemplate(templateId: String) {
        setActiveInvoiceTemplate(templateId)
    }

    fun markInvoiceSent(invoiceId: Long) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.markInvoiceSent(invoiceId, gymId)
        }
    }

    // Reports calculation
    fun setReportPeriod(period: ReportPeriod, start: Long = 0L, end: Long = 0L) {
        val now = System.currentTimeMillis()
        val s = if (start > 0) start else when (period) {
            ReportPeriod.DAILY -> {
                val c = Calendar.getInstance()
                c.set(Calendar.HOUR_OF_DAY, 0)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                c.timeInMillis
            }
            ReportPeriod.MONTHLY -> {
                val c = Calendar.getInstance()
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.set(Calendar.HOUR_OF_DAY, 0)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                c.timeInMillis
            }
            ReportPeriod.LAST_3_MONTHS -> now - (90L * 24 * 60 * 60 * 1000)
            ReportPeriod.LAST_6_MONTHS -> now - (180L * 24 * 60 * 60 * 1000)
            ReportPeriod.YEARLY -> {
                val c = Calendar.getInstance()
                c.set(Calendar.DAY_OF_YEAR, 1)
                c.set(Calendar.HOUR_OF_DAY, 0)
                c.set(Calendar.MINUTE, 0)
                c.set(Calendar.SECOND, 0)
                c.timeInMillis
            }
            ReportPeriod.CUSTOM -> _reportAnalytics.value.customStartDate
        }
        val e = if (end > 0) end else now

        calculateReports(period, s, e)
    }

    fun computeReports(period: ReportPeriod, start: Long = 0L, end: Long = 0L) {
        setReportPeriod(period, start, end)
    }

    private fun calculateReports(period: ReportPeriod, start: Long, end: Long) {
        val payments = allPayments.value.filter { it.paymentDate in start..end }
        val memberships = allMemberships.value.filter { it.startDate in start..end }
        val plans = allPlans.value

        val totalCollected = payments.sumOf { it.amount }
        val totalPending = memberships.sumOf { it.pendingAmount }
        val totalRevenue = totalCollected + totalPending

        val planStats = plans.map { plan ->
            val count = memberships.count { it.planId == plan.id }
            val rev = memberships.filter { it.planId == plan.id }.sumOf { it.totalAmount }
            val pct = if (totalRevenue > 0) ((rev / totalRevenue) * 100).toFloat() else 0f
            PlanRevenueStat(
                planName = plan.name,
                count = count,
                revenue = rev,
                percentage = pct
            )
        }.filter { it.count > 0 }

        val monthlyTrend = mutableListOf<MonthlyRevenuePoint>()
        val cal = Calendar.getInstance()
        for (i in 5 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -i)
            val monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
            val yr = cal.get(Calendar.YEAR)
            val mo = cal.get(Calendar.MONTH)

            val monthPayments = allPayments.value.filter { p ->
                val pCal = Calendar.getInstance()
                pCal.timeInMillis = p.paymentDate
                pCal.get(Calendar.YEAR) == yr && pCal.get(Calendar.MONTH) == mo
            }
            val monthRev = monthPayments.sumOf { it.amount }
            val memberCount = allMemberships.value.count { m ->
                val mCal = Calendar.getInstance()
                mCal.timeInMillis = m.startDate
                mCal.get(Calendar.YEAR) == yr && mCal.get(Calendar.MONTH) == mo
            }

            monthlyTrend.add(MonthlyRevenuePoint(monthLabel, monthRev, memberCount))
        }

        _reportAnalytics.value = ReportAnalyticsState(
            selectedPeriod = period,
            customStartDate = start,
            customEndDate = end,
            periodRevenue = totalRevenue,
            periodPaidAmount = totalCollected,
            periodPendingAmount = totalPending,
            newMembersCount = memberships.size,
            renewedMembersCount = memberships.count { it.planName.contains("Renew", ignoreCase = true) },
            planBreakdown = planStats,
            monthlyTrend = monthlyTrend
        )
    }

    // Share Invoice via Image or WhatsApp
    fun shareInvoice(context: Context, invoice: Invoice, gymSetting: GymSetting, onShareComplete: () -> Unit = {}) {
        val template = InvoiceTemplates.getTemplateById(invoice.templateId.ifEmpty { gymSetting.activeInvoiceTemplateId })
        InvoiceImageExporter.shareInvoiceImage(context, invoice, gymSetting, template)
        markInvoiceSent(invoice.id)
        onShareComplete()
    }

    fun shareInvoiceImage(
        context: Context,
        invoice: Invoice,
        gymSetting: GymSetting,
        templateConfig: InvoiceTemplateConfig
    ) {
        InvoiceImageExporter.shareInvoiceImage(context, invoice, gymSetting, templateConfig)
        markInvoiceSent(invoice.id)
    }

    fun downloadInvoiceImage(
        context: Context,
        invoice: Invoice,
        gymSetting: GymSetting,
        templateConfig: InvoiceTemplateConfig
    ) {
        viewModelScope.launch {
            val bitmap = InvoiceImageExporter.renderInvoiceToBitmap(context, invoice, gymSetting, templateConfig)
            val res = InvoiceImageExporter.saveInvoiceToGallery(context, bitmap, invoice.invoiceNumber)
            if (res.isSuccess) {
                showToast("Invoice saved to Pictures/MyFitness_Invoices")
            } else {
                showToast("Failed to save invoice: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun togglePlanActive(plan: MembershipPlan) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.updatePlan(plan.copy(isActive = !plan.isActive), gymId)
        }
    }

    fun createPlan(plan: MembershipPlan) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.insertPlan(plan, gymId)
            showToast("Plan '${plan.name}' created")
        }
    }

    fun updatePlan(plan: MembershipPlan) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.updatePlan(plan, gymId)
            showToast("Plan '${plan.name}' updated")
        }
    }

    fun deletePlan(plan: MembershipPlan) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            repository.deletePlan(plan, gymId)
            showToast("Plan deleted")
        }
    }

    fun updateOwnerSignature(name: String, styleId: String) {
        viewModelScope.launch {
            val current = gymSettings.value
            val gymId = getCurrentGymId()
            repository.updateSettings(
                current.copy(
                    ownerSignatureName = name,
                    ownerSignatureStyleId = styleId
                ),
                gymId
            )
            showToast("Digital signature saved")
        }
    }

    fun updateGymLocation(locationUrl: String) {
        viewModelScope.launch {
            val current = gymSettings.value
            val gymId = getCurrentGymId()
            repository.updateSettings(
                current.copy(gymLocationUrl = locationUrl),
                gymId
            )
            showToast("Gym location saved")
        }
    }

    fun markAllNotificationsAsRead() {
        markAllNotificationsRead()
    }

    fun collectPendingPayment(
        membershipId: Long,
        customerId: Long,
        amount: Double,
        paymentMethod: PaymentMethod,
        transactionRef: String = "",
        notes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        recordPendingPayment(
            membershipId = membershipId,
            customerId = customerId,
            amount = amount,
            paymentMethod = paymentMethod,
            transactionRef = transactionRef,
            notes = notes,
            onSuccess = onSuccess
        )
    }

    // Firebase Cloud Configuration
    fun saveFirebaseConfig(projectId: String, apiKey: String) {
        val app = getApplication<Application>()
        FirebaseConfig.saveConfig(app, projectId, apiKey)
        showToast("Firebase configuration updated")
        syncWithCloud()
    }

    fun saveFirebaseConfig(projectId: String, appId: String, apiKey: String) {
        saveFirebaseConfig(projectId, apiKey)
    }

    // -------------------------------------------------------------
    // BIOMETRIC & APP LOCK SECURITY
    // -------------------------------------------------------------
    fun lockApp() {
        securityManager.lockSession()
        _isAppLocked.value = true
    }

    fun unlockApp() {
        securityManager.authenticateSession()
        _isAppLocked.value = false
    }

    fun onAppForegrounded() {
        if (securityManager.shouldLockOnForeground()) {
            _isAppLocked.value = true
        }
    }

    fun onAppBackgrounded() {
        securityManager.onAppBackgrounded()
    }

    fun toggleBiometricSecurity(enabled: Boolean) {
        viewModelScope.launch {
            securityManager.setBiometricEnabled(enabled)
            val gymId = getCurrentGymId()
            val current = gymSettings.value
            val updated = securityManager.applyToGymSetting(current)
            repository.updateSettings(updated, gymId)
            if (!securityManager.isAppLockEnabled()) {
                _isAppLocked.value = false
                showToast("App Lock disabled")
            } else {
                if (enabled) {
                    securityManager.authenticateSession()
                    showToast("Biometric Lock enabled")
                } else {
                    showToast("Biometric Lock disabled")
                }
            }
        }
    }

    fun togglePinSecurity(enabled: Boolean) {
        viewModelScope.launch {
            securityManager.setPinEnabled(enabled)
            val gymId = getCurrentGymId()
            val current = gymSettings.value
            val updated = securityManager.applyToGymSetting(current)
            repository.updateSettings(updated, gymId)
            if (!securityManager.isAppLockEnabled()) {
                _isAppLocked.value = false
                showToast("Passcode lock disabled")
            } else {
                showToast(if (enabled) "Passcode lock enabled" else "Passcode lock disabled")
            }
        }
    }

    fun setBiometricAutoLockMinutes(minutes: Int) {
        viewModelScope.launch {
            securityManager.setAutoLockTimeoutMinutes(minutes)
            val gymId = getCurrentGymId()
            val current = gymSettings.value
            val updated = securityManager.applyToGymSetting(current)
            repository.updateSettings(updated, gymId)
            val desc = when (minutes) {
                0 -> "Immediately on exit"
                -1 -> "Never auto-lock"
                1 -> "After 1 minute"
                else -> "After $minutes minutes"
            }
            showToast("Auto-Lock set to: $desc")
        }
    }

    fun setSecurityPin(pin: String) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            val current = gymSettings.value
            if (pin.isBlank()) {
                securityManager.removePin()
                val updated = securityManager.applyToGymSetting(current)
                repository.updateSettings(updated, gymId)
                showToast("Security Passcode removed")
            } else {
                securityManager.setPin(pin)
                val updated = securityManager.applyToGymSetting(current)
                repository.updateSettings(updated, gymId)
                showToast("Security Passcode set & encrypted")
            }
        }
    }

    fun verifySecurityPin(inputPin: String): Boolean {
        return securityManager.verifyPin(inputPin)
    }

    fun toggleOtpVerification(enabled: Boolean) {
        viewModelScope.launch {
            val gymId = getCurrentGymId()
            val current = gymSettings.value
            val updated = current.copy(
                requireOtpForMemberCreation = enabled,
                requireOtpForInvoiceSend = enabled,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSettings(updated, gymId)
            showToast(if (enabled) "Customer Mobile OTP Verification enabled" else "Customer Mobile OTP Verification disabled")
        }
    }
}
