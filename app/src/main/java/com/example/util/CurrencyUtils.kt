package com.example.util

/**
 * Currency utility methods delegating to high-precision NumberFormatter.
 * Supports thousands separators, large amounts into billions, and localized currency symbols.
 */
object CurrencyUtils {
    const val CURRENCY_CODE = NumberFormatter.CURRENCY_PKR

    /**
     * Formats paisa (Long) into formatted string with thousands separators,
     * e.g. "PKR 12,500" or "PKR 12,500.50" or "-PKR 4,000"
     */
    fun formatPaisa(
        paisa: Long,
        showCurrency: Boolean = true,
        currencyPrefix: String = CURRENCY_CODE
    ): String {
        return NumberFormatter.formatPaisa(
            paisa = paisa,
            currencyPrefix = currencyPrefix,
            showCurrency = showCurrency
        )
    }

    /**
     * Converts user text input into paisa (minor unit Long).
     * Rejects negative, empty, non-numeric, or numbers exceeding the maximum limit.
     */
    fun parseToPaisa(input: String): Long? {
        return NumberFormatter.parseInputToPaisa(input)
    }

    /**
     * Allows 0 for cases where 0 is acceptable (e.g. paid amount on purchase).
     */
    fun parseToPaisaAllowZero(input: String): Long? {
        return NumberFormatter.parseInputAllowZero(input)
    }

    /**
     * Converts paisa to raw decimal string for prefilling edit text fields (e.g. "1250" or "1250.50")
     */
    fun paisaToEditableString(paisa: Long): String {
        return NumberFormatter.paisaToEditableString(paisa)
    }
}
