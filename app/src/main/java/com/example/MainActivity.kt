package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ArcadeRepository
import com.example.data.GameType
import com.example.ui.ArcadeViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.components.TopTelemetryBar
import com.example.ui.games.aviator.AviatorScreen
import com.example.ui.games.aviator.AviatorViewModel
import com.example.ui.games.dice.DiceScreen
import com.example.ui.games.dice.DiceViewModel
import com.example.ui.games.mines.MinesScreen
import com.example.ui.games.mines.MinesViewModel
import com.example.ui.games.plinko.PlinkoScreen
import com.example.ui.games.plinko.PlinkoViewModel
import com.example.ui.games.wheel.WheelScreen
import com.example.ui.games.wheel.WheelViewModel
import com.example.ui.hangar.HangarScreen
import com.example.ui.hub.ArcadeHubScreen
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SkyCrashTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThrustCrimson
import com.example.ui.wallet.WalletSheet
import com.example.ui.wallet.WalletTab
import kotlinx.coroutines.launch

private const val ROUTE_AVIATOR = "aviator"
private const val ROUTE_ARCADE = "arcade"
private const val ROUTE_HANGAR = "hangar"
private const val ROUTE_MINES = "mines"
private const val ROUTE_PLINKO = "plinko"
private const val ROUTE_WHEEL = "wheel"
private const val ROUTE_DICE = "dice"

private data class TopTabItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    setContent {
      SkyCrashTheme {
        SkyCrashApp()
      }
    }
  }
}

@Composable
fun SkyCrashApp() {
  val context = LocalContext.current
  val repository = remember { ArcadeRepository.getInstance(context) }

  val arcadeViewModel: ArcadeViewModel = viewModel(factory = ArcadeViewModel.Factory(repository))
  val aviatorViewModel: AviatorViewModel = viewModel(factory = AviatorViewModel.Factory(repository))
  val minesViewModel: MinesViewModel = viewModel(factory = MinesViewModel.Factory(repository))
  val plinkoViewModel: PlinkoViewModel = viewModel(factory = PlinkoViewModel.Factory(repository))
  val wheelViewModel: WheelViewModel = viewModel(factory = WheelViewModel.Factory(repository))
  val diceViewModel: DiceViewModel = viewModel(factory = DiceViewModel.Factory(repository))

  val uiState by arcadeViewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_AVIATOR

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var walletSheetTab by remember { mutableStateOf<WalletTab?>(null) }

  val showMessage: (String) -> Unit = remember(snackbarHostState, scope) {
    { msg ->
      scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(msg)
      }
    }
  }

  LaunchedEffect(arcadeViewModel) {
    arcadeViewModel.toastMessage.collect { msg ->
      showMessage(msg)
    }
  }

  if (!uiState.isAuthLoaded) {
    Box(modifier = Modifier.fillMaxSize().background(ObsidianBg))
    return
  }

  if (!uiState.profile.hasCompletedAuthGate) {
    LoginScreen(
      authError = uiState.authError,
      onClearError = arcadeViewModel::clearAuthError,
      onLogin = arcadeViewModel::login,
      onRegister = arcadeViewModel::register,
      onDemoLogin = arcadeViewModel::loginDemo,
      onContinueGuest = arcadeViewModel::continueAsGuest,
    )
    return
  }

  val topTabs = remember {
    listOf(
      TopTabItem(
        route = ROUTE_AVIATOR,
        label = "Aviator",
        selectedIcon = Icons.Filled.FlightTakeoff,
        unselectedIcon = Icons.Outlined.FlightTakeoff,
      ),
      TopTabItem(
        route = ROUTE_ARCADE,
        label = "Arcade",
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView,
      ),
      TopTabItem(
        route = ROUTE_HANGAR,
        label = "Hangar",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
      ),
    )
  }

  val isTopLevel = currentRoute in setOf(ROUTE_AVIATOR, ROUTE_ARCADE, ROUTE_HANGAR)
  val topTitle = when (currentRoute) {
    ROUTE_AVIATOR -> "SkyCrash"
    ROUTE_ARCADE -> "Arcade Hub"
    ROUTE_HANGAR -> "Pilot Hangar"
    ROUTE_MINES -> "Sky Mines"
    ROUTE_PLINKO -> "Turbo Plinko"
    ROUTE_WHEEL -> "Neon Wheel"
    ROUTE_DICE -> "Cyber Dice"
    else -> "SkyCrash"
  }

  val navigateTopTab: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBg),
  ) {
    val useNavRail = maxWidth >= 700.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavRail && isTopLevel) {
        NavigationRail(
          containerColor = CockpitSurface,
          contentColor = TextPrimary,
          modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
          Spacer(modifier = Modifier.height(16.dp))
          topTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationRailItem(
              selected = selected,
              onClick = { navigateTopTab(tab.route) },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.label,
                )
              },
              label = { Text(tab.label) },
              colors = NavigationRailItemDefaults.colors(
                selectedIconColor = ObsidianBg,
                selectedTextColor = ThrustCrimson,
                indicatorColor = ThrustCrimson,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
              ),
              modifier = Modifier.testTag("nav_${tab.route}"),
            )
          }
        }
      }

      Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          TopTelemetryBar(
            title = topTitle,
            credits = uiState.profile.credits,
            showBackButton = !isTopLevel,
            onBackClick = { navController.popBackStack() },
            onCreditsClick = { walletSheetTab = WalletTab.DEPOSIT },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
          )
        },
        bottomBar = {
          if (!useNavRail && isTopLevel) {
            NavigationBar(
              containerColor = CockpitSurface,
              contentColor = TextPrimary,
              windowInsets = WindowInsets.navigationBars,
            ) {
              topTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTopTab(tab.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ObsidianBg,
                    selectedTextColor = if (tab.route == ROUTE_AVIATOR) ThrustCrimson else RadarCyan,
                    indicatorColor = if (tab.route == ROUTE_AVIATOR) ThrustCrimson else RadarCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                  ),
                  modifier = Modifier.testTag("nav_${tab.route}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = ROUTE_AVIATOR,
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .let { mod ->
              if (!isTopLevel) mod.windowInsetsPadding(WindowInsets.navigationBars) else mod
            },
        ) {
          composable(ROUTE_AVIATOR) {
            AviatorScreen(
              viewModel = aviatorViewModel,
              credits = uiState.profile.credits,
              hapticsEnabled = uiState.profile.hapticsEnabled,
              onShowMessage = showMessage,
            )
          }

          composable(ROUTE_ARCADE) {
            ArcadeHubScreen(
              uiState = uiState,
              onSelectGame = { game ->
                when (game) {
                  GameType.AVIATOR -> navigateTopTab(ROUTE_AVIATOR)
                  GameType.MINES -> navController.navigate(ROUTE_MINES)
                  GameType.PLINKO -> navController.navigate(ROUTE_PLINKO)
                  GameType.WHEEL -> navController.navigate(ROUTE_WHEEL)
                  GameType.DICE -> navController.navigate(ROUTE_DICE)
                }
              },
              onClaimDaily = arcadeViewModel::claimDailyBonus,
              onRefillLow = arcadeViewModel::refillIfLow,
            )
          }

          composable(ROUTE_HANGAR) {
            HangarScreen(
              uiState = uiState,
              onOpenDeposit = { walletSheetTab = WalletTab.DEPOSIT },
              onOpenWithdraw = { walletSheetTab = WalletTab.WITHDRAW },
              onClaimDaily = arcadeViewModel::claimDailyBonus,
              onRefillLow = arcadeViewModel::refillIfLow,
              onToggleHaptics = arcadeViewModel::toggleHaptics,
              onClearHistory = arcadeViewModel::clearHistory,
              onChangePassword = arcadeViewModel::changePassword,
              onLogout = arcadeViewModel::logout,
            )
          }

          composable(ROUTE_MINES) {
            MinesScreen(
              viewModel = minesViewModel,
              credits = uiState.profile.credits,
              hapticsEnabled = uiState.profile.hapticsEnabled,
              onShowMessage = showMessage,
            )
          }

          composable(ROUTE_PLINKO) {
            PlinkoScreen(
              viewModel = plinkoViewModel,
              credits = uiState.profile.credits,
              hapticsEnabled = uiState.profile.hapticsEnabled,
              onShowMessage = showMessage,
            )
          }

          composable(ROUTE_WHEEL) {
            WheelScreen(
              viewModel = wheelViewModel,
              credits = uiState.profile.credits,
              hapticsEnabled = uiState.profile.hapticsEnabled,
              onShowMessage = showMessage,
            )
          }

          composable(ROUTE_DICE) {
            DiceScreen(
              viewModel = diceViewModel,
              credits = uiState.profile.credits,
              hapticsEnabled = uiState.profile.hapticsEnabled,
              onShowMessage = showMessage,
            )
          }
        }
      }
    }
  }

  walletSheetTab?.let { initialTab ->
    WalletSheet(
      initialTab = initialTab,
      balance = uiState.profile.credits,
      defaultPhone = uiState.profile.loggedInPhone,
      canClaimDailyBonus = uiState.canClaimDailyBonus,
      onDismiss = { walletSheetTab = null },
      onDeposit = { amount, method, accountRef ->
        arcadeViewModel.deposit(amount, method, accountRef) {
          walletSheetTab = null
        }
      },
      onWithdraw = { amount, method, accountRef ->
        arcadeViewModel.withdraw(amount, method, accountRef) {
          walletSheetTab = null
        }
      },
      onClaimDailyBonus = arcadeViewModel::claimDailyBonus,
      onRefillLow = arcadeViewModel::refillIfLow,
    )
  }
}
