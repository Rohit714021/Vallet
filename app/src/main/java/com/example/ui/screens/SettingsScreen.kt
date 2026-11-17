package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetPeriod
import com.example.data.local.BudgetRuleEntity
import com.example.data.local.TransactionType
import com.example.data.prefs.CurrencyFormatter
import com.example.data.prefs.VaultCurrency
import com.example.ui.components.VaultCardBox
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.SettingsUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  state: SettingsUiState,
  onSetCurrency: (VaultCurrency) -> Unit,
  onSetAppLock: (Boolean) -> Unit,
  onSavePin: (String) -> Unit,
  onClearPin: () -> Unit,
  onSetYearlyReminder: (Boolean) -> Unit,
  onSetBudgetNotifications: (Boolean) -> Unit,
  onSaveBudgetRule: (BudgetRuleEntity) -> Unit,
  onDeleteBudgetRule: (Long) -> Unit,
  onOpenManageSetup: () -> Unit,
  onExportEncryptedDb: (Uri, String) -> Unit,
  onRestoreEncryptedDb: (Uri, String) -> Unit,
  onClearStatus: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
  val curYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
  val currency = state.settings.currency

  var pendingBackupPassphrase by remember { mutableStateOf("") }
  var showBackupPassphraseDialog by remember { mutableStateOf(false) }

  var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
  var showRestorePassphraseDialog by remember { mutableStateOf(false) }

  var showAddBudgetDialog by remember { mutableStateOf(false) }
  var showSetPinDialog by remember { mutableStateOf(false) }

  val notificationPermLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    onSetBudgetNotifications(granted)
  }

  val createBackupLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri ->
    if (uri != null && pendingBackupPassphrase.isNotBlank()) {
      onExportEncryptedDb(uri, pendingBackupPassphrase)
      pendingBackupPassphrase = ""
    }
  }

  val openRestoreLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      pendingRestoreUri = uri
      showRestorePassphraseDialog = true
    }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().testTag("settings_screen"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text("Security, Budgets & Drive Vault", style = MaterialTheme.typography.headlineLarge)
      }

      // Status banner for backup/restore/PIN actions
      state.statusMessage?.let { msg ->
        item {
          val bannerColor = if (state.isStatusError) CoralCrimson else ElectricMint
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(bannerColor.copy(alpha = 0.14f))
              .border(1.dp, bannerColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = bannerColor, modifier = Modifier.weight(1f))
            TextButton(onClick = onClearStatus) { Text("Dismiss") }
          }
        }
      }

      // 1. Encrypted Google Drive Database Backup & Restore
      item {
        VaultCardBox(highlightedBorder = ElectricMint.copy(alpha = 0.45f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = ElectricMint)
            Text("Encrypted Google Drive Database", style = MaterialTheme.typography.titleLarge)
          }
          Spacer(Modifier.height(6.dp))
          Text(
            "Your expense ledger runs 100% offline in Room. To safeguard data across devices, export the complete SQLite database encrypted with AES-256-GCM directly into Google Drive via the system picker.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(10.dp))
          Text(
            text = if (state.settings.lastBackupTimestamp > 0L) {
              "Last Backup: ${dateFmt.format(Date(state.settings.lastBackupTimestamp))} (${state.settings.lastBackupFileName})"
            } else {
              "Last Backup: Never backed up (${state.transactionsCount} transactions, ${state.accountsCount} accounts)"
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (state.settings.lastBackupTimestamp > 0L) ElectricMint else WarmAmber,
          )
          Spacer(Modifier.height(12.dp))

          if (state.isWorking) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricMint, strokeWidth = 2.dp)
              Text("Encrypting / processing database...", style = MaterialTheme.typography.bodyMedium)
            }
          } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = { showBackupPassphraseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
                modifier = Modifier.weight(1f).testTag("backup_drive_button"),
              ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Backup to Drive")
              }
              OutlinedButton(
                onClick = { openRestoreLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f).testTag("restore_drive_button"),
              ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore from Drive")
              }
            }
          }

          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Yearly Backup Reminder", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = state.settings.yearlyBackupReminderEnabled,
              onCheckedChange = onSetYearlyReminder,
            )
          }
        }
      }

      // 2. Budget Rules & Custom Alerts
      item {
        VaultCardBox(highlightedBorder = WarmAmber.copy(alpha = 0.35f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = WarmAmber)
              Text("Budgets & Custom Alerts", style = MaterialTheme.typography.titleLarge)
            }
            Button(
              onClick = { showAddBudgetDialog = true },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("add_budget_rule_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Add Rule")
            }
          }
          Spacer(Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Push notification when threshold crossed", style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = state.settings.budgetAlertNotificationsEnabled,
              onCheckedChange = { enable ->
                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  onSetBudgetNotifications(enable)
                }
              },
            )
          }
          Spacer(Modifier.height(8.dp))
          if (state.budgetRules.isEmpty()) {
            Text(
              "No budget rules yet. Add Daily, Monthly, Yearly, or Category caps with custom % alerts.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            val catsById = state.categories.associateBy { it.id }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              state.budgetRules.forEach { rule ->
                val scopeLabel = rule.categoryId?.let { catsById[it]?.name } ?: "All Spending"
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VaultSurface)
                    .border(1.dp, VaultBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("${rule.name} (${rule.period.label})", style = MaterialTheme.typography.titleSmall)
                    Text(
                      "$scopeLabel · Limit ${CurrencyFormatter.format(rule.limitAmount, currency, false)} · Alert @ ${rule.alertThresholdPercent}%",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(
                    onClick = { onDeleteBudgetRule(rule.id) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete budget rule")
                  }
                }
              }
            }
          }
        }
      }

      // 3. Currency Switcher
      item {
        VaultCardBox {
          Text("Display Currency", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(4.dp))
          Text(
            "Default is Indian Rupee (INR). Switching updates all charts, balances, and calculators.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            VaultCurrency.entries.forEach { cur ->
              FilterChip(
                selected = currency == cur,
                onClick = { onSetCurrency(cur) },
                label = { Text("${cur.symbol} ${cur.code}") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ElectricMint.copy(alpha = 0.2f),
                  selectedLabelColor = ElectricMint,
                ),
                modifier = Modifier.testTag("currency_${cur.code.lowercase()}"),
              )
            }
          }
        }
      }

      // 4. Privacy & Biometric / PIN Lock
      item {
        VaultCardBox {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CoolSapphire)
            Text("Biometric & PIN Privacy Lock", style = MaterialTheme.typography.titleLarge)
          }
          Spacer(Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Require Biometrics / Device PIN on Launch", style = MaterialTheme.typography.titleSmall)
              Text(
                "Protects balances and ledger behind system fingerprint, face, or PIN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = state.settings.appLockEnabled,
              onCheckedChange = onSetAppLock,
              modifier = Modifier.testTag("biometric_lock_switch"),
            )
          }
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = { showSetPinDialog = true },
              modifier = Modifier.testTag("set_pin_button"),
            ) {
              Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(if (state.settings.hasPinConfigured) "Update 4-Digit PIN" else "Set 4-Digit Fallback PIN")
            }
            if (state.settings.hasPinConfigured) {
              TextButton(onClick = onClearPin) {
                Text("Clear PIN", color = CoralCrimson)
              }
            }
          }
        }
      }

      // 5. Dedicated Setup Window for Banks, Categories & Sub-Categories
      item {
        VaultCardBox {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Banks, Categories & Sub-Categories", style = MaterialTheme.typography.titleLarge)
              Text(
                "${state.accountsCount} accounts · ${state.categories.size} categories & sub-categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            OutlinedButton(
              onClick = onOpenManageSetup,
              modifier = Modifier.testTag("settings_open_setup_button"),
            ) {
              Text("Open Setup")
            }
          }
        }
      }

      item { Spacer(Modifier.height(76.dp)) }
    }
  }

  // Backup Passphrase Dialog
  if (showBackupPassphraseDialog) {
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showBackupPassphraseDialog = false },
      title = { Text("Encrypt Database for Google Drive") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Choose a strong vault passphrase. Without it, the encrypted database stored on Google Drive cannot be decrypted.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = pass1,
            onValueChange = { pass1 = it; error = null },
            label = { Text("Vault Passphrase (min 4 chars)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
          )
          OutlinedTextField(
            value = pass2,
            onValueChange = { pass2 = it; error = null },
            label = { Text("Confirm Passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
          )
          error?.let { Text(it, color = CoralCrimson, style = MaterialTheme.typography.bodySmall) }
        }
      },
      confirmButton = {
        Button(onClick = {
          when {
            pass1.length < 4 -> error = "Passphrase must be at least 4 characters."
            pass1 != pass2 -> error = "Passphrases do not match."
            else -> {
              pendingBackupPassphrase = pass1
              showBackupPassphraseDialog = false
              createBackupLauncher.launch("ObsidianVault_DB_${curYear}.enc")
            }
          }
        }) { Text("Choose Google Drive Location") }
      },
      dismissButton = {
        TextButton(onClick = { showBackupPassphraseDialog = false }) { Text("Cancel") }
      },
    )
  }

  // Restore Passphrase Dialog
  if (showRestorePassphraseDialog && pendingRestoreUri != null) {
    var pass by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showRestorePassphraseDialog = false },
      title = { Text("Decrypt & Restore Database") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Enter the passphrase used when encrypting this backup. Restoring will replace your current local ledger.",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Vault Passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          val uri = pendingRestoreUri
          if (uri != null) {
            onRestoreEncryptedDb(uri, pass)
          }
          showRestorePassphraseDialog = false
          pendingRestoreUri = null
        }) { Text("Decrypt & Restore") }
      },
      dismissButton = {
        TextButton(onClick = {
          showRestorePassphraseDialog = false
          pendingRestoreUri = null
        }) { Text("Cancel") }
      },
    )
  }

  // Add Budget Rule Dialog
  if (showAddBudgetDialog) {
    var ruleName by remember { mutableStateOf("Monthly Living Cap") }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var limitText by remember { mutableStateOf("") }
    var alertPct by remember { mutableIntStateOf(80) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    val parentCategories = remember(state.categories) {
      state.categories.filter { it.parentId == null && it.scope == TransactionType.EXPENSE }
    }

    AlertDialog(
      onDismissRequest = { showAddBudgetDialog = false },
      title = { Text("New Budget & Alert Rule") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = ruleName,
            onValueChange = { ruleName = it },
            label = { Text("Rule Name") },
            singleLine = true,
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BudgetPeriod.entries.forEach { p ->
              FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
            }
          }
          OutlinedTextField(
            value = limitText,
            onValueChange = { limitText = it },
            label = { Text("Spending Limit (${currency.symbol})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
          )
          Text("Alert Threshold: $alertPct%", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(50, 75, 80, 90, 100).forEach { pct ->
              FilterChip(selected = alertPct == pct, onClick = { alertPct = pct }, label = { Text("$pct%") })
            }
          }
          Text("Scope (Optional Category)", style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = selectedCategoryId == null,
              onClick = { selectedCategoryId = null },
              label = { Text("Overall Spend") },
            )
            parentCategories.forEach { cat ->
              FilterChip(
                selected = selectedCategoryId == cat.id,
                onClick = { selectedCategoryId = cat.id },
                label = { Text(cat.name) },
              )
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          val lim = limitText.toDoubleOrNull() ?: 0.0
          if (lim > 0.0 && ruleName.isNotBlank()) {
            onSaveBudgetRule(
              BudgetRuleEntity(
                name = ruleName.trim(),
                period = period,
                categoryId = selectedCategoryId,
                limitAmount = lim,
                alertThresholdPercent = alertPct,
                notificationsEnabled = true,
                isActive = true,
              )
            )
            showAddBudgetDialog = false
          }
        }) { Text("Save Rule") }
      },
      dismissButton = {
        TextButton(onClick = { showAddBudgetDialog = false }) { Text("Cancel") }
      },
    )
  }

  // Set 4-Digit Fallback PIN Dialog
  if (showSetPinDialog) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showSetPinDialog = false },
      title = { Text("Set 4-Digit Vault PIN") },
      text = {
        OutlinedTextField(
          value = pin,
          onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
          label = { Text("4 Digits") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
        )
      },
      confirmButton = {
        Button(
          enabled = pin.length == 4,
          onClick = {
            onSavePin(pin)
            showSetPinDialog = false
          },
        ) { Text("Save PIN") }
      },
      dismissButton = {
        TextButton(onClick = { showSetPinDialog = false }) { Text("Cancel") }
      },
    )
  }
}
