package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.DashboardMetrics
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToCustomers: () -> Unit = {},
    onQuickAddSale: () -> Unit,
    onQuickAddExpense: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentShop by viewModel.currentShop.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.dashboardPeriod.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val recentSales by viewModel.salesList.collectAsStateWithLifecycle()
    val recentExpenses by viewModel.expensesList.collectAsStateWithLifecycle()

    val periods = listOf(
        DateUtils.TimePeriod.TODAY to strings.today,
        DateUtils.TimePeriod.THIS_WEEK to strings.thisWeek,
        DateUtils.TimePeriod.THIS_MONTH to strings.thisMonth,
        DateUtils.TimePeriod.ALL_TIME to strings.allTime
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Tabs: Today / This Week / This Month / All Time
            val currentPeriodIndex = periods.indexOfFirst { it.first == selectedPeriod }.coerceAtLeast(0)
            PrimaryTabRow(
                selectedTabIndex = currentPeriodIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("dashboard_period_tabs")
            ) {
                periods.forEachIndexed { index, (period, title) ->
                    Tab(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setDashboardPeriod(period) },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_${period.name.lowercase()}")
                    )
                }
            }
        }

        // Hero Card: Net Profit / Loss
        item {
            val periodTitle = periods.find { it.first == selectedPeriod }?.second ?: strings.today
            ProfitHeroCard(
                metrics = metrics,
                periodTitle = periodTitle,
                currencyUnit = strings.currencyUnit
            )
        }

        // 2x2 Metric Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = strings.totalSales,
                        amount = metrics.totalSales,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        tintColor = ProfitGreen,
                        currencyUnit = strings.currencyUnit,
                        containerColor = ProfitGreen.copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_sales")
                            .clickable { onNavigateToSales() }
                    )
                    MetricCard(
                        title = strings.totalExpenses,
                        amount = metrics.totalExpenses,
                        icon = Icons.Default.ReceiptLong,
                        tintColor = LossRed,
                        currencyUnit = strings.currencyUnit,
                        containerColor = LossRed.copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_expenses")
                            .clickable { onNavigateToExpenses() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = strings.totalPurchases,
                        amount = metrics.totalPurchases,
                        icon = Icons.Default.ShoppingCart,
                        tintColor = Color(0xFF3B82F6),
                        currencyUnit = strings.currencyUnit,
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_purchases")
                            .clickable { onNavigateToSuppliers() }
                    )
                    MetricCard(
                        title = strings.cashReceived,
                        amount = metrics.cashIn,
                        icon = Icons.Default.Payments,
                        tintColor = ProfitGreen,
                        currencyUnit = strings.currencyUnit,
                        containerColor = ProfitGreen.copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_cash_received")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = strings.netCash,
                        amount = metrics.netCash,
                        icon = Icons.Default.AccountBalanceWallet,
                        tintColor = if (metrics.netCash >= 0) ProfitGreen else LossRed,
                        currencyUnit = strings.currencyUnit,
                        containerColor = (if (metrics.netCash >= 0) ProfitGreen else LossRed).copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_net_cash")
                    )
                    MetricCard(
                        title = strings.badDebt,
                        amount = metrics.badDebt,
                        icon = Icons.Default.ReceiptLong,
                        tintColor = LossRed,
                        currencyUnit = strings.currencyUnit,
                        containerColor = LossRed.copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_bad_debt")
                    )
                }
            }
        }

        // Side-by-side Overview: "You will get: X" (Receivables) vs "You will give: Y" (Payables)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_get_give_overview")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // You will get (Receivable from Customers)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ProfitGreen.copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToCustomers() }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = strings.youWillGet.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatPaisa(
                                    paisa = metrics.totalReceivableFromCustomers,
                                    currencyPrefix = strings.currencyUnit
                                ),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = ProfitGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.customersOweYou,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // You will give (Payable to Suppliers)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WarningAmber.copy(alpha = 0.10f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToSuppliers() }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = strings.youWillGive.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatPaisa(
                                    paisa = metrics.totalPayableToSuppliers,
                                    currencyPrefix = strings.currencyUnit
                                ),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = WarningAmber,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.totalPayable,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Entry Buttons
        item {
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onQuickAddSale,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_add_sale_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addSale, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onQuickAddExpense,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("quick_add_expense_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.addExpense, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Recent Activity Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentTransactions,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.viewAll,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToSales() }
                )
            }
        }

        if (recentSales.isEmpty() && recentExpenses.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.noTransactionsYet,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentShop != null) "${strings.addSale} / ${strings.addExpense}" else strings.noShopSelectedState,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Show up to 4 recent sales
            items(recentSales.take(4)) { sale ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ProfitGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (sale.note.isNotBlank()) sale.note else strings.navSales,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = DateUtils.formatDateTime(sale.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "+${CurrencyUtils.formatPaisa(sale.amount, currencyPrefix = strings.currencyUnit)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ProfitGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProfitHeroCard(
    metrics: DashboardMetrics,
    periodTitle: String,
    currencyUnit: String
) {
    val strings = LocalAppStrings.current
    val isProfit = metrics.profit >= 0
    val cardColor = if (isProfit) ProfitGreen else LossRed

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.14f)),
        border = BorderStroke(1.5.dp, cardColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_net_profit")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (isProfit) strings.netProfit else strings.netLoss} ($periodTitle)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isProfit) strings.profitTag else strings.lossTag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = CurrencyUtils.formatPaisa(metrics.profit, currencyPrefix = currencyUnit),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = cardColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${strings.totalSales}: ${CurrencyUtils.formatPaisa(metrics.totalSales, false)}  •  ${strings.totalExpenses}: ${CurrencyUtils.formatPaisa(metrics.totalExpenses, false)}  •  ${strings.totalPurchases}: ${CurrencyUtils.formatPaisa(metrics.totalPurchases, false)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: Long,
    icon: ImageVector,
    tintColor: Color,
    containerColor: Color,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Surface(
                    shape = CircleShape,
                    color = tintColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyUtils.formatPaisa(amount, currencyPrefix = currencyUnit),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
