package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopId"])]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val name: String,
    val phone: String = "",
    val note: String = "",
    val creditLimit: Long? = null, // in paisa, nullable
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_payments",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["customerId"]),
        Index(value = ["date"])
    ]
)
data class CustomerPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val customerId: Long,
    val date: Long = System.currentTimeMillis(),
    val amount: Long, // in paisa
    val note: String = ""
)

@Entity(
    tableName = "customer_write_offs",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["customerId"]),
        Index(value = ["date"])
    ]
)
data class CustomerWriteOff(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val customerId: Long,
    val date: Long = System.currentTimeMillis(),
    val amount: Long, // in paisa (bad debt)
    val note: String = ""
)

@Entity(
    tableName = "suppliers",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopId"])]
)
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val name: String,
    val phone: String = "",
    val note: String = ""
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopId"]), Index(value = ["supplierId"])]
)
data class Purchase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val supplierId: Long,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val totalAmount: Long, // in paisa
    val paidAmount: Long = 0L // in paisa
)

@Entity(
    tableName = "supplier_payments",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Purchase::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["supplierId"]),
        Index(value = ["purchaseId"])
    ]
)
data class SupplierPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val supplierId: Long,
    val purchaseId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val amount: Long, // in paisa
    val note: String = ""
)

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["date"]),
        Index(value = ["customerId"])
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val date: Long = System.currentTimeMillis(),
    val amount: Long, // in paisa (full sale amount)
    val receivedAmount: Long = amount, // in paisa (amount received in cash/instant)
    val customerId: Long? = null, // nullable reference to Customer
    val note: String = ""
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shopId"]), Index(value = ["date"]), Index(value = ["category"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopId: Long,
    val date: Long = System.currentTimeMillis(),
    val category: String,
    val amount: Long, // in paisa
    val note: String = ""
)
