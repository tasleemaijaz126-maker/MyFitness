package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GymSetting
import com.example.data.local.entity.Invoice
import com.example.data.model.InvoiceTemplateConfig
import com.example.data.model.PaymentStatus
import com.example.util.OwnerSignatureDisplay
import com.example.util.QrCodeView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicePreviewView(
    invoice: Invoice,
    gymSetting: GymSetting,
    templateConfig: InvoiceTemplateConfig,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val invoiceDate = dateFormat.format(Date(invoice.generatedDate))
    val startDateStr = dateFormat.format(Date(invoice.startDate))
    val expiryDateStr = dateFormat.format(Date(invoice.expiryDate))

    when (templateConfig.id.lowercase()) {
        "gold", "gold_elite", "classic_gold" -> {
            GoldInvoiceLayout(invoice, gymSetting, templateConfig, invoiceDate, startDateStr, expiryDateStr, modifier)
        }
        "platinum", "platinum_pro", "ocean_corporate", "titanium_grid" -> {
            PlatinumInvoiceLayout(invoice, gymSetting, templateConfig, invoiceDate, startDateStr, expiryDateStr, modifier)
        }
        "diamond", "diamond_prestige", "royal_velvet", "neon_cyber" -> {
            DiamondInvoiceLayout(invoice, gymSetting, templateConfig, invoiceDate, startDateStr, expiryDateStr, modifier)
        }
        else -> { // Silver Classic (Default)
            SilverInvoiceLayout(invoice, gymSetting, templateConfig, invoiceDate, startDateStr, expiryDateStr, modifier)
        }
    }
}

// =========================================================================
// 1. SILVER CLASSIC TEMPLATE
// =========================================================================
@Composable
private fun SilverInvoiceLayout(
    invoice: Invoice,
    gymSetting: GymSetting,
    templateConfig: InvoiceTemplateConfig,
    invoiceDate: String,
    startDateStr: String,
    expiryDateStr: String,
    modifier: Modifier = Modifier
) {
    val headerColor = Color(0xFF1E293B)
    val accentColor = Color(0xFF64748B)
    val bgColor = Color(0xFFF8FAFC)
    val textColor = Color(0xFF0F172A)
    val cardSurface = Color(0xFFF1F5F9)
    val borderColor = Color(0xFFCBD5E1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_preview_${invoice.invoiceNumber}")
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(headerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = gymSetting.gymName.ifBlank { "MY FITNESS ERP" }.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                                color = headerColor
                            )
                            Text(
                                text = if (gymSetting.gymTagline.isNotBlank()) gymSetting.gymTagline else "OFFICIAL FITNESS TAX INVOICE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = accentColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (gymSetting.gymAddress.isNotBlank() || gymSetting.gymCity.isNotBlank()) {
                        Text(
                            text = "📍 ${listOf(gymSetting.gymAddress, gymSetting.gymCity).filter { it.isNotBlank() }.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = textColor.copy(alpha = 0.75f)
                        )
                    }
                    Text(
                        text = "📞 ${gymSetting.gymPhone.ifBlank { "+91 98765 43210" }}   |   ✉️ ${gymSetting.gymEmail.ifBlank { "contact@myfitness.com" }}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = textColor.copy(alpha = 0.75f)
                    )
                    if (gymSetting.gymGstin.isNotBlank()) {
                        Text(
                            text = "GSTIN / Tax ID: ${gymSetting.gymGstin}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }

                // Invoice Meta Right
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = headerColor
                    ) {
                        Text(
                            text = templateConfig.badgeText.ifBlank { "TAX INVOICE" },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "INVOICE NO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                        color = accentColor
                    )
                    Text(
                        text = "#${invoice.invoiceNumber.replace("#", "")}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = headerColor
                    )
                    Text(
                        text = "Date: $invoiceDate",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = textColor.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = borderColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Member & Validity Period Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BILLED TO / MEMBER DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = accentColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(invoice.customerName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                        Text("Mobile: +91 ${invoice.customerMobile}", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
                        Text("Member ID: #CUST-${invoice.customerId}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = textColor.copy(alpha = 0.65f))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("MEMBERSHIP VALIDITY PERIOD", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = accentColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$startDateStr  to  $expiryDateStr", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = headerColor)
                        Text("Plan: ${invoice.durationText}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = textColor.copy(alpha = 0.85f))
                        Text("Status: Active Access", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF059669)))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Table Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE2E8F0)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ITEM / PLAN DESCRIPTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = headerColor, modifier = Modifier.weight(2f))
                    Text("DURATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = headerColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("AMOUNT (INR)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = headerColor, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }

            // Table Item Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(2f)) {
                    Text(invoice.planName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                    Text("✓ Full Gym Floor Access  ✓ Trainer Guidance & Lockers", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = textColor.copy(alpha = 0.65f))
                }
                Text(invoice.durationText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = textColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.totalAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = headerColor, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }

            HorizontalDivider(color = borderColor)
            Spacer(modifier = Modifier.height(12.dp))

            // Payment Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = cardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text("PAYMENT INFORMATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = accentColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Mode: ${invoice.paymentMethod.displayName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (invoice.paymentStatus) {
                                PaymentStatus.PAID -> Color(0xFF059669)
                                PaymentStatus.PARTIALLY_PAID -> Color(0xFFD97706)
                                else -> Color(0xFFDC2626)
                            }
                        ) {
                            Text(
                                text = invoice.paymentStatus.displayName.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = Color.White
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Amount:", style = MaterialTheme.typography.bodySmall, color = textColor)
                            Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.totalAmount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = textColor)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount Paid:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                            Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.paidAmount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                        }
                        if (invoice.pendingAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pending Due:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFDC2626))
                                Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.pendingAmount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFDC2626))
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance Due:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF059669))
                                Text("₹0.00 (Settled)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Terms & QR & Signature
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("TERMS & CONDITIONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = accentColor)
                    Text(
                        text = gymSetting.invoiceTerms.ifBlank { "1. Membership fee is non-refundable & non-transferable.\n2. Please carry digital ID upon entry.\n3. Follow gym safety guidelines." },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 3
                    )
                }

                if (templateConfig.showQrCode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val qrUrl = if (gymSetting.gymLocationUrl.isNotBlank()) gymSetting.gymLocationUrl else "https://maps.google.com/?q=${gymSetting.gymName}"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeView(content = qrUrl, modifier = Modifier.size(48.dp), darkColor = headerColor.toArgb(), lightColor = android.graphics.Color.WHITE)
                        }
                        Text("Scan Location", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = accentColor)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OwnerSignatureDisplay(name = gymSetting.ownerSignatureName, styleId = gymSetting.ownerSignatureStyleId, modifier = Modifier.width(100.dp).height(32.dp), color = headerColor)
                    HorizontalDivider(modifier = Modifier.width(90.dp), thickness = 1.dp, color = accentColor)
                    Text("Authorized Signatory", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = textColor.copy(alpha = 0.8f))
                    Text(gymSetting.ownerSignatureName.ifBlank { "Gym Owner" }, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = textColor.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("✓ AUTHENTIC DIGITAL INVOICE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = Color(0xFF059669))
                Text("System Generated via My Fitness ERP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = textColor.copy(alpha = 0.5f))
            }
        }
    }
}

// =========================================================================
// 2. GOLD ELITE TEMPLATE (Prestige Luxury Onyx & Gold Double Border)
// =========================================================================
@Composable
private fun GoldInvoiceLayout(
    invoice: Invoice,
    gymSetting: GymSetting,
    templateConfig: InvoiceTemplateConfig,
    invoiceDate: String,
    startDateStr: String,
    expiryDateStr: String,
    modifier: Modifier = Modifier
) {
    val goldPrimary = Color(0xFFD97706)
    val goldLight = Color(0xFFFBBF24)
    val onyxBg = Color(0xFF18181B)
    val cardSurface = Color(0xFF27272A)
    val ivoryText = Color(0xFFFAF8F5)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_preview_${invoice.invoiceNumber}")
            .clip(RoundedCornerShape(16.dp))
            .background(onyxBg)
            .border(3.dp, goldPrimary, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, goldLight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Gold Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(goldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = onyxBg, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = gymSetting.gymName.ifBlank { "GOLD ELITE FITNESS" }.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Serif,
                                    letterSpacing = 0.8.sp
                                ),
                                color = goldLight
                            )
                            Text(
                                text = if (gymSetting.gymTagline.isNotBlank()) gymSetting.gymTagline else "VIP CLUB & LUXURY WELLNESS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = ivoryText.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // VIP Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = goldPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = onyxBg, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = templateConfig.badgeText.ifBlank { "★ VIP GOLD ELITE ★" },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                                color = onyxBg
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Invoice Number & Date Gold Pill Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = cardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, goldPrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("INVOICE #${invoice.invoiceNumber}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif), color = goldLight)
                        Text("Date: $invoiceDate", style = MaterialTheme.typography.labelSmall, color = ivoryText.copy(alpha = 0.85f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Billed To & Validity Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = cardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, goldPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HONORED MEMBER:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = goldPrimary)
                            Text(invoice.customerName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif), color = ivoryText)
                            Text("Mobile: +91 ${invoice.customerMobile}", style = MaterialTheme.typography.bodySmall, color = ivoryText.copy(alpha = 0.75f))
                            Text("Member ID: #CUST-${invoice.customerId}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ivoryText.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("MEMBERSHIP ACCESS:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = goldPrimary)
                            Text("$startDateStr to", style = MaterialTheme.typography.bodySmall, color = ivoryText.copy(alpha = 0.8f))
                            Text(expiryDateStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = goldLight)
                            Text("Duration: ${invoice.durationText}", style = MaterialTheme.typography.labelSmall, color = ivoryText.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gold Plan Table Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = cardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, goldPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(invoice.planName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif), color = goldLight)
                            Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.totalAmount)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = ivoryText)
                        }
                        Text("Duration: ${invoice.durationText} • Unlimited Floor, Spa & VIP Facilities", style = MaterialTheme.typography.labelSmall, color = ivoryText.copy(alpha = 0.65f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Golden Financial Summary Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = goldPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, goldPrimary)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Payment: ${invoice.paymentMethod.displayName}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = ivoryText)
                            Text("Status: ${invoice.paymentStatus.displayName.uppercase()}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = goldLight)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PAID: ₹${String.format(Locale.getDefault(), "%,.2f", invoice.paidAmount)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF34D399))
                            if (invoice.pendingAmount > 0) {
                                Text("DUE: ₹${String.format(Locale.getDefault(), "%,.2f", invoice.pendingAmount)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF87171))
                            } else {
                                Text("BALANCE: ₹0.00", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF34D399))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Signatory & QR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (templateConfig.showQrCode) {
                        val qrUrl = if (gymSetting.gymLocationUrl.isNotBlank()) gymSetting.gymLocationUrl else "https://maps.google.com/?q=${gymSetting.gymName}"
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, goldPrimary, RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                QrCodeView(content = qrUrl, modifier = Modifier.size(48.dp), darkColor = android.graphics.Color.BLACK, lightColor = android.graphics.Color.WHITE)
                            }
                            Text("Scan Location", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = goldLight)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OwnerSignatureDisplay(name = gymSetting.ownerSignatureName, styleId = gymSetting.ownerSignatureStyleId, modifier = Modifier.width(110.dp).height(32.dp), color = goldLight)
                        HorizontalDivider(modifier = Modifier.width(100.dp), thickness = 1.dp, color = goldPrimary)
                        Text("Authorized Signatory", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = goldLight)
                        Text(gymSetting.ownerSignatureName.ifBlank { "Gym Owner" }, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = ivoryText.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. PLATINUM CORPORATE TEMPLATE (Midnight Navy & Electric Cyan Split Cards)
// =========================================================================
@Composable
private fun PlatinumInvoiceLayout(
    invoice: Invoice,
    gymSetting: GymSetting,
    templateConfig: InvoiceTemplateConfig,
    invoiceDate: String,
    startDateStr: String,
    expiryDateStr: String,
    modifier: Modifier = Modifier
) {
    val navyBg = Color(0xFF0F172A)
    val cyanAccent = Color(0xFF0284C7)
    val cyanLight = Color(0xFF38BDF8)
    val panelBg = Color(0xFF1E293B)
    val textWhite = Color(0xFFF8FAFC)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_preview_${invoice.invoiceNumber}")
            .clip(RoundedCornerShape(16.dp))
            .background(navyBg)
            .border(2.5.dp, cyanAccent, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Corporate Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(cyanAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = gymSetting.gymName.ifBlank { "PLATINUM GYM ERP" }.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace),
                            color = cyanLight
                        )
                        Text("CORPORATE FITNESS & WELLNESS PASS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = textWhite.copy(alpha = 0.75f))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = cyanAccent.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cyanAccent)
                ) {
                    Text(
                        text = "INV #${invoice.invoiceNumber}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = cyanLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Split 2-Card Row: Member Info | Validity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = panelBg
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("MEMBER PROFILE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp), color = cyanLight)
                        Text(invoice.customerName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textWhite, maxLines = 1)
                        Text("ID: #CUST-${invoice.customerId}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = textWhite.copy(alpha = 0.6f))
                        Text("+91 ${invoice.customerMobile}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = textWhite.copy(alpha = 0.8f))
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = panelBg
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("VALIDITY WINDOW", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp), color = cyanLight)
                        Text(startDateStr, style = MaterialTheme.typography.bodySmall, color = textWhite.copy(alpha = 0.8f))
                        Text(expiryDateStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = cyanLight)
                        Text("Issued: $invoiceDate", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = textWhite.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Item Table Data Grid
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = panelBg
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MEMBERSHIP PACKAGE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = cyanLight)
                        Text("DURATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = cyanLight)
                        Text("FEE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = cyanLight)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = cyanAccent.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(invoice.planName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = textWhite)
                        Text(invoice.durationText, style = MaterialTheme.typography.bodySmall, color = textWhite.copy(alpha = 0.8f))
                        Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.totalAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = cyanLight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // KPI Financial Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF064E3B)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("PAID SETTLED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = Color(0xFFA7F3D0))
                        Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.paidAmount)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF34D399))
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (invoice.pendingAmount > 0) Color(0xFF7F1D1D) else Color(0xFF1E293B)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("BALANCE DUE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = if (invoice.pendingAmount > 0) Color(0xFFFECACA) else textWhite.copy(alpha = 0.6f))
                        Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.pendingAmount)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = if (invoice.pendingAmount > 0) Color(0xFFF87171) else Color(0xFF34D399))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signatory & Location QR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (templateConfig.showQrCode) {
                    val qrUrl = if (gymSetting.gymLocationUrl.isNotBlank()) gymSetting.gymLocationUrl else "https://maps.google.com/?q=${gymSetting.gymName}"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeView(content = qrUrl, modifier = Modifier.size(46.dp), darkColor = android.graphics.Color.BLACK, lightColor = android.graphics.Color.WHITE)
                        }
                        Text("Scan Location", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = cyanLight)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OwnerSignatureDisplay(name = gymSetting.ownerSignatureName, styleId = gymSetting.ownerSignatureStyleId, modifier = Modifier.width(110.dp).height(32.dp), color = cyanLight)
                    HorizontalDivider(modifier = Modifier.width(100.dp), thickness = 1.dp, color = cyanAccent)
                    Text("Authorized Signatory", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = cyanLight)
                    Text(gymSetting.ownerSignatureName.ifBlank { "Management" }, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = textWhite.copy(alpha = 0.6f))
                }
            }
        }
    }
}

// =========================================================================
// 4. DIAMOND PRESTIGE TEMPLATE (Royal Violet & Crystalline Glow)
// =========================================================================
@Composable
private fun DiamondInvoiceLayout(
    invoice: Invoice,
    gymSetting: GymSetting,
    templateConfig: InvoiceTemplateConfig,
    invoiceDate: String,
    startDateStr: String,
    expiryDateStr: String,
    modifier: Modifier = Modifier
) {
    val violetPrimary = Color(0xFF7C3AED)
    val violetDeep = Color(0xFF4C1D95)
    val violetLight = Color(0xFFC4B5FD)
    val surfaceViolet = Color(0xFF2E1065)
    val textWhite = Color(0xFFFAF5FF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_preview_${invoice.invoiceNumber}")
            .clip(RoundedCornerShape(18.dp))
            .background(surfaceViolet)
            .border(2.5.dp, violetPrimary, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Diamond Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(violetPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = gymSetting.gymName.ifBlank { "DIAMOND FITNESS CLUB" }.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                            color = textWhite
                        )
                        Text("DIAMOND PRESTIGE ATHLETIC SUITE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = violetLight)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = violetPrimary
                ) {
                    Text(
                        text = templateConfig.badgeText.ifBlank { "💎 DIAMOND PASS" },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = violetPrimary.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Member & Invoice Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = violetDeep,
                border = androidx.compose.foundation.BorderStroke(1.dp, violetPrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("REGISTERED MEMBER", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp), color = violetLight)
                        Text(invoice.customerName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = textWhite)
                        Text("Mobile: +91 ${invoice.customerMobile}", style = MaterialTheme.typography.bodySmall, color = textWhite.copy(alpha = 0.8f))
                        Text("Member ID: #CUST-${invoice.customerId}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = textWhite.copy(alpha = 0.6f))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("INVOICE NO: #${invoice.invoiceNumber.replace("#", "")}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = violetLight)
                        Text("Date: $invoiceDate", style = MaterialTheme.typography.bodySmall, color = textWhite.copy(alpha = 0.8f))
                        Text("Valid: $startDateStr - $expiryDateStr", style = MaterialTheme.typography.labelSmall, color = textWhite.copy(alpha = 0.7f))
                        Text("Tier: Diamond Access", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF34D399))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Plan Table Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = violetDeep,
                border = androidx.compose.foundation.BorderStroke(1.dp, violetPrimary)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(invoice.planName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = textWhite)
                        Text("${invoice.durationText} • Diamond Tier Facilities & Priority Access", style = MaterialTheme.typography.labelSmall, color = violetLight)
                    }
                    Text("₹${String.format(Locale.getDefault(), "%,.2f", invoice.totalAmount)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = textWhite)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Summary Block
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1B4B),
                border = androidx.compose.foundation.BorderStroke(1.dp, violetPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Mode: ${invoice.paymentMethod.displayName}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = textWhite)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Status: ${invoice.paymentStatus.displayName.uppercase()}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = violetLight)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Paid: ₹${String.format(Locale.getDefault(), "%,.2f", invoice.paidAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF34D399))
                        if (invoice.pendingAmount > 0) {
                            Text("Due: ₹${String.format(Locale.getDefault(), "%,.2f", invoice.pendingAmount)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFF87171))
                        } else {
                            Text("Due: ₹0.00", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF34D399))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Signatory & QR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (templateConfig.showQrCode) {
                    val qrUrl = if (gymSetting.gymLocationUrl.isNotBlank()) gymSetting.gymLocationUrl else "https://maps.google.com/?q=${gymSetting.gymName}"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeView(content = qrUrl, modifier = Modifier.size(48.dp), darkColor = android.graphics.Color.BLACK, lightColor = android.graphics.Color.WHITE)
                        }
                        Text("Scan Location", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = violetLight)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OwnerSignatureDisplay(name = gymSetting.ownerSignatureName, styleId = gymSetting.ownerSignatureStyleId, modifier = Modifier.width(110.dp).height(32.dp), color = violetLight)
                    HorizontalDivider(modifier = Modifier.width(100.dp), thickness = 1.dp, color = violetPrimary)
                    Text("Authorized Signatory", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = violetLight)
                    Text(gymSetting.ownerSignatureName.ifBlank { "Management" }, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = textWhite.copy(alpha = 0.6f))
                }
            }
        }
    }
}
