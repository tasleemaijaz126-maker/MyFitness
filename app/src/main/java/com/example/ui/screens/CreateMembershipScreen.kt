package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.MembershipPlan
import com.example.data.model.InvoiceTemplates
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.ui.components.OtpVerificationDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleVip
import com.example.ui.viewmodel.GymViewModel
import com.example.util.PhoneAuthHelper
import com.example.util.PhoneUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMembershipScreen(
    viewModel: GymViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToInvoices: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.activePlans.collectAsStateWithLifecycle()
    val settings by viewModel.gymSettings.collectAsStateWithLifecycle()
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()

    // Form State: Customer Details
    var customerName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var address by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var isMobileVerified by remember { mutableStateOf(false) }
    var requireOtpVerification by remember(settings) { mutableStateOf(settings.requireOtpForMemberCreation) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Form State: Plan & Dates
    var selectedPlan by remember(plans) { mutableStateOf(plans.firstOrNull()) }
    val now = System.currentTimeMillis()
    var startDate by remember { mutableLongStateOf(now) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var expiryDate by remember {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.add(Calendar.MONTH, selectedPlan?.durationMonths ?: 1)
        mutableLongStateOf(cal.timeInMillis)
    }

    // Form State: Payment Details
    var totalAmountText by remember(selectedPlan) {
        mutableStateOf((selectedPlan?.price?.toInt() ?: 1200).toString())
    }
    var paidAmountText by remember(selectedPlan) {
        mutableStateOf((selectedPlan?.price?.toInt() ?: 1200).toString())
    }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var isPaymentMethodExpanded by remember { mutableStateOf(false) }
    var transactionRef by remember { mutableStateOf("TXN-${System.currentTimeMillis() % 1000000}") }

    // Invoice Template Selection
    var selectedTemplateId by remember(settings) {
        mutableStateOf(settings.activeInvoiceTemplateId)
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun updateExpiryDate(start: Long, plan: MembershipPlan?) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = start
        if (plan != null && plan.durationMonths > 0) {
            cal.add(Calendar.MONTH, plan.durationMonths)
        } else if (plan != null && plan.durationDays > 0) {
            cal.add(Calendar.DAY_OF_YEAR, plan.durationDays)
        } else {
            cal.add(Calendar.MONTH, 1)
        }
        expiryDate = cal.timeInMillis
    }

    val totalAmt = totalAmountText.toDoubleOrNull() ?: (selectedPlan?.price ?: 0.0)
    val paidAmt = paidAmountText.toDoubleOrNull() ?: totalAmt
    val pendingAmt = (totalAmt - paidAmt).coerceAtLeast(0.0)

    val paymentStatus = when {
        paidAmt >= totalAmt -> PaymentStatus.PAID
        paidAmt > 0 -> PaymentStatus.PARTIALLY_PAID
        else -> PaymentStatus.PENDING
    }

    BackHandler(onBack = onNavigateBack)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_membership_screen"),
        contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = AppStrings.get("create_member", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "New customer registration & instant invoice",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 1: Customer Personal Details
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CrimsonPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Customer Information",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CrimsonPrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_name"),
                        singleLine = true
                    )

                    // Mobile input + OTP Verification Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = {
                                val filtered = it.filter { ch -> ch.isDigit() || ch == '+' }.take(13)
                                mobileNumber = filtered
                                isMobileVerified = false
                            },
                            label = { Text("Mobile Number (10 digits) *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = if (isMobileVerified) EmeraldSuccess else CrimsonPrimary) },
                            trailingIcon = {
                                if (isMobileVerified) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = EmeraldSuccess)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_mobile_number"),
                            singleLine = true
                        )

                        if (isMobileVerified) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF064E3B),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldSuccess))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verified", color = EmeraldSuccess, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        } else if (requireOtpVerification) {
                            Button(
                                onClick = {
                                    val normalized = PhoneUtils.normalizeToE164(mobileNumber)
                                    if (normalized != null) {
                                        showOtpDialog = true
                                    } else {
                                        viewModel.showToast("Please enter a valid 10-digit mobile number")
                                    }
                                },
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("verify_otp_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Verify", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // OTP Verification Requirement Toggle (Optional vs Mandatory)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (requireOtpVerification) AmberAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (requireOtpVerification) AmberAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (requireOtpVerification) Icons.Default.Security else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (requireOtpVerification) AmberAccent else EmeraldSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "OTP Verification",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (requireOtpVerification) "ON: SMS OTP verification required before saving" else "OFF: Direct creation without SMS OTP verification",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (requireOtpVerification) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = requireOtpVerification,
                                onCheckedChange = { requireOtpVerification = it },
                                modifier = Modifier.testTag("switch_require_otp")
                            )
                        }
                    }

                    // Gender Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Gender:", style = MaterialTheme.typography.labelMedium)
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick = { gender = g },
                                label = { Text(g) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Residential Address / Area") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text("Emergency Contact Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Section 2: Membership Plan & Duration
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header: Step 2 & Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(PurpleVip, CrimsonPrimary))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "2",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Membership Plan & Duration",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Selected package & validity timeline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 1. Featured Selected Plan Card (Prominent, High-Contrast ERP Presentation)
                    val currentPlan = selectedPlan
                    if (currentPlan != null) {
                        val durationLabel = when {
                            currentPlan.durationMonths == 1 -> "1 Month"
                            currentPlan.durationMonths == 3 -> "3 Months"
                            currentPlan.durationMonths == 6 -> "6 Months"
                            currentPlan.durationMonths == 12 -> "1 Year"
                            currentPlan.durationMonths > 0 -> "${currentPlan.durationMonths} Months"
                            currentPlan.durationDays > 0 -> "${currentPlan.durationDays} Days"
                            else -> "1 Month"
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    listOf(
                                        PurpleVip.copy(alpha = 0.7f),
                                        CrimsonPrimary.copy(alpha = 0.5f),
                                        AmberAccent.copy(alpha = 0.3f)
                                    )
                                ),
                                width = 1.5.dp
                            ),
                            shadowElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            PurpleVip.copy(alpha = 0.25f),
                                                            CrimsonPrimary.copy(alpha = 0.18f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                tint = PurpleVip,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = PurpleVip.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = currentPlan.category.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp,
                                                            letterSpacing = 0.6.sp
                                                        ),
                                                        color = PurpleVip,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = AmberAccent.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = durationLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = AmberAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = currentPlan.name,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 17.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Price Display
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "₹${currentPlan.price.toInt()}",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 22.sp
                                            ),
                                            color = EmeraldSuccess
                                        )
                                        Text(
                                            text = "Plan Fee",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (currentPlan.description.isNotBlank()) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        thickness = 0.8.dp
                                    )
                                    Text(
                                        text = currentPlan.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    // 2. Selectable Plan Options Carousel / Chips
                    if (plans.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "AVAILABLE PACKAGES (${plans.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap to switch",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurpleVip
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                items(plans, key = { it.id }) { plan ->
                                    val isSelected = selectedPlan?.id == plan.id
                                    val planDuration = when {
                                        plan.durationMonths == 1 -> "1M"
                                        plan.durationMonths == 3 -> "3M"
                                        plan.durationMonths == 6 -> "6M"
                                        plan.durationMonths == 12 -> "1Y"
                                        plan.durationMonths > 0 -> "${plan.durationMonths}M"
                                        plan.durationDays > 0 -> "${plan.durationDays}D"
                                        else -> "1M"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) PurpleVip.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                                        border = if (isSelected) {
                                            CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.linearGradient(listOf(PurpleVip, CrimsonPrimary)),
                                                width = 1.5.dp
                                            )
                                        } else {
                                            CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))),
                                                width = 0.8.dp
                                            )
                                        },
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable {
                                                selectedPlan = plan
                                                totalAmountText = plan.price.toInt().toString()
                                                paidAmountText = plan.price.toInt().toString()
                                                updateExpiryDate(startDate, plan)
                                            }
                                            .testTag("plan_select_${plan.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSelected) PurpleVip else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = planDuration,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 9.sp
                                                        ),
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }

                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = PurpleVip,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = plan.name,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )

                                            Text(
                                                text = "₹${plan.price.toInt()}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = EmeraldSuccess
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Redesigned Timeline Layout: Start Date -> Duration -> Expiry Date
                    val durationText = when {
                        selectedPlan?.durationMonths == 1 -> "1 Month"
                        selectedPlan?.durationMonths == 3 -> "3 Months"
                        selectedPlan?.durationMonths == 6 -> "6 Months"
                        selectedPlan?.durationMonths == 12 -> "1 Year"
                        (selectedPlan?.durationMonths ?: 0) > 0 -> "${selectedPlan?.durationMonths} Months"
                        (selectedPlan?.durationDays ?: 0) > 0 -> "${selectedPlan?.durationDays} Days"
                        else -> "1 Month"
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Timeline Title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MEMBERSHIP VALIDITY TIMELINE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CrimsonPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Auto-Calculated",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 9.sp
                                        ),
                                        color = CrimsonPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Timeline Flow
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Date (Interactive & Editable)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CrimsonPrimary.copy(alpha = 0.08f),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            listOf(CrimsonPrimary.copy(alpha = 0.4f), CrimsonPrimary.copy(alpha = 0.2f))
                                        ),
                                        width = 1.dp
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showDatePickerDialog = true }
                                        .testTag("membership_start_date_picker")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CrimsonPrimary.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Select Start Date",
                                                tint = CrimsonPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Start Date",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "✏️",
                                                    fontSize = 8.sp
                                                )
                                            }
                                            Text(
                                                text = dateFormat.format(Date(startDate)),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // Middle Timeline Connector / Duration Pill
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(8.dp)
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = PurpleVip.copy(alpha = 0.2f),
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = Brush.linearGradient(listOf(PurpleVip.copy(alpha = 0.6f), PurpleVip.copy(alpha = 0.6f))),
                                                width = 1.dp
                                            )
                                        ) {
                                            Text(
                                                text = durationText,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = PurpleVip,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(8.dp)
                                                .height(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }
                                }

                                // Expiry Date (Computed)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.08f),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            listOf(EmeraldSuccess.copy(alpha = 0.4f), EmeraldSuccess.copy(alpha = 0.2f))
                                        ),
                                        width = 1.dp
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EmeraldSuccess.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "Expiry Date",
                                                tint = EmeraldSuccess,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Expiry Date",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = dateFormat.format(Date(expiryDate)),
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                ),
                                                color = EmeraldSuccess,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Payment & Billing
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AmberAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "3", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Payment & Billing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = totalAmountText,
                            onValueChange = { totalAmountText = it },
                            label = { Text("Total Fee (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text("Paid Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_paid_amount"),
                            singleLine = true
                        )
                    }

                    // Payment summary status chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${paymentStatus.displayName}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = when (paymentStatus) {
                                PaymentStatus.PAID -> EmeraldSuccess
                                PaymentStatus.PARTIALLY_PAID -> AmberAccent
                                else -> CrimsonPrimary
                            }
                        )
                        if (pendingAmt > 0) {
                            Text(
                                text = "Pending Due: ₹${pendingAmt.toInt()}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = CrimsonPrimary
                            )
                        }
                    }

                    // Payment Method Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isPaymentMethodExpanded,
                        onExpandedChange = { isPaymentMethodExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = paymentMethod.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Mode") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentMethodExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isPaymentMethodExpanded,
                            onDismissRequest = { isPaymentMethodExpanded = false }
                        ) {
                            PaymentMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.displayName) },
                                    onClick = {
                                        paymentMethod = method
                                        isPaymentMethodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        label = { Text("Transaction / Receipt Ref") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Section 4: Invoice Template Picker
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "4", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Select Invoice Template",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(InvoiceTemplates.availableTemplates) { template ->
                            val isSelected = selectedTemplateId == template.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTemplateId = template.id },
                                label = { Text(template.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(template.headerColor),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Action Submit Button
        item {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (customerName.isBlank()) {
                        viewModel.showToast("Please enter customer name")
                        return@Button
                    }
                    val normalizedPhone = PhoneUtils.normalizeToE164(mobileNumber)
                    if (normalizedPhone == null) {
                        viewModel.showToast("Please enter a valid 10-digit mobile number")
                        return@Button
                    }
                    if (requireOtpVerification && !isMobileVerified) {
                        viewModel.showToast("Mobile OTP verification is required. Please verify via SMS OTP.")
                        showOtpDialog = true
                        return@Button
                    }
                    val plan = selectedPlan ?: plans.firstOrNull()
                    if (plan == null) {
                        viewModel.showToast("Please select a membership plan")
                        return@Button
                    }

                    isSubmitting = true
                    viewModel.registerNewMember(
                        name = customerName.trim(),
                        mobileNumber = normalizedPhone,
                        email = email.trim(),
                        gender = gender,
                        address = address.trim(),
                        emergencyContact = emergencyContact.trim(),
                        isMobileVerified = isMobileVerified,
                        plan = plan,
                        startDate = startDate,
                        expiryDate = expiryDate,
                        totalAmount = totalAmt,
                        paidAmount = paidAmt,
                        paymentMethod = paymentMethod,
                        paymentStatus = paymentStatus,
                        transactionRef = transactionRef.trim(),
                        templateId = selectedTemplateId,
                        onSuccess = { _, _, invoiceNum ->
                            isSubmitting = false
                            onNavigateToInvoices(invoiceNum)
                        }
                    )
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_create_member_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Creating Membership...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                } else {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Register Member & Generate Invoice",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
    }

    // OTP Verification Dialog
    if (showOtpDialog) {
        OtpVerificationDialog(
            mobileNumber = mobileNumber,
            onDismiss = { showOtpDialog = false },
            onVerified = {
                isMobileVerified = true
                showOtpDialog = false
                viewModel.showToast("Mobile verified successfully!")
            }
        )
    }

    // Membership Start Date Picker Dialog
    if (showDatePickerDialog) {
        val initialUtcMillis = remember(startDate) {
            val localCal = Calendar.getInstance().apply { timeInMillis = startDate }
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH)
                )
            }
            utcCal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val newLocalCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            startDate = newLocalCal.timeInMillis
                            updateExpiryDate(startDate, selectedPlan)
                        }
                        showDatePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePickerDialog = false },
                    modifier = Modifier.testTag("date_picker_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select Membership Start Date",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}
