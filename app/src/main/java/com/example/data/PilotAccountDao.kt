package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PilotAccountDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertAccount(account: PilotAccountEntity)

  @Query("SELECT * FROM pilot_accounts WHERE phoneNumber = :phone LIMIT 1")
  suspend fun getByPhone(phone: String): PilotAccountEntity?

  @Query("SELECT * FROM pilot_accounts WHERE phoneNumber = :phone LIMIT 1")
  fun observeByPhone(phone: String): Flow<PilotAccountEntity?>

  @Query("UPDATE pilot_accounts SET passwordHash = :newHash, salt = :newSalt WHERE phoneNumber = :phone")
  suspend fun updatePassword(phone: String, newHash: String, newSalt: String)
}
