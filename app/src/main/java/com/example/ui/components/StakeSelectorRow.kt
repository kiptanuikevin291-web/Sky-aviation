package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitPanelElevated
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CreditGold
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.TextSecondary

@Composable
fun StakeSelectorRow(
  stake: Double,
  maxCredits: Double,
  enabled: Boolean,
  onStakeChange: (Double) -> Unit,
  tagPrefix: String = "stake",
  modifier: Modifier = Modifier,
) {
  val presets = listOf(10.0, 25.0, 50.0, 100.0, 250.0)

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(CockpitSurface)
        .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
        .padding(horizontal = 6.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = { onStakeChange((stake - 10.0).coerceAtLeast(10.0)) },
        enabled = enabled && stake > 10.0,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("${tagPrefix}_decrease_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Remove,
          contentDescription = "Decrease stake",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "STAKE",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
        Text(
          text = Formatters.credits(stake),
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
            fontWeight = FontWeight.Bold,
          ),
          color = CreditGold,
        )
      }

      IconButton(
        onClick = {
          val next = (stake + 10.0).coerceAtMost(maxCredits.coerceAtLeast(10.0))
          onStakeChange(next)
        },
        enabled = enabled && stake < maxCredits,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("${tagPrefix}_increase_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = "Increase stake",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      presets.forEach { preset ->
        val selected = stake == preset
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) RadarCyan.copy(alpha = 0.2f) else CockpitPanelElevated)
            .border(
              1.dp,
              if (selected) RadarCyan else CockpitBorder,
              RoundedCornerShape(10.dp),
            )
            .clickable(enabled = enabled) {
              onStakeChange(preset.coerceAtMost(maxCredits.coerceAtLeast(10.0)))
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("${tagPrefix}_preset_${preset.toInt()}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "${preset.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) RadarCyan else MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Box(
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(10.dp))
          .background(CockpitPanelElevated)
          .border(1.dp, CockpitBorder, RoundedCornerShape(10.dp))
          .clickable(enabled = enabled) {
            onStakeChange((stake / 2.0).coerceAtLeast(10.0).let { (it / 5.0).toInt() * 5.0 }.coerceAtLeast(10.0))
          }
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("${tagPrefix}_half_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(text = "½", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
      }

      Box(
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(10.dp))
          .background(CockpitPanelElevated)
          .border(1.dp, CockpitBorder, RoundedCornerShape(10.dp))
          .clickable(enabled = enabled) {
            val doubled = (stake * 2.0).coerceAtMost(maxCredits.coerceAtLeast(10.0))
            onStakeChange(doubled)
          }
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("${tagPrefix}_double_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(text = "2x", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
      }
    }
  }
}
