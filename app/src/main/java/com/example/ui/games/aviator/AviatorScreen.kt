package com.example.ui.games.aviator

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GameRoundEntity
import com.example.ui.components.Formatters
import com.example.ui.components.StakeSelectorRow
import com.example.ui.theme.CashoutEmerald
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitPanel
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CreditGold
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PulseViolet
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThrustCrimson
import kotlinx.coroutines.delay

@Composable
fun AviatorScreen(
  viewModel: AviatorViewModel,
  credits: Double,
  hapticsEnabled: Boolean,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val flightHistory by viewModel.playerFlightHistory.collectAsStateWithLifecycle()
  val haptics = LocalHapticFeedback.current
  var bannerWin by remember { mutableStateOf<AviatorEvent.CashoutWin?>(null) }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        is AviatorEvent.CashoutWin -> {
          if (hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          }
          bannerWin = event
          delay(2200L)
          if (bannerWin == event) bannerWin = null
        }
        is AviatorEvent.PlaneCrashed -> {
          if (hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          }
        }
        is AviatorEvent.Info -> onShowMessage(event.message)
      }
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg),
  ) {
    val isWide = maxWidth >= 700.dp

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 920.dp)
        .align(Alignment.TopCenter),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Recent Flight Multipliers Strip
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          items(state.recentMultipliers) { mult ->
            val pillColor = when {
              mult >= 10.0 -> CreditGold
              mult >= 2.0 -> PulseViolet
              else -> RadarCyan
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(pillColor.copy(alpha = 0.14f))
                .border(1.dp, pillColor.copy(alpha = 0.45f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
              Text(
                text = Formatters.multiplier(mult),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = pillColor,
              )
            }
          }
        }
      }

      // Cashout Celebration Banner
      item {
        AnimatedVisibility(visible = bannerWin != null) {
          bannerWin?.let { win ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CashoutEmerald.copy(alpha = 0.16f))
                .border(1.dp, CashoutEmerald, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = "Cashout success",
                  tint = CashoutEmerald,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Cashed Out @ ${Formatters.multiplier(win.multiplier)}",
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
              }
              Text(
                text = "+${Formatters.credits(win.payout)}",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                  fontWeight = FontWeight.Bold,
                ),
                color = CashoutEmerald,
              )
            }
          }
        }
      }

      // Main Aviator Radar & Jet Canvas
      item {
        AviatorCanvas(
          state = state,
          modifier = Modifier.height(if (isWide) 280.dp else 235.dp),
        )
      }

      // Dual Bet Controls
      item {
        if (isWide && state.showSecondPanel) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            AviatorBetPanel(
              label = "PANEL A",
              slot = state.slotA,
              phase = state.phase,
              currentMultiplier = state.currentMultiplier,
              maxCredits = credits,
              onStakeChange = { viewModel.updateStake(0, it) },
              onActionClick = { viewModel.onPrimarySlotAction(0) },
              onToggleAutoCashout = { viewModel.toggleAutoCashout(0, it) },
              onAutoCashoutTargetChange = { viewModel.updateAutoCashoutTarget(0, it) },
              onToggleAutoBet = { viewModel.toggleAutoBet(0, it) },
              modifier = Modifier.weight(1f),
            )
            AviatorBetPanel(
              label = "PANEL B",
              slot = state.slotB,
              phase = state.phase,
              currentMultiplier = state.currentMultiplier,
              maxCredits = credits,
              onStakeChange = { viewModel.updateStake(1, it) },
              onActionClick = { viewModel.onPrimarySlotAction(1) },
              onToggleAutoCashout = { viewModel.toggleAutoCashout(1, it) },
              onAutoCashoutTargetChange = { viewModel.updateAutoCashoutTarget(1, it) },
              onToggleAutoBet = { viewModel.toggleAutoBet(1, it) },
              modifier = Modifier.weight(1f),
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AviatorBetPanel(
              label = "PANEL A",
              slot = state.slotA,
              phase = state.phase,
              currentMultiplier = state.currentMultiplier,
              maxCredits = credits,
              onStakeChange = { viewModel.updateStake(0, it) },
              onActionClick = { viewModel.onPrimarySlotAction(0) },
              onToggleAutoCashout = { viewModel.toggleAutoCashout(0, it) },
              onAutoCashoutTargetChange = { viewModel.updateAutoCashoutTarget(0, it) },
              onToggleAutoBet = { viewModel.toggleAutoBet(0, it) },
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
            ) {
              TextButton(
                onClick = viewModel::toggleSecondPanel,
                modifier = Modifier.testTag("toggle_second_panel_button"),
              ) {
                Icon(
                  imageVector = if (state.showSecondPanel) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                  contentDescription = "Toggle second bet panel",
                  tint = RadarCyan,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (state.showSecondPanel) "Hide Panel B" else "Add 2nd Bet Panel",
                  color = RadarCyan,
                )
              }
            }

            AnimatedVisibility(visible = state.showSecondPanel) {
              AviatorBetPanel(
                label = "PANEL B",
                slot = state.slotB,
                phase = state.phase,
                currentMultiplier = state.currentMultiplier,
                maxCredits = credits,
                onStakeChange = { viewModel.updateStake(1, it) },
                onActionClick = { viewModel.onPrimarySlotAction(1) },
                onToggleAutoCashout = { viewModel.toggleAutoCashout(1, it) },
                onAutoCashoutTargetChange = { viewModel.updateAutoCashoutTarget(1, it) },
                onToggleAutoBet = { viewModel.toggleAutoBet(1, it) },
              )
            }
          }
        }
      }

      // Player's Flight Log
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = "Your flight log",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Your Recent Flights",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
          )
        }
      }

      if (flightHistory.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(CockpitPanel)
              .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
              .padding(20.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Rounded.FlightTakeoff,
                contentDescription = "No flights yet",
                tint = RadarCyan,
                modifier = Modifier.size(28.dp),
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Place a bet on Panel A or B to log your first flight!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        }
      } else {
        items(flightHistory.take(8), key = { it.id }) { round ->
          FlightLogRow(round)
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun AviatorBetPanel(
  label: String,
  slot: BetSlotState,
  phase: AviatorPhase,
  currentMultiplier: Double,
  maxCredits: Double,
  onStakeChange: (Double) -> Unit,
  onActionClick: () -> Unit,
  onToggleAutoCashout: (Boolean) -> Unit,
  onAutoCashoutTargetChange: (Double) -> Unit,
  onToggleAutoBet: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tagSuffix = if (slot.slotIndex == 0) "a" else "b"
  val canEditStake = slot.status == BetSlotStatus.IDLE ||
    slot.status == BetSlotStatus.CASHED_OUT ||
    slot.status == BetSlotStatus.LOST

  val buttonColor: Color
  val buttonLabel: String
  val buttonSubtext: String?

  when (slot.status) {
    BetSlotStatus.ACTIVE -> {
      if (phase == AviatorPhase.FLYING) {
        val livePayout = slot.lockedStake * currentMultiplier
        buttonColor = CashoutEmerald
        buttonLabel = "CASH OUT"
        buttonSubtext = Formatters.credits(livePayout)
      } else {
        buttonColor = CockpitBorder
        buttonLabel = "WAITING..."
        buttonSubtext = null
      }
    }
    BetSlotStatus.QUEUED -> {
      buttonColor = ThrustCrimson
      buttonLabel = "CANCEL BET"
      buttonSubtext = if (phase == AviatorPhase.FLYING) "Queued for next takeoff" else "Locked for takeoff"
    }
    BetSlotStatus.IDLE, BetSlotStatus.CASHED_OUT, BetSlotStatus.LOST -> {
      buttonColor = RadarCyan
      buttonLabel = if (phase == AviatorPhase.COUNTDOWN) "BET NOW" else "BET NEXT FLIGHT"
      buttonSubtext = Formatters.credits(slot.stake)
    }
  }

  Column(
    modifier = modifier
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
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { onToggleAutoBet(!slot.autoBetEnabled) }
          .padding(horizontal = 6.dp, vertical = 2.dp)
          .testTag("aviator_auto_bet_$tagSuffix"),
      ) {
        Text(
          text = "Auto-Bet",
          style = MaterialTheme.typography.labelSmall,
          color = if (slot.autoBetEnabled) RadarCyan else TextSecondary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Switch(
          checked = slot.autoBetEnabled,
          onCheckedChange = onToggleAutoBet,
          colors = SwitchDefaults.colors(
            checkedThumbColor = ObsidianBg,
            checkedTrackColor = RadarCyan,
          ),
        )
      }
    }

    StakeSelectorRow(
      stake = slot.stake,
      maxCredits = maxCredits,
      enabled = canEditStake,
      onStakeChange = onStakeChange,
      tagPrefix = "aviator_stake_$tagSuffix",
    )

    // Auto-Cashout Control Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(CockpitSurface)
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
          checked = slot.autoCashoutEnabled,
          onCheckedChange = onToggleAutoCashout,
          colors = SwitchDefaults.colors(
            checkedThumbColor = ObsidianBg,
            checkedTrackColor = CashoutEmerald,
          ),
          modifier = Modifier.testTag("aviator_auto_cashout_switch_$tagSuffix"),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Auto Cashout",
          style = MaterialTheme.typography.bodyMedium,
          color = TextPrimary,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { onAutoCashoutTargetChange(slot.autoCashoutTarget - 0.25) },
          enabled = slot.autoCashoutTarget > 1.25,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("aviator_autocash_dec_$tagSuffix"),
        ) {
          Icon(Icons.Rounded.Remove, contentDescription = "Decrease auto cashout", tint = TextPrimary)
        }
        Text(
          text = Formatters.multiplier(slot.autoCashoutTarget),
          style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
          ),
          color = if (slot.autoCashoutEnabled) CashoutEmerald else TextSecondary,
        )
        IconButton(
          onClick = { onAutoCashoutTargetChange(slot.autoCashoutTarget + 0.25) },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("aviator_autocash_inc_$tagSuffix"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "Increase auto cashout", tint = TextPrimary)
        }
      }
    }

    Button(
      onClick = onActionClick,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = buttonColor,
        contentColor = if (buttonColor == ThrustCrimson) TextPrimary else ObsidianBg,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .testTag("aviator_action_button_$tagSuffix"),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = buttonLabel,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        if (buttonSubtext != null) {
          Text(
            text = buttonSubtext,
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }
}

@Composable
private fun FlightLogRow(round: GameRoundEntity) {
  val isWin = round.payout > 0.0
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(CockpitPanel)
      .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = round.detailSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary,
      )
      Text(
        text = "Stake ${Formatters.credits(round.wager)} • ${Formatters.shortTime(round.timestampMillis)}",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }
    Text(
      text = if (isWin) "+${Formatters.credits(round.payout)}" else "-${Formatters.credits(round.wager)}",
      style = MaterialTheme.typography.labelLarge.copy(
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
        fontWeight = FontWeight.Bold,
      ),
      color = if (isWin) CashoutEmerald else ThrustCrimson,
    )
  }
}
