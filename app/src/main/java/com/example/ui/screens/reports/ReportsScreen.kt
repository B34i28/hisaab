package com.example.ui.screens.reports

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.DashboardMetrics
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DailyReportItem
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.LocalAppStrings
import com.example.util.ReportExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentShop by viewModel.currentShop.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.reportsPeriod.collectAsStateWithLifecycle()
    val effectiveRange by viewModel.effectiveReportsRange.collectAsStateWithLifecycle()
    val metrics by viewModel.reportsMetrics.collectAsStateWithLifecycle()
    val sales by viewModel.reportsSales.collectAsStateWithLifecycle()
    val expenses by viewModel.reportsExpenses.collectAsStateWithLifecycle()
    val dailyItems by viewModel.dayByDayReports.collectAsStateWithLifecycle()
    val customersSummary by viewModel.customersSummary.collectAsStateWithLifecycle()
    val customerPayments by viewModel.reportsCustomerPayments.collectAsStateWithLifecycle()
    val customerWriteOffs by viewModel.reportsCustomerWriteOffs.collectAsStateWithLifecycle()

    val creditSalesTotal = remember(sales) {
        sales.filter { it.customerId != null }.sumOf { (it.amount - it.receivedAmount).coerceAtLeast(0L) }
    }
    val customerPaymentsTotal = remember(customerPayments) {
        customerPayments.sumOf { it.amount }
    }
    val writeOffsTotal = remember(customerWriteOffs) {
        customerWriteOffs.sumOf { it.amount }
    }
    val totalReceivable = remember(customersSummary) {
        customersSummary.filter { it.balance > 0L }.sumOf { it.balance }
    }

    var showCustomRangePicker by remember { mutableStateOf(false) }

    val presetPeriods = listOf(
        DateUtils.TimePeriod.TODAY to strings.today,
        DateUtils.TimePeriod.THIS_WEEK to strings.thisWeek,
        DateUtils.TimePeriod.THIS_MONTH to strings.thisMonth,
        DateUtils.TimePeriod.LAST_MONTH to strings.lastMonth,
        DateUtils.TimePeriod.ALL_TIME to strings.allTime
    )

    // Formatted date range description
    val dateRangeLabel = remember(selectedPeriod, effectiveRange, strings) {
        if (selectedPeriod == DateUtils.TimePeriod.ALL_TIME) {
            strings.allTime
        } else if (selectedPeriod == DateUtils.TimePeriod.TODAY) {
            "${strings.today} (${DateUtils.formatDateShort(effectiveRange.first)})"
        } else {
            "${DateUtils.formatDateShort(effectiveRange.first)} – ${DateUtils.formatDateShort(effectiveRange.second)}"
        }
    }

    // Expense breakdown by category
    val categoryBreakdown = remember(expenses) {
        val totalExp = expenses.sumOf { it.amount }
        expenses.groupBy { it.category }
            .map { (cat, list) ->
                val amount = list.sumOf { it.amount }
                val percentage = if (totalExp > 0) (amount.toFloat() / totalExp.toFloat()) else 0f
                Triple(cat, amount, percentage)
            }
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // Presets Horizontal Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reports_period_chips"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetPeriods) { (period, label) ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setReportsPeriod(period) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("report_chip_${period.name.lowercase()}")
                    )
                }

                item {
                    FilterChip(
                        selected = selectedPeriod == DateUtils.TimePeriod.CUSTOM,
                        onClick = { showCustomRangePicker = true },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(if (selectedPeriod == DateUtils.TimePeriod.CUSTOM) strings.custom else "${strings.custom}...") },
                        modifier = Modifier.testTag("report_chip_custom")
                    )
                }
            }
        }


        // Active Date Range Indicator & Quick Date Picker Button
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Selected Date Range",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateRangeLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showCustomRangePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("custom_date_range_button")
                    ) {
                        Text("Change", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Shop Financial Performance Summary Card (Selected Shop Only)
        item {
            ShopFinancialSummaryCard(
                shopName = currentShop?.name ?: "Shop",
                dateRangeLabel = dateRangeLabel,
                metrics = metrics,
                creditSalesTotal = creditSalesTotal,
                customerPaymentsTotal = customerPaymentsTotal,
                writeOffsTotal = writeOffsTotal,
                receivableBalance = totalReceivable
            )
        }

        // Customer Outstanding (Udhaar) Table Section
        item {
            CustomerOutstandingReportCard(customersSummary = customersSummary)
        }

        // Export and Share Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Export & Share Report",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export CSV
                    Button(
                        onClick = {
                            ReportExportUtils.shareCsvReport(
                                context = context,
                                shopName = currentShop?.name ?: "Shop",
                                dateRangeStr = dateRangeLabel,
                                metrics = metrics,
                                creditSalesTotal = creditSalesTotal,
                                customerPaymentsTotal = customerPaymentsTotal,
                                writeOffsTotal = writeOffsTotal,
                                receivableBalance = totalReceivable,
                                customerSummaries = customersSummary,
                                expenses = expenses,
                                dailyItems = dailyItems
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E824C)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_csv_button")
                    ) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontWeight = FontWeight.Bold)
                    }

                    // Share as PDF
                    Button(
                        onClick = {
                            ReportExportUtils.sharePdfReport(
                                context = context,
                                shopName = currentShop?.name ?: "Shop",
                                dateRangeStr = dateRangeLabel,
                                metrics = metrics,
                                creditSalesTotal = creditSalesTotal,
                                customerPaymentsTotal = customerPaymentsTotal,
                                writeOffsTotal = writeOffsTotal,
                                receivableBalance = totalReceivable,
                                customerSummaries = customersSummary,
                                expenses = expenses,
                                dailyItems = dailyItems
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_pdf_button")
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share as PDF", fontWeight = FontWeight.Bold)
                    }
                }

                // Share as text summary
                OutlinedButton(
                    onClick = {
                        val shareText = viewModel.generateShareReportText()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Summary Text"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("share_text_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Text Summary (WhatsApp / SMS)")
                }
            }
        }

        // Expenses Breakdown by Category
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expenses by Category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = CurrencyUtils.formatPaisa(metrics.totalExpenses),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LossRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (categoryBreakdown.isEmpty()) {
                        Text(
                            text = "No expenses recorded in this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        categoryBreakdown.forEach { (cat, amount, ratio) ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "${CurrencyUtils.formatPaisa(amount)} (${(ratio * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Day-by-Day Table Section
        item {
            DayByDayReportTable(dailyItems = dailyItems)
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    if (showCustomRangePicker) {
        CustomDateRangeDialog(
            initialStart = effectiveRange.first,
            initialEnd = effectiveRange.second,
            onDismiss = { showCustomRangePicker = false },
            onRangeSelected = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showCustomRangePicker = false
            }
        )
    }
}

@Composable
private fun ShopFinancialSummaryCard(
    shopName: String,
    dateRangeLabel: String,
    metrics: DashboardMetrics,
    creditSalesTotal: Long = 0L,
    customerPaymentsTotal: Long = 0L,
    writeOffsTotal: Long = 0L,
    receivableBalance: Long = 0L
) {
    val strings = LocalAppStrings.current
    val isProfit = metrics.profit >= 0
    val profitColor = if (isProfit) ProfitGreen else LossRed

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_summary_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "${strings.activeShop}: $shopName",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = dateRangeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Profit / Loss banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = profitColor.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isProfit) strings.netProfit else strings.netLoss,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = profitColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatPaisa(metrics.profit, currencyPrefix = strings.currencyUnit),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = profitColor
                    )
                    Text(
                        text = "${strings.totalSales} - ${strings.totalExpenses} - ${strings.totalPurchases} - ${strings.badDebt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Column Metrics: Sales | Purchases | Expenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.totalSales, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(metrics.totalSales, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = ProfitGreen)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.totalPurchases, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(metrics.totalPurchases, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF3B82F6))
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(strings.totalExpenses, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(metrics.totalExpenses, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = LossRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Cash In & Cash Out & Net Cash
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.cashReceived, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(metrics.cashIn, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ProfitGreen)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.cashPaidOut, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(metrics.cashOut, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF7C3AED))
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(strings.netCash, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val isNetCashPos = metrics.netCash >= 0
                    Text(
                        text = CurrencyUtils.formatPaisa(metrics.netCash, currencyPrefix = strings.currencyUnit),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isNetCashPos) ProfitGreen else LossRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Receivables vs Payables vs Bad Debt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(strings.youWillGet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(receivableBalance, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ProfitGreen)
                }
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(strings.youWillGive, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(metrics.totalPayableToSuppliers, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = WarningAmber)
                }
                Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
                    Text(strings.badDebt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyUtils.formatPaisa(writeOffsTotal, currencyPrefix = strings.currencyUnit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LossRed)
                }
            }
        }
    }
}

@Composable
private fun CustomerOutstandingReportCard(customersSummary: List<com.example.data.repository.CustomerSummary>) {
    val strings = LocalAppStrings.current
    val owingCustomers = remember(customersSummary) {
        customersSummary.filter { it.balance > 0L }.sortedByDescending { it.balance }
    }
    val totalOwing = remember(owingCustomers) {
        owingCustomers.sumOf { it.balance }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_customer_outstanding_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.customerOutstanding,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${owingCustomers.size} ${strings.customersOwing}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = CurrencyUtils.formatPaisa(totalOwing, currencyPrefix = strings.currencyUnit),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = WarningAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (owingCustomers.isEmpty()) {
                Text(
                    text = strings.noOwingCustomers,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val horizontalScroll = rememberScrollState()
                Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.width(480.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.customer, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(160.dp))
                            Text(strings.phone, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(130.dp))
                            Text(strings.balance, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(170.dp), textAlign = TextAlign.End)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    owingCustomers.forEachIndexed { index, item ->
                        val isEven = index % 2 == 0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isEven) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.Transparent,
                            modifier = Modifier.width(480.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.customer.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.width(160.dp)
                                )
                                Text(
                                    text = item.customer.phone ?: "—",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(130.dp)
                                )
                                Text(
                                    text = CurrencyUtils.formatPaisa(item.balance, currencyPrefix = strings.currencyUnit),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = WarningAmber,
                                    modifier = Modifier.width(170.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DayByDayReportTable(dailyItems: List<DailyReportItem>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_by_day_table_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day-by-Day Activity Table",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${dailyItems.size} active days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dailyItems.isEmpty()) {
                Text(
                    text = "No activity recorded in this date range.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                // Table Container with horizontal scrolling for clear column view
                val horizontalScroll = rememberScrollState()

                Column(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                    // Header Row
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.width(520.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp))
                            Text("Sales", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(105.dp), textAlign = TextAlign.End)
                            Text("Expenses", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(105.dp), textAlign = TextAlign.End)
                            Text("Purchases", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(105.dp), textAlign = TextAlign.End)
                            Text("Profit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(105.dp), textAlign = TextAlign.End)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Daily Rows
                    dailyItems.forEachIndexed { index, item ->
                        val isEven = index % 2 == 0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isEven) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.Transparent,
                            modifier = Modifier.width(520.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.dateLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.width(100.dp)
                                )
                                Text(
                                    text = CurrencyUtils.formatPaisa(item.sales, false),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.sales > 0) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(105.dp),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = CurrencyUtils.formatPaisa(item.expenses, false),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.expenses > 0) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(105.dp),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = CurrencyUtils.formatPaisa(item.purchases, false),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.purchases > 0) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(105.dp),
                                    textAlign = TextAlign.End
                                )
                                val isProf = item.profit >= 0
                                val profSign = if (isProf) "+" else ""
                                Text(
                                    text = "$profSign${CurrencyUtils.formatPaisa(item.profit, false)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isProf) ProfitGreen else LossRed,
                                    modifier = Modifier.width(105.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangeDialog(
    initialStart: Long,
    initialEnd: Long,
    onDismiss: () -> Unit,
    onRangeSelected: (start: Long, end: Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart,
        initialSelectedEndDateMillis = initialEnd
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        onRangeSelected(start, end)
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_date_range_button")
            ) {
                Text("Apply Range")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Select Report Date Range",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}
