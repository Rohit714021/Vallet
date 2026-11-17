package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.BudgetRuleEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.VaultRepository
import com.example.data.prefs.VaultPreferences
import com.example.data.prefs.VaultSettingsState
import com.example.domain.BudgetAnalyticsEngine
import com.example.domain.BudgetProgressSummary
import com.example.domain.NotificationHelper
import com.example.domain.PeriodBudgetPace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
  val settings: VaultSettingsState = VaultSettingsState(),
  val accounts: List<AccountEntity> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val filteredTransactions: List<TransactionEntity> = emptyList(),
  val totalTransactionCount: Int = 0,
  val netWorth: Double = 0.0,
  val totalBankAndCash: Double = 0.0,
  val totalInvestments: Double = 0.0,
  val totalDebts: Double = 0.0,
  val periodPaces: List<PeriodBudgetPace> = emptyList(),
  val triggeredAlerts: List<BudgetProgressSummary> = emptyList(),
  val upcomingRecurring: List<RecurringRuleEntity> = emptyList(),
  val isYearlyBackupDue: Boolean = false,
  val searchQuery: String = "",
)

private data class DashboardCoreStreams(
  val settings: VaultSettingsState,
  val accounts: List<AccountEntity>,
  val categories: List<CategoryEntity>,
  val transactions: List<TransactionEntity>,
  val budgetRules: List<BudgetRuleEntity>,
)

class DashboardViewModel(
  private val repository: VaultRepository,
  private val preferences: VaultPreferences,
  private val notificationHelper: NotificationHelper,
) : ViewModel() {

  private val searchQuery = MutableStateFlow("")

  init {
    viewModelScope.launch {
      repository.processAutoDueRecurringRules()
    }
  }

  private val coreFlow = combine(
    preferences.settingsFlow,
    repository.accounts,
    repository.categories,
    repository.transactions,
    repository.budgetRules,
  ) { settings, accounts, categories, transactions, rules ->
    DashboardCoreStreams(settings, accounts, categories, transactions, rules)
  }

  val uiState: StateFlow<DashboardUiState> = combine(
    coreFlow,
    repository.recurringRules,
    searchQuery,
  ) { core, recurring, query ->
    val categoriesById = core.categories.associateBy { it.id }
    val bankAndCash = core.accounts
      .filter { it.type == AccountType.BANK || it.type == AccountType.CASH }
      .sumOf { it.balance }
    val investments = core.accounts
      .filter { it.type == AccountType.INVESTMENT }
      .sumOf { it.balance }
    val debts = core.accounts
      .filter { it.type == AccountType.DEBT || it.type == AccountType.CREDIT_CARD }
      .sumOf { it.balance }
    val netWorth = bankAndCash + investments - debts

    val paces = BudgetAnalyticsEngine.computePeriodPaces(core.transactions, core.budgetRules)
    val allEvaluated = BudgetAnalyticsEngine.evaluateBudgetRules(
      core.transactions,
      core.budgetRules,
      categoriesById,
    )
    val alerts = allEvaluated.filter { it.isAlertTriggered }

    val q = query.trim().lowercase()
    val filtered = if (q.isEmpty()) {
      core.transactions
    } else {
      core.transactions.filter { tx ->
        val catName = categoriesById[tx.categoryId]?.name.orEmpty().lowercase()
        val subName = tx.subCategoryId?.let { categoriesById[it]?.name }.orEmpty().lowercase()
        tx.toLabel.lowercase().contains(q) ||
          tx.fromLabel.lowercase().contains(q) ||
          tx.notes.lowercase().contains(q) ||
          tx.locationName.lowercase().contains(q) ||
          tx.eventTitle.lowercase().contains(q) ||
          catName.contains(q) ||
          subName.contains(q)
      }
    }

    val yearMillis = 365L * 24 * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    val backupDue = core.settings.yearlyBackupReminderEnabled && (
      (core.settings.lastBackupTimestamp == 0L && core.transactions.isNotEmpty()) ||
        (core.settings.lastBackupTimestamp > 0L && (now - core.settings.lastBackupTimestamp) > yearMillis)
      )

    DashboardUiState(
      settings = core.settings,
      accounts = core.accounts,
      categories = core.categories,
      filteredTransactions = filtered,
      totalTransactionCount = core.transactions.size,
      netWorth = netWorth,
      totalBankAndCash = bankAndCash,
      totalInvestments = investments,
      totalDebts = debts,
      periodPaces = paces,
      triggeredAlerts = alerts,
      upcomingRecurring = recurring.filter { it.isActive },
      isYearlyBackupDue = backupDue,
      searchQuery = query,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = DashboardUiState(),
  )

  fun updateSearchQuery(query: String) {
    searchQuery.value = query
  }

  fun logTransaction(
    tx: TransactionEntity,
    recurringFrequency: RecurrenceFrequency?,
    autoLogRecurring: Boolean,
  ) {
    viewModelScope.launch {
      repository.logTransaction(tx, recurringFrequency, autoLogRecurring)
      if (tx.type == TransactionType.EXPENSE) {
        evaluateAndNotifyBudgetAlerts()
      }
    }
  }

  fun deleteTransaction(tx: TransactionEntity) {
    viewModelScope.launch {
      repository.deleteTransactionWithReversal(tx)
    }
  }

  fun settleRecurringNow(rule: RecurringRuleEntity) {
    viewModelScope.launch {
      repository.settleRecurringRuleNow(rule)
      if (rule.type == TransactionType.EXPENSE) {
        evaluateAndNotifyBudgetAlerts()
      }
    }
  }

  fun toggleRecurringActive(rule: RecurringRuleEntity) {
    viewModelScope.launch {
      repository.saveRecurringRule(rule.copy(isActive = !rule.isActive))
    }
  }

  fun deleteRecurringRule(id: Long) {
    viewModelScope.launch {
      repository.deleteRecurringRule(id)
    }
  }

  fun addSubCategory(name: String, parentId: Long, scope: TransactionType, isMilestone: Boolean) {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      repository.addCategory(
        CategoryEntity(
          name = clean,
          parentId = parentId,
          scope = scope,
          iconKey = "label",
          colorHex = if (isMilestone) 0xFFF6B94D else 0xFF2EE6A8,
          isMilestoneDefault = isMilestone,
        )
      )
    }
  }

  private suspend fun evaluateAndNotifyBudgetAlerts() {
    val settings = preferences.settingsFlow.first()
    if (!settings.budgetAlertNotificationsEnabled) return
    val txs = repository.transactions.first()
    val rules = repository.budgetRules.first()
    val cats = repository.categories.first().associateBy { it.id }
    val evaluated = BudgetAnalyticsEngine.evaluateBudgetRules(txs, rules, cats)
    evaluated.filter { it.isAlertTriggered && it.rule.notificationsEnabled }.forEach { alert ->
      val pct = alert.percentSpent.toInt()
      val label = alert.categoryName ?: alert.rule.period.label
      notificationHelper.postBudgetAlert(
        title = "Budget Alert: ${alert.rule.name}",
        message = "$label spending reached $pct% of your configured limit.",
        notificationId = (alert.rule.id.toInt() + 1000),
      )
    }
  }
}
