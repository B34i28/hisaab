package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Expense
import com.example.data.repository.CustomerLedgerItem
import com.example.data.repository.CustomerLedgerType
import com.example.data.repository.CustomerSummary
import com.example.data.repository.DashboardMetrics
import com.example.ui.viewmodel.DailyReportItem
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

object ReportExportUtils {

    /**
     * Generates and triggers Android Share Sheet with a formatted CSV report.
     */
    fun shareCsvReport(
        context: Context,
        shopName: String,
        dateRangeStr: String,
        metrics: DashboardMetrics,
        creditSalesTotal: Long = 0L,
        customerPaymentsTotal: Long = 0L,
        writeOffsTotal: Long = 0L,
        receivableBalance: Long = 0L,
        customerSummaries: List<CustomerSummary> = emptyList(),
        expenses: List<Expense>,
        dailyItems: List<DailyReportItem>
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanShopName = shopName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val csvFile = File(reportsDir, "Hisaab_${cleanShopName}_${System.currentTimeMillis()}.csv")

            val totalExpenses = expenses.sumOf { it.amount }
            val categoryBreakdown = expenses.groupBy { it.category }
                .map { (cat, list) ->
                    val sum = list.sumOf { it.amount }
                    val pct = if (totalExpenses > 0) (sum * 100 / totalExpenses) else 0L
                    Triple(cat, sum, pct)
                }.sortedByDescending { it.second }

            FileWriter(csvFile).use { writer ->
                writer.append("Hisaab Financial Report\n")
                writer.append("Shop Name,\"${shopName.replace("\"", "\"\"")}\"\n")
                writer.append("Date Range,\"${dateRangeStr.replace("\"", "\"\"")}\"\n")
                writer.append("Generated On,\"${DateUtils.formatDateTime(System.currentTimeMillis())}\"\n\n")

                // Financial Summary
                writer.append("--- FINANCIAL SUMMARY ---\n")
                writer.append("Metric,Amount (PKR)\n")
                writer.append("Total Sales,${CurrencyUtils.paisaToEditableString(metrics.totalSales)}\n")
                writer.append("Total Expenses,${CurrencyUtils.paisaToEditableString(metrics.totalExpenses)}\n")
                writer.append("Total Purchases,${CurrencyUtils.paisaToEditableString(metrics.totalPurchases)}\n")
                writer.append("Net Profit,${CurrencyUtils.paisaToEditableString(metrics.profit)}\n")
                writer.append("Cash In,${CurrencyUtils.paisaToEditableString(metrics.cashIn)}\n")
                writer.append("Cash Out,${CurrencyUtils.paisaToEditableString(metrics.cashOut)}\n")
                writer.append("Net Cash,${CurrencyUtils.paisaToEditableString(metrics.netCash)}\n")
                writer.append("Bad Debt (Write-Offs),${CurrencyUtils.paisaToEditableString(writeOffsTotal)}\n")
                writer.append("Credit Sales in Period,${CurrencyUtils.paisaToEditableString(creditSalesTotal)}\n")
                writer.append("Customer Payments Received,${CurrencyUtils.paisaToEditableString(customerPaymentsTotal)}\n")
                writer.append("Total Receivable (Customers Owe You),${CurrencyUtils.paisaToEditableString(receivableBalance)}\n")
                writer.append("Total Payable to Suppliers,${CurrencyUtils.paisaToEditableString(metrics.totalPayableToSuppliers)}\n\n")

                // Customer Outstanding Table
                writer.append("--- CUSTOMER-WISE OUTSTANDING (UDHAAR) ---\n")
                writer.append("Customer Name,Phone,Credit Limit (PKR),Balance Owed (PKR)\n")
                if (customerSummaries.isEmpty()) {
                    writer.append("No customer accounts,0.00,0.00,0.00\n")
                } else {
                    customerSummaries.forEach { cs ->
                        val limitStr = if (cs.customer.creditLimit != null) CurrencyUtils.paisaToEditableString(cs.customer.creditLimit) else "None"
                        writer.append("\"${cs.customer.name.replace("\"", "\"\"")}\",\"${cs.customer.phone}\",$limitStr,${CurrencyUtils.paisaToEditableString(cs.balance)}\n")
                    }
                }
                writer.append("\n")

                // Expense by Category
                writer.append("--- EXPENSES BY CATEGORY ---\n")
                writer.append("Category,Amount (PKR),Percentage\n")
                if (categoryBreakdown.isEmpty()) {
                    writer.append("No expenses recorded,0.00,0%\n")
                } else {
                    categoryBreakdown.forEach { (cat, sum, pct) ->
                        writer.append("\"${cat.replace("\"", "\"\"")}\",${CurrencyUtils.paisaToEditableString(sum)},${pct}%\n")
                    }
                }
                writer.append("\n")

                // Day-by-Day Breakdown
                writer.append("--- DAY-BY-DAY BREAKDOWN ---\n")
                writer.append("Date,Sales (PKR),Expenses (PKR),Purchases (PKR),Net Profit (PKR)\n")
                if (dailyItems.isEmpty()) {
                    writer.append("No activity recorded,0.00,0.00,0.00,0.00\n")
                } else {
                    dailyItems.forEach { item ->
                        writer.append("${item.dateLabel},${CurrencyUtils.paisaToEditableString(item.sales)},${CurrencyUtils.paisaToEditableString(item.expenses)},${CurrencyUtils.paisaToEditableString(item.purchases)},${CurrencyUtils.paisaToEditableString(item.profit)}\n")
                    }
                }
            }

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "$shopName - Financial Report ($dateRangeStr)")
                putExtra(Intent.EXTRA_TEXT, "Here is the Hisaab CSV financial report for $shopName ($dateRangeStr).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Export CSV Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generates and triggers Android Share Sheet with a rendered PDF report.
     */
    fun sharePdfReport(
        context: Context,
        shopName: String,
        dateRangeStr: String,
        metrics: DashboardMetrics,
        creditSalesTotal: Long = 0L,
        customerPaymentsTotal: Long = 0L,
        writeOffsTotal: Long = 0L,
        receivableBalance: Long = 0L,
        customerSummaries: List<CustomerSummary> = emptyList(),
        expenses: List<Expense>,
        dailyItems: List<DailyReportItem>
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanShopName = shopName.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val pdfFile = File(reportsDir, "Hisaab_${cleanShopName}_${System.currentTimeMillis()}.pdf")

            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width in pt
            val pageHeight = 842 // Standard A4 height in pt

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply { isAntiAlias = true }

            var y: Float

            // --- HEADER BANNER ---
            paint.color = Color.rgb(15, 107, 77) // Emerald Primary
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 75f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 20f
            textPaint.isFakeBoldText = true
            canvas.drawText("Hisaab Financial Statement", 24f, 36f, textPaint)

            textPaint.textSize = 12f
            textPaint.isFakeBoldText = false
            canvas.drawText("Shop: $shopName  |  Period: $dateRangeStr", 24f, 58f, textPaint)

            y = 95f

            // Report Meta
            textPaint.color = Color.DKGRAY
            textPaint.textSize = 9f
            canvas.drawText("Generated on: ${DateUtils.formatDateTime(System.currentTimeMillis())} (Offline records)", 24f, y, textPaint)
            y += 20f

            // --- FINANCIAL SUMMARY BOXES ---
            val isProfit = metrics.profit >= 0
            val profitBg = if (isProfit) Color.rgb(220, 245, 230) else Color.rgb(255, 230, 230)
            val profitTextColor = if (isProfit) Color.rgb(21, 128, 61) else Color.rgb(220, 38, 38)

            paint.color = profitBg
            canvas.drawRoundRect(RectF(24f, y, pageWidth - 24f, y + 55f), 8f, 8f, paint)

            textPaint.color = Color.DKGRAY
            textPaint.textSize = 10f
            canvas.drawText("NET PROFIT / LOSS", 36f, y + 20f, textPaint)

            textPaint.color = profitTextColor
            textPaint.textSize = 20f
            textPaint.isFakeBoldText = true
            canvas.drawText(NumberFormatter.formatPaisa(metrics.profit), 36f, y + 44f, textPaint)

            textPaint.isFakeBoldText = false
            textPaint.color = Color.BLACK
            textPaint.textSize = 10f
            val summaryDetails = "Sales: ${NumberFormatter.formatPaisa(metrics.totalSales, showCurrency = false)} - Exp: ${NumberFormatter.formatPaisa(metrics.totalExpenses, showCurrency = false)} - Purchases: ${NumberFormatter.formatPaisa(metrics.totalPurchases, showCurrency = false)} - Bad Debt: ${NumberFormatter.formatPaisa(metrics.badDebt, showCurrency = false)}"
            canvas.drawText(summaryDetails, 200f, y + 36f, textPaint)

            y += 70f

            // Row 1 Metrics
            paint.color = Color.rgb(245, 247, 245)
            canvas.drawRoundRect(RectF(24f, y, pageWidth - 24f, y + 48f), 8f, 8f, paint)

            textPaint.textSize = 9f
            textPaint.color = Color.GRAY
            canvas.drawText("TOTAL SALES", 36f, y + 18f, textPaint)
            canvas.drawText("TOTAL EXPENSES", 160f, y + 18f, textPaint)
            canvas.drawText("CASH IN", 290f, y + 18f, textPaint)
            canvas.drawText("NET CASH", 420f, y + 18f, textPaint)

            textPaint.textSize = 12f
            textPaint.isFakeBoldText = true
            textPaint.color = Color.rgb(21, 128, 61)
            canvas.drawText(NumberFormatter.formatPaisa(metrics.totalSales), 36f, y + 36f, textPaint)

            textPaint.color = Color.rgb(220, 38, 38)
            canvas.drawText(NumberFormatter.formatPaisa(metrics.totalExpenses), 160f, y + 36f, textPaint)

            textPaint.color = Color.rgb(21, 128, 61)
            canvas.drawText(NumberFormatter.formatPaisa(metrics.cashIn), 290f, y + 36f, textPaint)

            textPaint.color = if (metrics.netCash >= 0) Color.rgb(21, 128, 61) else Color.rgb(220, 38, 38)
            canvas.drawText(NumberFormatter.formatPaisa(metrics.netCash), 420f, y + 36f, textPaint)

            y += 60f

            // Row 2 Metrics: You Will Get vs You Will Give
            paint.color = Color.rgb(245, 247, 245)
            canvas.drawRoundRect(RectF(24f, y, pageWidth - 24f, y + 48f), 8f, 8f, paint)

            textPaint.textSize = 9f
            textPaint.color = Color.GRAY
            canvas.drawText("RECEIVABLES (YOU WILL GET)", 36f, y + 18f, textPaint)
            canvas.drawText("PAYABLES (YOU WILL GIVE)", 200f, y + 18f, textPaint)
            canvas.drawText("BAD DEBT (WRITE-OFFS)", 380f, y + 18f, textPaint)

            textPaint.textSize = 12f
            textPaint.isFakeBoldText = true
            textPaint.color = Color.rgb(21, 128, 61)
            canvas.drawText(NumberFormatter.formatPaisa(receivableBalance), 36f, y + 36f, textPaint)

            textPaint.color = Color.rgb(180, 83, 9)
            canvas.drawText(NumberFormatter.formatPaisa(metrics.totalPayableToSuppliers), 200f, y + 36f, textPaint)

            textPaint.color = Color.rgb(220, 38, 38)
            canvas.drawText(NumberFormatter.formatPaisa(writeOffsTotal), 380f, y + 36f, textPaint)

            y += 65f

            // --- CUSTOMER OUTSTANDING TABLE ---
            textPaint.color = Color.BLACK
            textPaint.textSize = 12f
            textPaint.isFakeBoldText = true
            canvas.drawText("CUSTOMER OUTSTANDING (UDHAAR)", 24f, y, textPaint)
            y += 16f

            paint.color = Color.rgb(230, 235, 230)
            canvas.drawRect(24f, y, pageWidth - 24f, y + 20f, paint)

            textPaint.textSize = 9f
            textPaint.color = Color.DKGRAY
            canvas.drawText("Customer Name", 30f, y + 14f, textPaint)
            canvas.drawText("Phone", 220f, y + 14f, textPaint)
            canvas.drawText("Credit Limit", 350f, y + 14f, textPaint)
            canvas.drawText("Balance Owed", 460f, y + 14f, textPaint)
            y += 22f

            textPaint.isFakeBoldText = false
            if (customerSummaries.isEmpty()) {
                canvas.drawText("No customer credit records.", 30f, y + 14f, textPaint)
                y += 20f
            } else {
                customerSummaries.take(8).forEach { cs ->
                    paint.color = Color.rgb(245, 245, 245)
                    canvas.drawLine(24f, y, pageWidth - 24f, y, paint)
                    canvas.drawText(cs.customer.name, 30f, y + 14f, textPaint)
                    canvas.drawText(cs.customer.phone.ifBlank { "-" }, 220f, y + 14f, textPaint)
                    val lim = if (cs.customer.creditLimit != null) NumberFormatter.formatPaisa(cs.customer.creditLimit) else "None"
                    canvas.drawText(lim, 350f, y + 14f, textPaint)
                    canvas.drawText(NumberFormatter.formatPaisa(cs.balance), 460f, y + 14f, textPaint)
                    y += 20f
                }
            }

            y += 25f

            // --- EXPENSES SUMMARY ---
            textPaint.color = Color.BLACK
            textPaint.textSize = 12f
            textPaint.isFakeBoldText = true
            canvas.drawText("EXPENSES BREAKDOWN", 24f, y, textPaint)
            y += 16f

            paint.color = Color.rgb(230, 235, 230)
            canvas.drawRect(24f, y, pageWidth - 24f, y + 20f, paint)
            textPaint.textSize = 9f
            textPaint.color = Color.DKGRAY
            canvas.drawText("Category", 30f, y + 14f, textPaint)
            canvas.drawText("Amount", 350f, y + 14f, textPaint)
            y += 22f

            textPaint.isFakeBoldText = false
            val topExpenses = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }.toList().sortedByDescending { it.second }.take(5)
            topExpenses.forEach { (cat, amt) ->
                canvas.drawText(cat, 30f, y + 14f, textPaint)
                canvas.drawText(NumberFormatter.formatPaisa(amt), 350f, y + 14f, textPaint)
                y += 20f
            }

            pdfDocument.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "$shopName - Financial Report ($dateRangeStr)")
                putExtra(Intent.EXTRA_TEXT, "Here is your Hisaab PDF financial report for $shopName ($dateRangeStr).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share PDF Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generates and triggers Android Share Sheet with a single Customer Statement PDF.
     */
    fun shareCustomerStatementPdf(
        context: Context,
        shopName: String,
        customer: Customer,
        summary: CustomerSummary,
        ledger: List<CustomerLedgerItem>
    ) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanCustName = customer.name.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val pdfFile = File(reportsDir, "Statement_${cleanCustName}_${System.currentTimeMillis()}.pdf")

            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply { isAntiAlias = true }

            // Header Banner
            paint.color = Color.rgb(15, 107, 77)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 80f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 20f
            textPaint.isFakeBoldText = true
            canvas.drawText("Customer Account Statement", 24f, 36f, textPaint)

            textPaint.textSize = 12f
            textPaint.isFakeBoldText = false
            canvas.drawText("Shop: $shopName  |  Customer: ${customer.name}", 24f, 60f, textPaint)

            var y = 105f

            // Customer Details & Balance
            paint.color = Color.rgb(245, 247, 245)
            canvas.drawRoundRect(RectF(24f, y, pageWidth - 24f, y + 65f), 8f, 8f, paint)

            textPaint.color = Color.BLACK
            textPaint.textSize = 12f
            textPaint.isFakeBoldText = true
            canvas.drawText("CURRENT OUTSTANDING BALANCE", 36f, y + 24f, textPaint)

            textPaint.textSize = 20f
            textPaint.color = if (summary.balance > 0) Color.rgb(220, 38, 38) else Color.rgb(21, 128, 61)
            canvas.drawText(NumberFormatter.formatPaisa(summary.balance), 36f, y + 50f, textPaint)

            textPaint.textSize = 10f
            textPaint.color = Color.DKGRAY
            textPaint.isFakeBoldText = false
            canvas.drawText("Phone: ${customer.phone.ifBlank { "N/A" }}", 320f, y + 24f, textPaint)
            val limitStr = if (customer.creditLimit != null) NumberFormatter.formatPaisa(customer.creditLimit) else "No Limit"
            canvas.drawText("Credit Limit: $limitStr", 320f, y + 42f, textPaint)

            y += 85f

            // Ledger Table Header
            textPaint.color = Color.BLACK
            textPaint.textSize = 12f
            textPaint.isFakeBoldText = true
            canvas.drawText("TRANSACTION HISTORY", 24f, y, textPaint)
            y += 16f

            paint.color = Color.rgb(230, 235, 230)
            canvas.drawRect(24f, y, pageWidth - 24f, y + 20f, paint)

            textPaint.textSize = 9f
            textPaint.color = Color.DKGRAY
            canvas.drawText("Date", 30f, y + 14f, textPaint)
            canvas.drawText("Type / Note", 130f, y + 14f, textPaint)
            canvas.drawText("Credit / Paid", 330f, y + 14f, textPaint)
            canvas.drawText("Running Balance", 450f, y + 14f, textPaint)
            y += 24f

            textPaint.isFakeBoldText = false
            ledger.take(25).forEach { item ->
                paint.color = Color.rgb(240, 240, 240)
                canvas.drawLine(24f, y, pageWidth - 24f, y, paint)

                canvas.drawText(DateUtils.formatDateShort(item.date), 30f, y + 14f, textPaint)
                val typeName = when (item.type) {
                    CustomerLedgerType.SALE -> "Credit Sale"
                    CustomerLedgerType.PAYMENT -> "Payment"
                    CustomerLedgerType.WRITE_OFF -> "Write-Off"
                }
                val noteStr = if (item.note.isNotBlank()) "$typeName (${item.note})" else typeName
                canvas.drawText(noteStr, 130f, y + 14f, textPaint)

                val amountStr = when (item.type) {
                    CustomerLedgerType.SALE -> "+${NumberFormatter.formatPaisa(item.creditAmount)}"
                    CustomerLedgerType.PAYMENT -> "-${NumberFormatter.formatPaisa(item.amount)}"
                    CustomerLedgerType.WRITE_OFF -> "-${NumberFormatter.formatPaisa(item.amount)}"
                }
                canvas.drawText(amountStr, 330f, y + 14f, textPaint)
                canvas.drawText(NumberFormatter.formatPaisa(item.runningBalance), 450f, y + 14f, textPaint)

                y += 20f
            }

            pdfDocument.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "$shopName - Customer Statement (${customer.name})")
                putExtra(Intent.EXTRA_TEXT, "Here is the customer account statement for ${customer.name} at $shopName.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share Customer Statement PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
