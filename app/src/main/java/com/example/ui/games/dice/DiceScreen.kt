package com.example.ui.games.dice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.data.ArcadeRepository
import com.example.data.GameType
import com.example.ui.components.Formatters
import com.example.ui.components.StakeSelectorRow
import com.example.ui.theme.CashoutEmerald
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitPanel
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PulseViolet
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThrustCrimson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

data class DiceRollHistoryItem(
  val value: Double,
  val won: Boolean,
)

data class DiceUiState(
  val stake: Double = 25.0,
  val target: Int = 50,
  val rollOver: Boolean = true,
  val isRolling: Boolean = false,
  val lastRoll: Double? = null,
  val lastWon: Boolean? = null,
  val lastPayout: Double = 0.0,
  val recentRolls: List<DiceRollHistoryItem> = emptyList(),
) {
  val winChancePercent: Double
    get() = if (rollOver) (100 - target).toDouble() else target.toDouble()

  val multiplier: Double
    get() = floor((98.0 / winChancePercent.coerceAtLeast(1.0)) * 100.0) / 100.0
}

sealed class DiceEvent {
  data class RollResult(val value: Double, val won: Boolean, val payout: Double) : DiceEvent()
  data class Info(val message: String) : DiceEvent()
}

class DiceViewModel(private val repository: ArcadeRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(DiceUiState())
  val uiState: StateFlow<DiceUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<DiceEvent>(extraBufferCapacity = 8)
  val events: SharedFlow<DiceEvent> = _events.asSharedFlow()

  fun updateStake(newStake: Double) {
    _uiState.update { if (!it.isRolling) it.copy(stake = newStake) else it }
  }

  fun updateTarget(newTarget: Int) {
    val clamped = newTarget.coerceIn(5, 95)
    _uiState.update { if (!it.isRolling) it.copy(target = clamped) else it }
  }

  fun toggleRollDirection() {
    _uiState.update {
      if (!it.isRolling) {
        it.copy(rollOver = !it.rollOver, target = (100 - it.target).coerceIn(5, 95))
      } else {
        it
      }
    }
  }

  fun roll() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isRolling) return@launch

      val deducted = repository.tryPlaceWager(state.stake)
      if (!deducted) {
        _events.tryEmit(DiceEvent.Info("Not enough KSh! Tap your balance pill to refill."))
        return@launch
      }

      _uiState.update { it.copy(isRolling = true) }

      // Brief crisp odometer tick
      repeat(4) {
        _uiState.update { it.copy(lastRoll = Random.nextInt(10000) / 100.0) }
        delay(55L)
      }

      val rollValue = Random.nextInt(10000) / 100.0
      val won = if (state.rollOver) rollValue > state.target else rollValue < state.target
      val mult = if (won) state.multiplier else 0.0
      val payout = if (won) floor(state.stake * mult * 10.0) / 10.0 else 0.0

      if (payout > 0.0) {
        repository.creditPayout(payout)
      }
      val condText = if (state.rollOver) "> ${state.target}" else "< ${state.target}"
      repository.recordRound(
        gameType = GameType.DICE,
        wager = state.stake,
        multiplier = mult,
        payout = payout,
        detailSummary = "Rolled ${String.format("%.2f", rollValue)} (Target $condText)",
      )

      _uiState.update {
        it.copy(
          isRolling = false,
          lastRoll = rollValue,
          lastWon = won,
          lastPayout = payout,
          recentRolls = (listOf(DiceRollHistoryItem(rollValue, won)) + it.recentRolls).take(14),
        )
      }
      _events.tryEmit(DiceEvent.RollResult(rollValue, won, payout))
    }
  }

  class Factory(private val repository: ArcadeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return DiceViewModel(repository) as T
    }
  }
}

@Composable
fun DiceScreen(
  viewModel: DiceViewModel,
  credits: Double,
  hapticsEnabled: Boolean,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val haptics = LocalHapticFeedback.current

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is DiceEvent.RollResult -> {
          if (hapticsEnabled) {
            haptics.performHapticFeedback(
              if (event.won) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
          }
        }
        is DiceEvent.Info -> onShowMessage(event.message)
      }
    }
  }

  val animatedMarker by animateFloatAsState(
    targetValue = ((state.lastRoll ?: 50.0) / 100.0).toFloat().coerceIn(0f, 1f),
    animationSpec = tween(220),
    label = "dice_marker",
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (state.recentRolls.isNotEmpty()) {
        item {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(state.recentRolls) { _, item ->
              val c = if (item.won) CashoutEmerald else ThrustCrimson
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(c.copy(alpha = 0.14f))
                  .border(1.dp, c.copy(alpha = 0.45f), RoundedCornerShape(50))
                  .padding(horizontal = 10.dp, vertical = 4.dp),
              ) {
                Text(
                  text = String.format("%.2f", item.value),
                  style = MaterialTheme.typography.labelMedium,
                  color = c,
                )
              }
            }
          }
        }
      }

      // Display Stage
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CockpitSurface)
            .border(1.5.dp, CockpitBorder, RoundedCornerShape(22.dp))
            .padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            text = if (state.lastRoll != null) String.format("%.2f", state.lastRoll) else "50.00",
            style = MaterialTheme.typography.displayLarge.copy(
              fontSize = 54.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = when (state.lastWon) {
              true -> CashoutEmerald
              false -> ThrustCrimson
              null -> TextPrimary
            },
          )

          Text(
            text = when {
              state.isRolling -> "GENERATING ROLL..."
              state.lastWon == true -> "HIT! +${Formatters.credits(state.lastPayout)}"
              state.lastWon == false -> "MISSED TARGET"
              else -> if (state.rollOver) "Roll Over ${state.target} to Win" else "Roll Under ${state.target} to Win"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when (state.lastWon) {
              true -> CashoutEmerald
              false -> ThrustCrimson
              null -> TextSecondary
            },
          )

          // Range Zone Visualizer
          Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
              modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            ) {
              val trackWidth = maxWidth
              val splitFrac = (state.target / 100f).coerceIn(0.05f, 0.95f)
              val leftColor = if (state.rollOver) ThrustCrimson else CashoutEmerald
              val rightColor = if (state.rollOver) CashoutEmerald else ThrustCrimson

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(14.dp)
                  .align(Alignment.Center)
                  .clip(RoundedCornerShape(50))
                  .border(1.dp, CockpitBorder, RoundedCornerShape(50)),
              ) {
                Box(
                  modifier = Modifier
                    .weight(splitFrac)
                    .fillMaxHeight()
                    .background(leftColor.copy(alpha = 0.78f))
                )
                Box(
                  modifier = Modifier
                    .weight(1f - splitFrac)
                    .fillMaxHeight()
                    .background(rightColor.copy(alpha = 0.78f))
                )
              }

              if (state.lastRoll != null) {
                val xOffset = (trackWidth - 18.dp) * animatedMarker
                Box(
                  modifier = Modifier
                    .offset(x = xOffset)
                    .size(18.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(TextPrimary)
                    .border(2.dp, ObsidianBg, CircleShape)
                )
              }
            }

            Slider(
              value = state.target.toFloat(),
              onValueChange = { viewModel.updateTarget(it.roundToInt()) },
              valueRange = 5f..95f,
              enabled = !state.isRolling,
              colors = SliderDefaults.colors(
                thumbColor = PulseViolet,
                activeTrackColor = PulseViolet,
                inactiveTrackColor = CockpitBorder,
              ),
              modifier = Modifier.testTag("dice_target_slider"),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("0", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
              Text("Target: ${state.target}", style = MaterialTheme.typography.labelMedium, color = RadarCyan)
              Text("100", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
          }

          // Odds Telemetry Cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DiceStatPill(
              label = "MULTIPLIER",
              value = Formatters.multiplier(state.multiplier),
              valueColor = RadarCyan,
              modifier = Modifier.weight(1f),
            )
            DiceStatPill(
              label = "WIN CHANCE",
              value = "${state.winChancePercent.toInt()}%",
              valueColor = CashoutEmerald,
              modifier = Modifier.weight(1f),
            )
            Box(
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(CockpitPanel)
                .border(1.dp, PulseViolet.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .clickable(enabled = !state.isRolling) { viewModel.toggleRollDirection() }
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .testTag("dice_direction_toggle"),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = "Switch roll over or under",
                    tint = PulseViolet,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("MODE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Text(
                  text = if (state.rollOver) "OVER ${state.target}" else "UNDER ${state.target}",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = PulseViolet,
                )
              }
            }
          }
        }
      }

      // Controls
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CockpitPanel)
            .border(1.dp, CockpitBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          StakeSelectorRow(
            stake = state.stake,
            maxCredits = credits,
            enabled = !state.isRolling,
            onStakeChange = viewModel::updateStake,
            tagPrefix = "dice_stake",
          )

          Button(
            onClick = viewModel::roll,
            enabled = !state.isRolling,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = PulseViolet,
              contentColor = TextPrimary,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("dice_roll_button"),
          ) {
            Icon(Icons.Rounded.Casino, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (state.isRolling) "ROLLING..." else "ROLL DICE • ${Formatters.credits(state.stake)}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DiceStatPill(
  label: String,
  value: String,
  valueColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(CockpitPanel)
      .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp))
      .padding(vertical = 8.dp, horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge.copy(
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
        fontWeight = FontWeight.Bold,
      ),
      color = valueColor,
    )
  }
}
