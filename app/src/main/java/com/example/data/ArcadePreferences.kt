package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.arcadeDataStore: DataStore<Preferences> by preferencesDataStore(name = "skycrash_prefs")

data class ArcadeProfile(
  val credits: Double = INITIAL_CREDITS,
  val hapticsEnabled: Boolean = true,
  val lastDailyClaimMillis: Long = 0L,
  val loggedInPhone: String? = null,
  val loggedInPilotName: String? = null,
  val isGuestMode: Boolean = false,
  val hasCompletedAuthGate: Boolean = false,
) {
  companion object {
    const val INITIAL_CREDITS = 2500.0
    const val DAILY_BONUS_AMOUNT = 500.0
    const val REFILL_AMOUNT = 1000.0
    const val REFILL_THRESHOLD = 100.0
  }
}

class ArcadePreferences(private val context: Context) {
  private object Keys {
    val CREDITS = doublePreferencesKey("credits")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val LAST_DAILY_CLAIM = longPreferencesKey("last_daily_claim")
    val LOGGED_IN_PHONE = stringPreferencesKey("logged_in_phone")
    val LOGGED_IN_NAME = stringPreferencesKey("logged_in_name")
    val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
    val HAS_COMPLETED_AUTH_GATE = booleanPreferencesKey("has_completed_auth_gate")
  }

  val profileFlow: Flow<ArcadeProfile> = context.arcadeDataStore.data.map { prefs ->
    ArcadeProfile(
      credits = prefs[Keys.CREDITS] ?: ArcadeProfile.INITIAL_CREDITS,
      hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
      lastDailyClaimMillis = prefs[Keys.LAST_DAILY_CLAIM] ?: 0L,
      loggedInPhone = prefs[Keys.LOGGED_IN_PHONE],
      loggedInPilotName = prefs[Keys.LOGGED_IN_NAME],
      isGuestMode = prefs[Keys.IS_GUEST_MODE] ?: false,
      hasCompletedAuthGate = prefs[Keys.HAS_COMPLETED_AUTH_GATE] ?: false,
    )
  }

  suspend fun setLoggedInSession(phone: String, pilotName: String) {
    context.arcadeDataStore.edit { prefs ->
      prefs[Keys.LOGGED_IN_PHONE] = phone
      prefs[Keys.LOGGED_IN_NAME] = pilotName
      prefs[Keys.IS_GUEST_MODE] = false
      prefs[Keys.HAS_COMPLETED_AUTH_GATE] = true
    }
  }

  suspend fun setGuestSession() {
    context.arcadeDataStore.edit { prefs ->
      prefs.remove(Keys.LOGGED_IN_PHONE)
      prefs.remove(Keys.LOGGED_IN_NAME)
      prefs[Keys.IS_GUEST_MODE] = true
      prefs[Keys.HAS_COMPLETED_AUTH_GATE] = true
    }
  }

  suspend fun clearSession() {
    context.arcadeDataStore.edit { prefs ->
      prefs.remove(Keys.LOGGED_IN_PHONE)
      prefs.remove(Keys.LOGGED_IN_NAME)
      prefs[Keys.IS_GUEST_MODE] = false
      prefs[Keys.HAS_COMPLETED_AUTH_GATE] = false
    }
  }

  suspend fun tryDeductCredits(amount: Double): Boolean {
    if (amount <= 0.0) return false
    var deducted = false
    context.arcadeDataStore.edit { prefs ->
      val current = prefs[Keys.CREDITS] ?: ArcadeProfile.INITIAL_CREDITS
      if (current + 0.0001 >= amount) {
        prefs[Keys.CREDITS] = (current - amount).coerceAtLeast(0.0)
        deducted = true
      }
    }
    return deducted
  }

  suspend fun addCredits(amount: Double) {
    if (amount <= 0.0) return
    context.arcadeDataStore.edit { prefs ->
      val current = prefs[Keys.CREDITS] ?: ArcadeProfile.INITIAL_CREDITS
      prefs[Keys.CREDITS] = current + amount
    }
  }

  suspend fun setHapticsEnabled(enabled: Boolean) {
    context.arcadeDataStore.edit { prefs ->
      prefs[Keys.HAPTICS_ENABLED] = enabled
    }
  }

  suspend fun claimDailyBonus(nowMillis: Long): Boolean {
    var claimed = false
    val cooldownMs = 24L * 60L * 60L * 1000L
    context.arcadeDataStore.edit { prefs ->
      val last = prefs[Keys.LAST_DAILY_CLAIM] ?: 0L
      if (nowMillis - last >= cooldownMs) {
        val current = prefs[Keys.CREDITS] ?: ArcadeProfile.INITIAL_CREDITS
        prefs[Keys.CREDITS] = current + ArcadeProfile.DAILY_BONUS_AMOUNT
        prefs[Keys.LAST_DAILY_CLAIM] = nowMillis
        claimed = true
      }
    }
    return claimed
  }

  suspend fun refillCreditsIfLow(): Boolean {
    var refilled = false
    context.arcadeDataStore.edit { prefs ->
      val current = prefs[Keys.CREDITS] ?: ArcadeProfile.INITIAL_CREDITS
      if (current < ArcadeProfile.REFILL_THRESHOLD) {
        prefs[Keys.CREDITS] = current + ArcadeProfile.REFILL_AMOUNT
        refilled = true
      }
    }
    return refilled
  }
}
