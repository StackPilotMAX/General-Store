package com.stackpilotmax.rameshvegetableshop.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class BackupManager(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)
    private val preferences = appContext.getSharedPreferences("sabzibill_settings", Context.MODE_PRIVATE)

    suspend fun exportJson(): String = database.withTransaction {
        val writable = database.openHelper.writableDatabase
        val data = JSONObject().apply {
            put("customers", exportTable(writable.query("SELECT * FROM customers ORDER BY id")))
            put("bills", exportTable(writable.query("SELECT * FROM bills ORDER BY id")))
            put("bill_items", exportTable(writable.query("SELECT * FROM bill_items ORDER BY id")))
            put("payments", exportTable(writable.query("SELECT * FROM payments ORDER BY id")))
            put("bill_revisions", exportTable(writable.query("SELECT * FROM bill_revisions ORDER BY id")))
            put(
                "settings",
                JSONObject().apply {
                    put("vendor_name", preferences.getString("vendor_name", "Ramesh Vegetable Shop"))
                    put("upi_id", preferences.getString("upi_id", ""))
                    put("motion_enabled", preferences.getBoolean("motion_enabled", true))
                    put("active_bill_count_visible", preferences.getBoolean("active_bill_count_visible", true))
                    put("festival_theme", preferences.getString("festival_theme", "diwali"))
                }
            )
        }
        val payload = data.toString()
        JSONObject().apply {
            put("format", "SabziBillBackup")
            put("formatVersion", 1)
            put("databaseVersion", 3)
            put("createdAt", System.currentTimeMillis())
            put("checksumSha256", sha256(payload))
            put("data", data)
        }.toString(2)
    }

    suspend fun importJson(raw: String): BackupSummary {
        val root = JSONObject(raw)
        require(root.optString("format") == "SabziBillBackup") {
            "Ye SabziBill backup file nahi hai"
        }
        require(root.optInt("formatVersion") == 1) {
            "Backup version supported nahi hai"
        }
        require(root.optInt("databaseVersion") in 1..3) {
            "Backup newer SabziBill version se bana hai; app update kijiye"
        }

        val data = root.getJSONObject("data")
        require(root.optString("checksumSha256") == sha256(data.toString())) {
            "Backup file corrupt ya modify hui hai"
        }

        val customers = data.getJSONArray("customers")
        val bills = data.getJSONArray("bills")
        val items = data.getJSONArray("bill_items")
        val payments = data.getJSONArray("payments")
        val revisions = data.optJSONArray("bill_revisions") ?: JSONArray()
        validateCounts(customers, bills, items, payments, revisions)

        database.withTransaction {
            val writable = database.openHelper.writableDatabase
            writable.execSQL("DELETE FROM bill_revisions")
            writable.execSQL("DELETE FROM bill_items")
            writable.execSQL("DELETE FROM payments")
            writable.execSQL("DELETE FROM bills")
            writable.execSQL("DELETE FROM customers")

            insertRows(writable, "customers", customers)
            insertRows(writable, "bills", bills)
            insertRows(writable, "bill_items", items)
            insertRows(writable, "payments", payments)
            insertRows(writable, "bill_revisions", revisions)
        }

        data.optJSONObject("settings")?.let { settings ->
            preferences.edit()
                .putString("vendor_name", settings.optString("vendor_name", "Ramesh Vegetable Shop"))
                .putString("upi_id", settings.optString("upi_id", ""))
                .putBoolean("motion_enabled", settings.optBoolean("motion_enabled", true))
                .putBoolean("active_bill_count_visible", settings.optBoolean("active_bill_count_visible", true))
                .putString("festival_theme", settings.optString("festival_theme", "diwali"))
                .apply()
        }

        return BackupSummary(
            customers = customers.length(),
            bills = bills.length(),
            items = items.length(),
            payments = payments.length(),
            revisions = revisions.length()
        )
    }

    private fun validateCounts(vararg arrays: JSONArray) {
        val total = arrays.sumOf { it.length() }
        require(total <= 250_000) { "Backup file bahut badi ya invalid hai" }
    }

    private fun exportTable(cursor: Cursor): JSONArray = cursor.use { rows ->
        val result = JSONArray()
        while (rows.moveToNext()) {
            val row = JSONObject()
            for (index in 0 until rows.columnCount) {
                val name = rows.getColumnName(index)
                when (rows.getType(index)) {
                    Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                    Cursor.FIELD_TYPE_INTEGER -> row.put(name, rows.getLong(index))
                    Cursor.FIELD_TYPE_FLOAT -> row.put(name, rows.getDouble(index))
                    Cursor.FIELD_TYPE_STRING -> row.put(name, rows.getString(index))
                    Cursor.FIELD_TYPE_BLOB -> error("Unexpected binary data in $name")
                }
            }
            result.put(row)
        }
        result
    }

    private fun insertRows(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        table: String,
        rows: JSONArray
    ) {
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val values = ContentValues()
            val keys = row.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = row.opt(key)
                when (value) {
                    null, JSONObject.NULL -> values.putNull(key)
                    is Boolean -> values.put(key, if (value) 1 else 0)
                    is Int -> values.put(key, value)
                    is Long -> values.put(key, value)
                    is Double -> values.put(key, value)
                    is Float -> values.put(key, value)
                    is Number -> values.put(key, value.toDouble())
                    else -> values.put(key, value.toString())
                }
            }
            val inserted = database.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
            check(inserted != -1L) { "Backup restore failed at $table row ${index + 1}" }
        }
    }

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

data class BackupSummary(
    val customers: Int,
    val bills: Int,
    val items: Int,
    val payments: Int,
    val revisions: Int
)
