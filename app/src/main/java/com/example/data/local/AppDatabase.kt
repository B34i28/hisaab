package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.CustomerPaymentDao
import com.example.data.local.dao.CustomerWriteOffDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.PurchaseDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.dao.ShopDao
import com.example.data.local.dao.SupplierDao
import com.example.data.local.dao.SupplierPaymentDao
import com.example.data.local.entity.Customer
import com.example.data.local.entity.CustomerPayment
import com.example.data.local.entity.CustomerWriteOff
import com.example.data.local.entity.Expense
import com.example.data.local.entity.Purchase
import com.example.data.local.entity.Sale
import com.example.data.local.entity.Shop
import com.example.data.local.entity.Supplier
import com.example.data.local.entity.SupplierPayment

@Database(
    entities = [
        Shop::class,
        Customer::class,
        CustomerPayment::class,
        CustomerWriteOff::class,
        Supplier::class,
        Purchase::class,
        SupplierPayment::class,
        Sale::class,
        Expense::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shopDao(): ShopDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun customerWriteOffDao(): CustomerWriteOffDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun supplierPaymentDao(): SupplierPaymentDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create customers table first so sales FK can reference it
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL DEFAULT '',
                        note TEXT NOT NULL DEFAULT '',
                        creditLimit INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(shopId) REFERENCES shops(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_shopId ON customers(shopId)")

                // 2. Create customer_payments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customer_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopId INTEGER NOT NULL,
                        customerId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        amount INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(shopId) REFERENCES shops(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_shopId ON customer_payments(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_customerId ON customer_payments(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_payments_date ON customer_payments(date)")

                // 3. Create customer_write_offs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customer_write_offs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopId INTEGER NOT NULL,
                        customerId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        amount INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(shopId) REFERENCES shops(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_write_offs_shopId ON customer_write_offs(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_write_offs_customerId ON customer_write_offs(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_write_offs_date ON customer_write_offs(date)")

                // 4. Recreate sales table with new receivedAmount, customerId columns and foreign keys
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sales_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        shopId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        amount INTEGER NOT NULL,
                        receivedAmount INTEGER NOT NULL,
                        customerId INTEGER,
                        note TEXT NOT NULL,
                        FOREIGN KEY(shopId) REFERENCES shops(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                // Copy existing sales data into sales_new, setting receivedAmount = amount for backward compatibility
                db.execSQL(
                    """
                    INSERT INTO sales_new (id, shopId, date, amount, receivedAmount, customerId, note)
                    SELECT id, shopId, date, amount, amount, NULL, note FROM sales
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE sales")
                db.execSQL("ALTER TABLE sales_new RENAME TO sales")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_shopId ON sales(shopId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_date ON sales(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_customerId ON sales(customerId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shop_ledger.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
