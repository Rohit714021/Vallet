package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.LockGateScreen
import com.example.ui.components.TransactionFormSheet
import com.example.ui.navigation.AnalyticsRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.ManageSetupRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.WealthRoute
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ManageSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WealthScreen
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianVaultTheme
import com.example.ui.theme.VaultSurface
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.VaultViewModelFactory
import com.example.ui.viewmodel.WealthViewModel
import kotlinx.coroutines.flow.first

private data class NavTab(
  val label: String,
  val route: Any,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String,
)

class MainActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as ObsidianVaultApp
    val factory = VaultViewModelFactory(app)

    setContent {
      ObsidianVaultTheme(darkTheme = true) {
        val settingsState by app.preferences.settingsFlow.collectAsStateWithLifecycle(
          initialValue = com.example.data.prefs.VaultSettingsState()
        )
        var isUnlocked by rememberSaveable { mutableStateOf(false) }
        var lockInitialized by remember { mutableStateOf(false) }

        LaunchedEffect(settingsState.appLockEnabled) {
          if (!lockInitialized) {
            isUnlocked = !settingsState.appLockEnabled
            lockInitialized = true
            if (settingsState.appLockEnabled) {
              launchBiometricPrompt { isUnlocked = true }
            }
          } else if (!settingsState.appLockEnabled) {
            isUnlocked = true
          }
        }

        if (settingsState.appLockEnabled && !isUnlocked) {
          LockGateScreen(
            hasPinConfigured = settingsState.hasPinConfigured,
            onTriggerBiometric = {
              launchBiometricPrompt { isUnlocked = true }
            },
            onVerifyPin = { pin ->
              app.preferences.verifyPinFlow(pin).first()
            },
            onUnlocked = { isUnlocked = true },
          )
        } else {
          ObsidianVaultShell(factory = factory)
        }
      }
    }
  }

  private fun launchBiometricPrompt(onSuccess: () -> Unit) {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val canAuth = BiometricManager.from(this).canAuthenticate(authenticators)
    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
      return
    }
    val executor = ContextCompat.getMainExecutor(this)
    val prompt = BiometricPrompt(
      this,
      executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          onSuccess()
        }
      },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
      .setTitle("Unlock Obsidian Vault")
      .setSubtitle("Verify identity to access your private financial database")
      .setAllowedAuthenticators(authenticators)
      .build()
    prompt.authenticate(info)
  }
}

@Composable
private fun ObsidianVaultShell(factory: VaultViewModelFactory) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination

  val dashboardVm: DashboardViewModel = viewModel(factory = factory)
  val analyticsVm: AnalyticsViewModel = viewModel(factory = factory)
  val wealthVm: WealthViewModel = viewModel(factory = factory)
  val settingsVm: SettingsViewModel = viewModel(factory = factory)

  val dashboardState by dashboardVm.uiState.collectAsStateWithLifecycle()
  val analyticsState by analyticsVm.uiState.collectAsStateWithLifecycle()
  val wealthState by wealthVm.uiState.collectAsStateWithLifecycle()
  val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()

  var showAddTxSheet by remember { mutableStateOf(false) }

  val tabs = remember {
    listOf(
      NavTab("Vault", DashboardRoute, Icons.Filled.SpaceDashboard, Icons.Outlined.SpaceDashboard, "nav_vault"),
      NavTab("Trends", AnalyticsRoute, Icons.Filled.Insights, Icons.Outlined.Insights, "nav_trends"),
      NavTab("Wealth", WealthRoute, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_wealth"),
      NavTab("Security", SettingsRoute, Icons.Filled.Lock, Icons.Outlined.Lock, "nav_security"),
    )
  }

  fun navigateTo(route: Any) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  val isInSetupWindow = currentDest?.hasRoute(ManageSetupRoute::class) == true

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBg),
  ) {
    val useRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useRail && !isInSetupWindow) {
        NavigationRail(
          containerColor = VaultSurface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          tabs.forEach { tab ->
            val selected = currentDest?.hasRoute(tab.route::class) == true
            NavigationRailItem(
              selected = selected,
              onClick = { navigateTo(tab.route) },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.label,
                )
              },
              label = { Text(tab.label) },
              colors = NavigationRailItemDefaults.colors(
                selectedIconColor = ObsidianBg,
                selectedTextColor = ElectricMint,
                indicatorColor = ElectricMint,
              ),
              modifier = Modifier.testTag(tab.tag),
            )
          }
        }
      }

      Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
          if (!isInSetupWindow) {
            ExtendedFloatingActionButton(
              onClick = { showAddTxSheet = true },
              containerColor = ElectricMint,
              contentColor = ObsidianBg,
              icon = { Icon(Icons.Default.Add, contentDescription = "Log entry") },
              text = { Text("Log Entry", style = MaterialTheme.typography.labelLarge) },
              modifier = Modifier.testTag("fab_log_entry"),
            )
          }
        },
        bottomBar = {
          if (!useRail && !isInSetupWindow) {
            NavigationBar(containerColor = VaultSurface) {
              tabs.forEach { tab ->
                val selected = currentDest?.hasRoute(tab.route::class) == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTo(tab.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label) },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ObsidianBg,
                    selectedTextColor = ElectricMint,
                    indicatorColor = ElectricMint,
                  ),
                  modifier = Modifier.testTag(tab.tag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = DashboardRoute,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          composable<DashboardRoute> {
            DashboardScreen(
              state = dashboardState,
              onSearchChange = dashboardVm::updateSearchQuery,
              onDeleteTransaction = dashboardVm::deleteTransaction,
              onSettleRecurring = dashboardVm::settleRecurringNow,
              onNavigateToBudgetsAndBackup = { navigateTo(SettingsRoute) },
              onNavigateToManageSetup = { navController.navigate(ManageSetupRoute) },
              onOpenAddTransaction = { showAddTxSheet = true },
            )
          }
          composable<AnalyticsRoute> {
            AnalyticsScreen(
              state = analyticsState,
              onSelectTrendWindow = analyticsVm::selectTrendWindow,
              onSelectCategory = analyticsVm::selectCategory,
              onSelectEventYear = analyticsVm::selectEventYear,
              onSelectEventSubCategory = analyticsVm::selectEventSubCategory,
            )
          }
          composable<WealthRoute> {
            WealthScreen(
              state = wealthState,
              onSaveAccount = wealthVm::saveAccount,
              onDeleteAccount = wealthVm::deleteAccount,
              onUpdateInvestmentInputs = wealthVm::updateInvestmentInputs,
              onUpdateDebtInputs = wealthVm::updateDebtInputs,
              onLoadAccountToCalculator = wealthVm::loadAccountIntoCalculator,
            )
          }
          composable<SettingsRoute> {
            SettingsScreen(
              state = settingsState,
              onSetCurrency = settingsVm::setCurrency,
              onSetAppLock = settingsVm::setAppLockEnabled,
              onSavePin = settingsVm::savePin,
              onClearPin = settingsVm::clearPin,
              onSetYearlyReminder = settingsVm::setYearlyReminderEnabled,
              onSetBudgetNotifications = settingsVm::setBudgetNotificationsEnabled,
              onSaveBudgetRule = settingsVm::saveBudgetRule,
              onDeleteBudgetRule = settingsVm::deleteBudgetRule,
              onOpenManageSetup = { navController.navigate(ManageSetupRoute) },
              onExportEncryptedDb = settingsVm::exportEncryptedDatabase,
              onRestoreEncryptedDb = settingsVm::restoreEncryptedDatabase,
              onClearStatus = settingsVm::clearStatusMessage,
            )
          }
          composable<ManageSetupRoute> {
            ManageSetupScreen(
              accounts = dashboardState.accounts,
              categories = dashboardState.categories,
              currency = dashboardState.settings.currency,
              onSaveAccount = wealthVm::saveAccount,
              onDeleteAccount = wealthVm::deleteAccount,
              onAddCategory = settingsVm::addCategory,
              onDeleteCategory = settingsVm::deleteCategory,
              onBack = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }

  if (showAddTxSheet) {
    TransactionFormSheet(
      accounts = dashboardState.accounts,
      categories = dashboardState.categories,
      currency = dashboardState.settings.currency,
      onDismiss = { showAddTxSheet = false },
      onSaveTransaction = dashboardVm::logTransaction,
      onOpenManageSetup = { navController.navigate(ManageSetupRoute) },
    )
  }
}
