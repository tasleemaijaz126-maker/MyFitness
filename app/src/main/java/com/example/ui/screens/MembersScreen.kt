package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Invoice
import com.example.ui.components.CollectPaymentDialog
import com.example.ui.components.CustomerCard
import com.example.ui.components.CustomerDetailDialog
import com.example.ui.components.RenewMembershipDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.CustomerWithMembership
import com.example.ui.viewmodel.GymViewModel

@Composable
fun MembersScreen(
    viewModel: GymViewModel,
    initialFilter: String? = null,
    onNavigateToCreateMember: () -> Unit,
    onNavigateToInvoices: (Invoice?) -> Unit,
    modifier: Modifier = Modifier
) {
    val customersWithMembership by viewModel.customerDirectory.collectAsStateWithLifecycle()
    val plans by viewModel.activePlans.collectAsStateWithLifecycle()
    val settings by viewModel.gymSettings.collectAsStateWithLifecycle()
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    var selectedCustomerForDetail by remember { mutableStateOf<CustomerWithMembership?>(null) }
    var selectedCustomerForRenew by remember { mutableStateOf<CustomerWithMembership?>(null) }
    var selectedCustomerForPayment by remember { mutableStateOf<CustomerWithMembership?>(null) }

    val filterOptions = listOf(
        "ALL" to "All (${customersWithMembership.size})",
        "ACTIVE" to "Active",
        "INACTIVE" to "Inactive",
        "EXPIRING" to "Expiring Soon",
        "EXPIRED" to "Expired",
        "PENDING" to "Pending Dues"
    )

    Box(modifier = modifier.fillMaxSize().testTag("members_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Search Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = AppStrings.get("memberships", language),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Search, filter and manage all gym members",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Sort Menu Trigger
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_btn")
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Registration (Newest first)") },
                                    onClick = {
                                        viewModel.setSortBy("DATE_DESC")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Expiry (Expiring first)") },
                                    onClick = {
                                        viewModel.setSortBy("EXPIRY_ASC")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A - Z)") },
                                    onClick = {
                                        viewModel.setSortBy("NAME_ASC")
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Revenue (Highest paid)") },
                                    onClick = {
                                        viewModel.setSortBy("AMOUNT_DESC")
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(AppStrings.get("search_placeholder", language)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        singleLine = true
                    )

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filterOptions) { (key, label) ->
                            val isSelected = selectedStatusFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setStatusFilter(key) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("filter_chip_$key"),
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (key) {
                                        "EXPIRING" -> AmberAccent
                                        "PENDING", "EXPIRED" -> CrimsonPrimary
                                        "ACTIVE" -> EmeraldSuccess
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // Results count badge / row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = "Showing ${customersWithMembership.size} member(s)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedStatusFilter != "ALL") {
                        Text(
                            text = "Filtered by: ${filterOptions.find { it.first == selectedStatusFilter }?.second ?: selectedStatusFilter}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Customer Cards List
            if (customersWithMembership.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matching members found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Try adjusting your search or active filter tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(customersWithMembership, key = { it.customer.id }) { item ->
                    CustomerCard(
                        item = item,
                        onCardClick = { selectedCustomerForDetail = item },
                        onRenewClick = { selectedCustomerForRenew = item },
                        onCollectPaymentClick = { selectedCustomerForPayment = item },
                        onViewInvoiceClick = {
                            val inv = viewModel.allInvoices.value.find { it.customerId == item.customer.id }
                            onNavigateToInvoices(inv)
                        }
                    )
                }
            }
        }

        // Floating Action Button to Register Member
        FloatingActionButton(
            onClick = onNavigateToCreateMember,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp)
                .testTag("fab_create_member"),
            containerColor = CrimsonPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Member")
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

    // Renew Dialog
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
