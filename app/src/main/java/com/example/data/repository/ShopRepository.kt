package com.example.data.repository

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class SupplierSummary(
    val supplier: Supplier,
    val totalPurchases: Long,
    val totalPaid: Long,
    val balancePayable: Long // totalPurchases - totalPaid
)

data class CustomerSummary(
    val customer: Customer,
    val totalCreditGiven: Long,
    val totalReceivedBack: Long,
    val totalWriteOffs: Long,
    val balance: Long // totalCreditGiven - totalReceivedBack - totalWriteOffs
)

enum class CustomerLedgerType {
    SALE, PAYMENT, WRITE_OFF
}

data class CustomerLedgerItem(
    val id: Long,
    val type: CustomerLedgerType,
    val date: Long,
    val amount: Long,
    val receivedAmount: Long = 0L,
    val creditAmount: Long = 0L,
    val description: String = "",
    val note: String = "",
    val runningBalance: Long = 0L,
    val sourceSale: Sale? = null,
    val sourcePayment: CustomerPayment? = null,
    val sourceWriteOff: CustomerWriteOff? = null
)

data class DashboardMetrics(
    val totalSales: Long = 0L,
    val totalExpenses: Long = 0L,
    val totalPurchases: Long = 0L,
    val badDebt: Long = 0L,
    val profit: Long = 0L, // sales - expenses - purchases - badDebt
    val cashIn: Long = 0L, // sum(sale.receivedAmount) + sum(customerPayment.amount)
    val cashOut: Long = 0L, // expenses + sum(purchase.paidAmount) + sum(supplierPayment.amount)
    val netCash: Long = 0L, // cashIn - cashOut
    val totalPayableToSuppliers: Long = 0L,
    val totalReceivableFromCustomers: Long = 0L
) {
    val cashPaidOut: Long get() = cashOut
}

class ShopRepository(
    private val database: AppDatabase,
    private val dataStoreManager: DataStoreManager
) {
    private val shopDao = database.shopDao()
    private val customerDao = database.customerDao()
    private val customerPaymentDao = database.customerPaymentDao()
    private val customerWriteOffDao = database.customerWriteOffDao()
    private val supplierDao = database.supplierDao()
    private val purchaseDao = database.purchaseDao()
    private val paymentDao = database.supplierPaymentDao()
    private val saleDao = database.saleDao()
    private val expenseDao = database.expenseDao()

    // Shop operations
    val allShops: Flow<List<Shop>> = shopDao.getAllShops()
    val selectedShopId: Flow<Long?> = dataStoreManager.selectedShopId

    suspend fun setSelectedShopId(shopId: Long?) {
        dataStoreManager.setSelectedShopId(shopId)
    }

    suspend fun getShopCount(): Int = shopDao.getShopCount()

    suspend fun createShop(name: String): Long {
        val id = shopDao.insert(Shop(name = name.trim()))
        setSelectedShopId(id)
        return id
    }

    suspend fun updateShop(shop: Shop) = shopDao.update(shop)

    suspend fun deleteShop(shop: Shop) {
        customerWriteOffDao.deleteByShopId(shop.id)
        customerPaymentDao.deleteByShopId(shop.id)
        customerDao.deleteByShopId(shop.id)
        paymentDao.deleteByShopId(shop.id)
        purchaseDao.deleteByShopId(shop.id)
        supplierDao.deleteByShopId(shop.id)
        saleDao.deleteByShopId(shop.id)
        expenseDao.deleteByShopId(shop.id)
        shopDao.delete(shop)

        val remaining = shopDao.getAllShopsList()
        if (remaining.isNotEmpty()) {
            setSelectedShopId(remaining.first().id)
        } else {
            setSelectedShopId(null)
        }
    }

    // App Lock Preferences
    val isAppLockEnabled: Flow<Boolean> = dataStoreManager.isAppLockEnabled
    val customPin: Flow<String?> = dataStoreManager.customPin

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStoreManager.setAppLockEnabled(enabled)
    }

    suspend fun setCustomPin(pin: String?) {
        dataStoreManager.setCustomPin(pin)
    }

    // Theme & Language Preferences
    val themeMode: Flow<String> = dataStoreManager.themeMode
    val appLanguage: Flow<String> = dataStoreManager.appLanguage

    suspend fun setThemeMode(mode: String) {
        dataStoreManager.setThemeMode(mode)
    }

    suspend fun setAppLanguage(lang: String) {
        dataStoreManager.setAppLanguage(lang)
    }

    // Backup & Restore
    suspend fun createBackupJson(): String = com.example.util.BackupUtils.createBackupJson(database)

    suspend fun restoreBackup(backup: com.example.util.BackupData): com.example.util.BackupStats {
        val stats = com.example.util.BackupUtils.restoreDatabase(database, backup)
        val remaining = shopDao.getAllShopsList()
        if (remaining.isNotEmpty()) {
            setSelectedShopId(remaining.first().id)
        } else {
            setSelectedShopId(null)
        }
        return stats
    }

    // Sales operations
    fun getSales(shopId: Long): Flow<List<Sale>> = saleDao.getSalesByShop(shopId)

    fun getSalesByRange(shopId: Long, start: Long, end: Long): Flow<List<Sale>> =
        saleDao.getSalesByDateRange(shopId, start, end)

    fun getSalesByCustomer(shopId: Long, customerId: Long): Flow<List<Sale>> =
        saleDao.getSalesByCustomer(shopId, customerId)

    fun getCreditSales(shopId: Long): Flow<List<Sale>> =
        saleDao.getCreditSalesByShop(shopId)

    suspend fun getSaleById(shopId: Long, id: Long): Sale? =
        saleDao.getSaleByIdOnce(shopId, id)

    suspend fun insertSale(sale: Sale): Long = saleDao.insert(sale)
    suspend fun updateSale(sale: Sale) = saleDao.update(sale)
    suspend fun deleteSale(sale: Sale) = saleDao.delete(sale)

    // Expense operations
    fun getExpenses(shopId: Long): Flow<List<Expense>> = expenseDao.getExpensesByShop(shopId)

    fun getExpensesByRange(shopId: Long, start: Long, end: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(shopId, start, end)

    fun getDistinctCategories(shopId: Long): Flow<List<String>> =
        expenseDao.getDistinctCategories(shopId)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insert(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    // Supplier operations
    fun getSuppliers(shopId: Long): Flow<List<Supplier>> = supplierDao.getSuppliersByShop(shopId)

    fun getSupplierById(shopId: Long, id: Long): Flow<Supplier?> =
        supplierDao.getSupplierById(shopId, id)

    suspend fun insertSupplier(supplier: Supplier): Long = supplierDao.insert(supplier)
    suspend fun updateSupplier(supplier: Supplier) = supplierDao.update(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = supplierDao.delete(supplier)

    // Purchase operations
    fun getPurchases(shopId: Long): Flow<List<Purchase>> = purchaseDao.getPurchasesByShop(shopId)

    fun getPurchasesBySupplier(shopId: Long, supplierId: Long): Flow<List<Purchase>> =
        purchaseDao.getPurchasesBySupplier(shopId, supplierId)

    fun getPurchasesByRange(shopId: Long, start: Long, end: Long): Flow<List<Purchase>> =
        purchaseDao.getPurchasesByDateRange(shopId, start, end)

    suspend fun insertPurchase(purchase: Purchase): Long = purchaseDao.insert(purchase)
    suspend fun updatePurchase(purchase: Purchase) = purchaseDao.update(purchase)
    suspend fun deletePurchase(purchase: Purchase) = purchaseDao.delete(purchase)

    // Supplier Payments
    fun getPayments(shopId: Long): Flow<List<SupplierPayment>> =
        paymentDao.getPaymentsByShop(shopId)

    fun getPaymentsBySupplier(shopId: Long, supplierId: Long): Flow<List<SupplierPayment>> =
        paymentDao.getPaymentsBySupplier(shopId, supplierId)

    fun getPaymentsByRange(shopId: Long, start: Long, end: Long): Flow<List<SupplierPayment>> =
        paymentDao.getPaymentsByDateRange(shopId, start, end)

    suspend fun insertPayment(payment: SupplierPayment): Long = paymentDao.insert(payment)
    suspend fun updatePayment(payment: SupplierPayment) = paymentDao.update(payment)
    suspend fun deletePayment(payment: SupplierPayment) = paymentDao.delete(payment)

    // Customer operations
    fun getCustomers(shopId: Long): Flow<List<Customer>> = customerDao.getCustomersByShop(shopId)

    fun getCustomerById(shopId: Long, id: Long): Flow<Customer?> =
        customerDao.getCustomerById(shopId, id)

    suspend fun getCustomerByIdOnce(shopId: Long, id: Long): Customer? =
        customerDao.getCustomerByIdOnce(shopId, id)

    suspend fun insertCustomer(customer: Customer): Long = customerDao.insert(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.update(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.delete(customer)

    // Customer Payments
    fun getCustomerPayments(shopId: Long): Flow<List<CustomerPayment>> =
        customerPaymentDao.getPaymentsByShop(shopId)

    fun getCustomerPaymentsByCustomer(shopId: Long, customerId: Long): Flow<List<CustomerPayment>> =
        customerPaymentDao.getPaymentsByCustomer(shopId, customerId)

    fun getCustomerPaymentsByRange(shopId: Long, start: Long, end: Long): Flow<List<CustomerPayment>> =
        customerPaymentDao.getPaymentsByDateRange(shopId, start, end)

    suspend fun insertCustomerPayment(payment: CustomerPayment): Long =
        customerPaymentDao.insert(payment)

    suspend fun updateCustomerPayment(payment: CustomerPayment) =
        customerPaymentDao.update(payment)

    suspend fun deleteCustomerPayment(payment: CustomerPayment) =
        customerPaymentDao.delete(payment)

    // Customer Write-offs
    fun getCustomerWriteOffs(shopId: Long): Flow<List<CustomerWriteOff>> =
        customerWriteOffDao.getWriteOffsByShop(shopId)

    fun getCustomerWriteOffsByCustomer(shopId: Long, customerId: Long): Flow<List<CustomerWriteOff>> =
        customerWriteOffDao.getWriteOffsByCustomer(shopId, customerId)

    fun getCustomerWriteOffsByRange(shopId: Long, start: Long, end: Long): Flow<List<CustomerWriteOff>> =
        customerWriteOffDao.getWriteOffsByDateRange(shopId, start, end)

    suspend fun insertCustomerWriteOff(writeOff: CustomerWriteOff): Long =
        customerWriteOffDao.insert(writeOff)

    suspend fun updateCustomerWriteOff(writeOff: CustomerWriteOff) =
        customerWriteOffDao.update(writeOff)

    suspend fun deleteCustomerWriteOff(writeOff: CustomerWriteOff) =
        customerWriteOffDao.delete(writeOff)

    // Customer Summaries
    fun getCustomersWithSummary(shopId: Long): Flow<List<CustomerSummary>> {
        val customersFlow = customerDao.getCustomersByShop(shopId)
        val salesFlow = saleDao.getSalesByShop(shopId)
        val paymentsFlow = customerPaymentDao.getPaymentsByShop(shopId)
        val writeOffsFlow = customerWriteOffDao.getWriteOffsByShop(shopId)

        return combine(customersFlow, salesFlow, paymentsFlow, writeOffsFlow) { customers, sales, payments, writeOffs ->
            com.example.util.LedgerCalculator.calculateCustomerSummaries(customers, sales, payments, writeOffs)
        }
    }

    // Customer Chronological Ledger
    fun getCustomerLedger(shopId: Long, customerId: Long): Flow<List<CustomerLedgerItem>> {
        val salesFlow = saleDao.getSalesByCustomer(shopId, customerId)
        val paymentsFlow = customerPaymentDao.getPaymentsByCustomer(shopId, customerId)
        val writeOffsFlow = customerWriteOffDao.getWriteOffsByCustomer(shopId, customerId)

        return combine(salesFlow, paymentsFlow, writeOffsFlow) { sales, payments, writeOffs ->
            com.example.util.LedgerCalculator.buildCustomerLedger(sales, payments, writeOffs)
        }
    }

    // Supplier summaries with outstanding calculation
    fun getSuppliersWithSummary(shopId: Long): Flow<List<SupplierSummary>> {
        val suppliersFlow = supplierDao.getSuppliersByShop(shopId)
        val purchasesFlow = purchaseDao.getPurchasesByShop(shopId)
        val paymentsFlow = paymentDao.getPaymentsByShop(shopId)

        return combine(suppliersFlow, purchasesFlow, paymentsFlow) { suppliers, purchases, payments ->
            com.example.util.LedgerCalculator.calculateSupplierSummaries(suppliers, purchases, payments)
        }
    }

    // Dashboard Metrics for a given date range
    fun getDashboardMetrics(
        shopId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<DashboardMetrics> {
        val salesFlow = saleDao.getSalesByDateRange(shopId, startTime, endTime)
        val customerPaymentsFlow = customerPaymentDao.getPaymentsByDateRange(shopId, startTime, endTime)
        val writeOffsFlow = customerWriteOffDao.getWriteOffsByDateRange(shopId, startTime, endTime)
        val expensesFlow = expenseDao.getExpensesByDateRange(shopId, startTime, endTime)
        val purchasesFlow = purchaseDao.getPurchasesByDateRange(shopId, startTime, endTime)
        val supplierPaymentsFlow = paymentDao.getPaymentsByDateRange(shopId, startTime, endTime)

        // All-time for total payable to suppliers
        val allPurchasesFlow = purchaseDao.getPurchasesByShop(shopId)
        val allSupplierPaymentsFlow = paymentDao.getPaymentsByShop(shopId)

        // All-time for total receivable from customers
        val allCustomersFlow = customerDao.getCustomersByShop(shopId)
        val allSalesFlow = saleDao.getSalesByShop(shopId)
        val allCustomerPaymentsFlow = customerPaymentDao.getPaymentsByShop(shopId)
        val allWriteOffsFlow = customerWriteOffDao.getWriteOffsByShop(shopId)

        val periodPart1 = combine(salesFlow, customerPaymentsFlow, writeOffsFlow) { s, cp, wo ->
            Triple(s, cp, wo)
        }
        val periodPart2 = combine(expensesFlow, purchasesFlow, supplierPaymentsFlow) { e, p, sp ->
            Triple(e, p, sp)
        }
        val allTimeSuppliers = combine(allPurchasesFlow, allSupplierPaymentsFlow) { p, sp ->
            Pair(p, sp)
        }
        val allTimeCustomers = combine(allCustomersFlow, allSalesFlow, allCustomerPaymentsFlow, allWriteOffsFlow) { c, s, cp, wo ->
            com.example.util.LedgerCalculator.calculateTotalReceivable(c, s, cp, wo)
        }

        return combine(
            periodPart1,
            periodPart2,
            allTimeSuppliers,
            allTimeCustomers
        ) { (sales, customerPayments, writeOffs), (expenses, purchases, supplierPayments), (allPurchases, allSupplierPayments), totalReceivable ->
            com.example.util.LedgerCalculator.calculateDashboardMetrics(
                salesInPeriod = sales,
                customerPaymentsInPeriod = customerPayments,
                writeOffsInPeriod = writeOffs,
                expensesInPeriod = expenses,
                purchasesInPeriod = purchases,
                supplierPaymentsInPeriod = supplierPayments,
                allShopPurchases = allPurchases,
                allShopSupplierPayments = allSupplierPayments,
                totalReceivableFromCustomers = totalReceivable
            )
        }
    }
}
