package com.example.ui.games.plinko

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.random.Random

enum class PlinkoRisk(val label: String) {
  LOW("Low"),
  MEDIUM("Med"),
  HIGH("High"),
}

data class PlinkoBall(
  val id: Long,
  val stake: Double,
  val rows: Int,
  val multipliers: List<Double>,
  val x: Float, // normalized [-1..1] around center; pyramid spans linearly with y
  val y: Float, // 0f top to 1f buckets
  val vx: Float,
  val vy: Float,
)

data class PegPulse(
  val x: Float,
  val y: Float,
  val life: Float,
)

data class PlinkoDropResult(
  val id: Long,
  val multiplier: Double,
  val payout: Double,
)

data class PlinkoUiState(
  val stake: Double = 25.0,
  val rows: Int = 10,
  val risk: PlinkoRisk = PlinkoRisk.MEDIUM,
  val multipliers: List<Double> = multipliersFor(10, PlinkoRisk.MEDIUM),
  val balls: List<PlinkoBall> = emptyList(),
  val pegPulses: List<PegPulse> = emptyList(),
  val lastHitBucketIndex: Int? = null,
  val recentDrops: List<PlinkoDropResult> = emptyList(),
)

fun multipliersFor(rows: Int, risk: PlinkoRisk): List<Double> {
  return when (rows) {
    8 -> when (risk) {
      PlinkoRisk.LOW -> listOf(5.6, 2.1, 1.1, 1.0, 0.5, 1.0, 1.1, 2.1, 5.6)
      PlinkoRisk.MEDIUM -> listOf(13.0, 3.0, 1.3, 0.7, 0.4, 0.7, 1.3, 3.0, 13.0)
      PlinkoRisk.HIGH -> listOf(22.0, 4.0, 1.5, 0.3, 0.2, 0.3, 1.5, 4.0, 22.0)
    }
    12 -> when (risk) {
      PlinkoRisk.LOW -> listOf(10.0, 3.0, 1.6, 1.4, 1.1, 1.0, 0.5, 1.0, 1.1, 1.4, 1.6, 3.0, 10.0)
      PlinkoRisk.MEDIUM -> listOf(18.0, 6.0, 2.4, 1.4, 0.9, 0.5, 0.3, 0.5, 0.9, 1.4, 2.4, 6.0, 18.0)
      PlinkoRisk.HIGH -> listOf(25.0, 9.0, 3.2, 1.2, 0.5, 0.2, 0.2, 0.2, 0.5, 1.2, 3.2, 9.0, 25.0)
    }
    else -> when (risk) { // 10 rows
      PlinkoRisk.LOW -> listOf(8.9, 3.0, 1.4, 1.1, 1.0, 0.5, 1.0, 1.1, 1.4, 3.0, 8.9)
      PlinkoRisk.MEDIUM -> listOf(15.0, 4.0, 1.8, 1.2, 0.7, 0.4, 0.7, 1.2, 1.8, 4.0, 15.0)
      PlinkoRisk.HIGH -> listOf(24.0, 6.5, 2.2, 0.8, 0.3, 0.2, 0.3, 0.8, 2.2, 6.5, 24.0)
    }
  }
}

sealed class PlinkoEvent {
  data class BallLanded(val multiplier: Double, val payout: Double) : PlinkoEvent()
  data class Info(val message: String) : PlinkoEvent()
}

class PlinkoViewModel(private val repository: ArcadeRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(PlinkoUiState())
  val uiState: StateFlow<PlinkoUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<PlinkoEvent>(extraBufferCapacity = 12)
  val events: SharedFlow<PlinkoEvent> = _events.asSharedFlow()

  private var nextBallId = 1L

  init {
    startPhysicsLoop()
  }

  private fun startPhysicsLoop() {
    viewModelScope.launch {
      val dt = 0.016f
      while (isActive) {
        val current = _uiState.value
        if (current.balls.isNotEmpty() || current.pegPulses.isNotEmpty()) {
          stepSimulation(dt)
        }
        delay(16L)
      }
    }
  }

  private suspend fun stepSimulation(dt: Float) {
    val state = _uiState.value
    val nextPulses = state.pegPulses
      .mapNotNull { pulse ->
        val nl = pulse.life - dt * 3.2f
        if (nl > 0f) pulse.copy(life = nl) else null
      }
      .toMutableList()

    val remainingBalls = mutableListOf<PlinkoBall>()
    var latestBucketHit: Int? = null

    for (ball in state.balls) {
      val gravity = 1.45f
      var vx = ball.vx * 0.992f
      var vy = ball.vy + gravity * dt
      var nx = ball.x + vx * dt
      var ny = ball.y + vy * dt

      // Peg layout in normalized [0..1] space:
      // Row r in 0 until rows sits at py = 0.14 + (r / (rows - 1f)) * 0.72f
      // Row r has (r + 3) pegs spaced by dx = 0.88f / (rows + 2)
      val rows = ball.rows
      val spacingX = 0.88f / (rows + 2)
      val startY = 0.12f
      val endY = 0.86f
      val stepY = (endY - startY) / (rows - 1).coerceAtLeast(1)
      val collisionDist = spacingX * 0.42f

      val approxRow = ((ny - startY) / stepY).toInt().coerceIn(0, rows - 1)
      for (r in (approxRow - 1).coerceAtLeast(0)..(approxRow + 1).coerceAtMost(rows - 1)) {
        val py = startY + r * stepY
        val pegsInRow = r + 3
        val rowWidth = (pegsInRow - 1) * spacingX
        val leftX = 0.5f - rowWidth / 2f
        for (p in 0 until pegsInRow) {
          val px = leftX + p * spacingX
          val dx = nx - px
          val dy = ny - py
          val dist = hypot(dx, dy)
          if (dist < collisionDist && dist > 0.0001f) {
            val ux = dx / dist
            val uy = dy / dist
            // Push out of peg
            nx = px + ux * collisionDist
            ny = py + uy * collisionDist

            // Reflect velocity with slight jitter for natural Plinko dispersion
            val dot = vx * ux + vy * uy
            if (dot < 0f) {
              vx -= 1.55f * dot * ux
              vy -= 1.55f * dot * uy
              vx += (Random.nextFloat() - 0.5f) * 0.14f
              // Gentle inward guidance near outer pyramid boundary
              val halfBound = (rowWidth / 2f) + spacingX * 0.45f
              val offsetFromCenter = nx - 0.5f
              if (abs(offsetFromCenter) > halfBound * 0.9f) {
                vx -= offsetFromCenter * 0.45f
              }
              nextPulses.add(PegPulse(px, py, 1.0f))
            }
          }
        }
      }

      // Clamp to pyramid outer funnel walls
      val tY = ((ny - startY) / (endY - startY)).coerceIn(0f, 1f)
      val currentHalfSpan = ((2f + tY * (rows)) * spacingX) / 2f + spacingX * 0.3f
      if (nx < 0.5f - currentHalfSpan) {
        nx = 0.5f - currentHalfSpan
        vx = abs(vx) * 0.6f
      } else if (nx > 0.5f + currentHalfSpan) {
        nx = 0.5f + currentHalfSpan
        vx = -abs(vx) * 0.6f
      }

      if (ny >= 0.90f) {
        // Landed in bucket!
        val bucketCount = ball.multipliers.size
        val bottomSpan = (rows + 2) * spacingX
        val leftEdge = 0.5f - bottomSpan / 2f
        val rel = ((nx - leftEdge) / bottomSpan).coerceIn(0f, 0.9999f)
        val bucketIdx = (rel * bucketCount).toInt().coerceIn(0, bucketCount - 1)
        val mult = ball.multipliers[bucketIdx]
        val payout = floor(ball.stake * mult * 10.0) / 10.0

        latestBucketHit = bucketIdx
        if (payout > 0.0) {
          repository.creditPayout(payout)
        }
        repository.recordRound(
          gameType = GameType.PLINKO,
          wager = ball.stake,
          multiplier = mult,
          payout = payout,
          detailSummary = "${ball.rows} rows • Landed ${Formatters.multiplier(mult)}",
        )
        _events.tryEmit(PlinkoEvent.BallLanded(mult, payout))
        _uiState.update { s ->
          s.copy(
            recentDrops = (listOf(PlinkoDropResult(ball.id, mult, payout)) + s.recentDrops).take(12)
          )
        }
      } else {
        remainingBalls.add(ball.copy(x = nx, y = ny, vx = vx, vy = vy))
      }
    }

    _uiState.update {
      it.copy(
        balls = remainingBalls,
        pegPulses = nextPulses.takeLast(24),
        lastHitBucketIndex = latestBucketHit ?: it.lastHitBucketIndex,
      )
    }
  }

  fun updateStake(newStake: Double) {
    _uiState.update { it.copy(stake = newStake) }
  }

  fun updateRows(newRows: Int) {
    _uiState.update {
      if (it.balls.isEmpty()) {
        it.copy(
          rows = newRows,
          multipliers = multipliersFor(newRows, it.risk),
          lastHitBucketIndex = null,
        )
      } else {
        it
      }
    }
  }

  fun updateRisk(newRisk: PlinkoRisk) {
    _uiState.update {
      if (it.balls.isEmpty()) {
        it.copy(
          risk = newRisk,
          multipliers = multipliersFor(it.rows, newRisk),
          lastHitBucketIndex = null,
        )
      } else {
        it
      }
    }
  }

  fun dropBall() {
    viewModelScope.launch {
      val state = _uiState.value
      if (state.balls.size >= 8) {
        _events.tryEmit(PlinkoEvent.Info("Max 8 active orbs at once!"))
        return@launch
      }
      val deducted = repository.tryPlaceWager(state.stake)
      if (!deducted) {
        _events.tryEmit(PlinkoEvent.Info("Not enough KSh! Tap your balance pill to refill."))
        return@launch
      }

      val spawnJitter = (Random.nextFloat() - 0.5f) * 0.03f
      val ball = PlinkoBall(
        id = nextBallId++,
        stake = state.stake,
        rows = state.rows,
        multipliers = state.multipliers,
        x = 0.5f + spawnJitter,
        y = 0.04f,
        vx = (Random.nextFloat() - 0.5f) * 0.05f,
        vy = 0.08f,
      )
      _uiState.update { it.copy(balls = it.balls + ball) }
    }
  }

  class Factory(private val repository: ArcadeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return PlinkoViewModel(repository) as T
    }
  }
}

@Composable
fun PlinkoScreen(
  viewModel: PlinkoViewModel,
  credits: Double,
  hapticsEnabled: Boolean,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val haptics = LocalHapticFeedback.current
  val textMeasurer = rememberTextMeasurer()

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is PlinkoEvent.BallLanded -> {
          if (hapticsEnabled && event.multiplier >= 1.5) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          }
        }
        is PlinkoEvent.Info -> onShowMessage(event.message)
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
        .widthIn(max = 720.dp),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Recent Drops Pills
      if (state.recentDrops.isNotEmpty()) {
        item {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.recentDrops, key = { it.id }) { drop ->
              val c = when {
                drop.multiplier >= 5.0 -> CreditGold
                drop.multiplier >= 1.2 -> CashoutEmerald
                else -> ThrustCrimson
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(c.copy(alpha = 0.16f))
                  .border(1.dp, c.copy(alpha = 0.45f), RoundedCornerShape(50))
                  .padding(horizontal = 10.dp, vertical = 4.dp),
              ) {
                Text(
                  text = Formatters.multiplier(drop.multiplier),
                  style = MaterialTheme.typography.labelMedium,
                  color = c,
                )
              }
            }
          }
        }
      }

      // Plinko Canvas
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(315.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CockpitSurface)
            .border(1.5.dp, CockpitBorder, RoundedCornerShape(22.dp)),
        ) {
          androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rows = state.rows
            val spacingX = (0.88f * w) / (rows + 2)
            val startY = 0.12f * h
            val endY = 0.86f * h
            val stepY = (endY - startY) / (rows - 1).coerceAtLeast(1)

            // Draw Pegs
            for (r in 0 until rows) {
              val py = startY + r * stepY
              val pegsInRow = r + 3
              val rowWidth = (pegsInRow - 1) * spacingX
              val leftX = w * 0.5f - rowWidth / 2f
              for (p in 0 until pegsInRow) {
                val px = leftX + p * spacingX
                drawCircle(
                  color = Color(0xFF475569),
                  radius = (spacingX * 0.13f).coerceIn(3f, 6.5f),
                  center = Offset(px, py),
                )
                drawCircle(
                  color = TextPrimary.copy(alpha = 0.85f),
                  radius = (spacingX * 0.08f).coerceIn(2f, 4f),
                  center = Offset(px, py),
                )
              }
            }

            // Draw Peg Hit Pulses
            state.pegPulses.forEach { pulse ->
              drawCircle(
                color = RadarCyan.copy(alpha = pulse.life * 0.7f),
                radius = (1f - pulse.life) * 18f + 5f,
                center = Offset(pulse.x * w, pulse.y * h),
              )
            }

            // Draw Bottom Multiplier Buckets
            val mults = state.multipliers
            val bottomSpan = (rows + 2) * spacingX
            val leftEdge = w * 0.5f - bottomSpan / 2f
            val bucketW = bottomSpan / mults.size
            val bucketTop = 0.885f * h
            val bucketH = 0.095f * h

            mults.forEachIndexed { idx, mult ->
              val bx = leftEdge + idx * bucketW + 2f
              val bw = (bucketW - 4f).coerceAtLeast(8f)
              val isLastHit = state.lastHitBucketIndex == idx
              val bucketColor = bucketColorFor(mult)

              drawRoundRect(
                color = if (isLastHit) bucketColor else bucketColor.copy(alpha = 0.28f),
                topLeft = Offset(bx, bucketTop - if (isLastHit) 3f else 0f),
                size = Size(bw, bucketH),
                cornerRadius = CornerRadius(8f, 8f),
              )

              val label = if (mult >= 10.0) "${mult.toInt()}x" else "${mult}x"
              drawBucketText(
                textMeasurer = textMeasurer,
                text = label,
                x = bx,
                y = bucketTop,
                width = bw,
                height = bucketH,
                textColor = if (isLastHit) ObsidianBg else TextPrimary,
              )
            }

            // Draw Active Balls
            state.balls.forEach { ball ->
              val cx = ball.x * w
              val cy = ball.y * h
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(Color.White, RadarCyan, PulseViolet),
                  center = Offset(cx, cy),
                  radius = (spacingX * 0.25f).coerceIn(6f, 12f),
                ),
                radius = (spacingX * 0.24f).coerceIn(5.5f, 10.5f),
                center = Offset(cx, cy),
              )
            }
          }
        }
      }

      // Controls Panel
      item {
        val boardLocked = state.balls.isNotEmpty()
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CockpitPanel)
            .border(1.dp, CockpitBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Risk selector
            Column(modifier = Modifier.weight(1f)) {
              Text("RISK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlinkoRisk.entries.forEach { risk ->
                  val selected = state.risk == risk
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .minimumInteractiveComponentSize()
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (selected) PulseViolet.copy(alpha = 0.24f) else CockpitPanelElevated)
                      .border(1.dp, if (selected) PulseViolet else CockpitBorder, RoundedCornerShape(10.dp))
                      .clickable(enabled = !boardLocked) { viewModel.updateRisk(risk) }
                      .padding(vertical = 8.dp)
                      .testTag("plinko_risk_${risk.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = risk.label,
                      style = MaterialTheme.typography.labelMedium,
                      color = if (selected) PulseViolet else TextPrimary,
                    )
                  }
                }
              }
            }

            // Rows selector
            Column(modifier = Modifier.weight(1f)) {
              Text("ROWS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
              Spacer(modifier = Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(8, 10, 12).forEach { r ->
                  val selected = state.rows == r
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .minimumInteractiveComponentSize()
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (selected) RadarCyan.copy(alpha = 0.22f) else CockpitPanelElevated)
                      .border(1.dp, if (selected) RadarCyan else CockpitBorder, RoundedCornerShape(10.dp))
                      .clickable(enabled = !boardLocked) { viewModel.updateRows(r) }
                      .padding(vertical = 8.dp)
                      .testTag("plinko_rows_$r"),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "$r",
                      style = MaterialTheme.typography.labelMedium,
                      color = if (selected) RadarCyan else TextPrimary,
                    )
                  }
                }
              }
            }
          }

          StakeSelectorRow(
            stake = state.stake,
            maxCredits = credits,
            enabled = true,
            onStakeChange = viewModel::updateStake,
            tagPrefix = "plinko_stake",
          )

          Button(
            onClick = viewModel::dropBall,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = RadarCyan,
              contentColor = ObsidianBg,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("plinko_drop_button"),
          ) {
            Icon(Icons.Rounded.ArrowDownward, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "DROP ORB • ${Formatters.credits(state.stake)}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }
  }
}

private fun bucketColorFor(mult: Double): Color = when {
  mult >= 10.0 -> ThrustCrimson
  mult >= 3.0 -> CreditGold
  mult >= 1.1 -> CashoutEmerald
  else -> RadarCyan
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBucketText(
  textMeasurer: TextMeasurer,
  text: String,
  x: Float,
  y: Float,
  width: Float,
  height: Float,
  textColor: Color,
) {
  val fontSize = if (width < 46f) 8.5.sp else 10.5.sp
  val layout = textMeasurer.measure(
    text = text,
    style = TextStyle(
      color = textColor,
      fontSize = fontSize,
      fontWeight = FontWeight.Bold,
    ),
  )
  val tx = x + (width - layout.size.width) / 2f
  val ty = y + (height - layout.size.height) / 2f
  drawText(
    textLayoutResult = layout,
    topLeft = Offset(tx, ty),
  )
}
