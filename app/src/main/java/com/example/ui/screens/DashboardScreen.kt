package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RecurringRuleEntity
import com.example.data.local.TransactionEntity
import com.example.data.prefs.CurrencyFormatter
import com.example.ui.components.AssetDistributionBar
import com.example.ui.components.BudgetPaceTile
import com.example.ui.components.EmptyVaultState
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.VaultCardBox
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.DashboardUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
  state: DashboardUiState,
  onSearchChange: (String) -> Unit,
  onDeleteTransaction: (TransactionEntity) -> Unit,
  onSettleRecurring: (RecurringRuleEntity) -> Unit,
  onNavigateToBudgetsAndBackup: () -> Unit,
  onNavigateToManageSetup: () -> Unit,
  onOpenAddTransaction: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currency = state.settings.currency
  val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().testTag("dashboard_screen"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Header bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("Vault", style = MaterialTheme.typography.headlineLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = ElectricMint, modifier = Modifier.size(14.dp))
              Text(
                "Offline Room Ledger · Strictly Private",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          OutlinedButton(
            onClick = onNavigateToManageSetup,
            modifier = Modifier.testTag("manage_setup_shortcut"),
          ) {
            Text("Banks & Categories")
          }
        }
      }

      // Yearly Google Drive Encrypted Backup Reminder Banner
      if (state.isYearlyBackupDue) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(WarmAmber.copy(alpha = 0.14f))
              .border(1.dp, WarmAmber.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
              .clickable(onClick = onNavigateToBudgetsAndBackup)
              .padding(14.dp)
              .testTag("yearly_backup_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Backup due", tint = WarmAmber)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "Encrypted Google Drive Backup Due",
                style = MaterialTheme.typography.titleSmall,
                color = WarmAmber,
              )
              Text(
                "Save an AES-256 encrypted copy of your database to Google Drive for device safety.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text("Backup", style = MaterialTheme.typography.labelLarge, color = WarmAmber)
          }
        }
      }

      // Net Worth & Balance Distribution Hero Card
      item {
        VaultCardBox(highlightedBorder = ElectricMint.copy(alpha = 0.35f)) {
          Text(
            "NET WORTH",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = CurrencyFormatter.format(state.netWorth, currency),
            style = TabularNumberStyle.copy(
              fontSize = MaterialTheme.typography.displayMedium.fontSize,
              fontWeight = FontWeight.Bold,
            ),
            color = if (state.netWorth >= 0) ElectricMint else CoralCrimson,
            modifier = Modifier.testTag("net_worth_value"),
          )
          Spacer(modifier = Modifier.height(14.dp))
          AssetDistributionBar(
            banksTotal = state.totalBankAndCash,
            investmentsTotal = state.totalInvestments,
            debtsTotal = state.totalDebts,
            currency = currency,
          )
        }
      }

      // Triggered Custom Budget Alerts
      if (state.triggeredAlerts.isNotEmpty()) {
        item {
          VaultCardBox(highlightedBorder = CoralCrimson.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CoralCrimson, modifier = Modifier.size(18.dp))
              Text("Custom Budget Alerts", style = MaterialTheme.typography.titleMedium, color = CoralCrimson)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              state.triggeredAlerts.forEach { alert ->
                val badgeColor = if (alert.isOverBudget) CoralCrimson else WarmAmber
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${alert.rule.name} (${alert.rule.period.label})",
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = "${CurrencyFormatter.formatCompact(alert.spentAmount, currency)} of ${CurrencyFormatter.formatCompact(alert.rule.limitAmount, currency)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = "${alert.percentSpent.toInt()}%",
                    style = TabularNumberStyle.copy(
                      fontSize = MaterialTheme.typography.titleMedium.fontSize,
                      fontWeight = FontWeight.Bold,
                    ),
                    color = badgeColor,
                  )
                }
              }
            }
          }
        }
      }

      // Daily / Monthly / Yearly % Budget Spent Gauges
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Budget Pace (% Spent)", style = MaterialTheme.typography.titleLarge)
            Text(
              text = "Configure",
              style = MaterialTheme.typography.labelLarge,
              color = ElectricMint,
              modifier = Modifier.clickable(onClick = onNavigateToBudgetsAndBackup).padding(4.dp),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            state.periodPaces.forEach { pace ->
              BudgetPaceTile(
                pace = pace,
                currency = currency,
                onClickConfigure = onNavigateToBudgetsAndBackup,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      // Upcoming Recurring Payments
      if (state.upcomingRecurring.isNotEmpty()) {
        item {
          val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Repeat, contentDescription = null, tint = CoolSapphire, modifier = Modifier.size(18.dp))
              Text("Recurring Payments", style = MaterialTheme.typography.titleLarge)
            }
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              state.upcomingRecurring.forEach { rule ->
                Column(
                  modifier = Modifier
                    .width(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurface)
                    .border(1.dp, VaultBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = rule.frequency.label,
                      style = MaterialTheme.typography.labelSmall,
                      color = CoolSapphire,
                    )
                    Text(
                      text = "Due ${dateFmt.format(Date(rule.nextDueTimestamp))}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = rule.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = CurrencyFormatter.format(rule.amount, currency, showDecimals = false),
                    style = TabularNumberStyle.copy(
                      fontSize = MaterialTheme.typography.titleMedium.fontSize,
                      fontWeight = FontWeight.Bold,
                    ),
                    color = ElectricMint,
                  )
                  Button(
                    onClick = { onSettleRecurring(rule) },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = CoolSapphire.copy(alpha = 0.2f),
                      contentColor = CoolSapphire,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text("Log Payment Now", style = MaterialTheme.typography.labelLarge)
                  }
                }
              }
            }
          }
        }
      }

      // Transactions Header & Search
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Recent Ledger (${state.filteredTransactions.size})",
            style = MaterialTheme.typography.titleLarge,
          )
          OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search merchant, bank, location, category...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("tx_search_input"),
          )
        }
      }

      if (state.filteredTransactions.isEmpty()) {
        item {
          EmptyVaultState(
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            title = if (state.totalTransactionCount == 0) "Your Vault Ledger Is Ready" else "No Matching Entries",
            subtitle = if (state.totalTransactionCount == 0) {
              "Tap + Log Entry below to record an expense, income, recurring bill, travel trip, or investment."
            } else {
              "Try clearing your search filter."
            },
          )
          if (state.totalTransactionCount == 0) {
            Spacer(Modifier.height(10.dp))
            Button(
              onClick = onOpenAddTransaction,
              colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("empty_add_tx_button"),
            ) {
              Text("Log Your First Transaction")
            }
          }
        }
      } else {
        items(state.filteredTransactions, key = { it.id }) { tx ->
          TransactionRowCard(
            tx = tx,
            categoriesById = categoriesById,
            currency = currency,
            onDelete = { onDeleteTransaction(tx) },
          )
        }
      }

      item { Spacer(Modifier.height(76.dp)) }
    }
  }
}
