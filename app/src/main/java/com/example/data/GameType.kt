package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CashoutEmerald
import com.example.ui.theme.CreditGold
import com.example.ui.theme.PulseViolet
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.ThrustCrimson

enum class GameType(
  val route: String,
  val displayName: String,
  val tagline: String,
  val maxMultiplierBadge: String,
  val accentColor: Color,
) {
  AVIATOR(
    route = "aviator",
    displayName = "Aviator",
    tagline = "Ride the multiplier curve & cash out before fly-away",
    maxMultiplierBadge = "100x+ FLIGHT",
    accentColor = ThrustCrimson,
  ),
  MINES(
    route = "mines",
    displayName = "Sky Mines",
    tagline = "Sweep the 5x5 radar grid for gems while dodging mines",
    maxMultiplierBadge = "50x+ GRID",
    accentColor = CashoutEmerald,
  ),
  PLINKO(
    route = "plinko",
    displayName = "Turbo Plinko",
    tagline = "Drop neon orbs through 12 rows of kinetic pegs",
    maxMultiplierBadge = "25x DROP",
    accentColor = RadarCyan,
  ),
  WHEEL(
    route = "wheel",
    displayName = "Neon Wheel",
    tagline = "Pick your risk tier and spin the glowing prize rotor",
    maxMultiplierBadge = "12x SPIN",
    accentColor = CreditGold,
  ),
  DICE(
    route = "dice",
    displayName = "Cyber Dice",
    tagline = "Slide your target odds and roll over or under",
    maxMultiplierBadge = "49x ROLL",
    accentColor = PulseViolet,
  ),
}
