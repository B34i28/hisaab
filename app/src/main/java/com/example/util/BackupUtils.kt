package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Customer
import com.example.data.local.entity.CustomerPayment
import com.example.data.local.entity.CustomerWriteOff
import com.example.data.local.entity.Expense
import com.example.data.local.entity.Purchase
import com.example.data.local.entity.Sale
import com.example.data.local.entity.Shop
import com.example.data.local.entity.Supplier
import com.example.data.local.entity.SupplierPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupStats(
    val exportDate: Long,
    val appVersion: String,
    val shopsCount: Int,
    val suppliersCount: Int,
    val purchasesCount: Int,
    val paymentsCount: Int,
    val salesCount: Int,
    val expensesCount: Int,
    val customersCount: Int = 0,
    val customerPaymentsCount: Int = 0,
    val customerWriteOffsCount: Int = 0
)

data class BackupData(
    val exportDate: Long,
    val appVersion: String,
    val shops: List<Shop>,
    val suppliers: List<Supplier>,
    val purchases: List<Purchase>,
    val payments: List<SupplierPayment>,
    val sales: List<Sale>,
    val expenses: List<Expense>,
    val customers: List<Customer> = emptyList(),
    val customerPayments: List<CustomerPayment> = emptyList(),
    val customerWriteOffs: List<CustomerWriteOff> = emptyList()
)

object BackupUtils {

    /**
     * Extracts all records across all tables into a structured JSON string.
     */
    suspend fun createBackupJson(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val shops = database.shopDao().getAllShopsList()
        val suppliers = database.supplierDao().getAllSuppliersList()
        val purchases = database.purchaseDao().getAllPurchasesList()
        val payments = database.supplierPaymentDao().getAllPaymentsList()
        val sales = database.saleDao().getAllSalesList()
        val expenses = database.expenseDao().getAllExpensesList()
        val customers = database.customerDao().getAllCustomersList()
        val customerPayments = database.customerPaymentDao().getAllPaymentsList()
        val customerWriteOffs = database.customerWriteOffDao().getAllWriteOffsList()

        val root = JSONObject().apply {
            put("appName", "ShopLedger")
            put("version", 2)
            put("exportTimestamp", System.currentTimeMillis())

            // Shops
            val shopsArray = JSONArray()
            shops.forEach { shop ->
                shopsArray.put(JSONObject().apply {
                    put("id", shop.id)
                    put("name", shop.name)
                    put("createdAt", shop.createdAt)
                })
            }
            put("shops", shopsArray)

            // Customers
            val customersArray = JSONArray()
            customers.forEach { customer ->
                customersArray.put(JSONObject().apply {
                    put("id", customer.id)
                    put("shopId", customer.shopId)
                    put("name", customer.name)
                    put("phone", customer.phone)
                    put("note", customer.note)
                    if (customer.creditLimit != null) {
                        put("creditLimit", customer.creditLimit)
                    }
                    put("createdAt", customer.createdAt)
                })
            }
            put("customers", customersArray)

            // Customer Payments
            val customerPaymentsArray = JSONArray()
            customerPayments.forEach { payment ->
                customerPaymentsArray.put(JSONObject().apply {
                    put("id", payment.id)
                    put("shopId", payment.shopId)
                    put("customerId", payment.customerId)
                    put("date", payment.date)
                    put("amount", payment.amount)
                    put("note", payment.note)
                })
            }
            put("customerPayments", customerPaymentsArray)

            // Customer Write Offs
            val writeOffsArray = JSONArray()
            customerWriteOffs.forEach { writeOff ->
                writeOffsArray.put(JSONObject().apply {
                    put("id", writeOff.id)
                    put("shopId", writeOff.shopId)
                    put("customerId", writeOff.customerId)
                    put("date", writeOff.date)
                    put("amount", writeOff.amount)
                    put("note", writeOff.note)
                })
            }
            put("customerWriteOffs", writeOffsArray)

            // Suppliers
            val suppliersArray = JSONArray()
            suppliers.forEach { supplier ->
                suppliersArray.put(JSONObject().apply {
                    put("id", supplier.id)
                    put("shopId", supplier.shopId)
                    put("name", supplier.name)
                    put("phone", supplier.phone)
                    put("note", supplier.note)
                })
            }
            put("suppliers", suppliersArray)

            // Purchases
            val purchasesArray = JSONArray()
            purchases.forEach { purchase ->
                purchasesArray.put(JSONObject().apply {
                    put("id", purchase.id)
                    put("shopId", purchase.shopId)
                    put("supplierId", purchase.supplierId)
                    put("date", purchase.date)
                    put("description", purchase.description)
                    put("totalAmount", purchase.totalAmount)
                    put("paidAmount", purchase.paidAmount)
                })
            }
            put("purchases", purchasesArray)

            // Supplier Payments
            val paymentsArray = JSONArray()
            payments.forEach { payment ->
                paymentsArray.put(JSONObject().apply {
                    put("id", payment.id)
                    put("shopId", payment.shopId)
                    put("supplierId", payment.supplierId)
                    if (payment.purchaseId != null) {
                        put("purchaseId", payment.purchaseId)
                    }
                    put("date", payment.date)
                    put("amount", payment.amount)
                    put("note", payment.note)
                })
            }
            put("supplierPayments", paymentsArray)

            // Sales (including customerId and receivedAmount)
            val salesArray = JSONArray()
            sales.forEach { sale ->
                salesArray.put(JSONObject().apply {
                    put("id", sale.id)
                    put("shopId", sale.shopId)
                    put("date", sale.date)
                    put("amount", sale.amount)
                    put("receivedAmount", sale.receivedAmount)
                    if (sale.customerId != null) {
                        put("customerId", sale.customerId)
                    }
                    put("note", sale.note)
                })
            }
            put("sales", salesArray)

            // Expenses
            val expensesArray = JSONArray()
            expenses.forEach { expense ->
                expensesArray.put(JSONObject().apply {
                    put("id", expense.id)
                    put("shopId", expense.shopId)
                    put("date", expense.date)
                    put("category", expense.category)
                    put("amount", expense.amount)
                    put("note", expense.note)
                })
            }
            put("expenses", expensesArray)
        }

        root.toString(2)
    }

    /**
     * Parses and validates a JSON string into a structured BackupData object.
     */
    fun parseBackupJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        val appName = root.optString("appName", "")
        if (appName.isNotEmpty() && appName != "ShopLedger") {
            throw IllegalArgumentException("Invalid backup file: not a ShopLedger backup.")
        }

        val exportDate = root.optLong("exportTimestamp", System.currentTimeMillis())
        val version = root.optInt("version", 1).toString()

        val shops = mutableListOf<Shop>()
        val shopsArray = root.optJSONArray("shops") ?: JSONArray()
        for (i in 0 until shopsArray.length()) {
            val obj = shopsArray.getJSONObject(i)
            shops.add(
                Shop(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", "Restored Shop"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val customers = mutableListOf<Customer>()
        val customersArray = root.optJSONArray("customers") ?: JSONArray()
        for (i in 0 until customersArray.length()) {
            val obj = customersArray.getJSONObject(i)
            val creditLimit = if (obj.has("creditLimit") && !obj.isNull("creditLimit")) obj.getLong("creditLimit") else null
            customers.add(
                Customer(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    name = obj.optString("name", ""),
                    phone = obj.optString("phone", ""),
                    note = obj.optString("note", ""),
                    creditLimit = creditLimit,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val customerPayments = mutableListOf<CustomerPayment>()
        val custPaymentsArray = root.optJSONArray("customerPayments") ?: JSONArray()
        for (i in 0 until custPaymentsArray.length()) {
            val obj = custPaymentsArray.getJSONObject(i)
            customerPayments.add(
                CustomerPayment(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    customerId = obj.optLong("customerId", 0L),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    amount = obj.optLong("amount", 0L),
                    note = obj.optString("note", "")
                )
            )
        }

        val customerWriteOffs = mutableListOf<CustomerWriteOff>()
        val writeOffsArray = root.optJSONArray("customerWriteOffs") ?: JSONArray()
        for (i in 0 until writeOffsArray.length()) {
            val obj = writeOffsArray.getJSONObject(i)
            customerWriteOffs.add(
                CustomerWriteOff(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    customerId = obj.optLong("customerId", 0L),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    amount = obj.optLong("amount", 0L),
                    note = obj.optString("note", "")
                )
            )
        }

        val suppliers = mutableListOf<Supplier>()
        val suppliersArray = root.optJSONArray("suppliers") ?: JSONArray()
        for (i in 0 until suppliersArray.length()) {
            val obj = suppliersArray.getJSONObject(i)
            suppliers.add(
                Supplier(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    name = obj.optString("name", ""),
                    phone = obj.optString("phone", ""),
                    note = obj.optString("note", "")
                )
            )
        }

        val purchases = mutableListOf<Purchase>()
        val purchasesArray = root.optJSONArray("purchases") ?: JSONArray()
        for (i in 0 until purchasesArray.length()) {
            val obj = purchasesArray.getJSONObject(i)
            purchases.add(
                Purchase(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    supplierId = obj.optLong("supplierId", 0L),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    description = obj.optString("description", ""),
                    totalAmount = obj.optLong("totalAmount", 0L),
                    paidAmount = obj.optLong("paidAmount", 0L)
                )
            )
        }

        val payments = mutableListOf<SupplierPayment>()
        val paymentsArray = root.optJSONArray("supplierPayments") ?: JSONArray()
        for (i in 0 until paymentsArray.length()) {
            val obj = paymentsArray.getJSONObject(i)
            val purchaseId = if (obj.has("purchaseId") && !obj.isNull("purchaseId")) obj.getLong("purchaseId") else null
            payments.add(
                SupplierPayment(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    supplierId = obj.optLong("supplierId", 0L),
                    purchaseId = purchaseId,
                    date = obj.optLong("date", System.currentTimeMillis()),
                    amount = obj.optLong("amount", 0L),
                    note = obj.optString("note", "")
                )
            )
        }

        val sales = mutableListOf<Sale>()
        val salesArray = root.optJSONArray("sales") ?: JSONArray()
        for (i in 0 until salesArray.length()) {
            val obj = salesArray.getJSONObject(i)
            val totalAmt = obj.optLong("amount", 0L)
            // Backward-compatibility: if receivedAmount is not in older backup, default to amount (100% cash)
            val receivedAmt = if (obj.has("receivedAmount") && !obj.isNull("receivedAmount")) {
                obj.getLong("receivedAmount")
            } else {
                totalAmt
            }
            val customerId = if (obj.has("customerId") && !obj.isNull("customerId")) obj.getLong("customerId") else null

            sales.add(
                Sale(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    customerId = customerId,
                    date = obj.optLong("date", System.currentTimeMillis()),
                    amount = totalAmt,
                    receivedAmount = receivedAmt,
                    note = obj.optString("note", "")
                )
            )
        }

        val expenses = mutableListOf<Expense>()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)
            expenses.add(
                Expense(
                    id = obj.optLong("id", 0L),
                    shopId = obj.optLong("shopId", 0L),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    category = obj.optString("category", "Other"),
                    amount = obj.optLong("amount", 0L),
                    note = obj.optString("note", "")
                )
            )
        }

        return BackupData(
            exportDate = exportDate,
            appVersion = version,
            shops = shops,
            suppliers = suppliers,
            purchases = purchases,
            payments = payments,
            sales = sales,
            expenses = expenses,
            customers = customers,
            customerPayments = customerPayments,
            customerWriteOffs = customerWriteOffs
        )
    }

    /**
     * Executes the restoration into the Room database.
     */
    suspend fun restoreDatabase(
        database: AppDatabase,
        backup: BackupData
    ): BackupStats = withContext(Dispatchers.IO) {
        // Clear all tables in foreign-key reverse dependency order
        database.customerWriteOffDao().deleteAll()
        database.customerPaymentDao().deleteAll()
        database.supplierPaymentDao().deleteAll()
        database.purchaseDao().deleteAll()
        database.saleDao().deleteAll()
        database.customerDao().deleteAll()
        database.supplierDao().deleteAll()
        database.expenseDao().deleteAll()
        database.shopDao().deleteAll()

        // Insert restored records in foreign-key dependency order
        database.shopDao().insertAll(backup.shops)
        database.supplierDao().insertAll(backup.suppliers)
        database.customerDao().insertAll(backup.customers)
        database.purchaseDao().insertAll(backup.purchases)
        database.supplierPaymentDao().insertAll(backup.payments)
        database.saleDao().insertAll(backup.sales)
        database.customerPaymentDao().insertAll(backup.customerPayments)
        database.customerWriteOffDao().insertAll(backup.customerWriteOffs)
        database.expenseDao().insertAll(backup.expenses)

        BackupStats(
            exportDate = backup.exportDate,
            appVersion = backup.appVersion,
            shopsCount = backup.shops.size,
            suppliersCount = backup.suppliers.size,
            purchasesCount = backup.purchases.size,
            paymentsCount = backup.payments.size,
            salesCount = backup.sales.size,
            expensesCount = backup.expenses.size,
            customersCount = backup.customers.size,
            customerPaymentsCount = backup.customerPayments.size,
            customerWriteOffsCount = backup.customerWriteOffs.size
        )
    }

    /**
     * Saves JSON to a cache file and opens Android share sheet.
     */
    fun shareBackup(context: Context, jsonString: String) {
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(backupDir, "ShopLedger_Backup_$dateStamp.json")

        backupFile.writeText(jsonString)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "ShopLedger Backup - $dateStamp")
            putExtra(Intent.EXTRA_TEXT, "Here is your ShopLedger database backup. Keep it safe to restore your shops, ledgers, and accounts.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share or Save ShopLedger Backup"))
    }

    /**
     * Writes JSON content to a destination Uri (e.g. from CreateDocument contract).
     */
    fun writeToUri(context: Context, uri: Uri, jsonString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads JSON content from a source Uri (e.g. from OpenDocument contract).
     */
    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
