package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SkyCrashColorScheme = darkColorScheme(
  primary = ThrustCrimson,
  onPrimary = TextPrimary,
  primaryContainer = ThrustCrimsonSoft,
  onPrimaryContainer = TextPrimary,
  secondary = CashoutEmerald,
  onSecondary = ObsidianBg,
  secondaryContainer = CashoutEmeraldSoft,
  onSecondaryContainer = CashoutEmerald,
  tertiary = CreditGold,
  onTertiary = ObsidianBg,
  tertiaryContainer = CreditGoldSoft,
  onTertiaryContainer = CreditGold,
  background = ObsidianBg,
  onBackground = TextPrimary,
  surface = CockpitSurface,
  onSurface = TextPrimary,
  surfaceVariant = CockpitPanel,
  onSurfaceVariant = TextSecondary,
  surfaceContainerLow = CockpitSurface,
  surfaceContainer = CockpitPanel,
  surfaceContainerHigh = CockpitPanelElevated,
  outline = CockpitBorder,
  error = ThrustCrimson,
  onError = TextPrimary,
)

private val SkyCrashShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SkyCrashTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = SkyCrashColorScheme,
    typography = Typography,
    shapes = SkyCrashShapes,
    content = content,
  )
}
