package com.example.ui.games.mines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ArcadeRepository
import com.example.data.GameType
import com.example.ui.components.Formatters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.random.Random

enum class MinesPhase {
  IDLE,
  PLAYING,
  BUSTED,
  CASHED_OUT,
}

data class MinesUiState(
  val phase: MinesPhase = MinesPhase.IDLE,
  val stake: Double = 25.0,
  val mineCount: Int = 3,
  val minePositions: Set<Int> = emptySet(),
  val revealedPositions: Set<Int> = emptySet(),
  val explodedTile: Int? = null,
  val currentMultiplier: Double = 1.0,
  val nextMultiplier: Double = 1.12,
  val lastPayout: Double = 0.0,
)

sealed class MinesEvent {
  data object GemFound : MinesEvent()
  data object MineHit : MinesEvent()
  data class CashedOut(val payout: Double, val multiplier: Double) : MinesEvent()
  data class Info(val message: String) : MinesEvent()
}

class MinesViewModel(private val repository: ArcadeRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(
    MinesUiState(nextMultiplier = computeMultiplier(revealed = 1, mines = 3))
  )
  val uiState: StateFlow<MinesUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<MinesEvent>(extraBufferCapacity = 8)
  val events: SharedFlow<MinesEvent> = _events.asSharedFlow()

  fun updateStake(newStake: Double) {
    _uiState.update { state ->
      if (state.phase != MinesPhase.PLAYING) state.copy(stake = newStake) else state
    }
  }

  fun updateMineCount(mines: Int) {
    val clamped = mines.coerceIn(1, 12)
    _uiState.update { state ->
      if (state.phase != MinesPhase.PLAYING) {
        state.copy(
          mineCount = clamped,
          nextMultiplier = computeMultiplier(1, clamped),
        )
      } else {
        state
      }
    }
  }

  fun startRound() {
    viewModelScope.launch {
      val current = _uiState.value
      if (current.phase == MinesPhase.PLAYING) return@launch

      val deducted = repository.tryPlaceWager(current.stake)
      if (!deducted) {
        _events.tryEmit(MinesEvent.Info("Not enough KSh! Tap your balance pill to refill."))
        return@launch
      }

      val mines = (0 until 25).shuffled(Random).take(current.mineCount).toSet()
      _uiState.update {
        it.copy(
          phase = MinesPhase.PLAYING,
          minePositions = mines,
          revealedPositions = emptySet(),
          explodedTile = null,
          currentMultiplier = 1.0,
          nextMultiplier = computeMultiplier(1, it.mineCount),
          lastPayout = 0.0,
        )
      }
    }
  }

  fun revealTile(index: Int) {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.phase != MinesPhase.PLAYING) return@launch
      if (index !in 0 until 25 || index in state.revealedPositions) return@launch

      if (index in state.minePositions) {
        // Hit a mine!
        _uiState.update {
          it.copy(
            phase = MinesPhase.BUSTED,
            explodedTile = index,
            currentMultiplier = 0.0,
          )
        }
        repository.recordRound(
          gameType = GameType.MINES,
          wager = state.stake,
          multiplier = 0.0,
          payout = 0.0,
          detailSummary = "${state.revealedPositions.size} gems • Hit mine (${state.mineCount} mines)",
        )
        _events.tryEmit(MinesEvent.MineHit)
      } else {
        val newRevealed = state.revealedPositions + index
        val mult = computeMultiplier(newRevealed.size, state.mineCount)
        val safeTotal = 25 - state.mineCount

        if (newRevealed.size >= safeTotal) {
          // Cleared entire board! Auto cash out
          finishCashOut(state.stake, newRevealed, mult)
        } else {
          val nextMult = computeMultiplier(newRevealed.size + 1, state.mineCount)
          _uiState.update {
            it.copy(
              revealedPositions = newRevealed,
              currentMultiplier = mult,
              nextMultiplier = nextMult,
            )
          }
          _events.tryEmit(MinesEvent.GemFound)
        }
      }
    }
  }

  fun pickRandomTile() {
    val state = _uiState.value
    if (state.phase != MinesPhase.PLAYING) return
    val remaining = (0 until 25).filter { it !in state.revealedPositions }
    if (remaining.isNotEmpty()) {
      revealTile(remaining.random())
    }
  }

  fun cashOut() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.phase != MinesPhase.PLAYING || state.revealedPositions.isEmpty()) return@launch
      finishCashOut(state.stake, state.revealedPositions, state.currentMultiplier)
    }
  }

  private suspend fun finishCashOut(
    stake: Double,
    revealed: Set<Int>,
    multiplier: Double,
  ) {
    val payout = floor(stake * multiplier * 10.0) / 10.0
    _uiState.update {
      it.copy(
        phase = MinesPhase.CASHED_OUT,
        revealedPositions = revealed,
        currentMultiplier = multiplier,
        lastPayout = payout,
      )
    }
    repository.creditPayout(payout)
    repository.recordRound(
      gameType = GameType.MINES,
      wager = stake,
      multiplier = multiplier,
      payout = payout,
      detailSummary = "${revealed.size} gems cleared (${_uiState.value.mineCount} mines) @ ${Formatters.multiplier(multiplier)}",
    )
    _events.tryEmit(MinesEvent.CashedOut(payout, multiplier))
  }

  private fun computeMultiplier(revealed: Int, mines: Int): Double {
    if (revealed <= 0) return 1.0
    var factor = 0.97
    for (i in 0 until revealed) {
      val totalLeft = 25.0 - i
      val safeLeft = (25.0 - mines) - i
      if (safeLeft <= 0.0) break
      factor *= (totalLeft / safeLeft)
    }
    return floor(factor * 100.0) / 100.0
  }

  class Factory(private val repository: ArcadeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MinesViewModel(repository) as T
    }
  }
}
