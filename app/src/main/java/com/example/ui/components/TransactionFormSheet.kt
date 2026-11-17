package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.CategoryEntity
import com.example.data.local.RecurrenceFrequency
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.prefs.CurrencyFormatter
import com.example.data.prefs.VaultCurrency
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
  accounts: List<AccountEntity>,
  categories: List<CategoryEntity>,
  currency: VaultCurrency,
  onDismiss: () -> Unit,
  onSaveTransaction: (TransactionEntity, RecurrenceFrequency?, Boolean) -> Unit,
  onOpenManageSetup: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
  val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

  var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
  var amountText by remember { mutableStateOf("") }

  // Bank / Source Account dropdown
  val sourceAccounts = remember(accounts) { accounts.filter { it.type != AccountType.DEBT } }
  var selectedFromAccount by remember(sourceAccounts) { mutableStateOf(sourceAccounts.firstOrNull()) }
  var fromDropdownExpanded by remember { mutableStateOf(false) }
  var customFromText by remember { mutableStateOf("") }

  // Destination Account dropdown (used for Income deposit, Transfer, Invest, Debt Pay)
  var selectedToAccount by remember { mutableStateOf<AccountEntity?>(null) }
  var toDropdownExpanded by remember { mutableStateOf(false) }
  var toMerchantText by remember { mutableStateOf("") }

  // Sub-Category Dropdown -> automatically maps parent Category!
  val categoriesById = remember(categories) { categories.associateBy { it.id } }
  val scopedSubCategories = remember(categories, selectedType) {
    categories.filter { it.parentId != null && it.scope == selectedType }
  }
  var selectedSubCategory by remember(scopedSubCategories) { mutableStateOf(scopedSubCategories.firstOrNull()) }
  var subCatDropdownExpanded by remember { mutableStateOf(false) }

  // Automatically derived parent Category from chosen Sub-Category
  val mappedParentCategory = selectedSubCategory?.parentId?.let { categoriesById[it] }
    ?: categories.firstOrNull { it.parentId == null && it.scope == selectedType }

  // Date & Time default to current date and time, both editable
  var txTimestampMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var showTxDatePicker by remember { mutableStateOf(false) }
  var showTxTimePicker by remember { mutableStateOf(false) }

  var notesText by remember { mutableStateOf("") }

  // Yearly Important Purchase / Event
  var isImportantPurchase by remember(selectedSubCategory) {
    mutableStateOf(selectedSubCategory?.isMilestoneDefault == true || mappedParentCategory?.isMilestoneDefault == true)
  }
  var eventTitle by remember { mutableStateOf("") }

  // Travel, Location & Dates
  var isTravel by remember(mappedParentCategory) {
    mutableStateOf(mappedParentCategory?.name?.contains("Travel", ignoreCase = true) == true)
  }
  var locationName by remember { mutableStateOf("") }
  var tripName by remember { mutableStateOf("") }
  var travelStartMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var travelEndMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var pickingTravelStart by remember { mutableStateOf<Boolean?>(null) }

  // Recurring Payment
  var isRecurring by remember { mutableStateOf(false) }
  var recurrenceFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
  var autoLogRecurring by remember { mutableStateOf(false) }

  var validationError by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = VaultSurfaceElevated,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Log Entry", style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
          TextButton(onClick = {
            onDismiss()
            onOpenManageSetup()
          }) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Setup")
          }
          IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.Default.Close, contentDescription = "Close form")
          }
        }
      }

      // Transaction Type Selector
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TransactionType.entries.forEach { type ->
          FilterChip(
            selected = selectedType == type,
            onClick = {
              selectedType = type
              selectedToAccount = when (type) {
                TransactionType.INVESTMENT_CONTRIBUTION ->
                  accounts.firstOrNull { it.type == AccountType.INVESTMENT }
                TransactionType.DEBT_PAYMENT ->
                  accounts.firstOrNull { it.type == AccountType.DEBT || it.type == AccountType.CREDIT_CARD }
                TransactionType.INCOME ->
                  accounts.firstOrNull { it.type == AccountType.BANK || it.type == AccountType.CASH }
                TransactionType.TRANSFER ->
                  accounts.firstOrNull { it.id != selectedFromAccount?.id }
                else -> null
              }
            },
            label = { Text(type.label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ElectricMint.copy(alpha = 0.2f),
              selectedLabelColor = ElectricMint,
            ),
            modifier = Modifier.testTag("tx_type_${type.name.lowercase()}"),
          )
        }
      }

      // 1. Amount
      OutlinedTextField(
        value = amountText,
        onValueChange = { input ->
          if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            amountText = input
            validationError = null
          }
        },
        label = { Text("1. Amount (${currency.symbol})") },
        prefix = { Text("${currency.symbol} ", color = ElectricMint) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
      )

      // 2. From Which Bank (Dropdown)
      if (selectedType != TransactionType.INCOME) {
        ExposedDropdownMenuBox(
          expanded = fromDropdownExpanded,
          onExpandedChange = { fromDropdownExpanded = !fromDropdownExpanded },
        ) {
          val fromDisplay = selectedFromAccount?.let {
            "${it.name} (${CurrencyFormatter.formatCompact(it.balance, currency)})"
          } ?: "Select Bank / Account"
          OutlinedTextField(
            value = fromDisplay,
            onValueChange = {},
            readOnly = true,
            label = { Text("2. From Which Bank / Account") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromDropdownExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("tx_from_bank_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = fromDropdownExpanded,
            onDismissRequest = { fromDropdownExpanded = false },
          ) {
            sourceAccounts.forEach { acc ->
              DropdownMenuItem(
                text = {
                  Column {
                    Text(acc.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                      "${acc.institution.ifBlank { acc.type.label }} · ${CurrencyFormatter.format(acc.balance, currency, false)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                },
                onClick = {
                  selectedFromAccount = acc
                  fromDropdownExpanded = false
                },
              )
            }
          }
        }
      } else {
        OutlinedTextField(
          value = customFromText,
          onValueChange = { customFromText = it },
          label = { Text("2. Income From (Employer / Client / Source)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tx_from_input"),
        )
      }

      // Destination Account dropdown when Income / Transfer / Invest / Debt Pay
      val usesAccountDestination = selectedType in setOf(
        TransactionType.INCOME,
        TransactionType.TRANSFER,
        TransactionType.INVESTMENT_CONTRIBUTION,
        TransactionType.DEBT_PAYMENT,
      )
      if (usesAccountDestination) {
        val destLabel = when (selectedType) {
          TransactionType.INCOME -> "Deposit To Which Bank / Account"
          TransactionType.INVESTMENT_CONTRIBUTION -> "To Investment Holding"
          TransactionType.DEBT_PAYMENT -> "To Debt / Loan / Card"
          else -> "To Destination Bank / Account"
        }
        ExposedDropdownMenuBox(
          expanded = toDropdownExpanded,
          onExpandedChange = { toDropdownExpanded = !toDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedToAccount?.name ?: "Select Destination Account",
            onValueChange = {},
            readOnly = true,
            label = { Text(destLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toDropdownExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("tx_to_account_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = toDropdownExpanded,
            onDismissRequest = { toDropdownExpanded = false },
          ) {
            accounts.forEach { acc ->
              DropdownMenuItem(
                text = { Text("${acc.name} (${acc.type.label})") },
                onClick = {
                  selectedToAccount = acc
                  toDropdownExpanded = false
                },
              )
            }
          }
        }
      }

      // 3. Date & Time (Current Date & Time by default, editable)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          "3. Date & Time (Current default, tap to edit)",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = { showTxDatePicker = true },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("tx_date_button"),
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(dateFmt.format(Date(txTimestampMs)), maxLines = 1)
          }
          OutlinedButton(
            onClick = { showTxTimePicker = true },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("tx_time_button"),
          ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(timeFmt.format(Date(txTimestampMs)), maxLines = 1)
          }
        }
      }

      // 4. Sub-Category Dropdown -> Auto-Maps Parent Category!
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ExposedDropdownMenuBox(
          expanded = subCatDropdownExpanded,
          onExpandedChange = { subCatDropdownExpanded = !subCatDropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedSubCategory?.name ?: "Select Sub-Category",
            onValueChange = {},
            readOnly = true,
            label = { Text("4. Sub-Category (Auto-maps Category)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCatDropdownExpanded) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth()
              .testTag("tx_subcategory_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = subCatDropdownExpanded,
            onDismissRequest = { subCatDropdownExpanded = false },
          ) {
            scopedSubCategories.forEach { sub ->
              val parentName = sub.parentId?.let { categoriesById[it]?.name }.orEmpty()
              DropdownMenuItem(
                text = {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(sub.name, style = MaterialTheme.typography.titleSmall)
                    if (parentName.isNotEmpty()) {
                      Text(
                        parentName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricMint,
                      )
                    }
                  }
                },
                onClick = {
                  selectedSubCategory = sub
                  subCatDropdownExpanded = false
                },
              )
            }
          }
        }

        mappedParentCategory?.let { parent ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(ElectricMint.copy(alpha = 0.1f))
              .border(1.dp, ElectricMint.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("auto_mapped_category_pill"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricMint, modifier = Modifier.size(16.dp))
            Text(
              "Auto-Mapped Category: ${parent.name}",
              style = MaterialTheme.typography.labelLarge,
              color = ElectricMint,
            )
          }
        }
      }

      if (!usesAccountDestination || selectedToAccount == null) {
        OutlinedTextField(
          value = toMerchantText,
          onValueChange = { toMerchantText = it },
          label = { Text("Paid To / Merchant (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tx_to_input"),
        )
      }

      // Optional Important Yearly Purchase / Event Toggle
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(VaultSurface)
          .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
          .padding(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(18.dp))
            Column {
              Text("Important Purchase / Yearly Event", style = MaterialTheme.typography.titleSmall)
              Text("Track major annual purchases & milestones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(checked = isImportantPurchase, onCheckedChange = { isImportantPurchase = it })
        }
        AnimatedVisibility(visible = isImportantPurchase) {
          OutlinedTextField(
            value = eventTitle,
            onValueChange = { eventTitle = it },
            label = { Text("Event / Milestone Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          )
        }
      }

      // Optional Travel, Location & Dates Toggle
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(VaultSurface)
          .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
          .padding(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = SoftViolet, modifier = Modifier.size(18.dp))
            Column {
              Text("Travel, Location & Trip Dates", style = MaterialTheme.typography.titleSmall)
              Text("Group trip costs by destination & dates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(checked = isTravel, onCheckedChange = { isTravel = it })
        }
        AnimatedVisibility(visible = isTravel) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
              value = locationName,
              onValueChange = { locationName = it },
              label = { Text("Location / City") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = tripName,
              onValueChange = { tripName = it },
              label = { Text("Trip Name (optional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(onClick = { pickingTravelStart = true }, modifier = Modifier.weight(1f)) {
                Text("Start: ${dateFmt.format(Date(travelStartMs))}", maxLines = 1)
              }
              OutlinedButton(onClick = { pickingTravelStart = false }, modifier = Modifier.weight(1f)) {
                Text("End: ${dateFmt.format(Date(travelEndMs))}", maxLines = 1)
              }
            }
          }
        }
      }

      // Optional Recurring Payment Toggle
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(VaultSurface)
          .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
          .padding(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Repeat, contentDescription = null, tint = CoolSapphire, modifier = Modifier.size(18.dp))
            Column {
              Text("Recurring Payment Schedule", style = MaterialTheme.typography.titleSmall)
              Text("Repeat daily, weekly, monthly, or yearly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
        }
        AnimatedVisibility(visible = isRecurring) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              RecurrenceFrequency.entries.forEach { freq ->
                FilterChip(
                  selected = recurrenceFrequency == freq,
                  onClick = { recurrenceFrequency = freq },
                  label = { Text(freq.label) },
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Auto-log when due on app open", style = MaterialTheme.typography.bodyMedium)
              Switch(checked = autoLogRecurring, onCheckedChange = { autoLogRecurring = it })
            }
          }
        }
      }

      OutlinedTextField(
        value = notesText,
        onValueChange = { notesText = it },
        label = { Text("Notes (optional)") },
        modifier = Modifier.fillMaxWidth(),
      )

      validationError?.let { err ->
        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
      }

      Button(
        onClick = {
          val parsedAmount = amountText.toDoubleOrNull()
          if (parsedAmount == null || parsedAmount <= 0.0) {
            validationError = "Please enter a valid amount greater than 0."
            return@Button
          }
          val fromLabel = if (selectedType == TransactionType.INCOME) {
            customFromText.ifBlank { "Income Source" }
          } else {
            selectedFromAccount?.name ?: "Cash"
          }
          val toLabel = selectedToAccount?.name ?: toMerchantText.ifBlank {
            selectedSubCategory?.name ?: mappedParentCategory?.name ?: selectedType.label
          }
          val tx = TransactionEntity(
            amount = parsedAmount,
            type = selectedType,
            fromAccountId = if (selectedType == TransactionType.INCOME) null else selectedFromAccount?.id,
            fromLabel = fromLabel,
            toAccountId = selectedToAccount?.id,
            toLabel = toLabel,
            categoryId = mappedParentCategory?.id ?: 0L,
            subCategoryId = selectedSubCategory?.id,
            timestamp = txTimestampMs,
            notes = notesText.trim(),
            isImportantPurchase = isImportantPurchase,
            eventTitle = eventTitle.trim(),
            isTravel = isTravel,
            locationName = locationName.trim(),
            tripName = tripName.trim(),
            travelStartDate = if (isTravel) travelStartMs else null,
            travelEndDate = if (isTravel) travelEndMs else null,
          )
          onSaveTransaction(
            tx,
            if (isRecurring) recurrenceFrequency else null,
            autoLogRecurring,
          )
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_transaction_button"),
      ) {
        Text("Save to Vault", style = MaterialTheme.typography.titleMedium)
      }
    }
  }

  if (showTxDatePicker) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = txTimestampMs)
    DatePickerDialog(
      onDismissRequest = { showTxDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { pickedDate ->
            val oldCal = Calendar.getInstance().apply { timeInMillis = txTimestampMs }
            val newCal = Calendar.getInstance().apply {
              timeInMillis = pickedDate
              set(Calendar.HOUR_OF_DAY, oldCal.get(Calendar.HOUR_OF_DAY))
              set(Calendar.MINUTE, oldCal.get(Calendar.MINUTE))
              set(Calendar.SECOND, 0)
            }
            txTimestampMs = newCal.timeInMillis
          }
          showTxDatePicker = false
        }) { Text("Select") }
      },
      dismissButton = {
        TextButton(onClick = { showTxDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (showTxTimePicker) {
    val cal = remember(txTimestampMs) { Calendar.getInstance().apply { timeInMillis = txTimestampMs } }
    val timeState = rememberTimePickerState(
      initialHour = cal.get(Calendar.HOUR_OF_DAY),
      initialMinute = cal.get(Calendar.MINUTE),
      is24Hour = false,
    )
    AlertDialog(
      onDismissRequest = { showTxTimePicker = false },
      title = { Text("Select Entry Time") },
      text = { TimePicker(state = timeState) },
      confirmButton = {
        TextButton(onClick = {
          val updated = Calendar.getInstance().apply {
            timeInMillis = txTimestampMs
            set(Calendar.HOUR_OF_DAY, timeState.hour)
            set(Calendar.MINUTE, timeState.minute)
            set(Calendar.SECOND, 0)
          }
          txTimestampMs = updated.timeInMillis
          showTxTimePicker = false
        }) { Text("Set Time") }
      },
      dismissButton = {
        TextButton(onClick = { showTxTimePicker = false }) { Text("Cancel") }
      },
    )
  }

  pickingTravelStart?.let { isStart ->
    val initial = if (isStart) travelStartMs else travelEndMs
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
      onDismissRequest = { pickingTravelStart = null },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { picked ->
            if (isStart) {
              travelStartMs = picked
              if (travelEndMs < picked) travelEndMs = picked
            } else {
              travelEndMs = maxOf(picked, travelStartMs)
            }
          }
          pickingTravelStart = null
        }) { Text("Set Date") }
      },
      dismissButton = {
        TextButton(onClick = { pickingTravelStart = null }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }
}
