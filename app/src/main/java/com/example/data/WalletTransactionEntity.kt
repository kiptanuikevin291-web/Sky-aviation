package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val type: String, // "DEPOSIT" or "WITHDRAW"
  val amount: Double,
  val method: String,
  val accountRef: String,
  val referenceCode: String,
  val status: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)
