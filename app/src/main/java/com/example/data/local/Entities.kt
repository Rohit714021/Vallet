package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType(val label: String) {
  BANK("Bank Account"),
  CASH("Cash / Wallet"),
  CREDIT_CARD("Credit Card"),
  INVESTMENT("Investment"),
  DEBT("Debt / Loan"),
}

enum class TransactionType(val label: String) {
  EXPENSE("Expense"),
  INCOME("Income"),
  TRANSFER("Transfer"),
  INVESTMENT_CONTRIBUTION("Invest"),
  DEBT_PAYMENT("Debt Pay"),
}

enum class BudgetPeriod(val label: String) {
  DAILY("Daily"),
  MONTHLY("Monthly"),
  YEARLY("Yearly"),
}

enum class RecurrenceFrequency(val label: String) {
  DAILY("Daily"),
  WEEKLY("Weekly"),
  MONTHLY("Monthly"),
  YEARLY("Yearly"),
}

@Entity(tableName = "accounts")
data class AccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val institution: String,
  val type: AccountType,
  val balance: Double,
  val annualRatePercent: Double = 0.0,
  val tenureMonths: Int = 0,
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val parentId: Long? = null,
  val scope: TransactionType = TransactionType.EXPENSE,
  val iconKey: String = "category",
  val colorHex: Long = 0xFF2EE6A8,
  val isMilestoneDefault: Boolean = false,
)

@Entity(tableName = "transactions")
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val amount: Double,
  val type: TransactionType,
  val fromAccountId: Long? = null,
  val fromLabel: String,
  val toAccountId: Long? = null,
  val toLabel: String,
  val categoryId: Long,
  val subCategoryId: Long? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val notes: String = "",
  val isImportantPurchase: Boolean = false,
  val eventTitle: String = "",
  val isTravel: Boolean = false,
  val locationName: String = "",
  val tripName: String = "",
  val travelStartDate: Long? = null,
  val travelEndDate: Long? = null,
  val recurringRuleId: Long? = null,
)

@Entity(tableName = "budget_rules")
data class BudgetRuleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val period: BudgetPeriod,
  val categoryId: Long? = null,
  val limitAmount: Double,
  val alertThresholdPercent: Int = 80,
  val notificationsEnabled: Boolean = true,
  val isActive: Boolean = true,
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val amount: Double,
  val type: TransactionType,
  val fromAccountId: Long? = null,
  val fromLabel: String,
  val toAccountId: Long? = null,
  val toLabel: String,
  val categoryId: Long,
  val subCategoryId: Long? = null,
  val frequency: RecurrenceFrequency,
  val nextDueTimestamp: Long,
  val autoLog: Boolean = false,
  val isActive: Boolean = true,
)
