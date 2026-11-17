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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionType
import com.example.data.prefs.CurrencyFormatter
import com.example.data.prefs.VaultCurrency
import com.example.ui.components.VaultCardBox
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.WarmAmber

private enum class SetupTab(val label: String) {
  BANKS("Banks & Accounts"),
  CATEGORIES("Categories"),
  SUB_CATEGORIES("Sub-Categories"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageSetupScreen(
  accounts: List<AccountEntity>,
  categories: List<CategoryEntity>,
  currency: VaultCurrency,
  onSaveAccount: (AccountEntity) -> Unit,
  onDeleteAccount: (Long) -> Unit,
  onAddCategory: (name: String, parentId: Long?, scope: TransactionType, isMilestone: Boolean) -> Unit,
  onDeleteCategory: (Long) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var activeTab by remember { mutableStateOf(SetupTab.BANKS) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .testTag("manage_setup_screen"),
  ) {
    TopAppBar(
      title = { Text("Setup: Banks & Categories", style = MaterialTheme.typography.titleLarge) },
      navigationIcon = {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize().testTag("setup_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = VaultSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
      ),
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SetupTab.entries.forEach { tab ->
              FilterChip(
                selected = activeTab == tab,
                onClick = { activeTab = tab },
                label = { Text(tab.label) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ElectricMint.copy(alpha = 0.2f),
                  selectedLabelColor = ElectricMint,
                ),
                modifier = Modifier.testTag("setup_tab_${tab.name.lowercase()}"),
              )
            }
          }
        }

        when (activeTab) {
          SetupTab.BANKS -> {
            item {
              AddBankAccountCard(currency = currency, onSaveAccount = onSaveAccount)
            }
            item {
              Text("Saved Banks & Accounts (${accounts.size})", style = MaterialTheme.typography.titleLarge)
            }
            items(accounts, key = { "setup_acc_${it.id}" }) { acc ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(VaultSurface)
                  .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                  Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CoolSapphire)
                  Column {
                    Text(acc.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                      "${acc.institution.ifBlank { acc.type.label }} · ${acc.type.label}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    CurrencyFormatter.format(acc.balance, currency, showDecimals = false),
                    style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                    color = ElectricMint,
                  )
                  IconButton(onClick = { onDeleteAccount(acc.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete bank/account")
                  }
                }
              }
            }
          }

          SetupTab.CATEGORIES -> {
            item {
              AddParentCategoryCard(onAddCategory = onAddCategory)
            }
            item {
              Text("All Categories by Type", style = MaterialTheme.typography.titleLarge)
            }
            val parents = categories.filter { it.parentId == null }
            items(parents, key = { "setup_cat_${it.id}" }) { cat ->
              val subCount = categories.count { it.parentId == cat.id }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(VaultSurface)
                  .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                  Icon(Icons.Default.Category, contentDescription = null, tint = ElectricMint)
                  Column {
                    Text(cat.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                      "${cat.scope.label} · $subCount sub-categories",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                IconButton(onClick = { onDeleteCategory(cat.id) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = "Delete category")
                }
              }
            }
          }

          SetupTab.SUB_CATEGORIES -> {
            item {
              AddSubCategoryCard(categories = categories, onAddCategory = onAddCategory)
            }
            val parents = categories.filter { it.parentId == null }
            items(parents, key = { "setup_sub_group_${it.id}" }) { parent ->
              val subs = categories.filter { it.parentId == parent.id }
              VaultCardBox {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(parent.name, style = MaterialTheme.typography.titleMedium, color = ElectricMint)
                    Text("${parent.scope.label} · ${subs.size} sub-categories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  subs.forEach { sub ->
                    Row(
                      modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultSurface)
                        .border(1.dp, VaultBorder, RoundedCornerShape(10.dp))
                        .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = if (sub.isMilestoneDefault) WarmAmber else CoolSapphire, modifier = Modifier.size(14.dp))
                      Spacer(Modifier.width(6.dp))
                      Text(sub.name, style = MaterialTheme.typography.bodySmall)
                      IconButton(onClick = { onDeleteCategory(sub.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove sub-category", modifier = Modifier.size(14.dp))
                      }
                    }
                  }
                }
              }
            }
          }
        }

        item { Spacer(Modifier.height(48.dp)) }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBankAccountCard(
  currency: VaultCurrency,
  onSaveAccount: (AccountEntity) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var institution by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(AccountType.BANK) }
  var balanceText by remember { mutableStateOf("") }
  var rateText by remember { mutableStateOf("") }
  var tenureText by remember { mutableStateOf("") }
  var feedback by remember { mutableStateOf<String?>(null) }

  VaultCardBox(highlightedBorder = CoolSapphire.copy(alpha = 0.4f)) {
    Text("Add Bank / Wallet / Holding", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      AccountType.entries.forEach { t ->
        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
      }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = name,
      onValueChange = { name = it; feedback = null },
      label = { Text("Account Name (e.g., HDFC Salary)") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("setup_bank_name_input"),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = institution,
      onValueChange = { institution = it },
      label = { Text("Bank / Institution Name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("setup_bank_institution_input"),
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = balanceText,
      onValueChange = { balanceText = it },
      label = { Text("Current Balance (${currency.symbol})") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("setup_bank_balance_input"),
    )
    if (type == AccountType.INVESTMENT || type == AccountType.DEBT || type == AccountType.CREDIT_CARD) {
      Spacer(Modifier.height(8.dp))
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
          label = { Text("Tenure (months)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }
    }
    feedback?.let {
      Spacer(Modifier.height(6.dp))
      Text(it, color = ElectricMint, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(10.dp))
    Button(
      onClick = {
        val clean = name.trim()
        if (clean.isNotEmpty()) {
          onSaveAccount(
            AccountEntity(
              name = clean,
              institution = institution.trim(),
              type = type,
              balance = balanceText.toDoubleOrNull() ?: 0.0,
              annualRatePercent = rateText.toDoubleOrNull() ?: 0.0,
              tenureMonths = tenureText.toIntOrNull() ?: 0,
            )
          )
          name = ""
          institution = ""
          balanceText = ""
          rateText = ""
          tenureText = ""
          feedback = "Added $clean to your ledger."
        }
      },
      colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
      modifier = Modifier.fillMaxWidth().testTag("setup_save_bank_button"),
    ) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Save Bank / Account")
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddParentCategoryCard(
  onAddCategory: (name: String, parentId: Long?, scope: TransactionType, isMilestone: Boolean) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var scope by remember { mutableStateOf(TransactionType.EXPENSE) }
  var isMilestone by remember { mutableStateOf(false) }
  var feedback by remember { mutableStateOf<String?>(null) }

  VaultCardBox(highlightedBorder = ElectricMint.copy(alpha = 0.4f)) {
    Text("Add Main Category", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf(
        TransactionType.EXPENSE,
        TransactionType.INCOME,
        TransactionType.INVESTMENT_CONTRIBUTION,
        TransactionType.DEBT_PAYMENT,
      ).forEach { s ->
        FilterChip(selected = scope == s, onClick = { scope = s }, label = { Text(s.label) })
      }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = name,
      onValueChange = { name = it; feedback = null },
      label = { Text("Category Name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("setup_category_name_input"),
    )
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Tag as Yearly Milestone Category", style = MaterialTheme.typography.bodyMedium)
      Switch(checked = isMilestone, onCheckedChange = { isMilestone = it })
    }
    feedback?.let {
      Text(it, color = ElectricMint, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))
    Button(
      onClick = {
        val clean = name.trim()
        if (clean.isNotEmpty()) {
          onAddCategory(clean, null, scope, isMilestone)
          name = ""
          feedback = "Category '$clean' created."
        }
      },
      colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
      modifier = Modifier.fillMaxWidth().testTag("setup_save_category_button"),
    ) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Add Category")
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddSubCategoryCard(
  categories: List<CategoryEntity>,
  onAddCategory: (name: String, parentId: Long?, scope: TransactionType, isMilestone: Boolean) -> Unit,
) {
  var selectedScope by remember { mutableStateOf(TransactionType.EXPENSE) }
  val parents = remember(categories, selectedScope) {
    categories.filter { it.parentId == null && it.scope == selectedScope }
  }
  var selectedParent by remember(parents) { mutableStateOf(parents.firstOrNull()) }
  var parentExpanded by remember { mutableStateOf(false) }
  var subName by remember { mutableStateOf("") }
  var isMilestone by remember { mutableStateOf(false) }
  var feedback by remember { mutableStateOf<String?>(null) }

  VaultCardBox(highlightedBorder = WarmAmber.copy(alpha = 0.4f)) {
    Text("Add Sub-Category", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf(
        TransactionType.EXPENSE,
        TransactionType.INCOME,
        TransactionType.INVESTMENT_CONTRIBUTION,
        TransactionType.DEBT_PAYMENT,
      ).forEach { s ->
        FilterChip(selected = selectedScope == s, onClick = { selectedScope = s }, label = { Text(s.label) })
      }
    }
    Spacer(Modifier.height(8.dp))

    ExposedDropdownMenuBox(
      expanded = parentExpanded,
      onExpandedChange = { parentExpanded = !parentExpanded },
    ) {
      OutlinedTextField(
        value = selectedParent?.name ?: "Select Parent Category",
        onValueChange = {},
        readOnly = true,
        label = { Text("Parent Category") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
        modifier = Modifier
          .menuAnchor(MenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth()
          .testTag("setup_parent_category_dropdown"),
      )
      ExposedDropdownMenu(
        expanded = parentExpanded,
        onDismissRequest = { parentExpanded = false },
      ) {
        parents.forEach { p ->
          DropdownMenuItem(
            text = { Text(p.name) },
            onClick = {
              selectedParent = p
              parentExpanded = false
            },
          )
        }
      }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = subName,
      onValueChange = { subName = it; feedback = null },
      label = { Text("Sub-Category Name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("setup_subcategory_name_input"),
    )
    Spacer(Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("Yearly Milestone by default", style = MaterialTheme.typography.bodyMedium)
      Switch(checked = isMilestone, onCheckedChange = { isMilestone = it })
    }
    feedback?.let {
      Text(it, color = ElectricMint, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))
    Button(
      enabled = selectedParent != null,
      onClick = {
        val clean = subName.trim()
        val p = selectedParent
        if (clean.isNotEmpty() && p != null) {
          onAddCategory(clean, p.id, p.scope, isMilestone)
          subName = ""
          feedback = "Added '$clean' under ${p.name}."
        }
      },
      colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
      modifier = Modifier.fillMaxWidth().testTag("setup_save_subcategory_button"),
    ) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text("Add Sub-Category")
    }
  }
}
