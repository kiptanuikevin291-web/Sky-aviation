package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_rounds")
data class GameRoundEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val gameType: String,
  val wager: Double,
  val multiplier: Double,
  val payout: Double,
  val netProfit: Double,
  val detailSummary: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)
