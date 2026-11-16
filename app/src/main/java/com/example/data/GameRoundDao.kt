package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ArcadeStatsSummary(
  val totalRounds: Int,
  val totalWagered: Double,
  val totalPayout: Double,
  val bestMultiplier: Double,
  val bestPayout: Double,
)

@Dao
interface GameRoundDao {
  @Insert
  suspend fun insertRound(round: GameRoundEntity)

  @Query("SELECT * FROM game_rounds ORDER BY timestampMillis DESC LIMIT :limit")
  fun observeRecentRounds(limit: Int = 50): Flow<List<GameRoundEntity>>

  @Query("SELECT * FROM game_rounds WHERE gameType = :gameType ORDER BY timestampMillis DESC LIMIT :limit")
  fun observeRoundsByGame(gameType: String, limit: Int = 20): Flow<List<GameRoundEntity>>

  @Query("SELECT * FROM game_rounds WHERE payout > 0 ORDER BY payout DESC LIMIT :limit")
  fun observeTopWins(limit: Int = 10): Flow<List<GameRoundEntity>>

  @Query(
    """
    SELECT 
      COUNT(*) AS totalRounds,
      COALESCE(SUM(wager), 0.0) AS totalWagered,
      COALESCE(SUM(payout), 0.0) AS totalPayout,
      COALESCE(MAX(multiplier), 0.0) AS bestMultiplier,
      COALESCE(MAX(payout), 0.0) AS bestPayout
    FROM game_rounds
    """
  )
  fun observeStatsSummary(): Flow<ArcadeStatsSummary>

  @Query("DELETE FROM game_rounds")
  suspend fun clearAll()
}
