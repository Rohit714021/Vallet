package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetRuleEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionType
import com.example.data.local.VaultRepository
import com.example.data.prefs.VaultCurrency
import com.example.data.prefs.VaultPreferences
import com.example.data.prefs.VaultSettingsState
import com.example.data.security.BackupResult
import com.example.data.security.RestoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
  val settings: VaultSettingsState = VaultSettingsState(),
  val budgetRules: List<BudgetRuleEntity> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val accountsCount: Int = 0,
  val transactionsCount: Int = 0,
  val isWorking: Boolean = false,
  val statusMessage: String? = null,
  val isStatusError: Boolean = false,
)

private data class SettingsDataStreams(
  val settings: VaultSettingsState,
  val rules: List<BudgetRuleEntity>,
  val categories: List<CategoryEntity>,
  val accountsCount: Int,
  val txCount: Int,
)

class SettingsViewModel(
  private val repository: VaultRepository,
  private val preferences: VaultPreferences,
) : ViewModel() {

  private val isWorking = MutableStateFlow(false)
  private val statusMessage = MutableStateFlow<String?>(null)
  private val isStatusError = MutableStateFlow(false)

  private val dataStreams = combine(
    preferences.settingsFlow,
    repository.budgetRules,
    repository.categories,
    repository.accounts,
    repository.transactions,
  ) { settings, rules, categories, accounts, transactions ->
    SettingsDataStreams(settings, rules, categories, accounts.size, transactions.size)
  }

  val uiState: StateFlow<SettingsUiState> = combine(
    dataStreams,
    isWorking,
    statusMessage,
    isStatusError,
  ) { data, working, msg, err ->
    SettingsUiState(
      settings = data.settings,
      budgetRules = data.rules,
      categories = data.categories,
      accountsCount = data.accountsCount,
      transactionsCount = data.txCount,
      isWorking = working,
      statusMessage = msg,
      isStatusError = err,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SettingsUiState(),
  )

  fun setCurrency(currency: VaultCurrency) {
    viewModelScope.launch { preferences.setCurrency(currency) }
  }

  fun setAppLockEnabled(enabled: Boolean) {
    viewModelScope.launch { preferences.setAppLockEnabled(enabled) }
  }

  fun savePin(pin: String) {
    if (pin.length != 4) return
    viewModelScope.launch {
      preferences.savePin(pin)
      setStatus("4-digit fallback PIN enabled and vault lock activated.", isError = false)
    }
  }

  fun clearPin() {
    viewModelScope.launch {
      preferences.clearPin()
      setStatus("Fallback PIN cleared.", isError = false)
    }
  }

  fun setYearlyReminderEnabled(enabled: Boolean) {
    viewModelScope.launch { preferences.setYearlyReminderEnabled(enabled) }
  }

  fun setBudgetNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch { preferences.setBudgetNotificationsEnabled(enabled) }
  }

  fun saveBudgetRule(rule: BudgetRuleEntity) {
    viewModelScope.launch { repository.saveBudgetRule(rule) }
  }

  fun deleteBudgetRule(id: Long) {
    viewModelScope.launch { repository.deleteBudgetRule(id) }
  }

  fun addCategory(name: String, parentId: Long?, scope: TransactionType, isMilestone: Boolean) {
    val clean = name.trim()
    if (clean.isEmpty()) return
    viewModelScope.launch {
      repository.addCategory(
        CategoryEntity(
          name = clean,
          parentId = parentId,
          scope = scope,
          iconKey = if (isMilestone) "celebration" else "category",
          colorHex = if (isMilestone) 0xFFF6B94D else 0xFF2EE6A8,
          isMilestoneDefault = isMilestone,
        )
      )
    }
  }

  fun deleteCategory(id: Long) {
    viewModelScope.launch { repository.deleteCategory(id) }
  }

  fun exportEncryptedDatabase(uri: Uri, passphrase: String) {
    viewModelScope.launch {
      isWorking.value = true
      when (val res = repository.exportEncryptedDatabaseToUri(uri, passphrase)) {
        is BackupResult.Success -> {
          preferences.recordBackup(System.currentTimeMillis(), res.fileName)
          val kb = (res.sizeBytes / 1024L).coerceAtLeast(1L)
          setStatus("Encrypted database (${kb} KB) stored safely in ${res.fileName}.", isError = false)
        }
        is BackupResult.Failure -> {
          setStatus(res.message, isError = true)
        }
      }
      isWorking.value = false
    }
  }

  fun restoreEncryptedDatabase(uri: Uri, passphrase: String) {
    viewModelScope.launch {
      isWorking.value = true
      when (val res = repository.restoreEncryptedDatabaseFromUri(uri, passphrase)) {
        is RestoreResult.Success -> {
          val kb = (res.sizeBytes / 1024L).coerceAtLeast(1L)
          setStatus("Restored encrypted database (${kb} KB) from ${res.fileName}.", isError = false)
        }
        is RestoreResult.Failure -> {
          setStatus(res.message, isError = true)
        }
      }
      isWorking.value = false
    }
  }

  fun clearStatusMessage() {
    statusMessage.value = null
  }

  private fun setStatus(msg: String, isError: Boolean) {
    statusMessage.value = msg
    isStatusError.value = isError
  }
}
