package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops ORDER BY createdAt ASC")
    fun getAllShops(): Flow<List<Shop>>

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    fun getShopById(id: Long): Flow<Shop?>

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    suspend fun getShopByIdOnce(id: Long): Shop?

    @Query("SELECT COUNT(*) FROM shops")
    suspend fun getShopCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shop: Shop): Long

    @Update
    suspend fun update(shop: Shop)

    @Delete
    suspend fun delete(shop: Shop)

    @Query("DELETE FROM shops WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM shops ORDER BY id ASC")
    suspend fun getAllShopsList(): List<Shop>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shops: List<Shop>)

    @Query("DELETE FROM shops")
    suspend fun deleteAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE shopId = :shopId ORDER BY createdAt DESC, id DESC")
    fun getCustomersByShop(shopId: Long): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE shopId = :shopId AND id = :id LIMIT 1")
    fun getCustomerById(shopId: Long, id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE shopId = :shopId AND id = :id LIMIT 1")
    suspend fun getCustomerByIdOnce(shopId: Long, id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("DELETE FROM customers WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersList(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}

@Dao
interface CustomerPaymentDao {
    @Query("SELECT * FROM customer_payments WHERE shopId = :shopId ORDER BY date DESC, id DESC")
    fun getPaymentsByShop(shopId: Long): Flow<List<CustomerPayment>>

    @Query("SELECT * FROM customer_payments WHERE shopId = :shopId AND customerId = :customerId ORDER BY date DESC, id DESC")
    fun getPaymentsByCustomer(shopId: Long, customerId: Long): Flow<List<CustomerPayment>>

    @Query("SELECT * FROM customer_payments WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getPaymentsByDateRange(shopId: Long, startDate: Long, endDate: Long): Flow<List<CustomerPayment>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM customer_payments WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate")
    fun getTotalPayments(shopId: Long, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM customer_payments WHERE shopId = :shopId")
    fun getAllTimeTotalPayments(shopId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: CustomerPayment): Long

    @Update
    suspend fun update(payment: CustomerPayment)

    @Delete
    suspend fun delete(payment: CustomerPayment)

    @Query("DELETE FROM customer_payments WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Query("DELETE FROM customer_payments WHERE shopId = :shopId AND customerId = :customerId")
    suspend fun deleteByCustomerId(shopId: Long, customerId: Long)

    @Query("SELECT * FROM customer_payments ORDER BY id ASC")
    suspend fun getAllPaymentsList(): List<CustomerPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<CustomerPayment>)

    @Query("DELETE FROM customer_payments")
    suspend fun deleteAll()
}

@Dao
interface CustomerWriteOffDao {
    @Query("SELECT * FROM customer_write_offs WHERE shopId = :shopId ORDER BY date DESC, id DESC")
    fun getWriteOffsByShop(shopId: Long): Flow<List<CustomerWriteOff>>

    @Query("SELECT * FROM customer_write_offs WHERE shopId = :shopId AND customerId = :customerId ORDER BY date DESC, id DESC")
    fun getWriteOffsByCustomer(shopId: Long, customerId: Long): Flow<List<CustomerWriteOff>>

    @Query("SELECT * FROM customer_write_offs WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getWriteOffsByDateRange(shopId: Long, startDate: Long, endDate: Long): Flow<List<CustomerWriteOff>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM customer_write_offs WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate")
    fun getTotalWriteOffs(shopId: Long, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM customer_write_offs WHERE shopId = :shopId")
    fun getAllTimeTotalWriteOffs(shopId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(writeOff: CustomerWriteOff): Long

    @Update
    suspend fun update(writeOff: CustomerWriteOff)

    @Delete
    suspend fun delete(writeOff: CustomerWriteOff)

    @Query("DELETE FROM customer_write_offs WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Query("DELETE FROM customer_write_offs WHERE shopId = :shopId AND customerId = :customerId")
    suspend fun deleteByCustomerId(shopId: Long, customerId: Long)

    @Query("SELECT * FROM customer_write_offs ORDER BY id ASC")
    suspend fun getAllWriteOffsList(): List<CustomerWriteOff>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(writeOffs: List<CustomerWriteOff>)

    @Query("DELETE FROM customer_write_offs")
    suspend fun deleteAll()
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE shopId = :shopId ORDER BY name COLLATE NOCASE ASC")
    fun getSuppliersByShop(shopId: Long): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE shopId = :shopId AND id = :id LIMIT 1")
    fun getSupplierById(shopId: Long, id: Long): Flow<Supplier?>

    @Query("SELECT * FROM suppliers WHERE shopId = :shopId AND id = :id LIMIT 1")
    suspend fun getSupplierByIdOnce(shopId: Long, id: Long): Supplier?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: Supplier): Long

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)

    @Query("SELECT * FROM suppliers ORDER BY id ASC")
    suspend fun getAllSuppliersList(): List<Supplier>

    @Query("DELETE FROM suppliers WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<Supplier>)

    @Query("DELETE FROM suppliers")
    suspend fun deleteAll()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE shopId = :shopId ORDER BY date DESC, id DESC")
    fun getPurchasesByShop(shopId: Long): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE shopId = :shopId AND supplierId = :supplierId ORDER BY date DESC, id DESC")
    fun getPurchasesBySupplier(shopId: Long, supplierId: Long): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getPurchasesByDateRange(shopId: Long, startDate: Long, endDate: Long): Flow<List<Purchase>>

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM purchases WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate")
    fun getTotalPurchases(shopId: Long, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(paidAmount), 0) FROM purchases WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate")
    fun getTotalPaidOnPurchases(shopId: Long, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM purchases WHERE shopId = :shopId")
    fun getAllTimeTotalPurchases(shopId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(paidAmount), 0) FROM purchases WHERE shopId = :shopId")
    fun getAllTimePaidOnPurchases(shopId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: Purchase): Long

    @Update
    suspend fun update(purchase: Purchase)

    @Delete
    suspend fun delete(purchase: Purchase)

    @Query("SELECT * FROM purchases ORDER BY id ASC")
    suspend fun getAllPurchasesList(): List<Purchase>

    @Query("DELETE FROM purchases WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(purchases: List<Purchase>)

    @Query("DELETE FROM purchases")
    suspend fun deleteAll()
}

@Dao
interface SupplierPaymentDao {
    @Query("SELECT * FROM supplier_payments WHERE shopId = :shopId ORDER BY date DESC, id DESC")
    fun getPaymentsByShop(shopId: Long): Flow<List<SupplierPayment>>

    @Query("SELECT * FROM supplier_payments WHERE shopId = :shopId AND supplierId = :supplierId ORDER BY date DESC, id DESC")
    fun getPaymentsBySupplier(shopId: Long, supplierId: Long): Flow<List<SupplierPayment>>

    @Query("SELECT * FROM supplier_payments WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getPaymentsByDateRange(shopId: Long, startDate: Long, endDate: Long): Flow<List<SupplierPayment>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM supplier_payments WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate")
    fun getTotalPayments(shopId: Long, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM supplier_payments WHERE shopId = :shopId")
    fun getAllTimeTotalPayments(shopId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: SupplierPayment): Long

    @Update
    suspend fun update(payment: SupplierPayment)

    @Delete
    suspend fun delete(payment: SupplierPayment)

    @Query("SELECT * FROM supplier_payments ORDER BY id ASC")
    suspend fun getAllPaymentsList(): List<SupplierPayment>

    @Query("DELETE FROM supplier_payments WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<SupplierPayment>)

    @Query("DELETE FROM supplier_payments")
    suspend fun deleteAll()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE shopId = :shopId ORDER BY date DESC, id DESC")
    fun getSalesByShop(shopId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getSalesByDateRange(shopId: Long, startDate: Long, endDate: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE shopId = :shopId AND customerId = :customerId ORDER BY date DESC, id DESC")
    fun getSalesByCustomer(shopId: Long, customerId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE shopId = :shopId AND customerId IS NOT NULL ORDER BY date DESC, id DESC")
    fun getCreditSalesByShop(shopId: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE shopId = :shopId AND id = :id LIMIT 1")
    suspend fun getSaleByIdOnce(shopId: Long, id: Long): Sale?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM sales WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate")
    fun getTotalSales(shopId: Long, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM sales WHERE shopId = :shopId")
    fun getAllTimeTotalSales(shopId: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: Sale): Long

    @Update
    suspend fun update(sale: Sale)

    @Delete
    suspend fun delete(sale: Sale)

    @Query("SELECT * FROM sales ORDER BY id ASC")
    suspend fun getAllSalesList(): List<Sale>

    @Query("DELETE FROM sales WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<Sale>)

    @Query("DELETE FROM sales")
    suspend fun deleteAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE shopId = :shopId ORDER BY date DESC, id DESC")
    fun getExpensesByShop(shopId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    fun getExpensesByDateRange(shopId: Long, startDate: Long, endDate: Long): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE shopId = :shopId AND date >= :startDate AND date <= :endDate")
    fun getTotalExpenses(shopId: Long, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE shopId = :shopId")
    fun getAllTimeTotalExpenses(shopId: Long): Flow<Long>

    @Query("SELECT DISTINCT category FROM expenses WHERE shopId = :shopId ORDER BY category ASC")
    fun getDistinctCategories(shopId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY id ASC")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("DELETE FROM expenses WHERE shopId = :shopId")
    suspend fun deleteByShopId(shopId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
