package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.local.entity.GymSetting
import com.example.data.local.entity.Invoice
import com.example.data.model.InvoiceTemplateConfig
import com.example.data.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceImageExporter {

    // Fixed standard portrait invoice canvas dimensions
    const val INVOICE_WIDTH_PX = 1080
    const val INVOICE_HEIGHT_PX = 1528

    /**
     * Renders a crisp, fixed-size 1080x1528 px bitmap invoice
     * with balanced layout, prominent typography, and zero excessive empty margins.
     */
    fun renderInvoiceToBitmap(
        context: Context,
        invoice: Invoice,
        gymSetting: GymSetting,
        templateConfig: InvoiceTemplateConfig,
        widthPx: Int = INVOICE_WIDTH_PX,
        heightPx: Int = INVOICE_HEIGHT_PX
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val headerColor = templateConfig.headerColor.toInt()
        val accentColor = templateConfig.accentColor.toInt()
        val bgColor = templateConfig.backgroundColor.toInt()
        val textColor = templateConfig.textColor.toInt()

        val isGold = templateConfig.id.contains("gold", ignoreCase = true)
        val isPlatinum = templateConfig.id.contains("platinum", ignoreCase = true)
        val isDiamond = templateConfig.id.contains("diamond", ignoreCase = true)
        val isDarkTheme = isGold || isPlatinum || isDiamond

        val baseTypeface = when {
            isGold -> Typeface.SERIF
            isPlatinum -> Typeface.MONOSPACE
            else -> Typeface.SANS_SERIF
        }
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val invoiceDateStr = dateFormat.format(Date(invoice.generatedDate))
        val startDateStr = dateFormat.format(Date(invoice.startDate))
        val expiryDateStr = dateFormat.format(Date(invoice.expiryDate))

        // -------------------------------------------------------------
        // 1. Base Canvas Background & Outer Border
        // -------------------------------------------------------------
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val cardRect = RectF(28f, 28f, widthPx - 28f, heightPx - 28f)
        canvas.drawRoundRect(cardRect, 28f, 28f, bgPaint)

        // Outer Template Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            when {
                isGold -> {
                    color = 0xFFD97706.toInt()
                    strokeWidth = 6f
                }
                isPlatinum -> {
                    color = 0xFF0284C7.toInt()
                    strokeWidth = 5f
                }
                isDiamond -> {
                    color = 0xFF7C3AED.toInt()
                    strokeWidth = 5f
                }
                else -> {
                    color = 0xFFCBD5E1.toInt()
                    strokeWidth = 4f
                }
            }
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        // Inner decorative border for Gold
        if (isGold) {
            val innerRect = RectF(40f, 40f, widthPx - 40f, heightPx - 40f)
            val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = 0xFFFBBF24.toInt()
                strokeWidth = 2f
                alpha = 140
            }
            canvas.drawRoundRect(innerRect, 20f, 20f, innerBorder)
        }

        val paddingLeft = 56f
        val paddingRight = widthPx - 56f
        val contentWidth = paddingRight - paddingLeft

        // -------------------------------------------------------------
        // 2. Header Section (Top: 56f to ~280f)
        // -------------------------------------------------------------
        var currentY = 62f

        // Gym Icon Emblem
        val iconRadius = 38f
        val iconCenterX = paddingLeft + iconRadius
        val iconCenterY = currentY + iconRadius

        val iconBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFD97706.toInt()
                isPlatinum -> 0xFF0284C7.toInt()
                isDiamond -> 0xFF7C3AED.toInt()
                else -> headerColor
            }
            style = Paint.Style.FILL
        }
        canvas.drawCircle(iconCenterX, iconCenterY, iconRadius, iconBgPaint)

        // Stylized Dumbbell / Star / Diamond emblem inside circle
        val emblemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            strokeWidth = 4.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(iconCenterX - 18f, iconCenterY, iconCenterX + 18f, iconCenterY, emblemPaint)
        canvas.drawRect(
            iconCenterX - 24f, iconCenterY - 13f,
            iconCenterX - 16f, iconCenterY + 13f,
            emblemPaint.apply { style = Paint.Style.FILL }
        )
        canvas.drawRect(
            iconCenterX + 16f, iconCenterY - 13f,
            iconCenterX + 24f, iconCenterY + 13f,
            emblemPaint
        )

        // Gym Name & Tagline
        val textStartX = paddingLeft + (iconRadius * 2f) + 20f
        val gymNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFFAF5FF.toInt()
                else -> headerColor
            }
            textSize = 38f
            typeface = boldTypeface
            letterSpacing = 0.03f
        }
        val gymDisplayName = if (gymSetting.gymName.isNotBlank()) gymSetting.gymName.uppercase() else "MY FITNESS ERP"
        canvas.drawText(gymDisplayName, textStartX, currentY + 34f, gymNamePaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 20f
            typeface = baseTypeface
            alpha = 190
        }
        val tagline = if (gymSetting.gymTagline.isNotBlank()) {
            gymSetting.gymTagline
        } else when {
            isGold -> "VIP CLUB & LUXURY WELLNESS"
            isPlatinum -> "CORPORATE FITNESS PASS"
            isDiamond -> "PRESTIGE ATHLETIC SUITE"
            else -> "OFFICIAL FITNESS TAX INVOICE"
        }
        canvas.drawText(tagline, textStartX, currentY + 66f, taglinePaint)

        // Top-Right: Template Badge & Invoice Number
        val badgeText = templateConfig.badgeText
        if (templateConfig.showBadge) {
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when {
                    isGold -> 0xFFD97706.toInt()
                    isPlatinum -> 0xFF0284C7.toInt()
                    isDiamond -> 0xFF7C3AED.toInt()
                    else -> headerColor
                }
                style = Paint.Style.FILL
            }
            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isGold) 0xFF18181B.toInt() else android.graphics.Color.WHITE
                textSize = 19f
                typeface = boldTypeface
            }
            val bWidth = badgeTextPaint.measureText(badgeText) + 32f
            val bHeight = 38f
            val bLeft = paddingRight - bWidth
            val bTop = currentY
            canvas.drawRoundRect(RectF(bLeft, bTop, paddingRight, bTop + bHeight), 10f, 10f, badgeBgPaint)
            canvas.drawText(badgeText, bLeft + 16f, bTop + 26f, badgeTextPaint)
        }

        // Invoice Number & Date (Right side)
        val invNoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF94A3B8.toInt() else textColor
            textSize = 18f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        val invNoValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFC4B5FD.toInt()
                else -> headerColor
            }
            textSize = 32f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        val dateValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 20f
            typeface = baseTypeface
            textAlign = Paint.Align.RIGHT
            alpha = 200
        }

        canvas.drawText("INVOICE NO", paddingRight, currentY + 70f, invNoLabelPaint)
        canvas.drawText("#${invoice.invoiceNumber.replace("#", "")}", paddingRight, currentY + 106f, invNoValPaint)
        canvas.drawText("Date: $invoiceDateStr", paddingRight, currentY + 136f, dateValPaint)

        // Contact info lines under gym name (Left side)
        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF94A3B8.toInt() else textColor
            textSize = 19f
            typeface = baseTypeface
            alpha = 200
        }
        val fullAddr = listOf(gymSetting.gymAddress, gymSetting.gymCity).filter { it.isNotBlank() }.joinToString(", ")
        var addrY = currentY + 104f
        if (fullAddr.isNotBlank()) {
            canvas.drawText("📍 $fullAddr", paddingLeft, addrY, contactPaint)
            addrY += 27f
        }
        val phoneEmail = "📞 ${gymSetting.gymPhone.ifBlank { "+91 98765 43210" }}   |   ✉️ ${gymSetting.gymEmail.ifBlank { "contact@myfitness.com" }}"
        canvas.drawText(phoneEmail, paddingLeft, addrY, contactPaint)
        addrY += 27f

        if (gymSetting.gymGstin.isNotBlank()) {
            val gstinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when {
                    isGold -> 0xFFFBBF24.toInt()
                    isPlatinum -> 0xFF38BDF8.toInt()
                    isDiamond -> 0xFFC4B5FD.toInt()
                    else -> accentColor
                }
                textSize = 19f
                typeface = boldTypeface
            }
            canvas.drawText("GSTIN / Tax ID: ${gymSetting.gymGstin}", paddingLeft, addrY, gstinPaint)
            addrY += 27f
        }

        currentY = maxOf(addrY + 18f, currentY + 160f)

        // -------------------------------------------------------------
        // 3. Accent Divider Line
        // -------------------------------------------------------------
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFD97706.toInt()
                isPlatinum -> 0xFF0284C7.toInt()
                isDiamond -> 0xFF7C3AED.toInt()
                else -> 0xFFCBD5E1.toInt()
            }
            strokeWidth = 2.5f
            alpha = 140
        }
        canvas.drawLine(paddingLeft, currentY, paddingRight, currentY, divPaint)
        currentY += 26f

        // -------------------------------------------------------------
        // 4. Member Details & Validity Period Card (~190px height)
        // -------------------------------------------------------------
        val memberCardHeight = 175f
        val memberCardRect = RectF(paddingLeft, currentY, paddingRight, currentY + memberCardHeight)

        val memberCardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFF27272A.toInt()
                isPlatinum -> 0xFF1E293B.toInt()
                isDiamond -> 0xFF3B0764.toInt()
                else -> 0xFFF1F5F9.toInt()
            }
            style = Paint.Style.FILL
        }
        val memberCardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFD97706.toInt()
                isPlatinum -> 0xFF0284C7.toInt()
                isDiamond -> 0xFF7C3AED.toInt()
                else -> 0xFFE2E8F0.toInt()
            }
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(memberCardRect, 16f, 16f, memberCardBgPaint)
        canvas.drawRoundRect(memberCardRect, 16f, 16f, memberCardBorderPaint)

        val colPad = 22f
        val leftColX = paddingLeft + colPad
        val rightColX = paddingRight - colPad
        var cardInnerY = currentY + 34f

        // Left Col: Billed To Member
        val secHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFC4B5FD.toInt()
                else -> accentColor
            }
            textSize = 19f
            typeface = boldTypeface
            letterSpacing = 0.04f
        }
        canvas.drawText("BILLED TO / MEMBER DETAILS", leftColX, cardInnerY, secHeaderPaint)

        // Right Col: Validity Window
        val secHeaderRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFC4B5FD.toInt()
                else -> accentColor
            }
            textSize = 19f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
            letterSpacing = 0.04f
        }
        canvas.drawText("MEMBERSHIP VALIDITY PERIOD", rightColX, cardInnerY, secHeaderRightPaint)
        cardInnerY += 36f

        // Member Name & Validity Range
        val custNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) android.graphics.Color.WHITE else textColor
            textSize = 32f
            typeface = boldTypeface
        }
        canvas.drawText(invoice.customerName, leftColX, cardInnerY, custNamePaint)

        val validityRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFFAF5FF.toInt()
                else -> headerColor
            }
            textSize = 26f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("$startDateStr  to  $expiryDateStr", rightColX, cardInnerY, validityRangePaint)
        cardInnerY += 32f

        // Mobile & Member ID / Duration Text
        val custDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 22f
            typeface = baseTypeface
            alpha = 210
        }
        canvas.drawText("Mobile: +91 ${invoice.customerMobile}", leftColX, cardInnerY, custDetailPaint)

        val durationBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 22f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Plan Duration: ${invoice.durationText}", rightColX, cardInnerY, durationBadgePaint)
        cardInnerY += 28f

        val custIdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF94A3B8.toInt() else textColor
            textSize = 19f
            typeface = baseTypeface
            alpha = 170
        }
        canvas.drawText("Member ID: #CUST-${invoice.customerId}", leftColX, cardInnerY, custIdPaint)

        val statusTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF34D399.toInt()
            textSize = 19f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Status: Active Membership Access", rightColX, cardInnerY, statusTagPaint)

        currentY += memberCardHeight + 26f

        // -------------------------------------------------------------
        // 5. Itemized Membership Plan Table (~220px height)
        // -------------------------------------------------------------
        val tableHeaderHeight = 52f
        val tableHeaderRect = RectF(paddingLeft, currentY, paddingRight, currentY + tableHeaderHeight)

        val thBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFF3F3F46.toInt()
                isPlatinum -> 0xFF334155.toInt()
                isDiamond -> 0xFF4C1D95.toInt()
                else -> 0xFFE2E8F0.toInt()
            }
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(tableHeaderRect, 12f, 12f, thBgPaint)

        val thTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFFAF5FF.toInt()
                else -> headerColor
            }
            textSize = 20f
            typeface = boldTypeface
            letterSpacing = 0.03f
        }
        canvas.drawText("ITEM / PLAN DESCRIPTION", paddingLeft + 20f, currentY + 34f, thTextPaint)

        val thCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFFAF5FF.toInt()
                else -> headerColor
            }
            textSize = 20f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.03f
        }
        canvas.drawText("DURATION", paddingLeft + (contentWidth * 0.62f), currentY + 34f, thCenterPaint)

        val thRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFFAF5FF.toInt()
                else -> headerColor
            }
            textSize = 20f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
            letterSpacing = 0.03f
        }
        canvas.drawText("AMOUNT (INR)", paddingRight - 20f, currentY + 34f, thRightPaint)
        currentY += tableHeaderHeight + 28f

        // Table Item Row
        val planNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) android.graphics.Color.WHITE else textColor
            textSize = 28f
            typeface = boldTypeface
        }
        canvas.drawText(invoice.planName, paddingLeft + 20f, currentY, planNamePaint)

        val durationValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 24f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(invoice.durationText, paddingLeft + (contentWidth * 0.62f), currentY, durationValPaint)

        val planPricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFFAF5FF.toInt()
                else -> headerColor
            }
            textSize = 30f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", invoice.totalAmount)}", paddingRight - 20f, currentY, planPricePaint)
        currentY += 32f

        val planDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF94A3B8.toInt() else textColor
            textSize = 19f
            typeface = baseTypeface
            alpha = 180
        }
        canvas.drawText("✓ Full Gym Floor & Fitness Facilities   ✓ Trainer Guidance & Lockers", paddingLeft + 20f, currentY, planDescPaint)
        currentY += 36f

        canvas.drawLine(paddingLeft, currentY, paddingRight, currentY, divPaint)
        currentY += 26f

        // -------------------------------------------------------------
        // 6. Payment & Financial Summary Section (~230px height)
        // -------------------------------------------------------------
        val summaryCardHeight = 225f
        val summaryCardRect = RectF(paddingLeft, currentY, paddingRight, currentY + summaryCardHeight)

        val summaryCardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFF27272A.toInt()
                isPlatinum -> 0xFF1E293B.toInt()
                isDiamond -> 0xFF3B0764.toInt()
                else -> 0xFFF1F5F9.toInt()
            }
            style = Paint.Style.FILL
        }
        val summaryCardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFD97706.toInt()
                isPlatinum -> 0xFF0284C7.toInt()
                isDiamond -> 0xFF7C3AED.toInt()
                else -> 0xFFCBD5E1.toInt()
            }
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(summaryCardRect, 16f, 16f, summaryCardBgPaint)
        canvas.drawRoundRect(summaryCardRect, 16f, 16f, summaryCardBorderPaint)

        var sumInnerY = currentY + 38f
        val sumLeftX = paddingLeft + 24f
        val sumRightX = paddingRight - 24f
        val sumLabelX = paddingRight - 360f

        // Left: Payment Information
        canvas.drawText("PAYMENT INFORMATION", sumLeftX, sumInnerY, secHeaderPaint)

        // Right: Total Amount
        val sumLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 22f
            typeface = baseTypeface
        }
        val sumTotalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) android.graphics.Color.WHITE else textColor
            textSize = 26f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Total Plan Amount:", sumLabelX, sumInnerY, sumLabelPaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", invoice.totalAmount)}", sumRightX, sumInnerY, sumTotalValPaint)
        sumInnerY += 40f

        // Left: Payment Method Mode
        val payModePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) android.graphics.Color.WHITE else textColor
            textSize = 24f
            typeface = boldTypeface
        }
        canvas.drawText("Mode: ${invoice.paymentMethod.displayName}", sumLeftX, sumInnerY, payModePaint)

        // Right: Amount Paid (Emerald)
        val paidLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF34D399.toInt()
            textSize = 24f
            typeface = boldTypeface
        }
        val paidValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF34D399.toInt()
            textSize = 30f
            typeface = boldTypeface
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Amount Paid:", sumLabelX, sumInnerY, paidLabelPaint)
        canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", invoice.paidAmount)}", sumRightX, sumInnerY, paidValPaint)
        sumInnerY += 44f

        // Left: Payment Status Badge Pill
        val statusBadgeColor = when (invoice.paymentStatus) {
            PaymentStatus.PAID -> 0xFF059669.toInt()
            PaymentStatus.PARTIALLY_PAID -> 0xFFD97706.toInt()
            else -> 0xFFDC2626.toInt()
        }
        val statusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = statusBadgeColor
            style = Paint.Style.FILL
        }
        val statusText = invoice.paymentStatus.displayName.uppercase()
        val sTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 19f
            typeface = boldTypeface
        }
        val sWidth = sTextPaint.measureText(statusText) + 32f
        val sHeight = 40f
        val sRect = RectF(sumLeftX, sumInnerY - 26f, sumLeftX + sWidth, sumInnerY - 26f + sHeight)
        canvas.drawRoundRect(sRect, 10f, 10f, statusBgPaint)
        canvas.drawText(statusText, sumLeftX + 16f, sumInnerY - 26f + 27f, sTextPaint)

        // Right: Pending Due or Zero Due
        if (invoice.pendingAmount > 0) {
            val dueLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF87171.toInt()
                textSize = 24f
                typeface = boldTypeface
            }
            val dueValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF87171.toInt()
                textSize = 30f
                typeface = boldTypeface
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Pending Balance Due:", sumLabelX, sumInnerY, dueLabelPaint)
            canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", invoice.pendingAmount)}", sumRightX, sumInnerY, dueValPaint)
        } else {
            val clearLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF34D399.toInt()
                textSize = 20f
                typeface = boldTypeface
            }
            val clearValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF34D399.toInt()
                textSize = 22f
                typeface = boldTypeface
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Balance Due:", sumLabelX, sumInnerY, clearLabelPaint)
            canvas.drawText("₹0.00 (Fully Settled)", sumRightX, sumInnerY, clearValPaint)
        }
        sumInnerY += 38f

        // Payment Reference / Date text
        val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF94A3B8.toInt() else textColor
            textSize = 19f
            typeface = baseTypeface
            alpha = 180
        }
        canvas.drawText("Receipt Date: $invoiceDateStr  •  Official Digital ERP Record", sumLeftX, sumInnerY, refPaint)

        currentY += summaryCardHeight + 26f

        // -------------------------------------------------------------
        // 7. Divider before Terms & Location QR & Signatory
        // -------------------------------------------------------------
        canvas.drawLine(paddingLeft, currentY, paddingRight, currentY, divPaint)
        currentY += 26f

        // -------------------------------------------------------------
        // 8. Terms & Conditions (Left) & QR Code (Right) (~190px)
        // -------------------------------------------------------------
        val qrBoxSize = 160f
        val qrBoxLeft = paddingRight - qrBoxSize
        val qrBoxTop = currentY

        // Terms & Conditions on Left
        canvas.drawText("TERMS & CONDITIONS", paddingLeft, currentY + 10f, secHeaderPaint)

        val termsTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF94A3B8.toInt() else textColor
            textSize = 17f
            typeface = baseTypeface
            alpha = 190
        }
        val defaultTerms = "1. Membership fee is non-refundable and non-transferable.\n" +
                "2. Please carry your digital ID or phone number when visiting.\n" +
                "3. Follow gym safety guidelines, rules, and trainer etiquette."
        val termsToRender = gymSetting.invoiceTerms.ifBlank { defaultTerms }
        val termsWidth = (contentWidth - qrBoxSize - 40f).toInt()
        val termsLayout = StaticLayout.Builder.obtain(
            termsToRender,
            0,
            termsToRender.length,
            termsTextPaint,
            termsWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(5).build()

        canvas.save()
        canvas.translate(paddingLeft, currentY + 22f)
        termsLayout.draw(canvas)
        canvas.restore()

        // Google Maps Location QR Code on Right
        val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val qrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFD97706.toInt()
                isPlatinum -> 0xFF0284C7.toInt()
                isDiamond -> 0xFF7C3AED.toInt()
                else -> 0xFFCBD5E1.toInt()
            }
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val qrRect = RectF(qrBoxLeft, qrBoxTop, paddingRight, qrBoxTop + qrBoxSize)
        canvas.drawRoundRect(qrRect, 14f, 14f, qrBgPaint)
        canvas.drawRoundRect(qrRect, 14f, 14f, qrBorderPaint)

        val qrContent = if (gymSetting.gymLocationUrl.isNotBlank()) {
            gymSetting.gymLocationUrl
        } else {
            "https://maps.google.com/?q=${Uri.encode(gymSetting.gymName.ifBlank { "My Fitness Gym" })}"
        }
        val qrBitmap = QrCodeGenerator.generateQrBitmap(
            content = qrContent,
            sizePx = 320,
            darkColor = android.graphics.Color.BLACK,
            lightColor = android.graphics.Color.WHITE
        )
        if (qrBitmap != null) {
            val qrDstRect = RectF(qrBoxLeft + 10f, qrBoxTop + 10f, paddingRight - 10f, qrBoxTop + qrBoxSize - 10f)
            canvas.drawBitmap(qrBitmap, null, qrDstRect, null)
        }

        val qrCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 16f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
            alpha = 200
        }
        canvas.drawText("Scan to Locate Gym", qrBoxLeft + (qrBoxSize / 2f), qrBoxTop + qrBoxSize + 22f, qrCaptionPaint)

        currentY += maxOf(termsLayout.height + 40f, qrBoxSize + 36f) + 16f

        // -------------------------------------------------------------
        // 9. Signatory & Digital Verification Seal (~140px)
        // -------------------------------------------------------------
        val sigCenterX = paddingRight - 140f
        val sigCenterY = currentY + 34f

        // Draw owner's dynamic signature flourish
        SignatureHelper.drawSignatureOnCanvas(
            canvas = canvas,
            name = gymSetting.ownerSignatureName.ifBlank { "Gym Owner" },
            styleId = gymSetting.ownerSignatureStyleId,
            centerX = sigCenterX,
            centerY = sigCenterY,
            color = when {
                isGold -> 0xFFFBBF24.toInt()
                isPlatinum -> 0xFF38BDF8.toInt()
                isDiamond -> 0xFFC4B5FD.toInt()
                else -> headerColor
            },
            scale = 1.35f,
            context = context
        )

        val signLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFD97706.toInt()
                isPlatinum -> 0xFF0284C7.toInt()
                isDiamond -> 0xFF7C3AED.toInt()
                else -> accentColor
            }
            strokeWidth = 2.5f
            alpha = 180
        }
        canvas.drawLine(sigCenterX - 120f, sigCenterY + 28f, sigCenterX + 120f, sigCenterY + 28f, signLinePaint)

        val signLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFFCBD5E1.toInt() else textColor
            textSize = 19f
            typeface = boldTypeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Authorized Signatory", sigCenterX, sigCenterY + 54f, signLabelPaint)

        val ownerNameLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF94A3B8.toInt() else textColor
            textSize = 17f
            typeface = baseTypeface
            textAlign = Paint.Align.CENTER
            alpha = 170
        }
        canvas.drawText(gymSetting.ownerSignatureName.ifBlank { "Gym Management" }, sigCenterX, sigCenterY + 76f, ownerNameLabelPaint)

        // Bottom Left: Official Digital Seal / Watermark
        val sealBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                isGold -> 0xFFD97706.toInt()
                isPlatinum -> 0xFF0284C7.toInt()
                isDiamond -> 0xFF7C3AED.toInt()
                else -> 0xFF059669.toInt()
            }
            textSize = 19f
            typeface = boldTypeface
        }
        canvas.drawText("✓ AUTHENTIC DIGITAL INVOICE", paddingLeft, sigCenterY + 30f, sealBadgePaint)

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkTheme) 0xFF64748B.toInt() else textColor
            textSize = 17f
            typeface = baseTypeface
            alpha = 150
        }
        canvas.drawText("Generated via My Fitness Gym ERP • Thank you for training with us!", paddingLeft, sigCenterY + 56f, watermarkPaint)

        return bitmap
    }

    /**
     * Saves invoice image directly into user's Gallery (Pictures/MyFitness_Invoices)
     */
    suspend fun saveInvoiceToGallery(
        context: Context,
        bitmap: Bitmap,
        invoiceNumber: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val fileName = "MyFitness_Invoice_${invoiceNumber.replace("#", "").replace("/", "_")}.png"
            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MyFitness_Invoices")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                    savedUri = imageUri
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "MyFitness_Invoices").apply { mkdirs() }
                val imageFile = File(appDir, fileName)
                val out: OutputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf("image/png"),
                    null
                )
                savedUri = Uri.fromFile(imageFile)
            }

            if (savedUri != null) {
                Result.success(savedUri)
            } else {
                Result.failure(Exception("Could not create media store record"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a shareable content:// URI for the generated invoice bitmap via FileProvider
     */
    fun getShareableInvoiceUri(
        context: Context,
        bitmap: Bitmap,
        invoiceNumber: String
    ): Uri {
        val cacheFolder = File(context.cacheDir, "shared_invoices").apply { mkdirs() }
        val cleanName = invoiceNumber.replace("#", "").replace("/", "_")
        val imageFile = File(cacheFolder, "MyFitness_Invoice_$cleanName.png")

        val out = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
        out.close()

        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, imageFile)
    }

    /**
     * Directly triggers Android Intent Chooser to share the generated invoice image via WhatsApp/any app
     */
    fun shareInvoiceImage(
        context: Context,
        invoice: Invoice,
        gymSetting: GymSetting,
        templateConfig: InvoiceTemplateConfig
    ) {
        val bitmap = renderInvoiceToBitmap(context, invoice, gymSetting, templateConfig)
        val imageUri = getShareableInvoiceUri(context, bitmap, invoice.invoiceNumber)

        val shareText = buildString {
            appendLine("🏋️ *${gymSetting.gymName.ifBlank { "My Fitness Gym" }}*")
            appendLine("📄 *Invoice #${invoice.invoiceNumber}*")
            appendLine("👤 Member: *${invoice.customerName}*")
            appendLine("📦 Plan: *${invoice.planName}* (${invoice.durationText})")
            appendLine("💰 Paid: ₹${String.format(Locale.getDefault(), "%,.2f", invoice.paidAmount)}")
            if (invoice.pendingAmount > 0) {
                appendLine("⚠️ Due: ₹${String.format(Locale.getDefault(), "%,.2f", invoice.pendingAmount)}")
            }
            appendLine("📌 Status: ${invoice.paymentStatus.displayName.uppercase()}")
            appendLine("✨ _Thank you for training with us!_")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice #${invoice.invoiceNumber} - ${gymSetting.gymName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Invoice #${invoice.invoiceNumber} via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
