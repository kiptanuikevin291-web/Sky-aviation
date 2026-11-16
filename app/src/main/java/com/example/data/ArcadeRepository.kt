package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

sealed class WalletActionResult {
  data class Success(val transaction: WalletTransactionEntity) : WalletActionResult()
  data class Error(val message: String) : WalletActionResult()
}

sealed class AuthActionResult {
  data class Success(val account: PilotAccountEntity) : AuthActionResult()
  data class Error(val message: String) : AuthActionResult()
}

class ArcadeRepository(
  private val preferences: ArcadePreferences,
  private val dao: GameRoundDao,
  private val walletDao: WalletTransactionDao,
  private val accountDao: PilotAccountDao,
) {
  val profileFlow: Flow<ArcadeProfile> = preferences.profileFlow
  val recentRoundsFlow: Flow<List<GameRoundEntity>> = dao.observeRecentRounds(60)
  val topWinsFlow: Flow<List<GameRoundEntity>> = dao.observeTopWins(8)
  val statsSummaryFlow: Flow<ArcadeStatsSummary> = dao.observeStatsSummary()
  val walletTransactionsFlow: Flow<List<WalletTransactionEntity>> = walletDao.observeRecentTransactions(40)

  fun observeGameRounds(gameType: GameType, limit: Int = 20): Flow<List<GameRoundEntity>> =
    dao.observeRoundsByGame(gameType.name, limit)

  suspend fun registerPilot(
    rawPhone: String,
    password: String,
    pilotName: String,
  ): AuthActionResult {
    val phone = PasswordHasher.normalizePhone(rawPhone)
    if (phone.length < 9) {
      return AuthActionResult.Error("Enter a valid phone number (at least 9 digits)")
    }
    if (password.length < 6) {
      return AuthActionResult.Error("Password must be at least 6 characters")
    }
    val existing = accountDao.getByPhone(phone)
    if (existing != null) {
      return AuthActionResult.Error("An account with $phone already exists. Sign in instead.")
    }
    val cleanName = pilotName.trim().ifBlank { "Pilot ${phone.takeLast(4)}" }
    val salt = PasswordHasher.generateSaltHex()
    val hash = PasswordHasher.hashPassword(password, salt)
    val account = PilotAccountEntity(
      phoneNumber = phone,
      pilotName = cleanName,
      passwordHash = hash,
      salt = salt,
    )
    accountDao.insertAccount(account)
    preferences.setLoggedInSession(phone, cleanName)
    return AuthActionResult.Success(account)
  }

  suspend fun loginPilot(
    rawPhone: String,
    password: String,
  ): AuthActionResult {
    val phone = PasswordHasher.normalizePhone(rawPhone)
    if (phone.length < 9) {
      return AuthActionResult.Error("Enter a valid phone number")
    }
    if (password.isBlank()) {
      return AuthActionResult.Error("Enter your password")
    }
    val account = accountDao.getByPhone(phone)
      ?: return AuthActionResult.Error("No account found for $phone. Create one first.")

    if (!PasswordHasher.verify(password, account.salt, account.passwordHash)) {
      return AuthActionResult.Error("Incorrect password for $phone")
    }
    preferences.setLoggedInSession(account.phoneNumber, account.pilotName)
    return AuthActionResult.Success(account)
  }

  suspend fun loginOrCreateDemoPilot(): AuthActionResult {
    val demoPhone = "0712345678"
    val demoPass = "123456"
    val existing = accountDao.getByPhone(demoPhone)
    return if (existing != null) {
      preferences.setLoggedInSession(existing.phoneNumber, existing.pilotName)
      AuthActionResult.Success(existing)
    } else {
      registerPilot(demoPhone, demoPass, "Captain Sky")
    }
  }

  suspend fun changePassword(
    phoneNumber: String,
    currentPassword: String,
    newPassword: String,
  ): AuthActionResult {
    if (newPassword.length < 6) {
      return AuthActionResult.Error("New password must be at least 6 characters")
    }
    val account = accountDao.getByPhone(phoneNumber)
      ?: return AuthActionResult.Error("Account not found")
    if (!PasswordHasher.verify(currentPassword, account.salt, account.passwordHash)) {
      return AuthActionResult.Error("Current password is incorrect")
    }
    val newSalt = PasswordHasher.generateSaltHex()
    val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
    accountDao.updatePassword(phoneNumber, newHash, newSalt)
    return AuthActionResult.Success(account.copy(passwordHash = newHash, salt = newSalt))
  }

  suspend fun continueAsGuest() {
    preferences.setGuestSession()
  }

  suspend fun logout() {
    preferences.clearSession()
  }

  suspend fun tryPlaceWager(amount: Double): Boolean = preferences.tryDeductCredits(amount)

  suspend fun creditPayout(amount: Double) = preferences.addCredits(amount)

  suspend fun recordRound(
    gameType: GameType,
    wager: Double,
    multiplier: Double,
    payout: Double,
    detailSummary: String,
  ) {
    dao.insertRound(
      GameRoundEntity(
        gameType = gameType.name,
        wager = wager,
        multiplier = multiplier,
        payout = payout,
        netProfit = payout - wager,
        detailSummary = detailSummary,
      )
    )
  }

  suspend fun depositKsh(
    amount: Double,
    method: String,
    accountRef: String,
  ): WalletActionResult {
    if (amount < 10.0) {
      return WalletActionResult.Error("Minimum deposit is KSh 10")
    }
    if (amount > 100_000.0) {
      return WalletActionResult.Error("Maximum single demo deposit is KSh 100,000")
    }
    preferences.addCredits(amount)
    val tx = WalletTransactionEntity(
      type = "DEPOSIT",
      amount = amount,
      method = method,
      accountRef = accountRef.ifBlank { "Demo Account" },
      referenceCode = generateRefCode("DEP"),
      status = "Completed",
    )
    walletDao.insertTransaction(tx)
    return WalletActionResult.Success(tx)
  }

  suspend fun withdrawKsh(
    amount: Double,
    method: String,
    accountRef: String,
  ): WalletActionResult {
    if (amount < 50.0) {
      return WalletActionResult.Error("Minimum withdrawal is KSh 50")
    }
    val deducted = preferences.tryDeductCredits(amount)
    if (!deducted) {
      return WalletActionResult.Error("Insufficient KSh balance for this withdrawal")
    }
    val tx = WalletTransactionEntity(
      type = "WITHDRAW",
      amount = amount,
      method = method,
      accountRef = accountRef.ifBlank { "Demo Account" },
      referenceCode = generateRefCode("WDR"),
      status = "Completed",
    )
    walletDao.insertTransaction(tx)
    return WalletActionResult.Success(tx)
  }

  private fun generateRefCode(prefix: String): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val suffix = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    return "$prefix-$suffix"
  }

  suspend fun setHapticsEnabled(enabled: Boolean) = preferences.setHapticsEnabled(enabled)

  suspend fun claimDailyBonus(): Boolean = preferences.claimDailyBonus(System.currentTimeMillis())

  suspend fun refillIfLow(): Boolean = preferences.refillCreditsIfLow()

  suspend fun clearHistory() {
    dao.clearAll()
    walletDao.clearAll()
  }

  companion object {
    @Volatile
    private var INSTANCE: ArcadeRepository? = null

    fun getInstance(context: Context): ArcadeRepository {
      return INSTANCE ?: synchronized(this) {
        val db = SkyCrashDatabase.getInstance(context)
        val prefs = ArcadePreferences(context)
        val repo = ArcadeRepository(
          prefs,
          db.gameRoundDao(),
          db.walletTransactionDao(),
          db.pilotAccountDao(),
        )
        INSTANCE = repo
        repo
      }
    }
  }
}
