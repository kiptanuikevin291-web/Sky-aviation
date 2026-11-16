package com.example.ui.games.mines

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.Formatters
import com.example.ui.components.StakeSelectorRow
import com.example.ui.theme.CashoutEmerald
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitPanel
import com.example.ui.theme.CockpitPanelElevated
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CreditGold
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThrustCrimson

@Composable
fun MinesScreen(
  viewModel: MinesViewModel,
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
        MinesEvent.GemFound -> if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        MinesEvent.MineHit -> if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        is MinesEvent.CashedOut -> {
          if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onShowMessage("Cashed out +${Formatters.credits(event.payout)} @ ${Formatters.multiplier(event.multiplier)}")
        }
        is MinesEvent.Info -> onShowMessage(event.message)
      }
    }
  }

  val mineOptions = listOf(1, 3, 5, 8, 12)
  val isPlaying = state.phase == MinesPhase.PLAYING

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg),
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 680.dp)
        .align(Alignment.TopCenter),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Telemetry HUD Row
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CockpitPanel)
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("MULTIPLIER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = when (state.phase) {
                MinesPhase.BUSTED -> "BUSTED"
                else -> Formatters.multiplier(state.currentMultiplier)
              },
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                fontWeight = FontWeight.Bold,
              ),
              color = when (state.phase) {
                MinesPhase.BUSTED -> ThrustCrimson
                MinesPhase.CASHED_OUT -> CashoutEmerald
                else -> TextPrimary
              },
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GEMS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = "${state.revealedPositions.size}/${25 - state.mineCount}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
              ),
              color = CashoutEmerald,
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text("NEXT GEM", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = Formatters.multiplier(state.nextMultiplier),
              style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
              ),
              color = RadarCyan,
            )
          }
        }
      }

      // 5x5 Radar Grid
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CockpitSurface)
            .border(1.5.dp, CockpitBorder, RoundedCornerShape(20.dp))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          for (row in 0 until 5) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              for (col in 0 until 5) {
                val index = row * 5 + col
                val isRevealed = index in state.revealedPositions
                val isMine = index in state.minePositions
                val showMine = (state.phase == MinesPhase.BUSTED || state.phase == MinesPhase.CASHED_OUT) && isMine
                val showUnrevealedGem = (state.phase == MinesPhase.BUSTED || state.phase == MinesPhase.CASHED_OUT) && !isMine && !isRevealed
                val isExploded = state.explodedTile == index

                MinesTile(
                  index = index,
                  isRevealedGem = isRevealed && !isMine,
                  isMineShown = showMine,
                  isDimmedGem = showUnrevealedGem,
                  isExploded = isExploded,
                  enabled = isPlaying && !isRevealed,
                  onClick = { viewModel.revealTile(index) },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      // Mine Count Selector
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "MINES ON GRID",
              style = MaterialTheme.typography.labelMedium,
              color = TextSecondary,
            )
            Text(
              text = "${state.mineCount} Mines • ${25 - state.mineCount} Gems",
              style = MaterialTheme.typography.labelMedium,
              color = ThrustCrimson,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            mineOptions.forEach { count ->
              val selected = state.mineCount == count
              Box(
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (selected) ThrustCrimson.copy(alpha = 0.22f) else CockpitPanelElevated)
                  .border(1.dp, if (selected) ThrustCrimson else CockpitBorder, RoundedCornerShape(12.dp))
                  .clickable(enabled = !isPlaying) { viewModel.updateMineCount(count) }
                  .padding(vertical = 10.dp)
                  .testTag("mines_count_$count"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "$count",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) ThrustCrimson else TextPrimary,
                )
              }
            }
          }

          StakeSelectorRow(
            stake = state.stake,
            maxCredits = credits,
            enabled = !isPlaying,
            onStakeChange = viewModel::updateStake,
            tagPrefix = "mines_stake",
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (isPlaying) {
              OutlinedButton(
                onClick = viewModel::pickRandomTile,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .height(54.dp)
                  .testTag("mines_random_button"),
              ) {
                Icon(Icons.Rounded.Casino, contentDescription = "Random tile", tint = RadarCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Random", color = RadarCyan)
              }

              val cashoutCredits = state.stake * state.currentMultiplier
              Button(
                onClick = viewModel::cashOut,
                enabled = state.revealedPositions.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = CashoutEmerald,
                  contentColor = ObsidianBg,
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(54.dp)
                  .testTag("mines_cashout_button"),
              ) {
                Text(
                  text = if (state.revealedPositions.isEmpty()) {
                    "PICK A TILE"
                  } else {
                    "CASH OUT ${Formatters.credits(cashoutCredits)}"
                  },
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
              }
            } else {
              Button(
                onClick = viewModel::startRound,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = CashoutEmerald,
                  contentColor = ObsidianBg,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(54.dp)
                  .testTag("mines_start_button"),
              ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "DEPLOY GRID • ${Formatters.credits(state.stake)}",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MinesTile(
  index: Int,
  isRevealedGem: Boolean,
  isMineShown: Boolean,
  isDimmedGem: Boolean,
  isExploded: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val targetBg = when {
    isExploded -> ThrustCrimson.copy(alpha = 0.35f)
    isMineShown -> ThrustCrimson.copy(alpha = 0.18f)
    isRevealedGem -> CashoutEmerald.copy(alpha = 0.22f)
    isDimmedGem -> CockpitPanel.copy(alpha = 0.65f)
    else -> CockpitPanelElevated
  }
  val bg by animateColorAsState(targetValue = targetBg, label = "tile_bg")
  val scale by animateFloatAsState(
    targetValue = if (isRevealedGem || isExploded) 1.0f else 0.96f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "tile_scale",
  )

  val borderColor = when {
    isExploded -> ThrustCrimson
    isMineShown -> ThrustCrimson.copy(alpha = 0.55f)
    isRevealedGem -> CashoutEmerald
    else -> CockpitBorder
  }

  Box(
    modifier = modifier
      .aspectRatio(1f)
      .scale(scale)
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .border(1.2.dp, borderColor, RoundedCornerShape(12.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .testTag("mines_tile_$index"),
    contentAlignment = Alignment.Center,
  ) {
    when {
      isRevealedGem -> Icon(
        imageVector = Icons.Rounded.Diamond,
        contentDescription = "Gem discovered",
        tint = CashoutEmerald,
        modifier = Modifier.size(26.dp),
      )
      isMineShown -> Icon(
        imageVector = Icons.Rounded.LocalFireDepartment,
        contentDescription = "Mine",
        tint = if (isExploded) CreditGold else ThrustCrimson,
        modifier = Modifier.size(26.dp),
      )
      isDimmedGem -> Icon(
        imageVector = Icons.Rounded.Diamond,
        contentDescription = "Unopened gem",
        tint = CashoutEmerald.copy(alpha = 0.28f),
        modifier = Modifier.size(20.dp),
      )
      else -> Icon(
        imageVector = Icons.Rounded.Radar,
        contentDescription = "Hidden grid sector",
        tint = TextSecondary.copy(alpha = 0.35f),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
