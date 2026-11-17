package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.prefs.CurrencyFormatter
import com.example.data.prefs.VaultCurrency
import com.example.domain.FinancialCalculator
import com.example.ui.components.AssetDistributionBar
import com.example.ui.components.DualSeriesPoint
import com.example.ui.components.EmptyVaultState
import com.example.ui.components.ProjectionLineChart
import com.example.ui.components.VaultCardBox
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.WealthUiState

private enum class WealthTab(val label: String) {
  PORTFOLIO("Banks & Holdings"),
  CALCULATOR("Time & Rate Calculator"),
}

private enum class CalcMode(val label: String) {
  INVESTMENT("Investment Growth"),
  DEBT("Debt / Loan EMI"),
}

@Composable
fun WealthScreen(
  state: WealthUiState,
  onSaveAccount: (AccountEntity) -> Unit,
  onDeleteAccount: (Long) -> Unit,
  onUpdateInvestmentInputs: (lumpsum: Double?, monthlySip: Double?, rate: Double?, years: Int?) -> Unit,
  onUpdateDebtInputs: (principal: Double?, rate: Double?, months: Int?, extra: Double?) -> Unit,
  onLoadAccountToCalculator: (AccountEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  var activeTab by remember { mutableStateOf(WealthTab.PORTFOLIO) }
  var calcMode by remember { mutableStateOf(CalcMode.INVESTMENT) }
  var showAddAccountDialog by remember { mutableStateOf(false) }
  var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }

  val currency = state.settings.currency

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().testTag("wealth_screen"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Wealth & Calculators", style = MaterialTheme.typography.headlineLarge)
          Button(
            onClick = {
              editingAccount = null
              showAddAccountDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
            modifier = Modifier.testTag("add_account_button"),
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add Account")
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          WealthTab.entries.forEach { tab ->
            FilterChip(
              selected = activeTab == tab,
              onClick = { activeTab = tab },
              label = { Text(tab.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricMint.copy(alpha = 0.2f),
                selectedLabelColor = ElectricMint,
              ),
              modifier = Modifier.testTag("wealth_tab_${tab.name.lowercase()}"),
            )
          }
        }
      }

      if (activeTab == WealthTab.PORTFOLIO) {
        item {
          VaultCardBox(highlightedBorder = CoolSapphire.copy(alpha = 0.4f)) {
            Text("HOLDINGS DISTRIBUTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
              CurrencyFormatter.format(state.netWorth, currency),
              style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.displayMedium.fontSize, fontWeight = FontWeight.Bold),
              color = if (state.netWorth >= 0) ElectricMint else CoralCrimson,
            )
            Spacer(Modifier.height(12.dp))
            AssetDistributionBar(
              banksTotal = state.totalBanksAndCash,
              investmentsTotal = state.totalInvestments,
              debtsTotal = state.totalDebtsAndCards,
              currency = currency,
            )
          }
        }

        // Banks & Cash Section
        item {
          SectionHeader("Banks & Cash Wallets", CurrencyFormatter.format(state.totalBanksAndCash, currency), CoolSapphire)
        }
        if (state.bankAndCashAccounts.isEmpty()) {
          item {
            EmptyVaultState(
              icon = Icons.Default.AccountBalance,
              title = "No Bank Accounts Added",
              subtitle = "Add your savings or checking banks to visualize exact balances across institutions.",
            )
          }
        } else {
          items(state.bankAndCashAccounts, key = { "bank_${it.id}" }) { acc ->
            AccountPortfolioCard(
              account = acc,
              accent = CoolSapphire,
              currency = currency,
              onEdit = {
                editingAccount = acc
                showAddAccountDialog = true
              },
              onDelete = { onDeleteAccount(acc.id) },
              onCalculate = null,
            )
          }
        }

        // Investments Section
        item {
          SectionHeader("Investments", CurrencyFormatter.format(state.totalInvestments, currency), ElectricMint)
        }
        if (state.investments.isEmpty()) {
          item {
            EmptyVaultState(
              icon = Icons.Default.TrendingUp,
              title = "No Investment Holdings",
              subtitle = "Add Mutual Funds, Stocks, FD, or Gold with expected return rates to track and project growth.",
            )
          }
        } else {
          items(state.investments, key = { "inv_${it.id}" }) { acc ->
            AccountPortfolioCard(
              account = acc,
              accent = ElectricMint,
              currency = currency,
              onEdit = {
                editingAccount = acc
                showAddAccountDialog = true
              },
              onDelete = { onDeleteAccount(acc.id) },
              onCalculate = {
                onLoadAccountToCalculator(acc)
                calcMode = CalcMode.INVESTMENT
                activeTab = WealthTab.CALCULATOR
              },
            )
          }
        }

        // Debts & Credit Cards Section
        item {
          SectionHeader("Debts & Credit Cards", CurrencyFormatter.format(state.totalDebtsAndCards, currency), CoralCrimson)
        }
        val allLiabilities = state.debts + state.creditCards
        if (allLiabilities.isEmpty()) {
          item {
            EmptyVaultState(
              icon = Icons.Default.CreditCard,
              title = "Zero Tracked Debts",
              subtitle = "Add loans or credit cards with interest rates to plan faster debt payoff.",
            )
          }
        } else {
          items(allLiabilities, key = { "debt_${it.id}" }) { acc ->
            AccountPortfolioCard(
              account = acc,
              accent = CoralCrimson,
              currency = currency,
              onEdit = {
                editingAccount = acc
                showAddAccountDialog = true
              },
              onDelete = { onDeleteAccount(acc.id) },
              onCalculate = {
                onLoadAccountToCalculator(acc)
                calcMode = CalcMode.DEBT
                activeTab = WealthTab.CALCULATOR
              },
            )
          }
        }
      } else {
        // Time & Interest Rate Calculator Tab
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcMode.entries.forEach { mode ->
              FilterChip(
                selected = calcMode == mode,
                onClick = { calcMode = mode },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("calc_mode_${mode.name.lowercase()}"),
              )
            }
          }
        }

        if (calcMode == CalcMode.INVESTMENT) {
          item {
            InvestmentCalculatorContent(
              state = state,
              currency = currency,
              onUpdate = onUpdateInvestmentInputs,
            )
          }
        } else {
          item {
            DebtCalculatorContent(
              state = state,
              currency = currency,
              onUpdate = onUpdateDebtInputs,
            )
          }
        }
      }

      item { Spacer(Modifier.height(76.dp)) }
    }
  }

  if (showAddAccountDialog) {
    AddEditAccountDialog(
      existing = editingAccount,
      currency = currency,
      onDismiss = { showAddAccountDialog = false },
      onSave = {
        onSaveAccount(it)
        showAddAccountDialog = false
      },
    )
  }
}

@Composable
private fun SectionHeader(title: String, totalFormatted: String, color: Color) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(
      totalFormatted,
      style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold),
      color = color,
    )
  }
}

@Composable
private fun AccountPortfolioCard(
  account: AccountEntity,
  accent: Color,
  currency: VaultCurrency,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onCalculate: (() -> Unit)?,
) {
  VaultCardBox(highlightedBorder = accent.copy(alpha = 0.35f)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(account.name, style = MaterialTheme.typography.titleMedium)
        Text(
          buildString {
            append(account.institution.ifBlank { account.type.label })
            if (account.annualRatePercent > 0.0) append(" · ${account.annualRatePercent}% p.a.")
            if (account.tenureMonths > 0) append(" · ${FinancialCalculator.formatTenure(account.tenureMonths)}")
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        CurrencyFormatter.format(account.balance, currency, showDecimals = false),
        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.Bold),
        color = accent,
      )
    }

    if (account.type == AccountType.INVESTMENT && account.annualRatePercent > 0.0) {
      val oneYr = FinancialCalculator.projectSingleHoldingInOneYear(account.balance, account.annualRatePercent)
      Spacer(Modifier.height(6.dp))
      Text(
        "1-Year Projected Value: ${CurrencyFormatter.format(oneYr, currency, showDecimals = false)}",
        style = MaterialTheme.typography.bodySmall,
        color = ElectricMint,
      )
    }

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
        Text("Edit")
      }
      if (onCalculate != null) {
        OutlinedButton(onClick = onCalculate, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
          Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Project")
        }
      }
      Spacer(Modifier.weight(1f))
      IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete account", tint = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun InvestmentCalculatorContent(
  state: WealthUiState,
  currency: VaultCurrency,
  onUpdate: (lumpsum: Double?, monthlySip: Double?, rate: Double?, years: Int?) -> Unit,
) {
  val inputs = state.invInputs
  val proj = state.invProjection
  var lumpsumText by remember(inputs.lumpsum) { mutableStateOf(inputs.lumpsum.toLong().toString()) }
  var sipText by remember(inputs.monthlySip) { mutableStateOf(inputs.monthlySip.toLong().toString()) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    VaultCardBox(highlightedBorder = ElectricMint.copy(alpha = 0.4f)) {
      Text("PROJECTED FUTURE VALUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        CurrencyFormatter.format(proj.futureValue, currency, showDecimals = false),
        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.displayMedium.fontSize, fontWeight = FontWeight.Bold),
        color = ElectricMint,
        modifier = Modifier.testTag("inv_future_value"),
      )
      Spacer(Modifier.height(10.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
          Text("Total Invested", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(CurrencyFormatter.formatCompact(proj.totalInvested, currency), style = TabularNumberStyle, color = CoolSapphire)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Wealth Gained", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(CurrencyFormatter.formatCompact(proj.totalGain, currency), style = TabularNumberStyle, color = ElectricMint)
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Multiple", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${String.format("%.2f", proj.returnMultiple)}x", style = TabularNumberStyle, color = WarmAmber)
        }
      }
      Spacer(Modifier.height(14.dp))
      ProjectionLineChart(
        points = proj.yearlyPoints.map { DualSeriesPoint("Yr ${it.year}", it.totalValue, it.investedCapital) },
        primaryLabel = "Value",
        secondaryLabel = "Invested",
        primaryColor = ElectricMint,
        secondaryColor = CoolSapphire,
        currency = currency,
      )
    }

    VaultCardBox {
      Text("Investment Parameters", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = lumpsumText,
          onValueChange = { v ->
            lumpsumText = v
            v.toDoubleOrNull()?.let { onUpdate(it, null, null, null) }
          },
          label = { Text("Initial Lumpsum (${currency.symbol})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("calc_lumpsum_input"),
        )
        OutlinedTextField(
          value = sipText,
          onValueChange = { v ->
            sipText = v
            v.toDoubleOrNull()?.let { onUpdate(null, it, null, null) }
          },
          label = { Text("Monthly SIP (${currency.symbol})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("calc_sip_input"),
        )
      }
      Spacer(Modifier.height(10.dp))
      Text("Expected Annual Return: ${String.format("%.1f", inputs.annualRatePercent)}%", style = MaterialTheme.typography.labelLarge)
      Slider(
        value = inputs.annualRatePercent.toFloat().coerceIn(1f, 30f),
        onValueChange = { onUpdate(null, null, it.toDouble(), null) },
        valueRange = 1f..30f,
        colors = SliderDefaults.colors(thumbColor = ElectricMint, activeTrackColor = ElectricMint),
      )
      Text("Investment Time Horizon: ${inputs.years} Years", style = MaterialTheme.typography.labelLarge)
      Slider(
        value = inputs.years.toFloat().coerceIn(1f, 40f),
        onValueChange = { onUpdate(null, null, null, it.toInt()) },
        valueRange = 1f..40f,
        colors = SliderDefaults.colors(thumbColor = CoolSapphire, activeTrackColor = CoolSapphire),
      )
    }

    VaultCardBox {
      Text("Year-by-Year Compounding Milestone", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(8.dp))
      proj.yearlyPoints.forEach { pt ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Year ${pt.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            "In: ${CurrencyFormatter.formatCompact(pt.investedCapital, currency)}",
            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
            color = CoolSapphire,
          )
          Text(
            CurrencyFormatter.formatCompact(pt.totalValue, currency),
            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Bold),
            color = ElectricMint,
          )
        }
      }
    }
  }
}

@Composable
private fun DebtCalculatorContent(
  state: WealthUiState,
  currency: VaultCurrency,
  onUpdate: (principal: Double?, rate: Double?, months: Int?, extra: Double?) -> Unit,
) {
  val inputs = state.debtInputs
  val res = state.debtProjection
  var principalText by remember(inputs.principal) { mutableStateOf(inputs.principal.toLong().toString()) }
  var extraText by remember(inputs.extraMonthlyPayment) { mutableStateOf(inputs.extraMonthlyPayment.toLong().toString()) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    VaultCardBox(highlightedBorder = CoralCrimson.copy(alpha = 0.4f)) {
      Text("MONTHLY EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        CurrencyFormatter.format(res.baseMonthlyEmi, currency, showDecimals = false),
        style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.displayMedium.fontSize, fontWeight = FontWeight.Bold),
        color = CoralCrimson,
        modifier = Modifier.testTag("debt_emi_value"),
      )
      Spacer(Modifier.height(10.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
          Text("Total Interest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(CurrencyFormatter.formatCompact(res.totalInterestActual, currency), style = TabularNumberStyle, color = WarmAmber)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Interest Saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(CurrencyFormatter.formatCompact(res.interestSaved, currency), style = TabularNumberStyle, color = ElectricMint)
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Payoff Time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(FinancialCalculator.formatTenure(res.actualMonthsToPayoff), style = TabularNumberStyle, color = CoolSapphire)
        }
      }
      if (res.yearlySchedule.size >= 2) {
        Spacer(Modifier.height(14.dp))
        ProjectionLineChart(
          points = res.yearlySchedule.map { DualSeriesPoint("Yr ${it.year}", it.remainingBalance, it.cumulativeInterestPaid) },
          primaryLabel = "Balance",
          secondaryLabel = "Interest",
          primaryColor = CoralCrimson,
          secondaryColor = WarmAmber,
          currency = currency,
        )
      }
    }

    VaultCardBox {
      Text("Loan & Prepayment Parameters", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = principalText,
          onValueChange = { v ->
            principalText = v
            v.toDoubleOrNull()?.let { onUpdate(it, null, null, null) }
          },
          label = { Text("Loan Principal (${currency.symbol})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("calc_principal_input"),
        )
        OutlinedTextField(
          value = extraText,
          onValueChange = { v ->
            extraText = v
            v.toDoubleOrNull()?.let { onUpdate(null, null, null, it) }
          },
          label = { Text("Extra Monthly (${currency.symbol})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("calc_extra_input"),
        )
      }
      Spacer(Modifier.height(10.dp))
      Text("Interest Rate: ${String.format("%.1f", inputs.annualRatePercent)}% p.a.", style = MaterialTheme.typography.labelLarge)
      Slider(
        value = inputs.annualRatePercent.toFloat().coerceIn(1f, 36f),
        onValueChange = { onUpdate(null, it.toDouble(), null, null) },
        valueRange = 1f..36f,
        colors = SliderDefaults.colors(thumbColor = CoralCrimson, activeTrackColor = CoralCrimson),
      )
      Text("Tenure: ${FinancialCalculator.formatTenure(inputs.tenureMonths)}", style = MaterialTheme.typography.labelLarge)
      Slider(
        value = inputs.tenureMonths.toFloat().coerceIn(6f, 360f),
        onValueChange = { onUpdate(null, null, it.toInt(), null) },
        valueRange = 6f..360f,
        colors = SliderDefaults.colors(thumbColor = WarmAmber, activeTrackColor = WarmAmber),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditAccountDialog(
  existing: AccountEntity?,
  currency: VaultCurrency,
  onDismiss: () -> Unit,
  onSave: (AccountEntity) -> Unit,
) {
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var institution by remember { mutableStateOf(existing?.institution ?: "") }
  var type by remember { mutableStateOf(existing?.type ?: AccountType.BANK) }
  var balanceText by remember { mutableStateOf(existing?.balance?.toString() ?: "") }
  var rateText by remember { mutableStateOf(if ((existing?.annualRatePercent ?: 0.0) > 0.0) existing?.annualRatePercent.toString() else "") }
  var tenureText by remember { mutableStateOf(if ((existing?.tenureMonths ?: 0) > 0) existing?.tenureMonths.toString() else "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Add Bank / Investment / Debt" else "Edit Account") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          AccountType.entries.forEach { t ->
            FilterChip(
              selected = type == t,
              onClick = { type = t },
              label = { Text(t.label) },
            )
          }
        }
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Account Name (e.g., HDFC Savings, Nifty SIP)") },
          singleLine = true,
        )
        OutlinedTextField(
          value = institution,
          onValueChange = { institution = it },
          label = { Text("Bank / Broker / Lender") },
          singleLine = true,
        )
        OutlinedTextField(
          value = balanceText,
          onValueChange = { balanceText = it },
          label = { Text("Current Balance (${currency.symbol})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
        )
        if (type == AccountType.INVESTMENT || type == AccountType.DEBT || type == AccountType.CREDIT_CARD) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = rateText,
              onValueChange = { rateText = it },
              label = { Text("Rate % p.a.") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = tenureText,
              onValueChange = { tenureText = it },
              label = { Text("Months") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanName = name.trim()
          if (cleanName.isNotEmpty()) {
            onSave(
              AccountEntity(
                id = existing?.id ?: 0L,
                name = cleanName,
                institution = institution.trim(),
                type = type,
                balance = balanceText.toDoubleOrNull() ?: 0.0,
                annualRatePercent = rateText.toDoubleOrNull() ?: 0.0,
                tenureMonths = tenureText.toIntOrNull() ?: 0,
              )
            )
          }
        },
        modifier = Modifier.testTag("confirm_save_account_button"),
      ) { Text("Save") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
