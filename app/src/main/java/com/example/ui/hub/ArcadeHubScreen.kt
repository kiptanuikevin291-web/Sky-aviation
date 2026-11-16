package com.example.ui.hub

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ArcadeProfile
import com.example.data.GameType
import com.example.ui.ArcadeUiState
import com.example.ui.components.Formatters
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

@Composable
fun ArcadeHubScreen(
  uiState: ArcadeUiState,
  onSelectGame: (GameType) -> Unit,
  onClaimDaily: () -> Unit,
  onRefillLow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val companionGames = listOf(GameType.MINES, GameType.PLINKO, GameType.WHEEL, GameType.DICE)

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg),
  ) {
    val isWide = maxWidth >= 680.dp

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 920.dp)
        .align(Alignment.TopCenter),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Flagship Aviator Hero Card
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, ThrustCrimson.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .clickable { onSelectGame(GameType.AVIATOR) }
            .testTag("hub_aviator_hero_card"),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_aviator_hero),
            contentDescription = "SkyCrash Aviator cockpit banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    ObsidianBg.copy(alpha = 0.25f),
                    ObsidianBg.copy(alpha = 0.88f),
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ThrustCrimson)
                .padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.FlightTakeoff,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "FLAGSHIP CRASH",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "SkyCrash Aviator",
                  style = MaterialTheme.typography.headlineMedium,
                  color = TextPrimary,
                )
                Text(
                  text = GameType.AVIATOR.tagline,
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Button(
                onClick = { onSelectGame(GameType.AVIATOR) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = CashoutEmerald,
                  contentColor = ObsidianBg,
                ),
                modifier = Modifier.testTag("hub_launch_aviator_button"),
              ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("FLY", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }

      // Bonus / Low-Balance Refill Card
      if (uiState.profile.credits < ArcadeProfile.REFILL_THRESHOLD || uiState.canClaimDailyBonus) {
        item {
          val isRefill = uiState.profile.credits < ArcadeProfile.REFILL_THRESHOLD
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(
                if (isRefill) CashoutEmerald.copy(alpha = 0.14f) else CreditGold.copy(alpha = 0.14f)
              )
              .border(
                1.dp,
                if (isRefill) CashoutEmerald.copy(alpha = 0.55f) else CreditGold.copy(alpha = 0.55f),
                RoundedCornerShape(18.dp),
              )
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = if (isRefill) Icons.Rounded.AutoAwesome else Icons.Rounded.CardGiftcard,
                contentDescription = null,
                tint = if (isRefill) CashoutEmerald else CreditGold,
                modifier = Modifier.size(28.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = if (isRefill) "Low Balance Refill" else "Daily Hangar Bonus Ready",
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                )
                Text(
                  text = if (isRefill) "Claim +KSh 1,000 free demo refill" else "Collect +KSh 500 demo bonus",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextSecondary,
                )
              }
            }
            Button(
              onClick = if (isRefill) onRefillLow else onClaimDaily,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isRefill) CashoutEmerald else CreditGold,
                contentColor = ObsidianBg,
              ),
              modifier = Modifier.testTag(if (isRefill) "hub_refill_button" else "hub_daily_bonus_button"),
            ) {
              Text("CLAIM", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      }

      // Companion Arcade Games Grid Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "More Arcade Games",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
          Text(
            text = "4 MODES",
            style = MaterialTheme.typography.labelMedium,
            color = RadarCyan,
          )
        }
      }

      // 2x2 (or 4-col wide) Game Cards
      item {
        val columns = if (isWide) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          companionGames.chunked(columns).forEach { rowGames ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              rowGames.forEach { game ->
                ArcadeGameCard(
                  game = game,
                  icon = iconForGame(game),
                  onClick = { onSelectGame(game) },
                  modifier = Modifier.weight(1f),
                )
              }
              if (rowGames.size < columns) {
                repeat(columns - rowGames.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }

      // Mini Telemetry Summary Strip
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CockpitPanel)
            .border(1.dp, CockpitBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("FLIGHTS & ROUNDS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = "${uiState.stats.totalRounds}",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BEST MULTIPLIER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = Formatters.multiplier(uiState.stats.bestMultiplier),
              style = MaterialTheme.typography.titleLarge,
              color = RadarCyan,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("TOP PAYOUT", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
              text = Formatters.credits(uiState.stats.bestPayout),
              style = MaterialTheme.typography.titleLarge,
              color = CashoutEmerald,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ArcadeGameCard(
  game: GameType,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            game.accentColor.copy(alpha = 0.16f),
            CockpitPanel,
          )
        )
      )
      .border(1.2.dp, game.accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .padding(14.dp)
      .testTag("hub_card_${game.route}"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(game.accentColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = game.displayName,
          tint = game.accentColor,
          modifier = Modifier.size(24.dp),
        )
      }

      Text(
        text = game.maxMultiplierBadge,
        style = MaterialTheme.typography.labelSmall,
        color = game.accentColor,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(CockpitSurface)
          .padding(horizontal = 7.dp, vertical = 4.dp),
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = game.displayName,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
      )
      Text(
        text = game.tagline,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        minLines = 2,
        maxLines = 2,
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
        text = "PLAY NOW",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = game.accentColor,
      )
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.Rounded.PlayArrow,
        contentDescription = null,
        tint = game.accentColor,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

private fun iconForGame(game: GameType): ImageVector = when (game) {
  GameType.AVIATOR -> Icons.Rounded.FlightTakeoff
  GameType.MINES -> Icons.Rounded.Diamond
  GameType.PLINKO -> Icons.Rounded.Bolt
  GameType.WHEEL -> Icons.Rounded.Refresh
  GameType.DICE -> Icons.Rounded.Casino
}
