package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [GameRoundEntity::class, WalletTransactionEntity::class, PilotAccountEntity::class],
  version = 3,
  exportSchema = false,
)
abstract class SkyCrashDatabase : RoomDatabase() {
  abstract fun gameRoundDao(): GameRoundDao
  abstract fun walletTransactionDao(): WalletTransactionDao
  abstract fun pilotAccountDao(): PilotAccountDao

  companion object {
    @Volatile
    private var INSTANCE: SkyCrashDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `wallet_transactions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `type` TEXT NOT NULL,
            `amount` REAL NOT NULL,
            `method` TEXT NOT NULL,
            `accountRef` TEXT NOT NULL,
            `referenceCode` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `timestampMillis` INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `pilot_accounts` (
            `phoneNumber` TEXT NOT NULL PRIMARY KEY,
            `pilotName` TEXT NOT NULL,
            `passwordHash` TEXT NOT NULL,
            `salt` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    fun getInstance(context: Context): SkyCrashDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SkyCrashDatabase::class.java,
          "skycrash_arcade.db",
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
