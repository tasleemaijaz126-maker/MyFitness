package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Membership
import com.example.data.local.entity.MembershipPlan
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.CustomerWithMembership
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewMembershipDialog(
    item: CustomerWithMembership,
    plans: List<MembershipPlan>,
    onDismiss: () -> Unit,
    onConfirmRenew: (MembershipPlan, Long, Long, Double, Double, PaymentMethod, PaymentStatus, String) -> Unit
) {
    if (plans.isEmpty()) return

    var selectedPlan by remember { mutableStateOf(plans.first()) }
    var isPlanDropdownExpanded by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val baseStart = (item.latestMembership?.expiryDate ?: now).coerceAtLeast(now)

    var startDate by remember { mutableStateOf(baseStart) }
    val cal = Calendar.getInstance()
    cal.timeInMillis = startDate
    if (selectedPlan.durationMonths > 0) {
        cal.add(Calendar.MONTH, selectedPlan.durationMonths)
    } else {
        cal.add(Calendar.DAY_OF_YEAR, selectedPlan.durationDays.coerceAtLeast(30))
    }
    var expiryDate by remember { mutableStateOf(cal.timeInMillis) }

    var paidAmountText by remember { mutableStateOf(selectedPlan.price.toInt().toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var isMethodDropdownExpanded by remember { mutableStateOf(false) }
    var txnRef by remember { mutableStateOf("RNW-${System.currentTimeMillis() % 100000}") }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Renew Membership",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Member: ${item.customer.name} (+91 ${item.customer.mobileNumber})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Plan selection dropdown
                ExposedDropdownMenuBox(
                    expanded = isPlanDropdownExpanded,
                    onExpandedChange = { isPlanDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedPlan.name} (₹${selectedPlan.price.toInt()})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Plan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPlanDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isPlanDropdownExpanded,
                        onDismissRequest = { isPlanDropdownExpanded = false }
                    ) {
                        plans.forEach { plan ->
                            DropdownMenuItem(
                                text = { Text("${plan.name} – ₹${plan.price.toInt()}") },
                                onClick = {
                                    selectedPlan = plan
                                    paidAmountText = plan.price.toInt().toString()
                                    val c = Calendar.getInstance()
                                    c.timeInMillis = startDate
                                    if (plan.durationMonths > 0) c.add(Calendar.MONTH, plan.durationMonths)
                                    else c.add(Calendar.DAY_OF_YEAR, plan.durationDays.coerceAtLeast(30))
                                    expiryDate = c.timeInMillis
                                    isPlanDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Dates summary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Start: ${dateFormat.format(Date(startDate))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Expiry: ${dateFormat.format(Date(expiryDate))}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonPrimary
                        )
                    }
                }

                // Payment input
                OutlinedTextField(
                    value = paidAmountText,
                    onValueChange = { paidAmountText = it },
                    label = { Text("Paid Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Payment Method
                ExposedDropdownMenuBox(
                    expanded = isMethodDropdownExpanded,
                    onExpandedChange = { isMethodDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMethod.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMethodDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isMethodDropdownExpanded,
                        onDismissRequest = { isMethodDropdownExpanded = false }
                    ) {
                        PaymentMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.displayName) },
                                onClick = {
                                    selectedMethod = method
                                    isMethodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = txnRef,
                    onValueChange = { txnRef = it },
                    label = { Text("Txn / Receipt Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paid = paidAmountText.toDoubleOrNull() ?: selectedPlan.price
                    val status = if (paid >= selectedPlan.price) PaymentStatus.PAID else PaymentStatus.PARTIALLY_PAID
                    onConfirmRenew(
                        selectedPlan,
                        startDate,
                        expiryDate,
                        selectedPlan.price,
                        paid,
                        selectedMethod,
                        status,
                        txnRef
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("Confirm Renewal", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectPaymentDialog(
    item: CustomerWithMembership,
    onDismiss: () -> Unit,
    onConfirmPayment: (Long, Long, Double, PaymentMethod, String, String) -> Unit
) {
    val activeMem = item.latestMembership ?: return
    var amountText by remember { mutableStateOf(activeMem.pendingAmount.toInt().toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var isMethodDropdownExpanded by remember { mutableStateOf(false) }
    var txnRef by remember { mutableStateOf("DUE-${System.currentTimeMillis() % 100000}") }
    var notes by remember { mutableStateOf("Collected pending dues") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Collect Pending Dues",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Customer: ${item.customer.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Plan: ${activeMem.planName} | Total Dues: ₹${activeMem.pendingAmount.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberAccent
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Collecting (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = isMethodDropdownExpanded,
                    onExpandedChange = { isMethodDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMethod.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMethodDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isMethodDropdownExpanded,
                        onDismissRequest = { isMethodDropdownExpanded = false }
                    ) {
                        PaymentMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.displayName) },
                                onClick = {
                                    selectedMethod = method
                                    isMethodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = txnRef,
                    onValueChange = { txnRef = it },
                    label = { Text("Transaction Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirmPayment(
                            activeMem.id,
                            item.customer.id,
                            amount,
                            selectedMethod,
                            txnRef,
                            notes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Text("Record Payment", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorDialog(
    planToEdit: MembershipPlan? = null,
    onDismiss: () -> Unit,
    onSavePlan: (MembershipPlan) -> Unit
) {
    var name by remember { mutableStateOf(planToEdit?.name ?: "") }
    var priceText by remember { mutableStateOf(planToEdit?.price?.toInt()?.toString() ?: "999") }
    var durationMonthsText by remember { mutableStateOf(planToEdit?.durationMonths?.toString() ?: "1") }
    var durationDaysText by remember { mutableStateOf(planToEdit?.durationDays?.toString() ?: "30") }
    var category by remember { mutableStateOf(planToEdit?.category ?: "General Fitness") }
    var description by remember { mutableStateOf(planToEdit?.description ?: "") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("General Fitness", "Strength", "Cardio", "VIP", "Personal Training")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (planToEdit != null) "Edit Membership Plan" else "Create Membership Plan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plan Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationMonthsText,
                        onValueChange = { durationMonthsText = it },
                        label = { Text("Duration (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Benefits & Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val plan = MembershipPlan(
                            id = planToEdit?.id ?: 0L,
                            name = name.trim(),
                            durationMonths = durationMonthsText.toIntOrNull() ?: 1,
                            durationDays = (durationMonthsText.toIntOrNull() ?: 1) * 30,
                            price = priceText.toDoubleOrNull() ?: 999.0,
                            description = description.trim(),
                            category = category,
                            isActive = planToEdit?.isActive ?: true
                        )
                        onSavePlan(plan)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text(if (planToEdit != null) "Update Plan" else "Create Plan", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
