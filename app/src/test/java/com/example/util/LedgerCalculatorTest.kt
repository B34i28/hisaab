package com.example.util

import com.example.data.local.entity.Customer
import com.example.data.local.entity.CustomerPayment
import com.example.data.local.entity.CustomerWriteOff
import com.example.data.local.entity.Expense
import com.example.data.local.entity.Purchase
import com.example.data.local.entity.Sale
import com.example.data.local.entity.Supplier
import com.example.data.local.entity.SupplierPayment
import com.example.data.repository.CustomerLedgerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Complete unit tests for LedgerCalculator and NumberFormatter.
 * Tests profit calculation, supplier balance, cash flow, profit margins,
 * zero data edge cases, very large amounts, and thousands separator formatting.
 */
class LedgerCalculatorTest {

    // =========================================================================
    // 1. Profit Calculation Tests
    // =========================================================================

    @Test
    fun testCalculateProfit_positiveProfit() {
        // Sales: Rs 100,000 (10,000,000 paisa)
        // Expenses: Rs 20,000 (2,000,000 paisa)
        // Purchases: Rs 50,000 (5,000,000 paisa)
        // Expected Profit: Rs 30,000 (3,000,000 paisa)
        val profit = LedgerCalculator.calculateProfit(
            totalSales = 10_000_000L,
            totalExpenses = 2_000_000L,
            totalPurchases = 5_000_000L
        )
        assertEquals(3_000_000L, profit)
    }

    @Test
    fun testCalculateProfit_lossScenario() {
        // Sales: Rs 10,000, Expenses: Rs 8,000, Purchases: Rs 15,000 -> Loss of Rs 13,000
        val profit = LedgerCalculator.calculateProfit(
            totalSales = 1_000_000L,
            totalExpenses = 800_000L,
            totalPurchases = 1_500_000L
        )
        assertEquals(-1_300_000L, profit)
    }

    @Test
    fun testCalculateProfit_zeroData() {
        val profit = LedgerCalculator.calculateProfit(0L, 0L, 0L)
        assertEquals(0L, profit)
    }

    @Test
    fun testCalculateProfit_veryLargeAmounts() {
        // Rs 50 Billion = 5,000,000,000,000 paisa
        val sales = 5_000_000_000_000L
        val expenses = 1_000_000_000_000L
        val purchases = 2_500_000_000_000L
        val expected = 1_500_000_000_000L

        val profit = LedgerCalculator.calculateProfit(sales, expenses, purchases)
        assertEquals(expected, profit)
    }

    // =========================================================================
    // 2. Profit Margin Percentage Tests
    // =========================================================================

    @Test
    fun testCalculateProfitMargin_standard() {
        // Sales: 10,000,000 paisa, Profit: 2,500,000 paisa -> 25.0%
        val margin = LedgerCalculator.calculateProfitMargin(
            totalSales = 10_000_000L,
            netProfit = 2_500_000L
        )
        assertEquals(25.0, margin, 0.001)
    }

    @Test
    fun testCalculateProfitMargin_zeroSales_neverThrowsOrReturnsNaN() {
        val marginZero = LedgerCalculator.calculateProfitMargin(totalSales = 0L, netProfit = 0L)
        assertEquals(0.0, marginZero, 0.0001)

        val marginWithProfit = LedgerCalculator.calculateProfitMargin(totalSales = 0L, netProfit = 5000L)
        assertEquals(0.0, marginWithProfit, 0.0001)
    }

    @Test
    fun testCalculateProfitMargin_negativeSales_safe() {
        val margin = LedgerCalculator.calculateProfitMargin(totalSales = -100L, netProfit = -50L)
        assertEquals(0.0, margin, 0.0001)
    }

    @Test
    fun testCalculateProfitMargin_negativeProfit_scenario() {
        // Sales: 1,000,000, Profit: -200,000 -> -20.0%
        val margin = LedgerCalculator.calculateProfitMargin(
            totalSales = 1_000_000L,
            netProfit = -200_000L
        )
        assertEquals(-20.0, margin, 0.001)
    }

    @Test
    fun testCalculateProfitMargin_hundredPercent() {
        val margin = LedgerCalculator.calculateProfitMargin(
            totalSales = 500_000L,
            netProfit = 500_000L
        )
        assertEquals(100.0, margin, 0.001)
    }

    // =========================================================================
    // 3. Cash Paid Out and Net Cash Flow Tests
    // =========================================================================

    @Test
    fun testCashPaidOut() {
        // Expenses: Rs 5,000, Paid on Purchases: Rs 15,000, Supplier Payments: Rs 10,000
        val cashPaid = LedgerCalculator.calculateCashPaidOut(
            expenses = 500_000L,
            paidOnPurchases = 1_500_000L,
            supplierPayments = 1_000_000L
        )
        assertEquals(3_000_000L, cashPaid)
    }

    @Test
    fun testNetCashFlow() {
        val netCash = LedgerCalculator.calculateNetCashFlow(
            totalSales = 5_000_000L,
            cashPaidOut = 3_000_000L
        )
        assertEquals(2_000_000L, netCash)
    }

    // =========================================================================
    // 4. Supplier Balance & Bill Calculation Tests
    // =========================================================================

    @Test
    fun testSupplierBalance_partialPayment() {
        // Total Purchased: Rs 100,000, Total Paid: Rs 65,000 -> Remaining: Rs 35,000
        val balance = LedgerCalculator.calculateSupplierBalance(
            totalPurchases = 10_000_000L,
            totalPaid = 6_500_000L
        )
        assertEquals(3_500_000L, balance)
    }

    @Test
    fun testSupplierBalance_fullyPaid() {
        val balance = LedgerCalculator.calculateSupplierBalance(5_000_000L, 5_000_000L)
        assertEquals(0L, balance)
    }

    @Test
    fun testSupplierBalance_overpaid_advancePayment() {
        // Purchased 5,000,000, Paid 6,000,000 -> Negative balance (-1,000,000 paisa)
        val balance = LedgerCalculator.calculateSupplierBalance(5_000_000L, 6_000_000L)
        assertEquals(-1_000_000L, balance)
    }

    @Test
    fun testCalculateRemainingBill() {
        // Bill: 50,000, Paid Now: 20,000 -> 30,000
        val remaining = LedgerCalculator.calculateRemainingBill(5_000_000L, 2_000_000L)
        assertEquals(3_000_000L, remaining)

        // Paid entire bill
        assertEquals(0L, LedgerCalculator.calculateRemainingBill(5_000_000L, 5_000_000L))

        // Overpaid safeguard
        assertEquals(0L, LedgerCalculator.calculateRemainingBill(5_000_000L, 6_000_000L))

        // Zero bill
        assertEquals(0L, LedgerCalculator.calculateRemainingBill(0L, 0L))
    }

    // =========================================================================
    // 5. Supplier Summaries Aggregation and Sorting
    // =========================================================================

    @Test
    fun testCalculateSupplierSummaries_sortedByHighestBalance() {
        val sup1 = Supplier(id = 1L, shopId = 1L, name = "Supplier A", phone = "111", note = "")
        val sup2 = Supplier(id = 2L, shopId = 1L, name = "Supplier B", phone = "222", note = "")
        val sup3 = Supplier(id = 3L, shopId = 1L, name = "Supplier C", phone = "333", note = "")

        val purchases = listOf(
            Purchase(id = 1L, shopId = 1L, supplierId = 1L, date = 1000L, description = "Bill 1", totalAmount = 100_000L, paidAmount = 40_000L),
            Purchase(id = 2L, shopId = 1L, supplierId = 2L, date = 1000L, description = "Bill 2", totalAmount = 500_000L, paidAmount = 100_000L),
            Purchase(id = 3L, shopId = 1L, supplierId = 3L, date = 1000L, description = "Bill 3", totalAmount = 50_000L, paidAmount = 50_000L)
        )

        val payments = listOf(
            SupplierPayment(id = 1L, shopId = 1L, supplierId = 2L, date = 2000L, amount = 100_000L, note = "Payment 1")
        )

        // Balances:
        // Supplier 1: 100,000 - 40,000 = 60,000
        // Supplier 2: 500,000 - (100,000 + 100,000) = 300,000 (Highest)
        // Supplier 3: 50,000 - 50,000 = 0 (Lowest)

        val summaries = LedgerCalculator.calculateSupplierSummaries(
            suppliers = listOf(sup1, sup2, sup3),
            purchases = purchases,
            payments = payments
        )

        assertEquals(3, summaries.size)
        // First should be Supplier 2 (Rs 3,000 = 300,000 paisa)
        assertEquals(2L, summaries[0].supplier.id)
        assertEquals(300_000L, summaries[0].balancePayable)

        // Second should be Supplier 1 (Rs 600 = 60,000 paisa)
        assertEquals(1L, summaries[1].supplier.id)
        assertEquals(60_000L, summaries[1].balancePayable)

        // Third should be Supplier 3 (Rs 0 = 0 paisa)
        assertEquals(3L, summaries[2].supplier.id)
        assertEquals(0L, summaries[2].balancePayable)
    }

    @Test
    fun testCalculateSupplierSummaries_emptySuppliers() {
        val summaries = LedgerCalculator.calculateSupplierSummaries(
            suppliers = emptyList(),
            purchases = emptyList(),
            payments = emptyList()
        )
        assertTrue(summaries.isEmpty())
    }

    // =========================================================================
    // 6. Category Breakdown & Dashboard Metrics Edge Cases
    // =========================================================================

    @Test
    fun testCalculateCategoryBreakdown() {
        val expenses = listOf(
            Expense(id = 1L, shopId = 1L, category = "Rent", amount = 50_000L, date = 1000L, note = ""),
            Expense(id = 2L, shopId = 1L, category = "Electricity", amount = 15_000L, date = 1000L, note = ""),
            Expense(id = 3L, shopId = 1L, category = "Rent", amount = 10_000L, date = 2000L, note = "")
        )
        val breakdown = LedgerCalculator.calculateCategoryBreakdown(expenses)
        assertEquals(2, breakdown.size)
        // Rent: 60,000
        assertEquals("Rent", breakdown[0].first)
        assertEquals(60_000L, breakdown[0].second)
        // Electricity: 15,000
        assertEquals("Electricity", breakdown[1].first)
        assertEquals(15_000L, breakdown[1].second)
    }

    @Test
    fun testCalculateDashboardMetrics_zeroData() {
        val metrics = LedgerCalculator.calculateDashboardMetrics(
            salesInPeriod = emptyList(),
            expensesInPeriod = emptyList(),
            purchasesInPeriod = emptyList(),
            paymentsInPeriod = emptyList(),
            allShopPurchases = emptyList(),
            allShopPayments = emptyList()
        )
        assertEquals(0L, metrics.totalSales)
        assertEquals(0L, metrics.totalExpenses)
        assertEquals(0L, metrics.totalPurchases)
        assertEquals(0L, metrics.profit)
        assertEquals(0L, metrics.cashPaidOut)
        assertEquals(0L, metrics.totalPayableToSuppliers)
    }

    // =========================================================================
    // 7. NumberFormatter Tests (Thousands Separators, Urdu, Overflow Bounds)
    // =========================================================================

    @Test
    fun testNumberFormatter_thousandsSeparator() {
        assertEquals("PKR 1,250,000", NumberFormatter.formatPaisa(125_000_000L))
        assertEquals("PKR 1,250,000.50", NumberFormatter.formatPaisa(125_000_050L))
        assertEquals("-PKR 450,000", NumberFormatter.formatPaisa(-45_000_000L))
        assertEquals("PKR 0", NumberFormatter.formatPaisa(0L))
        assertEquals("1,250,000", NumberFormatter.formatPaisa(125_000_000L, showCurrency = false))
    }

    @Test
    fun testNumberFormatter_urduCurrencyPrefix() {
        assertEquals("روپے 1,250,000", NumberFormatter.formatPaisa(125_000_000L, currencyPrefix = "روپے"))
        assertEquals("-روپے 50,000", NumberFormatter.formatPaisa(-5_000_000L, currencyPrefix = "روپے"))
    }

    @Test
    fun testNumberFormatter_parsingWithCommasAndSpaces() {
        assertEquals(125_000_000L, NumberFormatter.parseInputToPaisa("1,250,000"))
        assertEquals(125_000_050L, NumberFormatter.parseInputToPaisa("1,250,000.50"))
        assertEquals(50_000L, NumberFormatter.parseInputToPaisa(" 500 "))
        assertNull(NumberFormatter.parseInputToPaisa(""))
        assertNull(NumberFormatter.parseInputToPaisa("-500"))
        assertNull(NumberFormatter.parseInputToPaisa("invalid"))
    }

    @Test
    fun testNumberFormatter_allowZero() {
        assertEquals(0L, NumberFormatter.parseInputAllowZero(""))
        assertEquals(0L, NumberFormatter.parseInputAllowZero("0"))
        assertEquals(0L, NumberFormatter.parseInputAllowZero("0.00"))
        assertEquals(10_000L, NumberFormatter.parseInputAllowZero("100"))
        assertNull(NumberFormatter.parseInputAllowZero("-50"))
    }

    @Test
    fun testNumberFormatter_overflowBoundsCheck() {
        // Exceeding MAX_AMOUNT_PAISA (> 999 Billion Rs) should be rejected
        val hugeInput = "1000000000000" // 1 Trillion Rupees
        assertNull(NumberFormatter.parseInputToPaisa(hugeInput))
        assertNull(NumberFormatter.parseInputAllowZero(hugeInput))

        // Acceptable large amount (e.g. 50 Billion Rs)
        val validLarge = "50000000000"
        assertEquals(5_000_000_000_000L, NumberFormatter.parseInputToPaisa(validLarge))
    }

    @Test
    fun testNumberFormatter_formatDouble() {
        assertEquals("1,234.56", NumberFormatter.formatDouble(1234.56, 2))
        assertEquals("0", NumberFormatter.formatDouble(Double.NaN, 2))
        assertEquals("0", NumberFormatter.formatDouble(Double.POSITIVE_INFINITY, 2))
    }

    // =========================================================================
    // 8. Customer Credit (Udhaar) Module Calculation Tests
    // =========================================================================

    @Test
    fun testCustomerCredit_pureCashSale() {
        val sale = Sale(
            id = 1L,
            shopId = 1L,
            date = 1000L,
            amount = 100_000L, // Rs 1,000
            receivedAmount = 100_000L, // 100% Cash
            customerId = null
        )

        // Remaining Udhaar must be 0
        val remainingUdhaar = LedgerCalculator.calculateRemainingUdhaar(sale.amount, sale.receivedAmount)
        assertEquals(0L, remainingUdhaar)

        // Customer balance calculation with no credit sales
        val balance = LedgerCalculator.calculateCustomerBalance(
            sales = listOf(sale),
            payments = emptyList(),
            writeOffs = emptyList()
        )
        assertEquals(0L, balance)

        // Cash In for period
        val cashIn = LedgerCalculator.calculateCashIn(
            cashSalesReceived = listOf(sale).sumOf { it.receivedAmount },
            customerPayments = 0L
        )
        assertEquals(100_000L, cashIn)

        // Profit recognizes full sales amount
        val profit = LedgerCalculator.calculateProfit(
            totalSales = sale.amount,
            totalExpenses = 0L,
            totalPurchases = 0L,
            badDebt = 0L
        )
        assertEquals(100_000L, profit)
    }

    @Test
    fun testCustomerCredit_partlyPaidCreditSale() {
        val customer = Customer(id = 1L, shopId = 1L, name = "Ahmad", createdAt = 500L)
        val sale = Sale(
            id = 2L,
            shopId = 1L,
            customerId = customer.id,
            date = 1000L,
            amount = 100_000L, // Total: Rs 1,000
            receivedAmount = 30_000L // Received: Rs 300
        )

        // Remaining Udhaar must be 70,000 paisa (Rs 700)
        val remaining = LedgerCalculator.calculateRemainingUdhaar(sale.amount, sale.receivedAmount)
        assertEquals(70_000L, remaining)

        // Customer balance should be 70,000
        val balance = LedgerCalculator.calculateCustomerBalance(
            sales = listOf(sale),
            payments = emptyList(),
            writeOffs = emptyList()
        )
        assertEquals(70_000L, balance)

        // Total sales (for profit) must be FULL amount on date of sale
        val totalSales = listOf(sale).sumOf { it.amount }
        assertEquals(100_000L, totalSales)

        // Cash In only includes the cash portion received now
        val cashIn = LedgerCalculator.calculateCashIn(
            cashSalesReceived = listOf(sale).sumOf { it.receivedAmount },
            customerPayments = 0L
        )
        assertEquals(30_000L, cashIn)
    }

    @Test
    fun testCustomerCredit_fullyCreditSale() {
        val customer = Customer(id = 2L, shopId = 1L, name = "Bilal", createdAt = 500L)
        val sale = Sale(
            id = 3L,
            shopId = 1L,
            customerId = customer.id,
            date = 1000L,
            amount = 50_000L, // Rs 500
            receivedAmount = 0L // 0 cash received now
        )

        // Remaining Udhaar must equal full amount
        val remaining = LedgerCalculator.calculateRemainingUdhaar(sale.amount, sale.receivedAmount)
        assertEquals(50_000L, remaining)

        // Customer balance is 50,000
        val balance = LedgerCalculator.calculateCustomerBalance(
            sales = listOf(sale),
            payments = emptyList(),
            writeOffs = emptyList()
        )
        assertEquals(50_000L, balance)

        // Cash In for this sale is 0
        val cashIn = LedgerCalculator.calculateCashIn(
            cashSalesReceived = listOf(sale).sumOf { it.receivedAmount },
            customerPayments = 0L
        )
        assertEquals(0L, cashIn)

        // Sales for profit is the full amount
        assertEquals(50_000L, listOf(sale).sumOf { it.amount })
    }

    @Test
    fun testCustomerCredit_laterPayment_noDoubleCountSales() {
        // Customer bought Rs 1,000 item on credit with Rs 300 received at sale time -> Rs 700 Udhaar
        val sale = Sale(
            id = 1L,
            shopId = 1L,
            customerId = 10L,
            date = 1000L,
            amount = 100_000L,
            receivedAmount = 30_000L
        )

        // Later payment received on date 2000L: Rs 400 (40,000 paisa)
        val payment = CustomerPayment(
            id = 1L,
            shopId = 1L,
            customerId = 10L,
            date = 2000L,
            amount = 40_000L,
            note = "Paid part of balance"
        )

        // Customer balance after payment: 70,000 - 40,000 = 30,000
        val balance = LedgerCalculator.calculateCustomerBalance(
            sales = listOf(sale),
            payments = listOf(payment),
            writeOffs = emptyList()
        )
        assertEquals(30_000L, balance)

        // On the payment date (period: 2000L..2000L), sales are NOT counted again (avoid double counting)
        val salesInPeriod2 = emptyList<Sale>()
        val totalSalesInPeriod2 = salesInPeriod2.sumOf { it.amount }
        assertEquals(0L, totalSalesInPeriod2)

        // But Cash In on date 2000L does receive the 40,000 payment
        val cashInPeriod2 = LedgerCalculator.calculateCashIn(
            cashSalesReceived = salesInPeriod2.sumOf { it.receivedAmount },
            customerPayments = listOf(payment).sumOf { it.amount }
        )
        assertEquals(40_000L, cashInPeriod2)
    }

    @Test
    fun testCustomerCredit_badDebtWriteOff_reducesProfitAndCustomerBalance() {
        val sale = Sale(
            id = 1L,
            shopId = 1L,
            customerId = 10L,
            date = 1000L,
            amount = 100_000L,
            receivedAmount = 70_000L // 30,000 balance owing
        )

        val writeOff = CustomerWriteOff(
            id = 1L,
            shopId = 1L,
            customerId = 10L,
            date = 3000L,
            amount = 10_000L,
            note = "Uncollectible customer moved away"
        )

        // Customer balance reduced by write-off: 30,000 - 10,000 = 20,000
        val balance = LedgerCalculator.calculateCustomerBalance(
            sales = listOf(sale),
            payments = emptyList(),
            writeOffs = listOf(writeOff)
        )
        assertEquals(20_000L, balance)

        // Profit = Sales - Expenses - Purchases - Bad Debt
        val profit = LedgerCalculator.calculateProfit(
            totalSales = 100_000L,
            totalExpenses = 20_000L,
            totalPurchases = 30_000L,
            badDebt = 10_000L
        )
        // 100,000 - 20,000 - 30,000 - 10,000 = 40,000
        assertEquals(40_000L, profit)

        // Write off has ZERO impact on cash flow (no money entered or left the cash drawer)
        val cashIn = LedgerCalculator.calculateCashIn(
            cashSalesReceived = listOf(sale).sumOf { it.receivedAmount },
            customerPayments = 0L
        )
        assertEquals(70_000L, cashIn)
    }

    @Test
    fun testCustomerCredit_deletingPayment_recomputesBalanceAccurately() {
        val sale = Sale(id = 1L, shopId = 1L, customerId = 5L, date = 1000L, amount = 80_000L, receivedAmount = 0L)
        val payment1 = CustomerPayment(id = 1L, shopId = 1L, customerId = 5L, date = 2000L, amount = 25_000L)
        val payment2 = CustomerPayment(id = 2L, shopId = 1L, customerId = 5L, date = 3000L, amount = 20_000L)

        // Initial balance with 2 payments: 80,000 - (25,000 + 20,000) = 35,000
        val balanceBefore = LedgerCalculator.calculateCustomerBalance(
            sales = listOf(sale),
            payments = listOf(payment1, payment2),
            writeOffs = emptyList()
        )
        assertEquals(35_000L, balanceBefore)

        // User deletes payment 2 -> payments list only contains payment 1
        val balanceAfterDelete = LedgerCalculator.calculateCustomerBalance(
            sales = listOf(sale),
            payments = listOf(payment1),
            writeOffs = emptyList()
        )
        assertEquals(55_000L, balanceAfterDelete)
    }

    @Test
    fun testCustomerCredit_multiShopIsolation() {
        val c1Shop1 = Customer(id = 1L, shopId = 101L, name = "Customer in Shop A", createdAt = 100L)
        val c2Shop2 = Customer(id = 2L, shopId = 202L, name = "Customer in Shop B", createdAt = 100L)

        val salesShop1 = listOf(
            Sale(id = 1L, shopId = 101L, customerId = c1Shop1.id, date = 1000L, amount = 50_000L, receivedAmount = 10_000L) // 40k Udhaar
        )
        val salesShop2 = listOf(
            Sale(id = 2L, shopId = 202L, customerId = c2Shop2.id, date = 1000L, amount = 80_000L, receivedAmount = 20_000L) // 60k Udhaar
        )

        // Shop 1 calculations: only Shop 1 customers & records
        val summariesShop1 = LedgerCalculator.calculateCustomerSummaries(
            customers = listOf(c1Shop1),
            sales = salesShop1,
            payments = emptyList(),
            writeOffs = emptyList()
        )
        assertEquals(1, summariesShop1.size)
        assertEquals(1L, summariesShop1[0].customer.id)
        assertEquals(40_000L, summariesShop1[0].balance)
        val totalRecShop1 = LedgerCalculator.calculateTotalReceivable(summariesShop1)
        assertEquals(40_000L, totalRecShop1)

        // Shop 2 calculations: only Shop 2 customers & records
        val summariesShop2 = LedgerCalculator.calculateCustomerSummaries(
            customers = listOf(c2Shop2),
            sales = salesShop2,
            payments = emptyList(),
            writeOffs = emptyList()
        )
        assertEquals(1, summariesShop2.size)
        assertEquals(2L, summariesShop2[0].customer.id)
        assertEquals(60_000L, summariesShop2[0].balance)
        val totalRecShop2 = LedgerCalculator.calculateTotalReceivable(summariesShop2)
        assertEquals(60_000L, totalRecShop2)

        // Confirm Shop 1 data never appears in Shop 2
        assertTrue(summariesShop1.none { it.customer.shopId == 202L })
        assertTrue(summariesShop2.none { it.customer.shopId == 101L })
    }

    @Test
    fun testCustomerLedger_chronologicalRunningBalance() {
        val customer = Customer(id = 1L, shopId = 1L, name = "Kamran", createdAt = 100L)
        val sales = listOf(
            Sale(id = 10L, shopId = 1L, customerId = 1L, date = 1000L, amount = 100_000L, receivedAmount = 20_000L), // +80,000 Udhaar
            Sale(id = 11L, shopId = 1L, customerId = 1L, date = 3000L, amount = 50_000L, receivedAmount = 0L)        // +50,000 Udhaar
        )
        val payments = listOf(
            CustomerPayment(id = 20L, shopId = 1L, customerId = 1L, date = 2000L, amount = 30_000L, note = "Payment 1") // -30,000
        )
        val writeOffs = listOf(
            CustomerWriteOff(id = 30L, shopId = 1L, customerId = 1L, date = 4000L, amount = 10_000L, note = "Discount/write-off") // -10,000
        )

        // Returned newest first for UI display:
        // index 0: date 4000: Write-off -10,000   -> Running Balance: 90,000
        // index 1: date 3000: Sale credit +50,000 -> Running Balance: 100,000
        // index 2: date 2000: Payment -30,000     -> Running Balance: 50,000
        // index 3: date 1000: Sale credit +80,000 -> Running Balance: 80,000
        val ledger = LedgerCalculator.buildCustomerLedger(sales, payments, writeOffs)

        assertEquals(4, ledger.size)

        assertEquals(CustomerLedgerType.WRITE_OFF, ledger[0].type)
        assertEquals(10_000L, ledger[0].amount)
        assertEquals(90_000L, ledger[0].runningBalance)

        assertEquals(CustomerLedgerType.SALE, ledger[1].type)
        assertEquals(50_000L, ledger[1].amount)
        assertEquals(100_000L, ledger[1].runningBalance)

        assertEquals(CustomerLedgerType.PAYMENT, ledger[2].type)
        assertEquals(30_000L, ledger[2].amount)
        assertEquals(50_000L, ledger[2].runningBalance)

        assertEquals(CustomerLedgerType.SALE, ledger[3].type)
        assertEquals(100_000L, ledger[3].amount)
        assertEquals(80_000L, ledger[3].runningBalance)
    }
}
