package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ArcadeProfile
import com.example.ui.theme.CashoutEmerald
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitPanel
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CreditGold
import com.example.ui.theme.CreditGoldSoft
import com.example.ui.theme.ThrustCrimson

@Composable
fun TopTelemetryBar(
  title: String,
  credits: Double,
  showBackButton: Boolean = false,
  onBackClick: () -> Unit = {},
  onCreditsClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val animatedCredits by animateFloatAsState(
    targetValue = credits.toFloat(),
    animationSpec = tween(durationMillis = 350),
    label = "animated_credits",
  )
  val isLowCredits = credits < ArcadeProfile.REFILL_THRESHOLD
  val pillBorder by animateColorAsState(
    targetValue = if (isLowCredits) CashoutEmerald else CockpitBorder,
    label = "pill_border",
  )

  Surface(
    color = CockpitSurface.copy(alpha = 0.94f),
    tonalElevation = 4.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        if (showBackButton) {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("top_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Navigate back",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
        } else {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(ThrustCrimson, CreditGold)
                )
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.FlightTakeoff,
              contentDescription = "SkyCrash emblem",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(50))
          .background(if (isLowCredits) CashoutEmerald.copy(alpha = 0.14f) else CockpitPanel)
          .border(1.dp, pillBorder, RoundedCornerShape(50))
          .clickable(onClick = onCreditsClick)
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("credits_pill_button"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(CreditGoldSoft),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Toll,
            contentDescription = "Virtual credits",
            tint = CreditGold,
            modifier = Modifier.size(14.dp),
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = Formatters.credits(animatedCredits.toDouble()),
          style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
            fontWeight = FontWeight.Bold,
          ),
          color = CreditGold,
        )
        if (isLowCredits) {
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Refill credits",
            tint = CashoutEmerald,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}
