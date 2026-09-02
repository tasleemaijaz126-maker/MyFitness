package com.example.data.model

object InvoiceTemplates {

    val SILVER = InvoiceTemplateConfig(
        id = "silver",
        name = "Silver Classic",
        subtitle = "Streamlined modern slate layout with refined linear dividers, minimalist header & clean data table",
        headerColor = 0xFF334155,
        accentColor = 0xFF64748B,
        backgroundColor = 0xFFF8FAFC,
        textColor = 0xFF0F172A,
        borderStyle = "SILVER_FRAME",
        showQrCode = true,
        showBadge = true,
        badgeText = "STANDARD TAX INVOICE",
        fontStyle = "MODERN"
    )

    val GOLD = InvoiceTemplateConfig(
        id = "gold",
        name = "Gold Elite",
        subtitle = "Prestige luxury onyx with metallic gold foil header, ornate badge, double-border & golden summary card",
        headerColor = 0xFF18181B,
        accentColor = 0xFFD97706,
        backgroundColor = 0xFFFFFBEB,
        textColor = 0xFF1C1917,
        borderStyle = "GOLD_ORNATE",
        showQrCode = true,
        showBadge = true,
        badgeText = "★ VIP CLUB MEMBERSHIP ★",
        fontStyle = "CLASSIC"
    )

    val PLATINUM = InvoiceTemplateConfig(
        id = "platinum",
        name = "Platinum Corporate",
        subtitle = "Contemporary high-tech deep midnight navy banner with cyan highlights, split card layout & bold metrics",
        headerColor = 0xFF0F172A,
        accentColor = 0xFF0284C7,
        backgroundColor = 0xFFF0F9FF,
        textColor = 0xFF082F49,
        borderStyle = "PLATINUM_SPLIT",
        showQrCode = true,
        showBadge = true,
        badgeText = "CORPORATE FITNESS PASS",
        fontStyle = "MONO"
    )

    val DIAMOND = InvoiceTemplateConfig(
        id = "diamond",
        name = "Diamond Prestige",
        subtitle = "Ultra-luxe royal violet & crystalline facet styling with holographic badge, glowing accents & receipt block",
        headerColor = 0xFF4C1D95,
        accentColor = 0xFF7C3AED,
        backgroundColor = 0xFFFAF5FF,
        textColor = 0xFF3B0764,
        borderStyle = "DIAMOND_CRYSTAL",
        showQrCode = true,
        showBadge = true,
        badgeText = "💎 DIAMOND PRESTIGE RECEIPT",
        fontStyle = "MODERN"
    )

    val ALL_TEMPLATES: List<InvoiceTemplateConfig> = listOf(
        SILVER,
        GOLD,
        PLATINUM,
        DIAMOND
    )

    val availableTemplates: List<InvoiceTemplateConfig> get() = ALL_TEMPLATES

    fun getTemplateById(id: String): InvoiceTemplateConfig {
        return when (id.lowercase()) {
            "silver", "silver_classic" -> SILVER
            "gold", "gold_elite", "classic_gold" -> GOLD
            "platinum", "platinum_pro", "ocean_corporate", "titanium_grid" -> PLATINUM
            "diamond", "diamond_prestige", "royal_velvet", "neon_cyber" -> DIAMOND
            else -> ALL_TEMPLATES.find { it.id.equals(id, ignoreCase = true) } ?: SILVER
        }
    }
}
