package com.stackpilotmax.rameshvegetableshop.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

object BillStatus {
    const val ACTIVE = "ACTIVE"
    const val VOIDED = "VOIDED"
}

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["normalizedName"]),
        Index(value = ["normalizedPhone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val openingDebt: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val normalizedName: String = "",
    @ColumnInfo(defaultValue = "''") val normalizedPhone: String = ""
)

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("customerId")]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val total: Double,
    val amountPaid: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val previousDebtSnapshot: Double = 0.0,
    @ColumnInfo(defaultValue = "'ACTIVE'") val status: String = BillStatus.ACTIVE,
    val voidedAt: Long? = null
)

@Entity(
    tableName = "bill_items",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("billId")]
)
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val vegetableName: String,
    val unit: String,
    val quantity: Double,
    val rate: Double,
    val amount: Double,
    @ColumnInfo(defaultValue = "0") val isDirectAmount: Boolean = false
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId"), Index("sourceBillId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val note: String = "Payment received",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val isReversed: Boolean = false,
    val reversedAt: Long? = null,
    @ColumnInfo(defaultValue = "NULL") val sourceBillId: Long? = null
)

@Entity(
    tableName = "bill_revisions",
    foreignKeys = [
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("billId")]
)
data class BillRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val revisionNumber: Int,
    val customerId: Long?,
    val customerName: String,
    val customerPhone: String,
    val total: Double,
    val amountPaid: Double,
    val previousDebtSnapshot: Double,
    val itemsSnapshot: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class BillWithItems(
    @Embedded val bill: BillEntity,
    @Relation(parentColumn = "id", entityColumn = "billId")
    val items: List<BillItemEntity>
)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE, createdAt")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE normalizedName = :normalizedName ORDER BY createdAt")
    suspend fun getByNormalizedName(normalizedName: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE normalizedPhone = :normalizedPhone AND normalizedPhone != '' ORDER BY createdAt")
    suspend fun getByNormalizedPhone(normalizedPhone: String): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET openingDebt = openingDebt + :amount WHERE id = :customerId")
    suspend fun addToOpeningDebt(customerId: Long, amount: Double)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteById(customerId: Long): Int
}

@Dao
interface BillDao {
    @Transaction
    @Query("SELECT * FROM bills ORDER BY updatedAt DESC")
    fun observeAllWithItems(): Flow<List<BillWithItems>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getWithItems(billId: Long): BillWithItems?

    @Query("SELECT * FROM bills WHERE updatedAt < :cutoff ORDER BY updatedAt ASC")
    suspend fun getBefore(cutoff: Long): List<BillEntity>

    @Insert
    suspend fun insertBill(bill: BillEntity): Long

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Insert
    suspend fun insertItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteItemsForBill(billId: Long)

    /** Never allow malformed legacy rows to subtract from a customer's debt. */
    @Query("SELECT COALESCE(SUM(CASE WHEN total > amountPaid THEN total - amountPaid ELSE 0 END), 0) FROM bills WHERE customerId = :customerId AND status = 'ACTIVE'")
    suspend fun outstandingBills(customerId: Long): Double

    @Query("UPDATE bills SET status = 'VOIDED', voidedAt = :voidedAt, updatedAt = :voidedAt WHERE id = :billId AND status = 'ACTIVE'")
    suspend fun voidBill(billId: Long, voidedAt: Long): Int

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteById(billId: Long): Int
}

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeForCustomer(customerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY createdAt DESC")
    suspend fun getForCustomer(customerId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE id = :paymentId LIMIT 1")
    suspend fun getById(paymentId: Long): PaymentEntity?

    @Query("SELECT * FROM payments WHERE sourceBillId = :billId AND isReversed = 0 ORDER BY createdAt DESC")
    suspend fun getActiveForBill(billId: Long): List<PaymentEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE customerId = :customerId AND isReversed = 0")
    suspend fun totalPaid(customerId: Long): Double

    @Query("UPDATE payments SET isReversed = 1, reversedAt = :reversedAt WHERE id = :paymentId AND isReversed = 0")
    suspend fun reverse(paymentId: Long, reversedAt: Long): Int
}

@Dao
interface BillRevisionDao {
    @Insert
    suspend fun insert(revision: BillRevisionEntity): Long

    @Query("SELECT COUNT(*) FROM bill_revisions WHERE billId = :billId")
    suspend fun countForBill(billId: Long): Int

    @Query("SELECT * FROM bill_revisions WHERE billId = :billId ORDER BY revisionNumber DESC")
    suspend fun getForBill(billId: Long): List<BillRevisionEntity>
}

@Database(
    entities = [
        CustomerEntity::class,
        BillEntity::class,
        BillItemEntity::class,
        PaymentEntity::class,
        BillRevisionEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun billDao(): BillDao
    abstract fun paymentDao(): PaymentDao
    abstract fun billRevisionDao(): BillRevisionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE customers ADD COLUMN normalizedPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE customers SET normalizedName = lower(trim(name))")
                db.execSQL(
                    "UPDATE customers SET normalizedPhone = replace(replace(replace(replace(replace(phone, ' ', ''), '-', ''), '(', ''), ')', ''), '+', '')"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_normalizedName ON customers(normalizedName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_normalizedPhone ON customers(normalizedPhone)")

                db.execSQL("ALTER TABLE bills ADD COLUMN previousDebtSnapshot REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bills ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE bills ADD COLUMN voidedAt INTEGER")

                db.execSQL("ALTER TABLE bill_items ADD COLUMN isDirectAmount INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE payments ADD COLUMN isReversed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payments ADD COLUMN reversedAt INTEGER")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bill_revisions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        billId INTEGER NOT NULL,
                        revisionNumber INTEGER NOT NULL,
                        customerId INTEGER,
                        customerName TEXT NOT NULL,
                        customerPhone TEXT NOT NULL,
                        total REAL NOT NULL,
                        amountPaid REAL NOT NULL,
                        previousDebtSnapshot REAL NOT NULL,
                        itemsSnapshot TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(billId) REFERENCES bills(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_revisions_billId ON bill_revisions(billId)")

                db.execSQL(
                    """
                    UPDATE bills
                    SET previousDebtSnapshot =
                        COALESCE((SELECT openingDebt FROM customers WHERE id = bills.customerId), 0)
                        + COALESCE((
                            SELECT SUM(previous.total - previous.amountPaid)
                            FROM bills AS previous
                            WHERE previous.customerId = bills.customerId
                              AND (
                                previous.createdAt < bills.createdAt
                                OR (previous.createdAt = bills.createdAt AND previous.id < bills.id)
                              )
                        ), 0)
                        - COALESCE((
                            SELECT SUM(payments.amount)
                            FROM payments
                            WHERE payments.customerId = bills.customerId
                              AND payments.createdAt < bills.createdAt
                        ), 0)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payments ADD COLUMN sourceBillId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payments_sourceBillId ON payments(sourceBillId)")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "sabzibill.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}
