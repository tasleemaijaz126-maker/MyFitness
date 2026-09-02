package com.example.data.local

import com.example.data.local.entity.GymSetting

object InitialData {

    suspend fun populateInitialData(database: AppDatabase) {
        // No dummy customers, fake memberships, mock payments or fake invoices are populated.
        // The database starts in a clean zero-state.
        val existing = database.gymSettingDao().getSettingsSnapshot()
        if (existing == null) {
            database.gymSettingDao().insertOrUpdate(
                GymSetting(
                    id = 1,
                    gymName = "My Fitness Club",
                    gymTagline = "Gym Management ERP",
                    gymAddress = "",
                    gymCity = "",
                    gymPhone = "",
                    gymEmail = "",
                    gymGstin = "",
                    currencySymbol = "₹",
                    activeInvoiceTemplateId = "modern_clean",
                    appTheme = "DARK",
                    appLanguage = "ENGLISH",
                    requireOtpForInvoiceSend = false,
                    invoiceTerms = "1. Membership is non-refundable & non-transferable.\n2. Gym rules must be respected at all times.\n3. Digital invoice QR valid for turnstile check-in.",
                    lastInvoiceSequence = 0
                )
            )
        }
    }
}
