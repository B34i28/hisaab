package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DataStoreManager
import com.example.data.local.entity.Customer
import com.example.data.local.entity.CustomerPayment
import com.example.data.local.entity.CustomerWriteOff
import com.example.data.local.entity.Expense
import com.example.data.local.entity.Purchase
import com.example.data.local.entity.Sale
import com.example.data.local.entity.Shop
import com.example.data.local.entity.Supplier
import com.example.data.local.entity.SupplierPayment
import com.example.data.repository.CustomerLedgerItem
import com.example.data.repository.CustomerSummary
import com.example.data.repository.DashboardMetrics
import com.example.data.repository.ShopRepository
import com.example.data.repository.SupplierSummary
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.LedgerCalculator
import com.example.util.ThemeMode
import com.example.util.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyReportItem(
    val dateMillis: Long,
    val dateLabel: String,
    val sales: Long,
    val expenses: Long,
    val purchases: Long,
    val profit: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShopRepository

    init {
        val database = AppDatabase.getInstance(application)
        val dataStoreManager = DataStoreManager(application)
        repository = ShopRepository(database, dataStoreManager)
    }

    // All shops
    val allShops: StateFlow<List<Shop>> = repository.allShops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Shop Id from DataStore with distinctUntilChanged to prevent redundant queries
    val selectedShopId: StateFlow<Long?> = repository.selectedShopId
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Active Shop Entity (pure transformation, zero side-effects on rapid switching)
    val currentShop: StateFlow<Shop?> = combine(allShops, selectedShopId) { shops, selectedId ->
        if (shops.isEmpty()) {
            null
        } else {
            shops.find { it.id == selectedId } ?: shops.first()
        }
    }.distinctUntilChanged { old, new -> old?.id == new?.id && old?.name == new?.name }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    // First run state: true when shops list has loaded and is empty
    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    // App Lock States
    val isAppLockEnabled: StateFlow<Boolean> = repository.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val customPin: StateFlow<String?> = repository.customPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var hasInitialLockCheckHappened = false

    init {
        viewModelScope.launch {
            allShops.collect {
                _isInitialLoading.value = false
            }
        }
        viewModelScope.launch {
            repository.isAppLockEnabled.collect { enabled ->
                if (enabled && !hasInitialLockCheckHappened) {
                    _isAppLocked.value = true
                    hasInitialLockCheckHappened = true
                }
            }
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (isAppLockEnabled.value) {
            _isAppLocked.value = true
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppLockEnabled(enabled)
            if (!enabled) {
                _isAppLocked.value = false
            }
        }
    }

    fun setCustomPin(pin: String?) {
        viewModelScope.launch {
            repository.setCustomPin(pin)
        }
    }

    // Theme Mode
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .map { ThemeMode.fromCode(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode.code)
        }
    }

    fun toggleTheme() {
        val current = themeMode.value
        val next = if (current == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    // App Language
    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage
        .map { AppLanguage.fromCode(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    fun setAppLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(lang.code)
        }
    }

    fun toggleLanguage() {
        val next = if (appLanguage.value == AppLanguage.URDU) AppLanguage.ENGLISH else AppLanguage.URDU
        setAppLanguage(next)
    }


    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.createBackupJson()
            onResult(json)
        }
    }

    fun restoreBackup(
        backup: com.example.util.BackupData,
        onSuccess: (com.example.util.BackupStats) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val stats = repository.restoreBackup(backup)
                onSuccess(stats)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to restore backup")
            }
        }
    }

    // Dashboard Time Period filter
    private val _dashboardPeriod = MutableStateFlow(DateUtils.TimePeriod.TODAY)
    val dashboardPeriod: StateFlow<DateUtils.TimePeriod> = _dashboardPeriod.asStateFlow()

    fun setDashboardPeriod(period: DateUtils.TimePeriod) {
        _dashboardPeriod.value = period
    }

    // Dashboard metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        currentShop,
        _dashboardPeriod
    ) { shop, period ->
        Pair(shop?.id, period)
    }.flatMapLatest { (shopId, period) ->
        if (shopId == null) {
            flowOf(DashboardMetrics())
        } else {
            val (start, end) = DateUtils.getRangeForPeriod(period)
            repository.getDashboardMetrics(shopId, start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Sales StateFlow
    val salesList: StateFlow<List<Sale>> = currentShop
        .flatMapLatest { shop ->
            if (shop == null) flowOf(emptyList())
            else repository.getSales(shop.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses StateFlow
    val expensesList: StateFlow<List<Expense>> = currentShop
        .flatMapLatest { shop ->
            if (shop == null) flowOf(emptyList())
            else repository.getExpenses(shop.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctCategories: StateFlow<List<String>> = currentShop
        .flatMapLatest { shop ->
            if (shop == null) flowOf(emptyList())
            else repository.getDistinctCategories(shop.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Suppliers with summary (total purchases, total paid, balance payable)
    val suppliersSummary: StateFlow<List<SupplierSummary>> = currentShop
        .flatMapLatest { shop ->
            if (shop == null) flowOf(emptyList())
            else repository.getSuppliersWithSummary(shop.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customers with summary (total credit given, total received back, write-offs, balance)
    val customersSummary: StateFlow<List<CustomerSummary>> = currentShop
        .flatMapLatest { shop ->
            if (shop == null) flowOf(emptyList())
            else repository.getCustomersWithSummary(shop.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Raw list of customers for dropdowns/pickers
    val rawCustomersList: StateFlow<List<com.example.data.local.entity.Customer>> = currentShop
        .flatMapLatest { shop ->
            if (shop == null) flowOf(emptyList())
            else repository.getCustomers(shop.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total Receivable (All-time sum of outstanding balances)
    val totalReceivable: StateFlow<Long> = customersSummary
        .map { list -> list.filter { it.balance > 0L }.sumOf { it.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)


    // Reports period filter & custom date range
    private val _reportsPeriod = MutableStateFlow(DateUtils.TimePeriod.THIS_MONTH)
    val reportsPeriod: StateFlow<DateUtils.TimePeriod> = _reportsPeriod.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    fun setReportsPeriod(period: DateUtils.TimePeriod) {
        _reportsPeriod.value = period
        if (period != DateUtils.TimePeriod.CUSTOM) {
            _customDateRange.value = null
        }
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _reportsPeriod.value = DateUtils.TimePeriod.CUSTOM
        _customDateRange.value = Pair(DateUtils.startOfDay(start), DateUtils.endOfDay(end))
    }

    val effectiveReportsRange: StateFlow<Pair<Long, Long>> = combine(
        _reportsPeriod,
        _customDateRange
    ) { period, customRange ->
        if (period == DateUtils.TimePeriod.CUSTOM && customRange != null) {
            customRange
        } else {
            DateUtils.getRangeForPeriod(period)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DateUtils.getThisMonthRange())

    val reportsMetrics: StateFlow<DashboardMetrics> = combine(
        currentShop,
        effectiveReportsRange
    ) { shop, range ->
        Pair(shop?.id, range)
    }.flatMapLatest { (shopId, range) ->
        if (shopId == null) {
            flowOf(DashboardMetrics())
        } else {
            repository.getDashboardMetrics(shopId, range.first, range.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val reportsSales: StateFlow<List<Sale>> = combine(
        currentShop,
        effectiveReportsRange
    ) { shop, range ->
        Pair(shop?.id, range)
    }.flatMapLatest { (shopId, range) ->
        if (shopId == null) flowOf(emptyList())
        else repository.getSalesByRange(shopId, range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportsExpenses: StateFlow<List<Expense>> = combine(
        currentShop,
        effectiveReportsRange
    ) { shop, range ->
        Pair(shop?.id, range)
    }.flatMapLatest { (shopId, range) ->
        if (shopId == null) {
            flowOf(emptyList())
        } else {
            repository.getExpensesByRange(shopId, range.first, range.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportsPurchases: StateFlow<List<Purchase>> = combine(
        currentShop,
        effectiveReportsRange
    ) { shop, range ->
        Pair(shop?.id, range)
    }.flatMapLatest { (shopId, range) ->
        if (shopId == null) {
            flowOf(emptyList())
        } else {
            repository.getPurchasesByRange(shopId, range.first, range.second)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayByDayReports: StateFlow<List<DailyReportItem>> = combine(
        reportsSales,
        reportsExpenses,
        reportsPurchases
    ) { sales, expenses, purchases ->
        LedgerCalculator.calculateDailyReportItems(sales, expenses, purchases)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // ACTION FUNCTIONS
    // ==========================================

    private var selectShopJob: kotlinx.coroutines.Job? = null
    fun selectShop(shopId: Long) {
        selectShopJob?.cancel()
        selectShopJob = viewModelScope.launch {
            repository.setSelectedShopId(shopId)
        }
    }


    fun createShop(name: String, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val newId = repository.createShop(name)
            onComplete?.invoke(newId)
        }
    }

    fun updateShop(shop: Shop) {
        viewModelScope.launch {
            repository.updateShop(shop)
        }
    }

    fun deleteShop(shop: Shop) {
        viewModelScope.launch {
            repository.deleteShop(shop)
            val remaining = allShops.value.filter { it.id != shop.id }
            if (remaining.isNotEmpty()) {
                repository.setSelectedShopId(remaining.first().id)
            } else {
                repository.setSelectedShopId(null)
            }
        }
    }

    // Sales actions
    fun addSale(
        amount: Long,
        receivedAmount: Long = amount,
        customerId: Long? = null,
        date: Long,
        note: String
    ) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            repository.insertSale(
                Sale(
                    shopId = shopId,
                    date = date,
                    amount = amount,
                    receivedAmount = receivedAmount,
                    customerId = customerId,
                    note = note.trim()
                )
            )
        }
    }

    // Overload for sales with 3 arguments (for backwards compatibility)
    fun addSale(amount: Long, date: Long, note: String) {
        addSale(amount = amount, receivedAmount = amount, customerId = null, date = date, note = note)
    }

    fun updateSale(sale: Sale) {
        viewModelScope.launch {
            repository.updateSale(sale)
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
        }
    }

    // Customer actions
    fun addCustomer(
        name: String,
        phone: String,
        note: String,
        creditLimit: Long?,
        onCreated: ((Long) -> Unit)? = null
    ) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            val id = repository.insertCustomer(
                com.example.data.local.entity.Customer(
                    shopId = shopId,
                    name = name.trim(),
                    phone = phone.trim(),
                    note = note.trim(),
                    creditLimit = creditLimit
                )
            )
            onCreated?.invoke(id)
        }
    }

    fun updateCustomer(customer: com.example.data.local.entity.Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun deleteCustomer(customer: com.example.data.local.entity.Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun getCustomerById(customerId: Long): StateFlow<com.example.data.local.entity.Customer?> {
        val shopId = currentShop.value?.id ?: 0L
        return repository.getCustomerById(shopId, customerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun getCustomerLedger(customerId: Long): StateFlow<List<com.example.data.repository.CustomerLedgerItem>> {
        val shopId = currentShop.value?.id ?: 0L
        return repository.getCustomerLedger(shopId, customerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Customer Payments actions
    fun addCustomerPayment(
        customerId: Long,
        amount: Long,
        date: Long,
        note: String
    ) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            repository.insertCustomerPayment(
                com.example.data.local.entity.CustomerPayment(
                    shopId = shopId,
                    customerId = customerId,
                    amount = amount,
                    date = date,
                    note = note.trim()
                )
            )
        }
    }

    fun updateCustomerPayment(payment: com.example.data.local.entity.CustomerPayment) {
        viewModelScope.launch {
            repository.updateCustomerPayment(payment)
        }
    }

    fun deleteCustomerPayment(payment: com.example.data.local.entity.CustomerPayment) {
        viewModelScope.launch {
            repository.deleteCustomerPayment(payment)
        }
    }

    // Customer Write-offs actions
    fun addCustomerWriteOff(
        customerId: Long,
        amount: Long,
        date: Long,
        note: String
    ) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            repository.insertCustomerWriteOff(
                com.example.data.local.entity.CustomerWriteOff(
                    shopId = shopId,
                    customerId = customerId,
                    amount = amount,
                    date = date,
                    note = note.trim()
                )
            )
        }
    }

    fun updateCustomerWriteOff(writeOff: com.example.data.local.entity.CustomerWriteOff) {
        viewModelScope.launch {
            repository.updateCustomerWriteOff(writeOff)
        }
    }

    fun deleteCustomerWriteOff(writeOff: com.example.data.local.entity.CustomerWriteOff) {
        viewModelScope.launch {
            repository.deleteCustomerWriteOff(writeOff)
        }
    }

    // Expense actions
    fun addExpense(amount: Long, category: String, date: Long, note: String) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    shopId = shopId,
                    category = category.trim(),
                    date = date,
                    amount = amount,
                    note = note.trim()
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Supplier actions
    fun addSupplier(name: String, phone: String, note: String, onCreated: ((Long) -> Unit)? = null) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            val id = repository.insertSupplier(
                Supplier(
                    shopId = shopId,
                    name = name.trim(),
                    phone = phone.trim(),
                    note = note.trim()
                )
            )
            onCreated?.invoke(id)
        }
    }

    fun updateSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.updateSupplier(supplier)
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    // Purchases actions
    fun addPurchase(
        supplierId: Long,
        totalAmount: Long,
        paidAmount: Long,
        date: Long,
        description: String
    ) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            repository.insertPurchase(
                Purchase(
                    shopId = shopId,
                    supplierId = supplierId,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    date = date,
                    description = description.trim()
                )
            )
        }
    }

    fun updatePurchase(purchase: Purchase) {
        viewModelScope.launch {
            repository.updatePurchase(purchase)
        }
    }

    fun deletePurchase(purchase: Purchase) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
        }
    }

    // Supplier Payments actions
    fun addSupplierPayment(
        supplierId: Long,
        purchaseId: Long?,
        amount: Long,
        date: Long,
        note: String
    ) {
        val shopId = currentShop.value?.id ?: return
        viewModelScope.launch {
            repository.insertPayment(
                SupplierPayment(
                    shopId = shopId,
                    supplierId = supplierId,
                    purchaseId = purchaseId,
                    amount = amount,
                    date = date,
                    note = note.trim()
                )
            )
        }
    }

    fun updateSupplierPayment(payment: SupplierPayment) {
        viewModelScope.launch {
            repository.updatePayment(payment)
        }
    }

    fun deleteSupplierPayment(payment: SupplierPayment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    // Specific Supplier Queries for Details Screen
    fun getSupplierById(supplierId: Long) = currentShop.flatMapLatest { shop ->
        if (shop == null) flowOf(null)
        else repository.getSupplierById(shop.id, supplierId)
    }

    fun getPurchasesForSupplier(supplierId: Long) = currentShop.flatMapLatest { shop ->
        if (shop == null) flowOf(emptyList())
        else repository.getPurchasesBySupplier(shop.id, supplierId)
    }

    fun getPaymentsForSupplier(supplierId: Long) = currentShop.flatMapLatest { shop ->
        if (shop == null) flowOf(emptyList())
        else repository.getPaymentsBySupplier(shop.id, supplierId)
    }

    // Reports Customer Payments & Write-Offs
    val reportsCustomerPayments: StateFlow<List<com.example.data.local.entity.CustomerPayment>> = combine(
        currentShop,
        effectiveReportsRange
    ) { shop, range ->
        Pair(shop?.id, range)
    }.flatMapLatest { (shopId, range) ->
        if (shopId == null) flowOf(emptyList())
        else repository.getCustomerPaymentsByRange(shopId, range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportsCustomerWriteOffs: StateFlow<List<com.example.data.local.entity.CustomerWriteOff>> = combine(
        currentShop,
        effectiveReportsRange
    ) { shop, range ->
        Pair(shop?.id, range)
    }.flatMapLatest { (shopId, range) ->
        if (shopId == null) flowOf(emptyList())
        else repository.getCustomerWriteOffsByRange(shopId, range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminder message for customer
    fun generateCustomerReminderText(customerName: String, balancePaisa: Long): String {
        val shopName = currentShop.value?.name ?: "Our Shop"
        val formattedAmount = com.example.util.NumberFormatter.formatPaisa(balancePaisa)
        return "Assalam o Alaikum $customerName, your pending balance at $shopName is $formattedAmount. Please pay at your earliest convenience. Thank you."
    }

    // Customer Full Statement Text for WhatsApp/Sharing
    fun generateCustomerStatementText(
        customer: com.example.data.local.entity.Customer,
        ledger: List<com.example.data.repository.CustomerLedgerItem>,
        summary: com.example.data.repository.CustomerSummary
    ): String {
        val shopName = currentShop.value?.name ?: "Shop"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("📜 *Customer Statement / کھاتہ*")
        sb.appendLine("🏪 *Shop:* $shopName")
        sb.appendLine("👤 *Customer:* ${customer.name}")
        if (customer.phone.isNotBlank()) sb.appendLine("📞 *Phone:* ${customer.phone}")
        sb.appendLine("------------------------------------")
        sb.appendLine("💳 *Total Credit Given:* ${com.example.util.NumberFormatter.formatPaisa(summary.totalCreditGiven)}")
        sb.appendLine("💵 *Total Received:* ${com.example.util.NumberFormatter.formatPaisa(summary.totalReceivedBack)}")
        if (summary.totalWriteOffs > 0) {
            sb.appendLine("❌ *Write-Offs:* ${com.example.util.NumberFormatter.formatPaisa(summary.totalWriteOffs)}")
        }
        sb.appendLine("⚖️ *Current Balance:* ${com.example.util.NumberFormatter.formatPaisa(summary.balance)}")
        sb.appendLine("------------------------------------")
        sb.appendLine("*Recent Transactions:*")
        ledger.take(15).forEach { item ->
            val dateStr = sdf.format(Date(item.date))
            val typeStr = when (item.type) {
                com.example.data.repository.CustomerLedgerType.SALE -> "Credit Sale (+${com.example.util.NumberFormatter.formatPaisa(item.creditAmount, showCurrency = false)})"
                com.example.data.repository.CustomerLedgerType.PAYMENT -> "Payment (-${com.example.util.NumberFormatter.formatPaisa(item.amount, showCurrency = false)})"
                com.example.data.repository.CustomerLedgerType.WRITE_OFF -> "Write-Off (-${com.example.util.NumberFormatter.formatPaisa(item.amount, showCurrency = false)})"
            }
            sb.appendLine("• $dateStr: $typeStr | Bal: ${com.example.util.NumberFormatter.formatPaisa(item.runningBalance, showCurrency = false)}")
            if (item.note.isNotBlank()) {
                sb.appendLine("  _Note: ${item.note}_")
            }
        }
        sb.appendLine("------------------------------------")
        sb.appendLine("_Generated by ShopLedger_")
        return sb.toString().trimIndent()
    }

    // Shareable text report for WhatsApp / Messaging
    fun generateShareReportText(): String {
        val shopName = currentShop.value?.name ?: "Shop"
        val periodName = _reportsPeriod.value.title
        val metrics = reportsMetrics.value

        val profitSign = if (metrics.profit >= 0) "+" else ""
        return """
            📊 *ShopLedger Financial Report*
            🏪 *Shop:* $shopName
            📅 *Period:* $periodName
            ------------------------------------
            💰 *Total Sales:* ${com.example.util.NumberFormatter.formatPaisa(metrics.totalSales)}
            📉 *Total Expenses:* ${com.example.util.NumberFormatter.formatPaisa(metrics.totalExpenses)}
            📦 *Total Purchases:* ${com.example.util.NumberFormatter.formatPaisa(metrics.totalPurchases)}
            ❌ *Bad Debt (Write-offs):* ${com.example.util.NumberFormatter.formatPaisa(metrics.badDebt)}
            ------------------------------------
            📈 *Net Profit:* $profitSign${com.example.util.NumberFormatter.formatPaisa(metrics.profit)}
            💵 *Cash In:* ${com.example.util.NumberFormatter.formatPaisa(metrics.cashIn)}
            💸 *Cash Out:* ${com.example.util.NumberFormatter.formatPaisa(metrics.cashOut)}
            💰 *Net Cash:* ${com.example.util.NumberFormatter.formatPaisa(metrics.netCash)}
            ------------------------------------
            🟢 *You Will Get (Customers):* ${com.example.util.NumberFormatter.formatPaisa(metrics.totalReceivableFromCustomers)}
            🔴 *You Will Give (Suppliers):* ${com.example.util.NumberFormatter.formatPaisa(metrics.totalPayableToSuppliers)}
            ------------------------------------
            _Generated by ShopLedger_
        """.trimIndent()
    }
}
