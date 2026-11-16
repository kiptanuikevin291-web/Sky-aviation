package com.example.ui.games.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.example.ui.theme.CockpitPanelElevated
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CreditGold
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PulseViolet
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThrustCrimson
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

enum class WheelRisk(val label: String, val slices: List<Double>) {
  LOW("Low", listOf(1.6, 1.2, 0.4, 1.2, 1.6, 0.4, 2.0, 1.2, 0.4, 1.2, 0.4, 0.0)),
  MEDIUM("Med", listOf(3.0, 0.0, 1.5, 0.0, 2.0, 0.0, 4.0, 0.0, 1.2, 0.0, 0.0, 0.0)),
  HIGH("High", listOf(6.0, 0.0, 0.0, 2.2, 0.0, 0.0, 3.5, 0.0, 0.0, 0.0, 0.0, 0.0)),
}

data class WheelUiState(
  val stake: Double = 25.0,
  val risk: WheelRisk = WheelRisk.MEDIUM,
  val isSpinning: Boolean = false,
  val targetRotationDeg: Float = 0f,
  val spinSequence: Int = 0,
  val pendingTargetIndex: Int? = null,
  val lastResultMultiplier: Double? = null,
  val lastResultPayout: Double = 0.0,
)

sealed class WheelEvent {
  data class SpinFinished(val multiplier: Double, val payout: Double) : WheelEvent()
  data class Info(val message: String) : WheelEvent()
}

class WheelViewModel(private val repository: ArcadeRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(WheelUiState())
  val uiState: StateFlow<WheelUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<WheelEvent>(extraBufferCapacity = 8)
  val events: SharedFlow<WheelEvent> = _events.asSharedFlow()

  private var pendingStake: Double = 0.0

  fun updateStake(newStake: Double) {
    _uiState.update { if (!it.isSpinning) it.copy(stake = newStake) else it }
  }

  fun updateRisk(newRisk: WheelRisk) {
    _uiState.update { if (!it.isSpinning) it.copy(risk = newRisk, lastResultMultiplier = null) else it }
  }

  fun spin() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.isSpinning) return@launch

      val deducted = repository.tryPlaceWager(state.stake)
      if (!deducted) {
        _events.tryEmit(WheelEvent.Info("Not enough KSh! Tap your balance pill to refill."))
        return@launch
      }

      pendingStake = state.stake
      val slices = state.risk.slices
      val targetIndex = Random.nextInt(slices.size)
      val sliceSpan = 360f / slices.size
      // Align center of targetIndex with top pointer (270 deg)
      val baseAngle = (270f - (targetIndex * sliceSpan + sliceSpan / 2f))
      val jitter = (Random.nextFloat() - 0.5f) * (sliceSpan * 0.55f)
      val currentBase = state.targetRotationDeg
      val fullSpins = 360f * 6f
      val currentMod = ((currentBase % 360f) + 360f) % 360f
      val desiredMod = (((baseAngle + jitter) % 360f) + 360f) % 360f
      val deltaMod = ((desiredMod - currentMod) + 360f) % 360f
      val nextTarget = currentBase + fullSpins + deltaMod

      _uiState.update {
        it.copy(
          isSpinning = true,
          targetRotationDeg = nextTarget,
          spinSequence = it.spinSequence + 1,
          pendingTargetIndex = targetIndex,
        )
      }
    }
  }

  fun onSpinAnimationComplete() {
    viewModelScope.launch {
      val state = _uiState.value
      if (!state.isSpinning) return@launch
      val idx = state.pendingTargetIndex ?: return@launch
      val mult = state.risk.slices[idx]
      val payout = floor(pendingStake * mult * 10.0) / 10.0

      if (payout > 0.0) {
        repository.creditPayout(payout)
      }
      repository.recordRound(
        gameType = GameType.WHEEL,
        wager = pendingStake,
        multiplier = mult,
        payout = payout,
        detailSummary = "${state.risk.label} Wheel • ${Formatters.multiplier(mult)}",
      )

      _uiState.update {
        it.copy(
          isSpinning = false,
          pendingTargetIndex = null,
          lastResultMultiplier = mult,
          lastResultPayout = payout,
        )
      }
      _events.tryEmit(WheelEvent.SpinFinished(mult, payout))
    }
  }

  class Factory(private val repository: ArcadeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return WheelViewModel(repository) as T
    }
  }
}

@Composable
fun WheelScreen(
  viewModel: WheelViewModel,
  credits: Double,
  hapticsEnabled: Boolean,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val haptics = LocalHapticFeedback.current
  val textMeasurer = rememberTextMeasurer()
  val wheelRotation = remember { Animatable(0f) }

  LaunchedEffect(state.spinSequence) {
    if (state.spinSequence > 0 && state.isSpinning) {
      wheelRotation.animateTo(
        targetValue = state.targetRotationDeg,
        animationSpec = tween(
          durationMillis = 3100,
          easing = CubicBezierEasing(0.12f, 0.88f, 0.16f, 1.0f),
        ),
      )
      viewModel.onSpinAnimationComplete()
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is WheelEvent.SpinFinished -> {
          if (hapticsEnabled) {
            haptics.performHapticFeedback(
              if (event.payout > 0.0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
          }
          if (event.payout > 0.0) {
            onShowMessage("Wheel hit ${Formatters.multiplier(event.multiplier)} • +${Formatters.credits(event.payout)}")
          }
        }
        is WheelEvent.Info -> onShowMessage(event.message)
      }
    }
  }

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
      // Wheel Stage
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CockpitSurface)
            .border(1.5.dp, CockpitBorder, RoundedCornerShape(22.dp))
            .padding(vertical = 18.dp, horizontal = 14.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier.size(275.dp),
              contentAlignment = Alignment.Center,
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                val slices = state.risk.slices
                val sliceSpan = 360f / slices.size
                val diameter = size.minDimension * 0.88f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = diameter / 2f

                // Outer neon ring
                drawCircle(
                  color = CockpitBorder,
                  radius = radius + 10f,
                  center = center,
                  style = Stroke(width = 6f),
                )

                slices.forEachIndexed { idx, mult ->
                  val startAngle = wheelRotation.value + idx * sliceSpan
                  val sliceColor = when {
                    mult >= 5.0 -> CreditGold
                    mult >= 2.0 -> PulseViolet
                    mult >= 1.0 -> CashoutEmerald
                    mult > 0.0 -> RadarCyan
                    else -> Color(0xFF1E293B)
                  }

                  drawArc(
                    color = sliceColor.copy(alpha = if (mult == 0.0) 0.95f else 0.88f),
                    startAngle = startAngle,
                    sweepAngle = sliceSpan,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                  )
                  drawArc(
                    color = ObsidianBg,
                    startAngle = startAngle,
                    sweepAngle = sliceSpan,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 2.5f),
                  )

                  // Draw label radially
                  val midAngle = startAngle + sliceSpan / 2f
                  val label = if (mult == 0.0) "0x" else if (mult % 1.0 == 0.0) "${mult.toInt()}x" else "${mult}x"
                  val textResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                      color = if (mult == 0.0) TextSecondary else ObsidianBg,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                    ),
                  )
                  rotate(degrees = midAngle, pivot = center) {
                    drawText(
                      textLayoutResult = textResult,
                      topLeft = Offset(
                        center.x + radius * 0.56f - textResult.size.width / 2f,
                        center.y - textResult.size.height / 2f,
                      ),
                    )
                  }
                }

                // Center hub
                drawCircle(
                  color = CockpitPanel,
                  radius = radius * 0.24f,
                  center = center,
                )
                drawCircle(
                  color = CreditGold,
                  radius = radius * 0.24f,
                  center = center,
                  style = Stroke(width = 4f),
                )

                // Top Clacker Pointer at 270° (top center)
                val pointerPath = Path().apply {
                  moveTo(center.x, center.y - radius + 12f)
                  lineTo(center.x - 14f, center.y - radius - 14f)
                  lineTo(center.x + 14f, center.y - radius - 14f)
                  close()
                }
                drawPath(path = pointerPath, color = ThrustCrimson)
              }

              // Center readout
              val res = state.lastResultMultiplier
              Text(
                text = when {
                  state.isSpinning -> "..."
                  res != null -> Formatters.multiplier(res)
                  else -> "SPIN"
                },
                style = MaterialTheme.typography.labelLarge.copy(
                  fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                  fontWeight = FontWeight.Bold,
                ),
                color = when {
                  res == null -> CreditGold
                  res > 0.0 -> CashoutEmerald
                  else -> ThrustCrimson
                },
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val lastMult = state.lastResultMultiplier
            if (!state.isSpinning && lastMult != null) {
              Text(
                text = if (state.lastResultPayout > 0.0) {
                  "WON +${Formatters.credits(state.lastResultPayout)} (${Formatters.multiplier(lastMult)})"
                } else {
                  "Landed on 0x — spin again!"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (state.lastResultPayout > 0.0) CashoutEmerald else ThrustCrimson,
              )
            } else {
              Text(
                text = if (state.isSpinning) "Rotor spinning..." else "Choose risk tier & spin the Neon Wheel",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        }
      }

      // Controls Panel
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
          Text("RISK TIER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            WheelRisk.entries.forEach { risk ->
              val selected = state.risk == risk
              Box(
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (selected) CreditGold.copy(alpha = 0.22f) else CockpitPanelElevated)
                  .border(1.dp, if (selected) CreditGold else CockpitBorder, RoundedCornerShape(12.dp))
                  .clickable(enabled = !state.isSpinning) { viewModel.updateRisk(risk) }
                  .padding(vertical = 10.dp)
                  .testTag("wheel_risk_${risk.name.lowercase()}"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = risk.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) CreditGold else TextPrimary,
                )
              }
            }
          }

          StakeSelectorRow(
            stake = state.stake,
            maxCredits = credits,
            enabled = !state.isSpinning,
            onStakeChange = viewModel::updateStake,
            tagPrefix = "wheel_stake",
          )

          Button(
            onClick = viewModel::spin,
            enabled = !state.isSpinning,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CreditGold,
              contentColor = ObsidianBg,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("wheel_spin_button"),
          ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (state.isSpinning) "SPINNING..." else "SPIN WHEEL • ${Formatters.credits(state.stake)}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }
  }
}
