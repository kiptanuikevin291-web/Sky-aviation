package com.example.ui.wallet

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import com.example.data.ArcadeProfile
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
import kotlin.math.floor

enum class WalletTab {
  DEPOSIT,
  WITHDRAW,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSheet(
  initialTab: WalletTab,
  balance: Double,
  defaultPhone: String?,
  canClaimDailyBonus: Boolean,
  onDismiss: () -> Unit,
  onDeposit: (amount: Double, method: String, accountRef: String) -> Unit,
  onWithdraw: (amount: Double, method: String, accountRef: String) -> Unit,
  onClaimDailyBonus: () -> Unit,
  onRefillLow: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
  var amountText by remember(initialTab) { mutableStateOf(if (initialTab == WalletTab.DEPOSIT) "500" else "250") }
  var selectedMethod by remember { mutableStateOf("M-Pesa (Demo)") }
  var accountRef by remember(defaultPhone) { mutableStateOf(defaultPhone ?: "0712345678") }
  var validationError by remember { mutableStateOf<String?>(null) }

  val methods = listOf("M-Pesa (Demo)", "Airtel Money (Demo)", "Card (Demo)")
  val depositPresets = listOf(100, 250, 500, 1000, 2500, 5000)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = CockpitPanel,
    contentColor = TextPrimary,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "KSh Arcade Wallet",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
          Text(
            text = "Balance: ${Formatters.credits(balance)}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
              fontWeight = FontWeight.Bold,
            ),
            color = CreditGold,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CockpitSurface)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = "Demo wallet notice",
            tint = RadarCyan,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Demo Mode",
            style = MaterialTheme.typography.labelSmall,
            color = RadarCyan,
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(CockpitSurface)
          .border(1.dp, CockpitBorder, RoundedCornerShape(14.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        WalletTabButton(
          label = "Deposit",
          icon = Icons.Rounded.ArrowDownward,
          selected = selectedTab == WalletTab.DEPOSIT,
          accentColor = CashoutEmerald,
          onClick = {
            selectedTab = WalletTab.DEPOSIT
            validationError = null
          },
          tag = "wallet_tab_deposit",
          modifier = Modifier.weight(1f),
        )
        WalletTabButton(
          label = "Withdraw",
          icon = Icons.Rounded.ArrowUpward,
          selected = selectedTab == WalletTab.WITHDRAW,
          accentColor = ThrustCrimson,
          onClick = {
            selectedTab = WalletTab.WITHDRAW
            validationError = null
          },
          tag = "wallet_tab_withdraw",
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = amountText,
        onValueChange = { raw ->
          val cleaned = raw.filter { it.isDigit() || it == '.' }
          amountText = cleaned
          validationError = null
        },
        label = { Text("Amount (KSh)") },
        prefix = { Text("KSh ", color = CreditGold, fontWeight = FontWeight.Bold) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = if (selectedTab == WalletTab.DEPOSIT) CashoutEmerald else ThrustCrimson,
          unfocusedBorderColor = CockpitBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedLabelColor = RadarCyan,
          unfocusedLabelColor = TextSecondary,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("wallet_amount_input"),
      )

      if (selectedTab == WalletTab.DEPOSIT) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          depositPresets.forEach { preset ->
            val selected = amountText == preset.toString()
            Box(
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) CashoutEmerald.copy(alpha = 0.2f) else CockpitPanelElevated)
                .border(1.dp, if (selected) CashoutEmerald else CockpitBorder, RoundedCornerShape(10.dp))
                .clickable {
                  amountText = preset.toString()
                  validationError = null
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("wallet_deposit_preset_$preset"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "KSh $preset",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) CashoutEmerald else TextPrimary,
              )
            }
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(0.25 to "25%", 0.5 to "50%", 0.75 to "75%", 1.0 to "MAX").forEach { (frac, label) ->
            Box(
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(CockpitPanelElevated)
                .border(1.dp, CockpitBorder, RoundedCornerShape(10.dp))
                .clickable {
                  val computed = floor(balance * frac).toInt().coerceAtLeast(0)
                  amountText = computed.toString()
                  validationError = null
                }
                .padding(vertical = 8.dp)
                .testTag("wallet_withdraw_pct_${label.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(text = label, style = MaterialTheme.typography.labelMedium, color = RadarCyan)
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("CHANNEL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          methods.forEach { method ->
            val selected = selectedMethod == method
            Box(
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) RadarCyan.copy(alpha = 0.2f) else CockpitPanelElevated)
                .border(1.dp, if (selected) RadarCyan else CockpitBorder, RoundedCornerShape(10.dp))
                .clickable { selectedMethod = method }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("wallet_method_${method.substringBefore(" ").lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = method,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) RadarCyan else TextPrimary,
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = accountRef,
        onValueChange = { accountRef = it },
        label = { Text("Phone / Account Reference (Demo)") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = RadarCyan,
          unfocusedBorderColor = CockpitBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedLabelColor = RadarCyan,
          unfocusedLabelColor = TextSecondary,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("wallet_account_input"),
      )

      validationError?.let { err ->
        Text(
          text = err,
          style = MaterialTheme.typography.bodyMedium,
          color = ThrustCrimson,
        )
      }

      val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
      Button(
        onClick = {
          if (selectedTab == WalletTab.DEPOSIT) {
            if (parsedAmount < 10.0) {
              validationError = "Enter at least KSh 10 to deposit."
            } else {
              onDeposit(parsedAmount, selectedMethod, accountRef)
            }
          } else {
            when {
              parsedAmount < 50.0 -> validationError = "Minimum withdrawal is KSh 50."
              parsedAmount > balance + 0.001 -> validationError = "Amount exceeds your KSh balance."
              else -> onWithdraw(parsedAmount, selectedMethod, accountRef)
            }
          }
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (selectedTab == WalletTab.DEPOSIT) CashoutEmerald else ThrustCrimson,
          contentColor = if (selectedTab == WalletTab.DEPOSIT) ObsidianBg else TextPrimary,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("wallet_confirm_button"),
      ) {
        Icon(
          imageVector = if (selectedTab == WalletTab.DEPOSIT) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
          contentDescription = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (selectedTab == WalletTab.DEPOSIT) {
            "DEPOSIT ${Formatters.credits(parsedAmount)}"
          } else {
            "WITHDRAW ${Formatters.credits(parsedAmount)}"
          },
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            onClaimDailyBonus()
            onDismiss()
          },
          enabled = canClaimDailyBonus,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("sheet_daily_bonus_button"),
        ) {
          Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = CreditGold, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (canClaimDailyBonus) "+KSh 500 Daily" else "Claimed",
            color = if (canClaimDailyBonus) CreditGold else TextSecondary,
          )
        }

        OutlinedButton(
          onClick = {
            onRefillLow()
            onDismiss()
          },
          enabled = balance < ArcadeProfile.REFILL_THRESHOLD,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("sheet_refill_button"),
        ) {
          Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = CashoutEmerald, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Refill +KSh 1,000",
            color = if (balance < ArcadeProfile.REFILL_THRESHOLD) CashoutEmerald else TextSecondary,
          )
        }
      }

      Text(
        text = "Note: Simulated KSh wallet for local gameplay & testing only. No real-money charges or payouts are processed.",
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }
  }
}

@Composable
private fun WalletTabButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  accentColor: androidx.compose.ui.graphics.Color,
  onClick: () -> Unit,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(10.dp))
      .background(if (selected) accentColor.copy(alpha = 0.2f) else CockpitSurface)
      .border(
        1.dp,
        if (selected) accentColor else CockpitSurface,
        RoundedCornerShape(10.dp),
      )
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp)
      .testTag(tag),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) accentColor else TextSecondary,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) accentColor else TextSecondary,
    )
  }
}
