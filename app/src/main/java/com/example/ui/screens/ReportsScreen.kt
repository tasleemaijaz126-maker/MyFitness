package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.ReportPeriod
import com.example.ui.components.CustomDateRangePickerDialog
import com.example.ui.components.MonthlyRevenueBarChart
import com.example.ui.components.PlanRevenueBreakdownSection
import com.example.ui.components.StatMetricCard
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleVip
import com.example.ui.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: GymViewModel,
    modifier: Modifier = Modifier
) {
    val reportState by viewModel.reportAnalytics.collectAsStateWithLifecycle()
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reportState.monthlyTrend.isEmpty()) {
            viewModel.computeReports(ReportPeriod.MONTHLY)
        }
    }

    val periodOptions = listOf(
        ReportPeriod.DAILY to "Daily",
        ReportPeriod.MONTHLY to "Monthly",
        ReportPeriod.LAST_3_MONTHS to "Last 3 Months",
        ReportPeriod.LAST_6_MONTHS to "Last 6 Months",
        ReportPeriod.YEARLY to "Yearly",
        ReportPeriod.CUSTOM to "Custom Range"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header & Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("reports", language),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Revenue analytics & membership performance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.showToast("Exported gym financial report summary!") },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Period Selection Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(periodOptions) { (period, label) ->
                    val isSelected = reportState.selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (period == ReportPeriod.CUSTOM) {
                                showCustomDateDialog = true
                            } else {
                                viewModel.computeReports(period)
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (period == ReportPeriod.CUSTOM) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) Color.White else AmberAccent
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(label)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Period Date Range Banner (Interactive for Custom Range)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = reportState.selectedPeriod == ReportPeriod.CUSTOM) {
                        showCustomDateDialog = true
                    },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(
                            if (reportState.selectedPeriod == ReportPeriod.CUSTOM) AmberAccent.copy(alpha = 0.5f) else CrimsonPrimary.copy(alpha = 0.3f),
                            Color(0xFF1E293B),
                            EmeraldSuccess.copy(alpha = 0.2f)
                        )
                    ),
                    width = 1.2.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (reportState.selectedPeriod == ReportPeriod.CUSTOM) AmberAccent else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${dateFormat.format(Date(reportState.customStartDate))} – ${dateFormat.format(Date(reportState.customEndDate))}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (reportState.selectedPeriod == ReportPeriod.CUSTOM) "Custom Date Filter Active (Tap to change)" else "Viewing ${reportState.selectedPeriod.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }} Report",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (reportState.selectedPeriod == ReportPeriod.CUSTOM) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (reportState.selectedPeriod == ReportPeriod.CUSTOM) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberAccent.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AmberAccent)
                            }
                        }
                    }
                }
            }
        }

        // Financial KPIs: Total Revenue, Paid Collections, Outstanding Dues
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Period Total Revenue",
                        value = "₹${String.format(Locale.getDefault(), "%,.0f", reportState.periodRevenue)}",
                        subtitle = "Gross generated",
                        icon = Icons.Default.TrendingUp,
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Collected Amount",
                        value = "₹${String.format(Locale.getDefault(), "%,.0f", reportState.periodPaidAmount)}",
                        subtitle = "Realized cash/UPI",
                        icon = Icons.Default.MonetizationOn,
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "New Registrations",
                        value = "${reportState.newMembersCount}",
                        subtitle = "Joined this period",
                        icon = Icons.Default.PersonAdd,
                        accentColor = PurpleVip,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Renewed Members",
                        value = "${reportState.renewedMembersCount}",
                        subtitle = "Extended passes",
                        icon = Icons.Default.Autorenew,
                        accentColor = AmberAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Monthly Revenue Trend Chart (Custom Canvas Bar Visualizer)
        item {
            MonthlyRevenueBarChart(points = reportState.monthlyTrend)
        }

        // Plan Breakdown List with Percentage Progress Bars
        item {
            PlanRevenueBreakdownSection(stats = reportState.planBreakdown)
        }
    }

    // Custom Date Range Picker Dialog
    if (showCustomDateDialog) {
        CustomDateRangePickerDialog(
            initialStartDate = reportState.customStartDate,
            initialEndDate = reportState.customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onApplyRange = { start, end ->
                showCustomDateDialog = false
                viewModel.computeReports(
                    period = ReportPeriod.CUSTOM,
                    start = start,
                    end = end
                )
            }
        )
    }
}
