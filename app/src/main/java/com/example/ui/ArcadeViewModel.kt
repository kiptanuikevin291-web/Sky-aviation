package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ArcadeProfile
import com.example.data.ArcadeRepository
import com.example.data.ArcadeStatsSummary
import com.example.data.AuthActionResult
import com.example.data.GameRoundEntity
import com.example.data.WalletActionResult
import com.example.data.WalletTransactionEntity
import com.example.ui.components.Formatters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArcadeUiState(
  val isAuthLoaded: Boolean = false,
  val profile: ArcadeProfile = ArcadeProfile(),
  val stats: ArcadeStatsSummary = ArcadeStatsSummary(0, 0.0, 0.0, 0.0, 0.0),
  val recentRounds: List<GameRoundEntity> = emptyList(),
  val topWins: List<GameRoundEntity> = emptyList(),
  val walletTransactions: List<WalletTransactionEntity> = emptyList(),
  val canClaimDailyBonus: Boolean = true,
  val authError: String? = null,
)

class ArcadeViewModel(private val repository: ArcadeRepository) : ViewModel() {
  private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

  private val _authError = MutableStateFlow<String?>(null)

  val uiState: StateFlow<ArcadeUiState> = combine(
    repository.profileFlow,
    repository.statsSummaryFlow,
    repository.recentRoundsFlow,
    repository.walletTransactionsFlow,
    _authError,
  ) { profile, stats, recent, walletTxs, authErr ->
    val cooldownMs = 24L * 60L * 60L * 1000L
    val canClaim = (System.currentTimeMillis() - profile.lastDailyClaimMillis) >= cooldownMs
    ArcadeUiState(
      isAuthLoaded = true,
      profile = profile,
      stats = stats,
      recentRounds = recent,
      walletTransactions = walletTxs,
      canClaimDailyBonus = canClaim,
      authError = authErr,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ArcadeUiState(isAuthLoaded = false),
  )

  fun clearAuthError() {
    _authError.value = null
  }

  fun login(phone: String, password: String) {
    viewModelScope.launch {
      when (val result = repository.loginPilot(phone, password)) {
        is AuthActionResult.Success -> {
          _authError.value = null
          _toastMessage.tryEmit("Welcome back, ${result.account.pilotName}!")
        }
        is AuthActionResult.Error -> {
          _authError.value = result.message
        }
      }
    }
  }

  fun register(phone: String, password: String, pilotName: String) {
    viewModelScope.launch {
      when (val result = repository.registerPilot(phone, password, pilotName)) {
        is AuthActionResult.Success -> {
          _authError.value = null
          _toastMessage.tryEmit("Pilot account created for ${result.account.phoneNumber}")
        }
        is AuthActionResult.Error -> {
          _authError.value = result.message
        }
      }
    }
  }

  fun loginDemo() {
    viewModelScope.launch {
      when (val result = repository.loginOrCreateDemoPilot()) {
        is AuthActionResult.Success -> {
          _authError.value = null
          _toastMessage.tryEmit("Signed in as ${result.account.pilotName} (${result.account.phoneNumber})")
        }
        is AuthActionResult.Error -> _authError.value = result.message
      }
    }
  }

  fun continueAsGuest() {
    viewModelScope.launch {
      _authError.value = null
      repository.continueAsGuest()
    }
  }

  fun changePassword(
    phoneNumber: String,
    currentPassword: String,
    newPassword: String,
    onResult: (Boolean, String?) -> Unit,
  ) {
    viewModelScope.launch {
      when (val result = repository.changePassword(phoneNumber, currentPassword, newPassword)) {
        is AuthActionResult.Success -> {
          _toastMessage.tryEmit("Password updated successfully")
          onResult(true, null)
        }
        is AuthActionResult.Error -> {
          onResult(false, result.message)
        }
      }
    }
  }

  fun logout() {
    viewModelScope.launch {
      _authError.value = null
      repository.logout()
      _toastMessage.tryEmit("Signed out")
    }
  }

  fun deposit(amount: Double, method: String, accountRef: String, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      when (val result = repository.depositKsh(amount, method, accountRef)) {
        is WalletActionResult.Success -> {
          _toastMessage.tryEmit("Deposited +${Formatters.credits(result.transaction.amount)} (${result.transaction.referenceCode})")
          onSuccess()
        }
        is WalletActionResult.Error -> {
          _toastMessage.tryEmit(result.message)
        }
      }
    }
  }

  fun withdraw(amount: Double, method: String, accountRef: String, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      when (val result = repository.withdrawKsh(amount, method, accountRef)) {
        is WalletActionResult.Success -> {
          _toastMessage.tryEmit("Withdrew -${Formatters.credits(result.transaction.amount)} (${result.transaction.referenceCode})")
          onSuccess()
        }
        is WalletActionResult.Error -> {
          _toastMessage.tryEmit(result.message)
        }
      }
    }
  }

  fun claimDailyBonus() {
    viewModelScope.launch {
      val claimed = repository.claimDailyBonus()
      if (claimed) {
        _toastMessage.tryEmit("+KSh 500 Daily Hangar Bonus claimed!")
      } else {
        _toastMessage.tryEmit("Daily bonus already claimed — check back tomorrow!")
      }
    }
  }

  fun refillIfLow() {
    viewModelScope.launch {
      val refilled = repository.refillIfLow()
      if (refilled) {
        _toastMessage.tryEmit("+KSh 1,000 Arcade Refill added!")
      } else {
        _toastMessage.tryEmit("Refill available whenever balance drops below KSh 100")
      }
    }
  }

  fun toggleHaptics(enabled: Boolean) {
    viewModelScope.launch {
      repository.setHapticsEnabled(enabled)
    }
  }

  fun clearHistory() {
    viewModelScope.launch {
      repository.clearHistory()
      _toastMessage.tryEmit("Flight & wallet ledger cleared")
    }
  }

  class Factory(private val repository: ArcadeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return ArcadeViewModel(repository) as T
    }
  }
}
