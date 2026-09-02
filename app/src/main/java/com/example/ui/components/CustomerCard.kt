package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleVip
import com.example.ui.viewmodel.CustomerWithMembership
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerCard(
    item: CustomerWithMembership,
    onCardClick: () -> Unit,
    onRenewClick: () -> Unit,
    onCollectPaymentClick: () -> Unit,
    onViewInvoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customer = item.customer
    val latestMem = item.latestMembership
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val now = System.currentTimeMillis()
    val expiryTimestamp = latestMem?.expiryDate ?: 0L
    val daysLeft = if (expiryTimestamp > 0) {
        ((expiryTimestamp - now) / (1000L * 60 * 60 * 24)).toInt()
    } else 0

    // Status Badge calculation
    val (statusColor, statusBg, statusText) = when {
        latestMem == null -> Triple(Color(0xFF94A3B8), Color(0xFF1E293B), "NO PLAN")
        latestMem.expiryDate < now -> Triple(Color(0xFFFDA4AF), Color(0xFF4C0519), "EXPIRED")
        daysLeft in 0..3 -> Triple(Color(0xFFFBBF24), Color(0xFF78350F), if (daysLeft == 0) "EXPIRES TODAY" else "EXPIRING IN ${daysLeft}D")
        item.totalPending > 0 -> Triple(Color(0xFFA5B4FC), Color(0xFF1E1B4B), "DUE ₹${item.totalPending.toInt()}")
        else -> Triple(Color(0xFF34D399), Color(0xFF064E3B), "ACTIVE")
    }

    // Hash-based Avatar Color
    val avatarColors = listOf(
        Pair(CrimsonPrimary, Color(0xFF881337)),
        Pair(AmberAccent, Color(0xFF78350F)),
        Pair(EmeraldSuccess, Color(0xFF064E3B)),
        Pair(PurpleVip, Color(0xFF4C1D95)),
        Pair(Color(0xFF0284C7), Color(0xFF0369A1))
    )
    val colorIndex = customer.name.hashCode().mod(avatarColors.size).coerceAtLeast(0)
    val (avatarPrimary, avatarDark) = avatarColors[colorIndex]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_card_${customer.id}")
            .clickable { onCardClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -------------------------------------------------------------
            // 1. IDENTITY HEADER: Avatar + Full Name & Mobile + Status Badge
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Initials Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(avatarPrimary, avatarDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = customer.name
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                        .ifBlank { customer.name.take(2).uppercase() }

                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    )
                }

                // Name & Mobile Number
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (customer.isMobileVerified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Mobile",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = "+91 ${customer.mobileNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Compact Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = statusColor
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. MEMBERSHIP DETAILS & PRICING PANEL
            // -------------------------------------------------------------
            if (latestMem != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Plan Name and Pricing Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "MEMBERSHIP PLAN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = latestMem.planName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${latestMem.totalAmount.toInt()}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    ),
                                    color = EmeraldSuccess
                                )
                                Text(
                                    text = "Plan Fee",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Dates Timeline & Duration Indicator Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${dateFormat.format(Date(latestMem.startDate))} → ${dateFormat.format(Date(latestMem.expiryDate))}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Days left indicator tag
                            val durationText = when {
                                latestMem.expiryDate < now -> "Expired"
                                daysLeft == 0 -> "Expires today"
                                daysLeft == 1 -> "1 day left"
                                else -> "$daysLeft days left"
                            }
                            val durationColor = when {
                                latestMem.expiryDate < now -> Color(0xFFFDA4AF)
                                daysLeft <= 3 -> AmberAccent
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val durationBg = when {
                                latestMem.expiryDate < now -> Color(0xFF4C0519).copy(alpha = 0.4f)
                                daysLeft <= 3 -> AmberAccent.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = durationBg
                            ) {
                                Text(
                                    text = durationText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = durationColor
                                )
                            }
                        }

                        // Payment Progress Bar if there are pending dues
                        if (latestMem.pendingAmount > 0) {
                            val progress = (latestMem.paidAmount / latestMem.totalAmount.coerceAtLeast(1.0)).toFloat()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Paid: ₹${latestMem.paidAmount.toInt()}  •  Due: ₹${latestMem.pendingAmount.toInt()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        ),
                                        color = AmberAccent
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = AmberAccent,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "No active membership plan recorded yet",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // 3. ACTION BUTTONS: Call, Invoice, (Collect Dues), Renew
            // -------------------------------------------------------------
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobileNumber}"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EmeraldSuccess
                    ),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Call",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = EmeraldSuccess
                    )
                }

                // Invoice Button
                FilledTonalButton(
                    onClick = onViewInvoiceClick,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Invoice",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // If Pending Dues -> Collect Dues Button
                if (item.totalPending > 0) {
                    FilledTonalButton(
                        onClick = onCollectPaymentClick,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AmberAccent.copy(alpha = 0.15f),
                            contentColor = AmberAccent
                        ),
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.35f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = AmberAccent
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Collect ₹${item.totalPending.toInt()}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = AmberAccent
                        )
                    }
                }

                // Renew Button (Prominent Action)
                FilledTonalButton(
                    onClick = onRenewClick,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CrimsonPrimary.copy(alpha = 0.18f),
                        contentColor = CrimsonPrimary
                    ),
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Renew",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = CrimsonPrimary
                    )
                }
            }
        }
    }
}

