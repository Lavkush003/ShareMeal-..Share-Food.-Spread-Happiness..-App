package com.sharemeal.app.data.local.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareMealMigrationTest {

    @Test
    fun migrationOneToTwoPreservesRowsAndAddsSyncColumnsIdempotently() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "sharemeal_migration_test_${System.currentTimeMillis()}.db"
        context.deleteDatabase(databaseName)

        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE users (" +
                            "id TEXT PRIMARY KEY NOT NULL, " +
                            "email TEXT NOT NULL, " +
                            "mobile TEXT NOT NULL, " +
                            "role TEXT NOT NULL, " +
                            "display_name TEXT NOT NULL, " +
                            "created_at INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE pending_sync (" +
                            "operation_id TEXT PRIMARY KEY NOT NULL, " +
                            "idempotency_key TEXT NOT NULL, " +
                            "entity_type TEXT NOT NULL, " +
                            "entity_id TEXT NOT NULL, " +
                            "created_at INTEGER NOT NULL)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 1 && newVersion == 2) {
                        ShareMealDatabase.MIGRATION_1_2.migrate(db)
                    }
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            val db = helper.writableDatabase
            db.execSQL(
                "INSERT INTO users (id, email, mobile, role, display_name, created_at) " +
                    "VALUES ('donor-1', 'donor@test.local', '0000000000', 'DONOR', 'Donor', 1)"
            )
            db.execSQL(
                "INSERT INTO pending_sync " +
                    "(operation_id, idempotency_key, entity_type, entity_id, created_at) " +
                    "VALUES ('op-1', 'key-1', 'donation', 'donation-1', 1)"
            )

            ShareMealDatabase.MIGRATION_1_2.migrate(db)
            ShareMealDatabase.MIGRATION_1_2.migrate(db)

            val userColumns = columnNames(db, "users")
            assertTrue(userColumns.contains("sync_server_id"))
            assertTrue(userColumns.contains("sync_server_updated_at"))
            assertTrue(userColumns.contains("sync_state"))
            assertTrue(userColumns.contains("sync_is_dirty"))
            assertTrue(userColumns.contains("sync_last_error"))

            val pendingColumns = columnNames(db, "pending_sync")
            assertTrue(pendingColumns.contains("path"))
            assertTrue(pendingColumns.contains("method"))
            assertTrue(pendingColumns.contains("payload_json"))
            assertTrue(pendingColumns.contains("attempt_count"))
            assertTrue(pendingColumns.contains("next_retry_at"))
            assertTrue(pendingColumns.contains("last_error"))
            assertTrue(pendingColumns.contains("server_snapshot_json"))
            assertTrue(pendingColumns.contains("status"))

            db.query("SELECT email, sync_state, sync_is_dirty FROM users WHERE id = 'donor-1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(
                        "donor@test.local",
                        cursor.getString(cursor.getColumnIndexOrThrow("email"))
                    )
                    assertEquals(
                        "SYNCED",
                        cursor.getString(cursor.getColumnIndexOrThrow("sync_state"))
                    )
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("sync_is_dirty")))
                }

            db.query("SELECT status, attempt_count FROM pending_sync WHERE operation_id = 'op-1'")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("PENDING", cursor.getString(cursor.getColumnIndexOrThrow("status")))
                    assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("attempt_count")))
                }
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun columnNames(db: SupportSQLiteDatabase, table: String): Set<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val names = linkedSetOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                names.add(cursor.getString(nameIndex))
            }
            names
        }
}
