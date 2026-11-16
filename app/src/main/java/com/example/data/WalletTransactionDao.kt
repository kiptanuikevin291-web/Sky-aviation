package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletTransactionDao {
  @Insert
  suspend fun insertTransaction(tx: WalletTransactionEntity)

  @Query("SELECT * FROM wallet_transactions ORDER BY timestampMillis DESC LIMIT :limit")
  fun observeRecentTransactions(limit: Int = 40): Flow<List<WalletTransactionEntity>>

  @Query("DELETE FROM wallet_transactions")
  suspend fun clearAll()
}
