package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GymSetting
import com.example.data.local.entity.Invoice
import com.example.data.local.entity.MembershipPlan
import com.example.data.model.InvoiceTemplates
import com.example.ui.components.AlertBannerCard
import com.example.ui.components.CollectPaymentDialog
import com.example.ui.components.CustomerCard
import com.example.ui.components.CustomerDetailDialog
import com.example.ui.components.RenewMembershipDialog
import com.example.ui.components.StatMetricCard
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleVip
import com.example.ui.viewmodel.CustomerWithMembership
import com.example.ui.viewmodel.GymViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: GymViewModel,
    onNavigateToCreateMember: () -> Unit,
    onNavigateToMembers: (String?) -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToInvoices: (Invoice?) -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kpi by viewModel.dashboardKpi.collectAsStateWithLifecycle()
    val customersWithMembership by viewModel.customerDirectory.collectAsStateWithLifecycle()
    val plans by viewModel.activePlans.collectAsStateWithLifecycle()
    val settings by viewModel.gymSettings.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()

    var selectedCustomerForDetail by remember { mutableStateOf<CustomerWithMembership?>(null) }
    var selectedCustomerForRenew by remember { mutableStateOf<CustomerWithMembership?>(null) }
    var selectedCustomerForPayment by remember { mutableStateOf<CustomerWithMembership?>(null) }

    val recentMembers = remember(customersWithMembership) {
        customersWithMembership.take(4)
    }

    val unreadNotifications = remember(notifications) {
        notifications.filter { !it.isRead }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp, start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header: Gym Branding & Revenue Highlights
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(CrimsonPrimary.copy(alpha = 0.6f), AmberAccent.copy(alpha = 0.2f))
                    )
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = settings.gymName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = AppStrings.get("tagline", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CrimsonPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "LIVE ERP",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CrimsonPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Revenue highlights in hero
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.get("month_revenue", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", kpi.currentMonthRevenue)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                    color = EmeraldSuccess
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = AppStrings.get("today_collections", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₹${String.format(Locale.getDefault(), "%,.0f", kpi.todayCollections)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AmberAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Column {
                Text(
                    text = AppStrings.get("quick_actions", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        QuickActionButton(
                            title = "New Member",
                            icon = Icons.Default.PersonAdd,
                            color = CrimsonPrimary,
                            onClick = onNavigateToCreateMember
                        )
                    }
                    item {
                        QuickActionButton(
                            title = "Manage Plans",
                            icon = Icons.Default.CardMembership,
                            color = PurpleVip,
                            onClick = onNavigateToPlans
                        )
                    }
                    item {
                        QuickActionButton(
                            title = "Invoices",
                            icon = Icons.Default.Receipt,
                            color = Color(0xFF0284C7),
                            onClick = { onNavigateToInvoices(null) }
                        )
                    }
                    item {
                        QuickActionButton(
                            title = "Reports",
                            icon = Icons.Default.Assessment,
                            color = EmeraldSuccess,
                            onClick = onNavigateToReports
                        )
                    }
                }
            }
        }

        // 4 Primary KPI Stat Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = AppStrings.get("total_members", language),
                        value = "${kpi.totalMembers}",
                        subtitle = "${kpi.activeMembers} Active",
                        icon = Icons.Default.People,
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToMembers("ALL") }
                    )
                    StatMetricCard(
                        title = AppStrings.get("active_members", language),
                        value = "${kpi.activeMembers}",
                        subtitle = "Valid pass",
                        icon = Icons.Default.CheckCircle,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToMembers("ACTIVE") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = AppStrings.get("expiring_soon", language),
                        value = "${kpi.expiringSoonCount}",
                        subtitle = "Next 3 days",
                        icon = Icons.Default.HourglassTop,
                        accentColor = AmberAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToMembers("EXPIRING") }
                    )
                    StatMetricCard(
                        title = AppStrings.get("pending_payments", language),
                        value = "₹${kpi.totalPendingDues.toInt()}",
                        subtitle = "${kpi.pendingPaymentCount} Members",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = CrimsonPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToMembers("PENDING") }
                    )
                }
            }
        }

        // Real-Time Membership Alerts Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = AppStrings.get("membership_alerts", language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (unreadNotifications.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = CrimsonPrimary
                            ) {
                                Text(
                                    text = "${unreadNotifications.size}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }
                        }
                    }

                    if (unreadNotifications.isNotEmpty()) {
                        TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                            Text("Mark All Read", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (kpi.expiringSoonCount > 0) {
                    AlertBannerCard(
                        title = "Memberships Expiring Soon",
                        description = "${kpi.expiringSoonCount} customer(s) will expire in the next 3 days. Send renewal invoice.",
                        badgeText = "${kpi.expiringSoonCount} DUE SOON",
                        badgeColor = AmberAccent,
                        icon = Icons.Default.HourglassTop,
                        onClick = { onNavigateToMembers("EXPIRING") }
                    )
                }

                if (kpi.pendingPaymentCount > 0) {
                    AlertBannerCard(
                        title = "Pending Membership Dues",
                        description = "₹${kpi.totalPendingDues.toInt()} outstanding across ${kpi.pendingPaymentCount} member(s).",
                        badgeText = "₹${kpi.totalPendingDues.toInt()} PENDING",
                        badgeColor = CrimsonPrimary,
                        icon = Icons.Default.Warning,
                        onClick = { onNavigateToMembers("PENDING") }
                    )
                }

                // Display latest staff notification
                if (notifications.isNotEmpty()) {
                    val latestNotif = notifications.first()
                    AlertBannerCard(
                        title = latestNotif.title,
                        description = latestNotif.message,
                        badgeText = "STAFF UPDATE",
                        badgeColor = EmeraldSuccess,
                        icon = Icons.Default.Notifications
                    )
                }
            }
        }

        // Recent Member Registrations
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("recent_members", language),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (kpi.totalMembers > 0) {
                    TextButton(onClick = { onNavigateToMembers("ALL") }) {
                        Text("View All (${kpi.totalMembers})")
                    }
                }
            }
        }

        if (recentMembers.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Members Registered Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap '+ New Member' below to register your first gym customer and generate a digital invoice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToCreateMember,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Register First Member", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(recentMembers, key = { it.customer.id }) { item ->
                CustomerCard(
                    item = item,
                    onCardClick = { selectedCustomerForDetail = item },
                    onRenewClick = { selectedCustomerForRenew = item },
                    onCollectPaymentClick = { selectedCustomerForPayment = item },
                    onViewInvoiceClick = {
                        val invoice = viewModel.allInvoices.value.find { it.customerId == item.customer.id }
                        onNavigateToInvoices(invoice)
                    }
                )
            }
        }
    }

    // Customer Detail Dialog
    if (selectedCustomerForDetail != null) {
        val cust = selectedCustomerForDetail!!
        val payments = viewModel.allPayments.value.filter { it.customerId == cust.customer.id }
        val invoices = viewModel.allInvoices.value.filter { it.customerId == cust.customer.id }

        CustomerDetailDialog(
            item = cust,
            payments = payments,
            invoices = invoices,
            onDismiss = { selectedCustomerForDetail = null },
            onRenewClick = {
                selectedCustomerForRenew = cust
                selectedCustomerForDetail = null
            },
            onCollectPaymentClick = {
                selectedCustomerForPayment = cust
                selectedCustomerForDetail = null
            },
            onViewInvoice = { inv ->
                selectedCustomerForDetail = null
                onNavigateToInvoices(inv)
            }
        )
    }

    // Renew Membership Dialog
    if (selectedCustomerForRenew != null) {
        RenewMembershipDialog(
            item = selectedCustomerForRenew!!,
            plans = plans,
            onDismiss = { selectedCustomerForRenew = null },
            onConfirmRenew = { plan, start, exp, total, paid, method, status, ref ->
                viewModel.renewMember(
                    customerId = selectedCustomerForRenew!!.customer.id,
                    customerName = selectedCustomerForRenew!!.customer.name,
                    customerMobile = selectedCustomerForRenew!!.customer.mobileNumber,
                    plan = plan,
                    startDate = start,
                    expiryDate = exp,
                    totalAmount = total,
                    paidAmount = paid,
                    paymentMethod = method,
                    paymentStatus = status,
                    transactionRef = ref,
                    templateId = settings.activeInvoiceTemplateId,
                    onSuccess = { selectedCustomerForRenew = null }
                )
            }
        )
    }

    // Collect Payment Dialog
    if (selectedCustomerForPayment != null) {
        CollectPaymentDialog(
            item = selectedCustomerForPayment!!,
            onDismiss = { selectedCustomerForPayment = null },
            onConfirmPayment = { memId, custId, amount, method, ref, notes ->
                viewModel.collectPendingPayment(memId, custId, amount, method, ref, notes)
                selectedCustomerForPayment = null
            }
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .testTag("quick_action_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
