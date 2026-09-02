package com.example.data.model

enum class MembershipStatus(val displayName: String) {
    ACTIVE("Active"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
    INACTIVE("Inactive"),
    PENDING_PAYMENT("Pending Payment")
}

enum class PaymentStatus(val displayName: String) {
    PAID("Paid"),
    PARTIALLY_PAID("Partially Paid"),
    PENDING("Pending"),
    OVERDUE("Overdue")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    UPI("UPI / QR"),
    CREDIT_DEBIT_CARD("Credit / Debit Card"),
    NET_BANKING("Net Banking"),
    CHEQUE("Cheque")
}

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    MARATHI("mr", "Marathi", "मराठी"),
    URDU("ur", "Urdu", "اردو")
}

enum class AppThemeMode(
    val code: String,
    val displayName: String,
    val description: String,
    val primaryHex: Long,
    val surfaceHex: Long,
    val accentHex: Long
) {
    CLASSIC(
        code = "CLASSIC",
        displayName = "Classic Gym",
        description = "High-contrast slate canvas with bold crimson sports headers and action accents",
        primaryHex = 0xFFE11D48,
        surfaceHex = 0xFF0F172A,
        accentHex = 0xFFF59E0B
    ),
    MODERN(
        code = "MODERN",
        displayName = "Modern Cyber",
        description = "Sleek deep midnight cyberpunk layout with vivid electric cyan & lime highlights",
        primaryHex = 0xFF06B6D4,
        surfaceHex = 0xFF0B132B,
        accentHex = 0xFF10B981
    ),
    PREMIUM(
        code = "PREMIUM",
        displayName = "Premium Gold",
        description = "Luxury deep charcoal & champagne gold theme with warm amber elegance",
        primaryHex = 0xFFF59E0B,
        surfaceHex = 0xFF18181B,
        accentHex = 0xFFD97706
    ),
    MINIMAL(
        code = "MINIMAL",
        displayName = "Minimal Clean",
        description = "Ultra-clean crisp titanium canvas with fresh emerald accents and airy layout",
        primaryHex = 0xFF059669,
        surfaceHex = 0xFFFFFFFF,
        accentHex = 0xFF10B981
    );

    companion object {
        fun fromString(value: String): AppThemeMode {
            return when (value.uppercase()) {
                "MODERN" -> MODERN
                "PREMIUM" -> PREMIUM
                "MINIMAL", "LIGHT" -> MINIMAL
                "CLASSIC", "DARK", "SYSTEM" -> CLASSIC
                else -> CLASSIC
            }
        }
    }
}

enum class ReportPeriod(val displayName: String) {
    DAILY("Daily"),
    MONTHLY("Current Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    YEARLY("Yearly (2026)"),
    CUSTOM("Custom Date Range")
}

data class PlanRevenueStat(
    val planName: String,
    val count: Int,
    val revenue: Double,
    val percentage: Float
)

data class MonthlyRevenuePoint(
    val monthLabel: String,
    val amount: Double,
    val memberCount: Int
)

data class InvoiceTemplateConfig(
    val id: String,
    val name: String,
    val subtitle: String,
    val headerColor: Long,
    val accentColor: Long,
    val backgroundColor: Long,
    val textColor: Long,
    val borderStyle: String, // "SILVER_FRAME", "GOLD_ORNATE", "PLATINUM_SPLIT", "DIAMOND_CRYSTAL"
    val showQrCode: Boolean = true,
    val showBadge: Boolean = true,
    val badgeText: String = "OFFICIAL INVOICE",
    val fontStyle: String = "MODERN" // "MODERN", "CLASSIC", "MONO"
) {
    val description: String get() = subtitle
}
