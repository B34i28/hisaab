package com.example

import com.example.util.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        assertEquals("PKR 1,500", CurrencyUtils.formatPaisa(150000L))
        assertEquals("PKR 1,500.50", CurrencyUtils.formatPaisa(150050L))
        assertEquals("-PKR 250", CurrencyUtils.formatPaisa(-25000L))
        assertEquals("PKR 0", CurrencyUtils.formatPaisa(0L))
    }

    @Test
    fun testCurrencyParsing() {
        assertEquals(150000L, CurrencyUtils.parseToPaisa("1500"))
        assertEquals(150050L, CurrencyUtils.parseToPaisa("1500.50"))
        assertEquals(150050L, CurrencyUtils.parseToPaisa("1,500.50"))
        assertNull(CurrencyUtils.parseToPaisa(""))
        assertNull(CurrencyUtils.parseToPaisa("-500"))
        assertNull(CurrencyUtils.parseToPaisa("abc"))
    }

    @Test
    fun testAllowZeroParsing() {
        assertEquals(0L, CurrencyUtils.parseToPaisaAllowZero(""))
        assertEquals(0L, CurrencyUtils.parseToPaisaAllowZero("0"))
        assertEquals(50000L, CurrencyUtils.parseToPaisaAllowZero("500"))
        assertNull(CurrencyUtils.parseToPaisaAllowZero("-100"))
    }
}
