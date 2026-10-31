package com.lapaola.facturas

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Long
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val clientName: String,
    val phone: String,
    val address: String,
    val total: Long
)

@Entity(
    tableName = "invoice_items",
    indices = [Index("invoiceId")],
    foreignKeys = [ForeignKey(
        entity = Invoice::class,
        parentColumns = ["id"],
        childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val name: String,
    val qty: Int,
    val unitPrice: Long
)

@Dao
abstract class AppDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    abstract fun products(): Flow<List<Product>>

    @Insert
    abstract suspend fun insertProduct(p: Product)

    @Query("DELETE FROM products WHERE id = :id")
    abstract suspend fun deleteProduct(id: Long)

    @Query("SELECT * FROM invoices ORDER BY id DESC")
    abstract fun invoices(): Flow<List<Invoice>>

    /** Clientes anteriores (para autocompletar), el más reciente primero. */
    @Query("SELECT * FROM invoices GROUP BY clientName ORDER BY MAX(dateMillis) DESC")
    abstract fun clients(): Flow<List<Invoice>>

    @Insert
    abstract suspend fun insertInvoice(i: Invoice): Long

    @Insert
    abstract suspend fun insertItems(items: List<InvoiceItem>)

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id")
    abstract suspend fun itemsOf(id: Long): List<InvoiceItem>

    @Query("DELETE FROM invoices WHERE id = :id")
    abstract suspend fun deleteInvoice(id: Long)

    @Transaction
    open suspend fun saveInvoice(inv: Invoice, items: List<InvoiceItem>): Long {
        val id = insertInvoice(inv)
        insertItems(items.map { it.copy(invoiceId = id) })
        return id
    }
}

@Database(
    entities = [Product::class, Invoice::class, InvoiceItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun build(ctx: Context): AppDb =
            Room.databaseBuilder(ctx, AppDb::class.java, "lapaola.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        listOf(
                            "Pan de Piña" to 2500, "Banano Maduro" to 600,
                            "Pan Tajado" to 3200, "Pan Integral" to 3000, "Croissant" to 4500
                        ).forEach { (n, p) ->
                            db.execSQL("INSERT INTO products (name, price) VALUES (?, ?)", arrayOf<Any>(n, p))
                        }
                    }
                })
                .build()
    }
}

class Settings(ctx: Context) {
    private val sp = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    var printerIp: String
        get() = sp.getString("ip", "") ?: ""
        set(v) { sp.edit().putString("ip", v).apply() }

    var paperMm: Int
        get() = sp.getInt("paper", 80)
        set(v) { sp.edit().putInt("paper", v).apply() }

    var noAccents: Boolean
        get() = sp.getBoolean("noacc", false)
        set(v) { sp.edit().putBoolean("noacc", v).apply() }
}
