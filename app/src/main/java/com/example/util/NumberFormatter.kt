package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * High-performance, overflow-safe number and currency formatting for ShopLedger.
 * Guarantees proper thousands separators (e.g. 1,250,000), avoids scientific notation (e.g. 1.5E7),
 * and safely supports amounts into hundreds of billions of rupees while guarding against arithmetic overflow.
 */
object NumberFormatter {

    const val CURRENCY_PKR = "PKR"
    const val CURRENCY_RS = "Rs"
    const val CURRENCY_URDU = "روپے"

    // Safe ceiling: 999 Billion Rupees (99.99 Trillion Paisa)
    // Fits comfortably in 64-bit signed Long (max ~9.22 Quintillion) without overflow in calculations.
    const val MAX_AMOUNT_PAISA = 99_999_999_999_900L

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val integerFormatter = DecimalFormat("#,##0", symbols)
    private val decimalFormatter = DecimalFormat("#,##0.00", symbols)

    /**
     * Formats paisa (Long) into rupee representation with thousands separators.
     * e.g., 125000000L paisa -> "PKR 1,250,000"
     * e.g., 125000050L paisa -> "PKR 1,250,000.50"
     * e.g., -5000000L paisa -> "-PKR 50,000"
     */
    fun formatPaisa(
        paisa: Long,
        currencyPrefix: String = CURRENCY_PKR,
        showCurrency: Boolean = true
    ): String {
        val isNegative = paisa < 0
        val absPaisa = if (paisa == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(paisa)
        val cents = absPaisa % 100

        val bd = BigDecimal(absPaisa).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val formattedNumber = if (cents > 0) {
            decimalFormatter.format(bd)
        } else {
            integerFormatter.format(bd)
        }

        val sign = if (isNegative) "-" else ""
        return if (showCurrency && currencyPrefix.isNotBlank()) {
            "$sign$currencyPrefix $formattedNumber"
        } else {
            "$sign$formattedNumber"
        }
    }

    /**
     * Formats an integer amount with thousands separators.
     * e.g., 1500000L -> "1,500,000"
     */
    fun formatNumber(value: Long): String {
        return integerFormatter.format(value)
    }

    /**
     * Formats a double with thousands separators and optional decimals.
     */
    fun formatDouble(value: Double, decimals: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val formatter = if (decimals == 0) {
            integerFormatter
        } else {
            DecimalFormat("#,##0." + "0".repeat(decimals), symbols)
        }
        return formatter.format(value)
    }

    /**
     * Formats a percentage safely, e.g. "+15.2%" or "-4.0%".
     */
    fun formatPercentage(percentage: Double, includeSign: Boolean = false): String {
        if (percentage.isNaN() || percentage.isInfinite()) return "0.0%"
        val sign = if (includeSign && percentage > 0) "+" else ""
        val rounded = BigDecimal(percentage).setScale(1, RoundingMode.HALF_UP).toDouble()
        return "$sign${rounded}%"
    }

    /**
     * Safely parses user input string (which may include commas or spaces) to minor units (paisa).
     * Returns null if empty, non-numeric, zero, negative, or exceeding MAX_AMOUNT_PAISA.
     */
    fun parseInputToPaisa(input: String): Long? {
        val clean = input.trim().replace(",", "").replace(" ", "")
        if (clean.isBlank()) return null
        return try {
            val bd = BigDecimal(clean)
            if (bd <= BigDecimal.ZERO) return null
            val paisaBd = bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
            val paisa = paisaBd.longValueExact()
            if (paisa > MAX_AMOUNT_PAISA) null else paisa
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safely parses user input string to paisa, allowing zero.
     * Returns null if invalid or exceeding MAX_AMOUNT_PAISA.
     */
    fun parseInputAllowZero(input: String): Long? {
        val clean = input.trim().replace(",", "").replace(" ", "")
        if (clean.isBlank()) return 0L
        return try {
            val bd = BigDecimal(clean)
            if (bd < BigDecimal.ZERO) return null
            val paisaBd = bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP)
            val paisa = paisaBd.longValueExact()
            if (paisa > MAX_AMOUNT_PAISA) null else paisa
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts paisa to raw string without thousands separators for prefilling editable fields.
     */
    fun paisaToEditableString(paisa: Long): String {
        if (paisa == 0L) return ""
        val bd = BigDecimal(paisa).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return bd.stripTrailingZeros().toPlainString()
    }
}
