package com.example.util

import com.example.data.local.entity.Customer
import com.example.data.local.entity.CustomerPayment
import com.example.data.local.entity.CustomerWriteOff
import com.example.data.local.entity.Expense
import com.example.data.local.entity.Purchase
import com.example.data.local.entity.Sale
import com.example.data.local.entity.Supplier
import com.example.data.local.entity.SupplierPayment
import com.example.data.repository.CustomerLedgerItem
import com.example.data.repository.CustomerLedgerType
import com.example.data.repository.CustomerSummary
import com.example.data.repository.DashboardMetrics
import com.example.data.repository.SupplierSummary
import com.example.ui.viewmodel.DailyReportItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure, unit-tested calculation engine for ShopLedger.
 * Contains core arithmetic for profit, supplier balances, customer credit (udhaar),
 * bad debt, cash in/out, margins, and running ledgers.
 * All functions are pure, deterministic, and free of side-effects.
 */
object LedgerCalculator {

    /**
     * Calculates net profit: Total Sales - Total Expenses - Total Purchases - Bad Debt.
     * All inputs and outputs are in minor currency units (paisa).
     */
    fun calculateProfit(
        totalSales: Long,
        totalExpenses: Long,
        totalPurchases: Long,
        badDebt: Long = 0L
    ): Long {
        return totalSales - totalExpenses - totalPurchases - badDebt
    }

    /**
     * Calculates profit margin percentage: (netProfit / totalSales) * 100.
     * Handles zero or negative sales safely without throwing ArithmeticException, NaN, or Infinity.
     */
    fun calculateProfitMargin(
        totalSales: Long,
        netProfit: Long
    ): Double {
        if (totalSales <= 0L) return 0.0
        val margin = (netProfit.toDouble() / totalSales.toDouble()) * 100.0
        return if (margin.isNaN() || margin.isInfinite()) {
            0.0
        } else {
            BigDecimal(margin).setScale(2, RoundingMode.HALF_UP).toDouble()
        }
    }

    /**
     * Calculates total cash paid out in a period:
     * Cash Paid Out = Expenses + Paid on Purchases + Subsequent Supplier Payments.
     */
    fun calculateCashPaidOut(
        expenses: Long,
        paidOnPurchases: Long,
        supplierPayments: Long
    ): Long {
        return expenses + paidOnPurchases + supplierPayments
    }

    /**
     * Calculates total cash received in a period:
     * Cash In = Cash received at sales + Subsequent Customer Payments.
     */
    fun calculateCashIn(
        cashSalesReceived: Long,
        customerPayments: Long
    ): Long {
        return cashSalesReceived + customerPayments
    }

    /**
     * Calculates net cash: Cash In - Cash Out.
     */
    fun calculateNetCash(
        cashIn: Long,
        cashOut: Long
    ): Long {
        return cashIn - cashOut
    }

    /**
     * Legacy helper for cash flow: Total Sales - Cash Paid Out.
     */
    fun calculateNetCashFlow(
        totalSales: Long,
        cashPaidOut: Long
    ): Long {
        return totalSales - cashPaidOut
    }

    /**
     * Calculates outstanding balance for a single supplier:
     * Total Purchases - (Paid on Purchases + Subsequent Payments).
     */
    fun calculateSupplierBalance(
        totalPurchases: Long,
        totalPaid: Long
    ): Long {
        return totalPurchases - totalPaid
    }

    /**
     * Calculates remaining bill when entering a new purchase:
     * totalBill - paidNow.
     * If paidNow > totalBill, returns 0.
     */
    fun calculateRemainingBill(
        totalBill: Long,
        paidNow: Long
    ): Long {
        return (totalBill - paidNow).coerceAtLeast(0L)
    }

    /**
     * Calculates remaining Udhaar for a sale: total - received.
     * If received > total, returns 0.
     */
    fun calculateRemainingUdhaar(
        totalAmount: Long,
        receivedAmount: Long
    ): Long {
        return (totalAmount - receivedAmount).coerceAtLeast(0L)
    }

    /**
     * Calculates customer balance:
     * sum(sale.amount - sale.receivedAmount for that customer) - sum(CustomerPayment.amount) - sum(CustomerWriteOff.amount)
     */
    fun calculateCustomerBalance(
        sales: List<Sale>,
        payments: List<CustomerPayment>,
        writeOffs: List<CustomerWriteOff>
    ): Long {
        val totalCreditGiven = sales.sumOf { (it.amount - it.receivedAmount).coerceAtLeast(0L) }
        val totalPayments = payments.sumOf { it.amount }
        val totalWriteOffs = writeOffs.sumOf { it.amount }
        return totalCreditGiven - totalPayments - totalWriteOffs
    }

    /**
     * Aggregates and calculates customer summaries sorted by highest balance owed.
     */
    fun calculateCustomerSummaries(
        customers: List<Customer>,
        sales: List<Sale>,
        payments: List<CustomerPayment>,
        writeOffs: List<CustomerWriteOff>
    ): List<CustomerSummary> {
        if (customers.isEmpty()) return emptyList()

        return customers.map { customer ->
            val customerSales = sales.filter { it.customerId == customer.id }
            val customerPayments = payments.filter { it.customerId == customer.id }
            val customerWriteOffs = writeOffs.filter { it.customerId == customer.id }

            val totalCreditGiven = customerSales.sumOf { (it.amount - it.receivedAmount).coerceAtLeast(0L) }
            val totalReceivedBack = customerPayments.sumOf { it.amount }
            val totalWriteOffsAmount = customerWriteOffs.sumOf { it.amount }
            val balance = totalCreditGiven - totalReceivedBack - totalWriteOffsAmount

            CustomerSummary(
                customer = customer,
                totalCreditGiven = totalCreditGiven,
                totalReceivedBack = totalReceivedBack,
                totalWriteOffs = totalWriteOffsAmount,
                balance = balance
            )
        }.sortedByDescending { it.balance }
    }

    /**
     * Calculates total receivable for a shop (sum of positive customer balances).
     */
    fun calculateTotalReceivable(
        customers: List<Customer>,
        sales: List<Sale>,
        payments: List<CustomerPayment>,
        writeOffs: List<CustomerWriteOff>
    ): Long {
        if (customers.isEmpty()) return 0L
        return calculateCustomerSummaries(customers, sales, payments, writeOffs)
            .filter { it.balance > 0L }
            .sumOf { it.balance }
    }

    fun calculateTotalReceivable(summaries: List<CustomerSummary>): Long {
        return summaries.filter { it.balance > 0L }.sumOf { it.balance }
    }

    /**
     * Builds a chronological combined ledger for a customer with running balances.
     * Items are sorted newest first for UI display, with accurate running balances.
     */
    fun buildCustomerLedger(
        sales: List<Sale>,
        payments: List<CustomerPayment>,
        writeOffs: List<CustomerWriteOff>
    ): List<CustomerLedgerItem> {
        val rawItems = mutableListOf<CustomerLedgerItem>()

        // 1. Credit Sales
        sales.filter { (it.amount - it.receivedAmount) > 0L }.forEach { sale ->
            val credit = sale.amount - sale.receivedAmount
            rawItems.add(
                CustomerLedgerItem(
                    id = sale.id,
                    type = CustomerLedgerType.SALE,
                    date = sale.date,
                    amount = sale.amount,
                    receivedAmount = sale.receivedAmount,
                    creditAmount = credit,
                    description = if (sale.note.isNotBlank()) sale.note else "Sale",
                    note = sale.note,
                    runningBalance = 0L,
                    sourceSale = sale
                )
            )
        }

        // 2. Customer Payments
        payments.forEach { payment ->
            rawItems.add(
                CustomerLedgerItem(
                    id = payment.id,
                    type = CustomerLedgerType.PAYMENT,
                    date = payment.date,
                    amount = payment.amount,
                    receivedAmount = payment.amount,
                    creditAmount = 0L,
                    description = if (payment.note.isNotBlank()) payment.note else "Payment",
                    note = payment.note,
                    runningBalance = 0L,
                    sourcePayment = payment
                )
            )
        }

        // 3. Customer Write-Offs
        writeOffs.forEach { writeOff ->
            rawItems.add(
                CustomerLedgerItem(
                    id = writeOff.id,
                    type = CustomerLedgerType.WRITE_OFF,
                    date = writeOff.date,
                    amount = writeOff.amount,
                    receivedAmount = 0L,
                    creditAmount = 0L,
                    description = if (writeOff.note.isNotBlank()) writeOff.note else "Write-Off",
                    note = writeOff.note,
                    runningBalance = 0L,
                    sourceWriteOff = writeOff
                )
            )
        }

        if (rawItems.isEmpty()) return emptyList()

        // Sort chronologically ascending to compute running balance
        val sortedAsc = rawItems.sortedWith(compareBy({ it.date }, { it.id }))
        var balance = 0L
        val computedList = sortedAsc.map { item ->
            when (item.type) {
                CustomerLedgerType.SALE -> balance += item.creditAmount
                CustomerLedgerType.PAYMENT -> balance -= item.amount
                CustomerLedgerType.WRITE_OFF -> balance -= item.amount
            }
            item.copy(runningBalance = balance)
        }

        // Return newest first for ledger list display
        return computedList.reversed()
    }

    /**
     * Aggregates and calculates supplier summaries sorted by highest balance payable.
     */
    fun calculateSupplierSummaries(
        suppliers: List<Supplier>,
        purchases: List<Purchase>,
        payments: List<SupplierPayment>
    ): List<SupplierSummary> {
        if (suppliers.isEmpty()) return emptyList()

        return suppliers.map { supplier ->
            val supplierPurchases = purchases.filter { it.supplierId == supplier.id }
            val supplierPayments = payments.filter { it.supplierId == supplier.id }

            val totalPurchases = supplierPurchases.sumOf { it.totalAmount }
            val paidAtPurchase = supplierPurchases.sumOf { it.paidAmount }
            val subsequentPayments = supplierPayments.sumOf { it.amount }
            val totalPaid = paidAtPurchase + subsequentPayments
            val balance = calculateSupplierBalance(totalPurchases, totalPaid)

            SupplierSummary(
                supplier = supplier,
                totalPurchases = totalPurchases,
                totalPaid = totalPaid,
                balancePayable = balance
            )
        }.sortedByDescending { it.balancePayable }
    }

    /**
     * Calculates dashboard metrics from raw transactions.
     */
    fun calculateDashboardMetrics(
        salesInPeriod: List<Sale>,
        customerPaymentsInPeriod: List<CustomerPayment> = emptyList(),
        writeOffsInPeriod: List<CustomerWriteOff> = emptyList(),
        expensesInPeriod: List<Expense>,
        purchasesInPeriod: List<Purchase>,
        supplierPaymentsInPeriod: List<SupplierPayment>,
        allShopPurchases: List<Purchase>,
        allShopSupplierPayments: List<SupplierPayment>,
        totalReceivableFromCustomers: Long = 0L
    ): DashboardMetrics {
        val totalSales = salesInPeriod.sumOf { it.amount }
        val totalExpenses = expensesInPeriod.sumOf { it.amount }
        val totalPurchases = purchasesInPeriod.sumOf { it.totalAmount }
        val badDebt = writeOffsInPeriod.sumOf { it.amount }
        val profit = calculateProfit(totalSales, totalExpenses, totalPurchases, badDebt)

        val cashSalesReceived = salesInPeriod.sumOf { it.receivedAmount }
        val customerPaymentsAmount = customerPaymentsInPeriod.sumOf { it.amount }
        val cashIn = calculateCashIn(cashSalesReceived, customerPaymentsAmount)

        val paidOnPurchasesInPeriod = purchasesInPeriod.sumOf { it.paidAmount }
        val supplierPaymentsAmount = supplierPaymentsInPeriod.sumOf { it.amount }
        val cashOut = calculateCashPaidOut(
            totalExpenses,
            paidOnPurchasesInPeriod,
            supplierPaymentsAmount
        )
        val netCash = calculateNetCash(cashIn, cashOut)

        // All-time payable to all suppliers for this shop
        val allPurchasesTotal = allShopPurchases.sumOf { it.totalAmount }
        val allPurchasesPaid = allShopPurchases.sumOf { it.paidAmount }
        val allPaymentsTotal = allShopSupplierPayments.sumOf { it.amount }
        val allTotalPaid = allPurchasesPaid + allPaymentsTotal
        val totalPayable = calculateSupplierBalance(allPurchasesTotal, allTotalPaid).coerceAtLeast(0L)

        return DashboardMetrics(
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            totalPurchases = totalPurchases,
            badDebt = badDebt,
            profit = profit,
            cashIn = cashIn,
            cashOut = cashOut,
            netCash = netCash,
            totalPayableToSuppliers = totalPayable,
            totalReceivableFromCustomers = totalReceivableFromCustomers.coerceAtLeast(0L)
        )
    }

    /**
     * Overload for 6 parameters (backward compatibility).
     */
    fun calculateDashboardMetrics(
        salesInPeriod: List<Sale>,
        expensesInPeriod: List<Expense>,
        purchasesInPeriod: List<Purchase>,
        paymentsInPeriod: List<SupplierPayment>,
        allShopPurchases: List<Purchase>,
        allShopPayments: List<SupplierPayment>
    ): DashboardMetrics = calculateDashboardMetrics(
        salesInPeriod = salesInPeriod,
        customerPaymentsInPeriod = emptyList(),
        writeOffsInPeriod = emptyList(),
        expensesInPeriod = expensesInPeriod,
        purchasesInPeriod = purchasesInPeriod,
        supplierPaymentsInPeriod = paymentsInPeriod,
        allShopPurchases = allShopPurchases,
        allShopSupplierPayments = allShopPayments,
        totalReceivableFromCustomers = 0L
    )

    /**
     * Aggregates transactions into daily report items sorted by date descending.
     */
    fun calculateDailyReportItems(
        sales: List<Sale>,
        expenses: List<Expense>,
        purchases: List<Purchase>
    ): List<DailyReportItem> {
        val allDates = mutableSetOf<Long>()
        sales.forEach { allDates.add(DateUtils.startOfDay(it.date)) }
        expenses.forEach { allDates.add(DateUtils.startOfDay(it.date)) }
        purchases.forEach { allDates.add(DateUtils.startOfDay(it.date)) }

        if (allDates.isEmpty()) return emptyList()

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        return allDates.sortedDescending().map { dayStart ->
            val dayEnd = DateUtils.endOfDay(dayStart)

            val daySales = sales.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }
            val dayExpenses = expenses.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }
            val dayPurchases = purchases.filter { it.date in dayStart..dayEnd }.sumOf { it.totalAmount }
            val dayProfit = calculateProfit(daySales, dayExpenses, dayPurchases)

            DailyReportItem(
                dateMillis = dayStart,
                dateLabel = dateFormat.format(Date(dayStart)),
                sales = daySales,
                expenses = dayExpenses,
                purchases = dayPurchases,
                profit = dayProfit
            )
        }
    }

    /**
     * Calculates category breakdown for expenses.
     */
    fun calculateCategoryBreakdown(expenses: List<Expense>): List<Pair<String, Long>> {
        if (expenses.isEmpty()) return emptyList()
        return expenses
            .groupBy { it.category.trim().ifBlank { "General" } }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
}
