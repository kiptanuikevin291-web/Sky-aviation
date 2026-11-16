package com.example.ui.games.aviator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ArcadeRepository
import com.example.data.GameRoundEntity
import com.example.data.GameType
import com.example.ui.components.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.floor
import kotlin.random.Random

enum class AviatorPhase {
  COUNTDOWN,
  FLYING,
  FLEW_AWAY,
}

enum class BetSlotStatus {
  IDLE,
  QUEUED,
  ACTIVE,
  CASHED_OUT,
  LOST,
}

data class BetSlotState(
  val slotIndex: Int,
  val stake: Double = 25.0,
  val autoCashoutEnabled: Boolean = false,
  val autoCashoutTarget: Double = 2.0,
  val autoBetEnabled: Boolean = false,
  val status: BetSlotStatus = BetSlotStatus.IDLE,
  val lockedStake: Double = 0.0,
  val cashedOutMultiplier: Double = 0.0,
  val cashedOutPayout: Double = 0.0,
)

data class AviatorUiState(
  val phase: AviatorPhase = AviatorPhase.COUNTDOWN,
  val countdownRemainingSec: Float = 4.0f,
  val countdownTotalSec: Float = 4.0f,
  val currentMultiplier: Double = 1.0,
  val flightElapsedSec: Float = 0f,
  val lastCrashMultiplier: Double = 1.0,
  val flightNumber: Int = 1042,
  val recentMultipliers: List<Double> = listOf(2.48, 1.32, 6.91, 1.08, 3.15, 14.20, 1.84, 2.05),
  val slotA: BetSlotState = BetSlotState(slotIndex = 0, stake = 25.0),
  val slotB: BetSlotState = BetSlotState(slotIndex = 1, stake = 50.0),
  val showSecondPanel: Boolean = true,
)

sealed class AviatorEvent {
  data class CashoutWin(val slotIndex: Int, val payout: Double, val multiplier: Double) : AviatorEvent()
  data class Info(val message: String) : AviatorEvent()
  data object PlaneCrashed : AviatorEvent()
}

class AviatorViewModel(private val repository: ArcadeRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(AviatorUiState())
  val uiState: StateFlow<AviatorUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<AviatorEvent>(extraBufferCapacity = 8)
  val events: SharedFlow<AviatorEvent> = _events.asSharedFlow()

  val playerFlightHistory: StateFlow<List<GameRoundEntity>> =
    repository.observeGameRounds(GameType.AVIATOR, 20).stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  init {
    startGameLoop()
  }

  private fun startGameLoop() {
    viewModelScope.launch {
      while (isActive) {
        // 1. COUNTDOWN PHASE
        val totalCountdown = 4.0f
        _uiState.update { state ->
          state.copy(
            phase = AviatorPhase.COUNTDOWN,
            countdownRemainingSec = totalCountdown,
            countdownTotalSec = totalCountdown,
            currentMultiplier = 1.0,
            flightElapsedSec = 0f,
            slotA = resetForCountdown(state.slotA),
            slotB = resetForCountdown(state.slotB),
          )
        }

        // Handle Auto-Bet if enabled
        val sNow = _uiState.value
        if (sNow.slotA.autoBetEnabled && sNow.slotA.status == BetSlotStatus.IDLE) {
          placeOrQueueBet(0)
        }
        if (sNow.slotB.autoBetEnabled && sNow.slotB.status == BetSlotStatus.IDLE && sNow.showSecondPanel) {
          placeOrQueueBet(1)
        }

        val countdownStart = System.currentTimeMillis()
        while (isActive) {
          val elapsed = (System.currentTimeMillis() - countdownStart) / 1000f
          val remaining = (totalCountdown - elapsed).coerceAtLeast(0f)
          _uiState.update { it.copy(countdownRemainingSec = remaining) }
          if (remaining <= 0f) break
          delay(32L)
        }

        // 2. TAKEOFF -> FLYING PHASE
        val crashTarget = generateCrashMultiplier()
        _uiState.update { state ->
          state.copy(
            phase = AviatorPhase.FLYING,
            currentMultiplier = 1.0,
            flightElapsedSec = 0f,
            slotA = activateQueuedSlot(state.slotA),
            slotB = activateQueuedSlot(state.slotB),
          )
        }

        val flightStart = System.currentTimeMillis()
        while (isActive) {
          val elapsedSec = (System.currentTimeMillis() - flightStart) / 1000f
          val rawMult = exp(0.095 * elapsedSec.toDouble())
          val multiplier = floor(rawMult * 100.0) / 100.0

          if (multiplier >= crashTarget) {
            // Crash!
            handleCrash(crashTarget)
            break
          }

          _uiState.update {
            it.copy(
              currentMultiplier = multiplier,
              flightElapsedSec = elapsedSec,
            )
          }

          // Check auto-cashouts
          checkAutoCashout(0, multiplier)
          checkAutoCashout(1, multiplier)

          delay(16L)
        }

        // 3. FLEW AWAY PAUSE
        delay(2400L)
        _uiState.update { it.copy(flightNumber = it.flightNumber + 1) }
      }
    }
  }

  private fun resetForCountdown(slot: BetSlotState): BetSlotState {
    return if (slot.status == BetSlotStatus.QUEUED) {
      slot
    } else {
      slot.copy(
        status = BetSlotStatus.IDLE,
        lockedStake = 0.0,
        cashedOutMultiplier = 0.0,
        cashedOutPayout = 0.0,
      )
    }
  }

  private fun activateQueuedSlot(slot: BetSlotState): BetSlotState {
    return if (slot.status == BetSlotStatus.QUEUED) {
      slot.copy(status = BetSlotStatus.ACTIVE)
    } else {
      slot
    }
  }

  private suspend fun checkAutoCashout(slotIndex: Int, currentMult: Double) {
    val slot = if (slotIndex == 0) _uiState.value.slotA else _uiState.value.slotB
    if (
      slot.status == BetSlotStatus.ACTIVE &&
      slot.autoCashoutEnabled &&
      currentMult >= slot.autoCashoutTarget
    ) {
      cashOutInternal(slotIndex, slot.autoCashoutTarget)
    }
  }

  private suspend fun handleCrash(crashMultiplier: Double) {
    val stateBefore = _uiState.value
    val slotA = stateBefore.slotA
    val slotB = stateBefore.slotB

    if (slotA.status == BetSlotStatus.ACTIVE) {
      repository.recordRound(
        gameType = GameType.AVIATOR,
        wager = slotA.lockedStake,
        multiplier = 0.0,
        payout = 0.0,
        detailSummary = "Flight #${stateBefore.flightNumber} flew away @ ${Formatters.multiplier(crashMultiplier)}",
      )
    }
    if (slotB.status == BetSlotStatus.ACTIVE) {
      repository.recordRound(
        gameType = GameType.AVIATOR,
        wager = slotB.lockedStake,
        multiplier = 0.0,
        payout = 0.0,
        detailSummary = "Flight #${stateBefore.flightNumber} flew away @ ${Formatters.multiplier(crashMultiplier)}",
      )
    }

    _uiState.update { state ->
      state.copy(
        phase = AviatorPhase.FLEW_AWAY,
        currentMultiplier = crashMultiplier,
        lastCrashMultiplier = crashMultiplier,
        recentMultipliers = (listOf(crashMultiplier) + state.recentMultipliers).take(16),
        slotA = if (state.slotA.status == BetSlotStatus.ACTIVE) {
          state.slotA.copy(status = BetSlotStatus.LOST)
        } else {
          state.slotA
        },
        slotB = if (state.slotB.status == BetSlotStatus.ACTIVE) {
          state.slotB.copy(status = BetSlotStatus.LOST)
        } else {
          state.slotB
        },
      )
    }
    if (slotA.status == BetSlotStatus.ACTIVE || slotB.status == BetSlotStatus.ACTIVE) {
      _events.tryEmit(AviatorEvent.PlaneCrashed)
    }
  }

  fun toggleSecondPanel() {
    _uiState.update { it.copy(showSecondPanel = !it.showSecondPanel) }
  }

  fun updateStake(slotIndex: Int, newStake: Double) {
    updateSlot(slotIndex) { slot ->
      if (slot.status == BetSlotStatus.IDLE) slot.copy(stake = newStake) else slot
    }
  }

  fun toggleAutoCashout(slotIndex: Int, enabled: Boolean) {
    updateSlot(slotIndex) { it.copy(autoCashoutEnabled = enabled) }
  }

  fun updateAutoCashoutTarget(slotIndex: Int, target: Double) {
    val clamped = ((target * 100.0).toInt() / 100.0).coerceIn(1.10, 50.0)
    updateSlot(slotIndex) { it.copy(autoCashoutTarget = clamped) }
  }

  fun toggleAutoBet(slotIndex: Int, enabled: Boolean) {
    updateSlot(slotIndex) { it.copy(autoBetEnabled = enabled) }
  }

  fun onPrimarySlotAction(slotIndex: Int) {
    viewModelScope.launch {
      val state = _uiState.value
      val slot = if (slotIndex == 0) state.slotA else state.slotB
      when (slot.status) {
        BetSlotStatus.IDLE, BetSlotStatus.CASHED_OUT, BetSlotStatus.LOST -> {
          placeOrQueueBet(slotIndex)
        }
        BetSlotStatus.QUEUED -> {
          cancelQueuedBet(slotIndex)
        }
        BetSlotStatus.ACTIVE -> {
          if (state.phase == AviatorPhase.FLYING) {
            cashOutInternal(slotIndex, state.currentMultiplier)
          }
        }
      }
    }
  }

  private suspend fun placeOrQueueBet(slotIndex: Int) {
    val state = _uiState.value
    val slot = if (slotIndex == 0) state.slotA else state.slotB
    if (slot.status == BetSlotStatus.QUEUED || slot.status == BetSlotStatus.ACTIVE) return

    val stake = slot.stake
    val deducted = repository.tryPlaceWager(stake)
    if (!deducted) {
      _events.tryEmit(AviatorEvent.Info("Not enough KSh! Tap your balance pill to refill."))
      return
    }

    updateSlot(slotIndex) {
      it.copy(
        status = BetSlotStatus.QUEUED,
        lockedStake = stake,
        cashedOutMultiplier = 0.0,
        cashedOutPayout = 0.0,
      )
    }
  }

  private suspend fun cancelQueuedBet(slotIndex: Int) {
    val slot = if (slotIndex == 0) _uiState.value.slotA else _uiState.value.slotB
    if (slot.status != BetSlotStatus.QUEUED) return
    repository.creditPayout(slot.lockedStake)
    updateSlot(slotIndex) {
      it.copy(
        status = BetSlotStatus.IDLE,
        lockedStake = 0.0,
      )
    }
  }

  private suspend fun cashOutInternal(slotIndex: Int, multiplier: Double) {
    val state = _uiState.value
    if (state.phase != AviatorPhase.FLYING) return
    val slot = if (slotIndex == 0) state.slotA else state.slotB
    if (slot.status != BetSlotStatus.ACTIVE) return

    val payout = floor(slot.lockedStake * multiplier * 10.0) / 10.0
    updateSlot(slotIndex) {
      it.copy(
        status = BetSlotStatus.CASHED_OUT,
        cashedOutMultiplier = multiplier,
        cashedOutPayout = payout,
      )
    }
    repository.creditPayout(payout)
    repository.recordRound(
      gameType = GameType.AVIATOR,
      wager = slot.lockedStake,
      multiplier = multiplier,
      payout = payout,
      detailSummary = "Flight #${state.flightNumber} cashed @ ${Formatters.multiplier(multiplier)}",
    )
    _events.tryEmit(AviatorEvent.CashoutWin(slotIndex, payout, multiplier))
  }

  private inline fun updateSlot(slotIndex: Int, transform: (BetSlotState) -> BetSlotState) {
    _uiState.update { state ->
      if (slotIndex == 0) {
        state.copy(slotA = transform(state.slotA))
      } else {
        state.copy(slotB = transform(state.slotB))
      }
    }
  }

  private fun generateCrashMultiplier(): Double {
    val roll = Random.nextDouble()
    if (roll < 0.035) {
      return (100 + Random.nextInt(5)) / 100.0
    }
    val raw = 0.96 / (1.0 - roll).coerceAtLeast(0.008)
    val clamped = raw.coerceIn(1.06, 95.0)
    return floor(clamped * 100.0) / 100.0
  }

  override fun onCleared() {
    super.onCleared()
    // Any queued bet is not persisted across app kill; no silent leakage needed in normal tab switch
  }

  class Factory(private val repository: ArcadeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return AviatorViewModel(repository) as T
    }
  }
}
