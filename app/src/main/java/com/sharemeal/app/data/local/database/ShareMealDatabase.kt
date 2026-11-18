package com.sharemeal.app.data.local.database

import android.database.Cursor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        DonorProfileEntity::class,
        NgoProfileEntity::class,
        FoodDonationEntity::class,
        DonationRequestEntity::class,
        PickupEntity::class,
        NotificationEntity::class,
        DonationHistoryEntity::class,
        PendingSyncEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class ShareMealDatabase : RoomDatabase() {

    abstract fun shareMealDao(): ShareMealDao

    companion object {
        const val DATABASE_NAME = "sharemeal.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "users",
                    "donor_profiles",
                    "ngo_profiles",
                    "food_donations",
                    "donation_requests",
                    "pickups",
                    "notifications",
                    "donation_history"
                ).forEach { table ->
                    addColumnIfMissing(db, table, "sync_server_id", "TEXT")
                    addColumnIfMissing(db, table, "sync_server_updated_at", "INTEGER")
                    addColumnIfMissing(
                        db,
                        table,
                        "sync_state",
                        "TEXT NOT NULL DEFAULT 'SYNCED'"
                    )
                    addColumnIfMissing(
                        db,
                        table,
                        "sync_is_dirty",
                        "INTEGER NOT NULL DEFAULT 0"
                    )
                    addColumnIfMissing(db, table, "sync_last_error", "TEXT")
                }

                addColumnIfMissing(
                    db,
                    "food_donations",
                    "is_draft",
                    "INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(
                    db,
                    "food_donations",
                    "updated_at",
                    "INTEGER NOT NULL DEFAULT 0"
                )

                addColumnIfMissing(
                    db,
                    "pending_sync",
                    "path",
                    "TEXT NOT NULL DEFAULT ''"
                )
                addColumnIfMissing(
                    db,
                    "pending_sync",
                    "method",
                    "TEXT NOT NULL DEFAULT 'POST'"
                )
                addColumnIfMissing(
                    db,
                    "pending_sync",
                    "payload_json",
                    "TEXT NOT NULL DEFAULT '{}'"
                )
                addColumnIfMissing(
                    db,
                    "pending_sync",
                    "attempt_count",
                    "INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfMissing(db, "pending_sync", "next_retry_at", "INTEGER")
                addColumnIfMissing(db, "pending_sync", "last_error", "TEXT")
                addColumnIfMissing(db, "pending_sync", "server_snapshot_json", "TEXT")
                addColumnIfMissing(
                    db,
                    "pending_sync",
                    "status",
                    "TEXT NOT NULL DEFAULT 'PENDING'"
                )
            }

            private fun addColumnIfMissing(
                db: SupportSQLiteDatabase,
                table: String,
                column: String,
                definition: String
            ) {
                if (!tableExists(db, table) || columnExists(db, table, column)) return
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            }

            private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
                val cursor = db.query(
                    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
                    arrayOf(table)
                )
                return cursor.use { it.count > 0 }
            }

            private fun columnExists(
                db: SupportSQLiteDatabase,
                table: String,
                column: String
            ): Boolean {
                val cursor: Cursor = db.query("PRAGMA table_info(`$table`)")
                return cursor.use {
                    val nameIndex = it.getColumnIndex("name")
                    if (nameIndex < 0) return false
                    while (it.moveToNext()) {
                        if (it.getString(nameIndex) == column) return true
                    }
                    false
                }
            }
        }
    }
}
