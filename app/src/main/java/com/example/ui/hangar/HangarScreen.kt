package com.example.ui.hangar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.ArcadeProfile
import com.example.data.GameRoundEntity
import com.example.data.GameType
import com.example.data.WalletTransactionEntity
import com.example.ui.ArcadeUiState
import com.example.ui.components.Formatters
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

private enum class LedgerSection {
  GAMES,
  WALLET,
}

@Composable
fun HangarScreen(
  uiState: ArcadeUiState,
  onOpenDeposit: () -> Unit,
  onOpenWithdraw: () -> Unit,
  onClaimDaily: () -> Unit,
  onRefillLow: () -> Unit,
  onToggleHaptics: (Boolean) -> Unit,
  onClearHistory: () -> Unit,
  onChangePassword: (phone: String, current: String, new: String, onResult: (Boolean, String?) -> Unit) -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSection by remember { mutableStateOf(LedgerSection.WALLET) }
  var selectedFilter by remember { mutableStateOf<GameType?>(null) }
  var showPasswordDialog by remember { mutableStateOf(false) }

  val filteredRounds = remember(uiState.recentRounds, selectedFilter) {
    if (selectedFilter == null) {
      uiState.recentRounds
    } else {
      uiState.recentRounds.filter { it.gameType == selectedFilter?.name }
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg),
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 840.dp)
        .align(Alignment.TopCenter),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Pilot Login Info Card
      item {
        val loggedInPhone = uiState.profile.loggedInPhone
        val pilotName = uiState.profile.loggedInPilotName ?: "Guest Pilot"
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CockpitPanel)
            .border(1.dp, RadarCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(RadarCyan.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Person,
                  contentDescription = "Pilot profile",
                  tint = RadarCyan,
                  modifier = Modifier.size(24.dp),
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = pilotName,
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Rounded.Phone,
                    contentDescription = null,
                    tint = if (loggedInPhone != null) CashoutEmerald else TextSecondary,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = loggedInPhone ?: "No phone linked (Guest)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (loggedInPhone != null) CashoutEmerald else TextSecondary,
                  )
                }
              }
            }

            TextButton(
              onClick = onLogout,
              modifier = Modifier.testTag("hangar_logout_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = if (loggedInPhone != null) "Sign out" else "Sign in",
                tint = if (loggedInPhone != null) ThrustCrimson else RadarCyan,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (loggedInPhone != null) "Sign Out" else "Sign In",
                color = if (loggedInPhone != null) ThrustCrimson else RadarCyan,
              )
            }
          }

          if (loggedInPhone != null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CockpitSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Rounded.Lock,
                  contentDescription = null,
                  tint = TextSecondary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Password: ••••••••",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
              TextButton(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.testTag("hangar_change_password_button"),
              ) {
                Text("Update", color = RadarCyan)
              }
            }
          }
        }
      }

      // KSh Bankroll + Deposit / Withdraw Card
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CockpitPanel)
            .border(1.5.dp, CreditGold.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("DEMO BALANCE (KSH)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
              Text(
                text = Formatters.credits(uiState.profile.credits),
                style = MaterialTheme.typography.displayMedium,
                color = CreditGold,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(CockpitSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "Demo KSh balance notice",
                tint = RadarCyan,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Demo Wallet",
                style = MaterialTheme.typography.labelSmall,
                color = RadarCyan,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onOpenDeposit,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = CashoutEmerald,
                contentColor = ObsidianBg,
              ),
              modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("hangar_deposit_button"),
            ) {
              Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Deposit", style = MaterialTheme.typography.labelLarge)
            }

            Button(
              onClick = onOpenWithdraw,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = ThrustCrimson,
                contentColor = TextPrimary,
              ),
              modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("hangar_withdraw_button"),
            ) {
              Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Withdraw", style = MaterialTheme.typography.labelLarge)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onClaimDaily,
              enabled = uiState.canClaimDailyBonus,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("hangar_daily_bonus_button"),
            ) {
              Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = CreditGold, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (uiState.canClaimDailyBonus) "+KSh 500 Daily" else "Claimed Today",
                color = if (uiState.canClaimDailyBonus) CreditGold else TextSecondary,
                style = MaterialTheme.typography.labelMedium,
              )
            }

            OutlinedButton(
              onClick = onRefillLow,
              enabled = uiState.profile.credits < ArcadeProfile.REFILL_THRESHOLD,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("hangar_refill_button"),
            ) {
              Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CashoutEmerald, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Refill +KSh 1,000",
                color = if (uiState.profile.credits < ArcadeProfile.REFILL_THRESHOLD) CashoutEmerald else TextSecondary,
                style = MaterialTheme.typography.labelMedium,
              )
            }
          }
        }
      }

      // Stats Grid
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          HangarStatCard(
            title = "TOTAL ROUNDS",
            value = "${uiState.stats.totalRounds}",
            valueColor = TextPrimary,
            modifier = Modifier.weight(1f),
          )
          HangarStatCard(
            title = "BEST MULTIPLIER",
            value = Formatters.multiplier(uiState.stats.bestMultiplier),
            valueColor = RadarCyan,
            modifier = Modifier.weight(1f),
          )
          HangarStatCard(
            title = "TOP PAYOUT",
            value = Formatters.credits(uiState.stats.bestPayout),
            valueColor = CashoutEmerald,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Haptics Setting
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CockpitPanel)
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Vibration, contentDescription = null, tint = RadarCyan)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Cockpit Haptics", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
              Text("Tactile feedback on cashouts & impacts", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
          }
          Switch(
            checked = uiState.profile.hapticsEnabled,
            onCheckedChange = onToggleHaptics,
            colors = SwitchDefaults.colors(
              checkedThumbColor = ObsidianBg,
              checkedTrackColor = CashoutEmerald,
            ),
            modifier = Modifier.testTag("hangar_haptics_switch"),
          )
        }
      }

      // Ledger Header + Section Switch
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Activity Ledger", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
          }
          if (uiState.recentRounds.isNotEmpty() || uiState.walletTransactions.isNotEmpty()) {
            TextButton(
              onClick = onClearHistory,
              modifier = Modifier.testTag("hangar_clear_history_button"),
            ) {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear history", tint = ThrustCrimson, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear", color = ThrustCrimson)
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CockpitSurface)
            .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          SectionTabChip(
            label = "Deposits & Withdrawals (${uiState.walletTransactions.size})",
            selected = selectedSection == LedgerSection.WALLET,
            onClick = { selectedSection = LedgerSection.WALLET },
            tag = "hangar_section_wallet",
            modifier = Modifier.weight(1f),
          )
          SectionTabChip(
            label = "Game Rounds (${uiState.recentRounds.size})",
            selected = selectedSection == LedgerSection.GAMES,
            onClick = { selectedSection = LedgerSection.GAMES },
            tag = "hangar_section_games",
            modifier = Modifier.weight(1f),
          )
        }
      }

      if (selectedSection == LedgerSection.WALLET) {
        if (uiState.walletTransactions.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CockpitPanel)
                .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "No deposits or withdrawals yet. Tap Deposit or Withdraw above to test your KSh wallet.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        } else {
          items(uiState.walletTransactions, key = { it.id }) { tx ->
            WalletTransactionRow(tx)
          }
        }
      } else {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChipBox(
              label = "All",
              selected = selectedFilter == null,
              onClick = { selectedFilter = null },
              tag = "hangar_filter_all",
            )
            GameType.entries.forEach { game ->
              FilterChipBox(
                label = game.displayName,
                selected = selectedFilter == game,
                onClick = { selectedFilter = game },
                tag = "hangar_filter_${game.route}",
              )
            }
          }
        }

        if (filteredRounds.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CockpitPanel)
                .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "No rounds logged yet. Launch Aviator or an Arcade game to build your flight record!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }
        } else {
          items(filteredRounds, key = { it.id }) { round ->
            LedgerRoundRow(round)
          }
        }
      }
    }
  }

  val phoneForDialog = uiState.profile.loggedInPhone
  if (showPasswordDialog && phoneForDialog != null) {
    ChangePasswordDialog(
      phoneNumber = phoneForDialog,
      onDismiss = { showPasswordDialog = false },
      onSubmit = { currentPw, newPw, onResult ->
        onChangePassword(phoneForDialog, currentPw, newPw) { ok, err ->
          if (ok) showPasswordDialog = false
          onResult(ok, err)
        }
      },
    )
  }
}

@Composable
private fun ChangePasswordDialog(
  phoneNumber: String,
  onDismiss: () -> Unit,
  onSubmit: (current: String, new: String, onResult: (Boolean, String?) -> Unit) -> Unit,
) {
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = CockpitPanel,
    titleContentColor = TextPrimary,
    textContentColor = TextSecondary,
    title = { Text("Update Password") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Account: $phoneNumber", style = MaterialTheme.typography.labelMedium, color = RadarCyan)
        OutlinedTextField(
          value = currentPassword,
          onValueChange = {
            currentPassword = it
            errorText = null
          },
          label = { Text("Current Password") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RadarCyan,
            unfocusedBorderColor = CockpitBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
          ),
          modifier = Modifier.testTag("change_pw_current_input"),
        )
        OutlinedTextField(
          value = newPassword,
          onValueChange = {
            newPassword = it
            errorText = null
          },
          label = { Text("New Password (min 6 chars)") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RadarCyan,
            unfocusedBorderColor = CockpitBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
          ),
          modifier = Modifier.testTag("change_pw_new_input"),
        )
        errorText?.let { err ->
          Text(err, color = ThrustCrimson, style = MaterialTheme.typography.bodyMedium)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmit(currentPassword, newPassword) { _, err ->
            errorText = err
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CashoutEmerald, contentColor = ObsidianBg),
        modifier = Modifier.testTag("change_pw_save_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    },
  )
}

@Composable
private fun SectionTabChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(9.dp))
      .background(if (selected) RadarCyan.copy(alpha = 0.2f) else CockpitSurface)
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 8.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) RadarCyan else TextSecondary,
    )
  }
}

@Composable
private fun WalletTransactionRow(tx: WalletTransactionEntity) {
  val isDeposit = tx.type == "DEPOSIT"
  val accent = if (isDeposit) CashoutEmerald else ThrustCrimson
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(CockpitPanel)
      .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (isDeposit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
          contentDescription = tx.type,
          tint = accent,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (isDeposit) "Deposit" else "Withdraw",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = tx.referenceCode,
            style = MaterialTheme.typography.labelSmall,
            color = RadarCyan,
          )
        }
        Text(
          text = "${tx.method} • ${tx.accountRef}",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        Text(
          text = "${tx.status} • ${Formatters.shortTime(tx.timestampMillis)}",
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
    }

    Text(
      text = if (isDeposit) "+${Formatters.credits(tx.amount)}" else "-${Formatters.credits(tx.amount)}",
      style = MaterialTheme.typography.titleMedium.copy(
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
        fontWeight = FontWeight.Bold,
      ),
      color = accent,
    )
  }
}

@Composable
private fun HangarStatCard(
  title: String,
  value: String,
  valueColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(CockpitPanel)
      .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
        fontWeight = FontWeight.Bold,
      ),
      color = valueColor,
    )
  }
}

@Composable
private fun FilterChipBox(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(50))
      .background(if (selected) RadarCyan.copy(alpha = 0.22f) else CockpitPanelElevated)
      .border(1.dp, if (selected) RadarCyan else CockpitBorder, RoundedCornerShape(50))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) RadarCyan else TextPrimary,
    )
  }
}

@Composable
private fun LedgerRoundRow(round: GameRoundEntity) {
  val game = GameType.entries.firstOrNull { it.name == round.gameType }
  val isWin = round.payout > 0.0
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(CockpitPanel)
      .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = game?.displayName ?: round.gameType,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = game?.accentColor ?: RadarCyan,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = Formatters.shortTime(round.timestampMillis),
          style = MaterialTheme.typography.labelSmall,
          color = TextSecondary,
        )
      }
      Text(
        text = round.detailSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary,
      )
      Text(
        text = "Stake ${Formatters.credits(round.wager)}",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }

    Text(
      text = if (isWin) "+${Formatters.credits(round.payout)}" else "-${Formatters.credits(round.wager)}",
      style = MaterialTheme.typography.titleMedium.copy(
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
        fontWeight = FontWeight.Bold,
      ),
      color = if (isWin) CashoutEmerald else ThrustCrimson,
    )
  }
}
