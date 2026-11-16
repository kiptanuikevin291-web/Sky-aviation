package com.example.ui.components

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Formatters {
  private val intFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

  fun credits(amount: Double): String {
    val rounded = if (amount >= 1000.0) {
      intFormat.format(amount.roundToInt())
    } else if (amount % 1.0 == 0.0) {
      intFormat.format(amount.toInt())
    } else {
      String.format(Locale.US, "%.1f", amount)
    }
    return "KSh $rounded"
  }

  fun multiplier(value: Double): String = String.format(Locale.US, "%.2fx", value)

  fun shortTime(timestampMillis: Long): String {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return fmt.format(Date(timestampMillis))
  }
}
