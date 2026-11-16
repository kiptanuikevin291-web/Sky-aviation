package com.example.ui.games.aviator

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Formatters
import com.example.ui.theme.CashoutEmerald
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitPanel
import com.example.ui.theme.CreditGold
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PulseViolet
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThrustCrimson
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun AviatorCanvas(
  state: AviatorUiState,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "aviator_fx")
  val radarPulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "radar_pulse",
  )
  val flameFlicker by infiniteTransition.animateFloat(
    initialValue = 0.75f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(140, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "flame_flicker",
  )

  val tierColor by animateColorAsState(
    targetValue = when {
      state.phase == AviatorPhase.FLEW_AWAY -> ThrustCrimson
      state.phase == AviatorPhase.COUNTDOWN -> RadarCyan
      state.currentMultiplier >= 10.0 -> CreditGold
      state.currentMultiplier >= 2.0 -> PulseViolet
      else -> RadarCyan
    },
    label = "tier_color",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(ObsidianBg)
      .border(1.5.dp, CockpitBorder, RoundedCornerShape(22.dp)),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Atmospheric radial backdrop
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(
            tierColor.copy(alpha = if (state.phase == AviatorPhase.FLEW_AWAY) 0.28f else 0.18f),
            CockpitPanel.copy(alpha = 0.7f),
            ObsidianBg,
          ),
          center = Offset(w * 0.65f, h * 0.45f),
          radius = w.coerceAtLeast(h) * 0.85f,
        )
      )

      // Subtle scrolling HUD coordinate grid
      val gridStepX = w / 8f
      val gridStepY = h / 5f
      val scrollShift = if (state.phase == AviatorPhase.FLYING) {
        (state.flightElapsedSec * 36f) % gridStepX
      } else {
        0f
      }
      for (i in 0..9) {
        val x = i * gridStepX - scrollShift
        if (x in 0f..w) {
          drawLine(
            color = CockpitBorder.copy(alpha = 0.45f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f,
          )
        }
      }
      for (j in 1..4) {
        val y = j * gridStepY
        drawLine(
          color = CockpitBorder.copy(alpha = 0.45f),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1f,
        )
      }

      // Ground runway axis
      val padL = w * 0.06f
      val padR = w * 0.90f
      val baseY = h * 0.86f
      val topY = h * 0.18f

      drawLine(
        color = CockpitBorder,
        start = Offset(padL, baseY),
        end = Offset(w * 0.96f, baseY),
        strokeWidth = 2f,
      )

      when (state.phase) {
        AviatorPhase.COUNTDOWN -> {
          // Radar sweep ring at launch pad
          val origin = Offset(padL + 16f, baseY - 12f)
          drawCircle(
            color = RadarCyan.copy(alpha = (1f - radarPulse) * 0.45f),
            radius = 22f + radarPulse * 70f,
            center = origin,
            style = Stroke(width = 2.5f),
          )
          drawJetPlane(
            center = origin,
            angleDeg = -18f,
            glowColor = RadarCyan,
            flameScale = 0.45f * flameFlicker,
          )
        }

        AviatorPhase.FLYING -> {
          val progress = (state.flightElapsedSec / 9.5f).coerceIn(0.03f, 1.0f)
          val wobble = sin(state.flightElapsedSec * 3.1f) * 8f * (if (progress > 0.7f) 1f else 0f)
          val endX = padL + (padR - padL) * progress
          val endY = (baseY - (baseY - topY) * progress.pow(1.45f)) + wobble

          // Draw smooth trajectory curve
          val steps = 36
          val linePath = Path().apply { moveTo(padL, baseY) }
          val fillPath = Path().apply { moveTo(padL, baseY) }
          var prevPoint = Offset(padL, baseY)
          var tangentAngleDeg = -28f

          for (s in 1..steps) {
            val t = s / steps.toFloat()
            val curProgress = progress * t
            val px = padL + (padR - padL) * curProgress
            val py = (baseY - (baseY - topY) * curProgress.pow(1.45f)) +
              (if (s == steps) wobble else sin(state.flightElapsedSec * 3.1f * t) * 4f * t)
            linePath.lineTo(px, py)
            fillPath.lineTo(px, py)
            if (s == steps) {
              val dx = px - prevPoint.x
              val dy = py - prevPoint.y
              tangentAngleDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
            }
            prevPoint = Offset(px, py)
          }

          fillPath.lineTo(endX, baseY)
          fillPath.close()

          drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
              colors = listOf(
                ThrustCrimson.copy(alpha = 0.42f),
                tierColor.copy(alpha = 0.14f),
                Color.Transparent,
              ),
              startY = topY,
              endY = baseY,
            ),
          )

          // Outer glow stroke
          drawPath(
            path = linePath,
            color = ThrustCrimson.copy(alpha = 0.35f),
            style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
          // Core trajectory beam
          drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(
              colors = listOf(RadarCyan, ThrustCrimson, CreditGold)
            ),
            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )

          drawJetPlane(
            center = Offset(endX, endY),
            angleDeg = tangentAngleDeg,
            glowColor = ThrustCrimson,
            flameScale = flameFlicker,
          )
        }

        AviatorPhase.FLEW_AWAY -> {
          // Dotted fading trail indicating fly-away
          val flyAwayX = w * 1.08f
          val flyAwayY = -h * 0.08f
          drawLine(
            color = ThrustCrimson.copy(alpha = 0.35f),
            start = Offset(padL, baseY),
            end = Offset(flyAwayX, flyAwayY),
            strokeWidth = 3f,
          )
        }
      }
    }

    // Top telemetry strip inside canvas
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(CockpitPanel.copy(alpha = 0.85f))
          .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(
              when (state.phase) {
                AviatorPhase.FLYING -> CashoutEmerald
                AviatorPhase.COUNTDOWN -> RadarCyan
                AviatorPhase.FLEW_AWAY -> ThrustCrimson
              }
            )
        )
        Text(
          text = "FLT-${state.flightNumber}",
          style = MaterialTheme.typography.labelMedium,
          color = TextSecondary,
        )
      }

      if (state.phase == AviatorPhase.FLYING) {
        Text(
          text = "AIRBORNE",
          style = MaterialTheme.typography.labelSmall,
          color = CashoutEmerald,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CashoutEmerald.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }

    // Center hero readout
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when (state.phase) {
        AviatorPhase.COUNTDOWN -> {
          Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
              progress = {
                (state.countdownRemainingSec / state.countdownTotalSec).coerceIn(0f, 1f)
              },
              modifier = Modifier.size(82.dp),
              color = RadarCyan,
              trackColor = CockpitBorder,
              strokeWidth = 6.dp,
            )
            Text(
              text = String.format("%.1fs", state.countdownRemainingSec),
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                fontWeight = FontWeight.Bold,
              ),
              color = TextPrimary,
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "PREPARING FOR TAKEOFF",
            style = MaterialTheme.typography.labelLarge,
            color = RadarCyan,
            letterSpacing = 1.2.sp,
          )
        }

        AviatorPhase.FLYING -> {
          Text(
            text = Formatters.multiplier(state.currentMultiplier),
            style = MaterialTheme.typography.displayLarge.copy(
              fontSize = 52.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = when {
              state.currentMultiplier >= 10.0 -> CreditGold
              state.currentMultiplier >= 2.0 -> CashoutEmerald
              else -> TextPrimary
            },
          )
        }

        AviatorPhase.FLEW_AWAY -> {
          Text(
            text = "FLEW AWAY!",
            style = MaterialTheme.typography.titleMedium,
            color = ThrustCrimson,
            letterSpacing = 1.6.sp,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = Formatters.multiplier(state.lastCrashMultiplier),
            style = MaterialTheme.typography.displayLarge.copy(
              fontSize = 46.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = ThrustCrimson,
          )
        }
      }
    }
  }
}

private fun DrawScope.drawJetPlane(
  center: Offset,
  angleDeg: Float,
  glowColor: Color,
  flameScale: Float,
) {
  rotate(degrees = angleDeg, pivot = center) {
    // Plasma afterburner flame behind tail
    val flameLen = 30f * flameScale
    val flamePath = Path().apply {
      moveTo(center.x - 16f, center.y - 5f)
      lineTo(center.x - 16f - flameLen, center.y)
      lineTo(center.x - 16f, center.y + 5f)
      close()
    }
    drawPath(
      path = flamePath,
      brush = Brush.horizontalGradient(
        colors = listOf(Color.Transparent, CreditGold, Color.White),
        startX = center.x - 16f - flameLen,
        endX = center.x - 16f,
      ),
    )

    // Soft halo
    drawCircle(
      color = glowColor.copy(alpha = 0.25f),
      radius = 30f,
      center = center,
    )

    // Delta wing jet fuselage
    val wingPath = Path().apply {
      moveTo(center.x + 26f, center.y) // Nose
      lineTo(center.x + 2f, center.y - 7f)
      lineTo(center.x - 16f, center.y - 20f) // Top wingtip
      lineTo(center.x - 10f, center.y - 4f)
      lineTo(center.x - 20f, center.y - 8f) // Tail fin top
      lineTo(center.x - 17f, center.y)
      lineTo(center.x - 20f, center.y + 8f) // Tail fin bottom
      lineTo(center.x - 10f, center.y + 4f)
      lineTo(center.x - 16f, center.y + 20f) // Bottom wingtip
      lineTo(center.x + 2f, center.y + 7f)
      close()
    }
    drawPath(
      path = wingPath,
      brush = Brush.linearGradient(
        colors = listOf(ThrustCrimson, Color(0xFFFF6B81)),
        start = Offset(center.x - 18f, center.y - 16f),
        end = Offset(center.x + 26f, center.y + 8f),
      ),
    )

    // Cockpit canopy highlight
    val canopyPath = Path().apply {
      moveTo(center.x + 15f, center.y)
      lineTo(center.x + 4f, center.y - 3.5f)
      lineTo(center.x - 2f, center.y)
      lineTo(center.x + 4f, center.y + 3.5f)
      close()
    }
    drawPath(
      path = canopyPath,
      color = RadarCyan,
    )
  }
}
