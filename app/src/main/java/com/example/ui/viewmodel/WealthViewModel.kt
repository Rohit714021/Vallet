package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.VaultRepository
import com.example.data.prefs.VaultPreferences
import com.example.data.prefs.VaultSettingsState
import com.example.domain.DebtPayoffResult
import com.example.domain.FinancialCalculator
import com.example.domain.InvestmentProjection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvestmentCalcInputs(
  val lumpsum: Double = 100000.0,
  val monthlySip: Double = 10000.0,
  val annualRatePercent: Double = 12.0,
  val years: Int = 10,
)

data class DebtCalcInputs(
  val principal: Double = 500000.0,
  val annualRatePercent: Double = 9.5,
  val tenureMonths: Int = 60,
  val extraMonthlyPayment: Double = 2000.0,
)

data class WealthUiState(
  val settings: VaultSettingsState = VaultSettingsState(),
  val bankAndCashAccounts: List<AccountEntity> = emptyList(),
  val creditCards: List<AccountEntity> = emptyList(),
  val investments: List<AccountEntity> = emptyList(),
  val debts: List<AccountEntity> = emptyList(),
  val totalBanksAndCash: Double = 0.0,
  val totalInvestments: Double = 0.0,
  val totalDebtsAndCards: Double = 0.0,
  val netWorth: Double = 0.0,
  val invInputs: InvestmentCalcInputs = InvestmentCalcInputs(),
  val invProjection: InvestmentProjection = FinancialCalculator.calculateInvestmentProjection(
    100000.0, 10000.0, 12.0, 10
  ),
  val debtInputs: DebtCalcInputs = DebtCalcInputs(),
  val debtProjection: DebtPayoffResult = FinancialCalculator.calculateDebtPayoff(
    500000.0, 9.5, 60, 2000.0
  ),
)

class WealthViewModel(
  private val repository: VaultRepository,
  preferences: VaultPreferences,
) : ViewModel() {

  private val invInputs = MutableStateFlow(InvestmentCalcInputs())
  private val debtInputs = MutableStateFlow(DebtCalcInputs())

  val uiState: StateFlow<WealthUiState> = combine(
    preferences.settingsFlow,
    repository.accounts,
    invInputs,
    debtInputs,
  ) { settings, accounts, invIn, debtIn ->
    val banks = accounts.filter { it.type == AccountType.BANK || it.type == AccountType.CASH }
    val cards = accounts.filter { it.type == AccountType.CREDIT_CARD }
    val invs = accounts.filter { it.type == AccountType.INVESTMENT }
    val debtList = accounts.filter { it.type == AccountType.DEBT }

    val bankSum = banks.sumOf { it.balance }
    val invSum = invs.sumOf { it.balance }
    val debtSum = debtList.sumOf { it.balance } + cards.sumOf { it.balance }

    WealthUiState(
      settings = settings,
      bankAndCashAccounts = banks,
      creditCards = cards,
      investments = invs,
      debts = debtList,
      totalBanksAndCash = bankSum,
      totalInvestments = invSum,
      totalDebtsAndCards = debtSum,
      netWorth = bankSum + invSum - debtSum,
      invInputs = invIn,
      invProjection = FinancialCalculator.calculateInvestmentProjection(
        lumpsum = invIn.lumpsum,
        monthlySip = invIn.monthlySip,
        annualRatePercent = invIn.annualRatePercent,
        years = invIn.years,
      ),
      debtInputs = debtIn,
      debtProjection = FinancialCalculator.calculateDebtPayoff(
        principal = debtIn.principal,
        annualRatePercent = debtIn.annualRatePercent,
        tenureMonths = debtIn.tenureMonths,
        extraMonthlyPayment = debtIn.extraMonthlyPayment,
      ),
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = WealthUiState(),
  )

  fun saveAccount(account: AccountEntity) {
    viewModelScope.launch {
      repository.saveAccount(account)
    }
  }

  fun deleteAccount(id: Long) {
    viewModelScope.launch {
      repository.deleteAccount(id)
    }
  }

  fun updateInvestmentInputs(
    lumpsum: Double? = null,
    monthlySip: Double? = null,
    annualRatePercent: Double? = null,
    years: Int? = null,
  ) {
    invInputs.update { cur ->
      cur.copy(
        lumpsum = lumpsum ?: cur.lumpsum,
        monthlySip = monthlySip ?: cur.monthlySip,
        annualRatePercent = annualRatePercent ?: cur.annualRatePercent,
        years = years ?: cur.years,
      )
    }
  }

  fun updateDebtInputs(
    principal: Double? = null,
    annualRatePercent: Double? = null,
    tenureMonths: Int? = null,
    extraMonthlyPayment: Double? = null,
  ) {
    debtInputs.update { cur ->
      cur.copy(
        principal = principal ?: cur.principal,
        annualRatePercent = annualRatePercent ?: cur.annualRatePercent,
        tenureMonths = tenureMonths ?: cur.tenureMonths,
        extraMonthlyPayment = extraMonthlyPayment ?: cur.extraMonthlyPayment,
      )
    }
  }

  fun loadAccountIntoCalculator(account: AccountEntity) {
    when (account.type) {
      AccountType.INVESTMENT -> {
        invInputs.update {
          it.copy(
            lumpsum = account.balance.coerceAtLeast(0.0),
            annualRatePercent = if (account.annualRatePercent > 0) account.annualRatePercent else 12.0,
            years = if (account.tenureMonths >= 12) (account.tenureMonths / 12) else 10,
          )
        }
      }
      AccountType.DEBT, AccountType.CREDIT_CARD -> {
        debtInputs.update {
          it.copy(
            principal = account.balance.coerceAtLeast(0.0),
            annualRatePercent = if (account.annualRatePercent > 0) account.annualRatePercent else 10.5,
            tenureMonths = if (account.tenureMonths > 0) account.tenureMonths else 36,
          )
        }
      }
      else -> Unit
    }
  }
}
