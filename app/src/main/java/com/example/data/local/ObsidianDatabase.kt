package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    AccountEntity::class,
    CategoryEntity::class,
    TransactionEntity::class,
    BudgetRuleEntity::class,
    RecurringRuleEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
@TypeConverters(VaultConverters::class)
abstract class ObsidianDatabase : RoomDatabase() {
  abstract fun accountDao(): AccountDao
  abstract fun categoryDao(): CategoryDao
  abstract fun transactionDao(): TransactionDao
  abstract fun budgetRuleDao(): BudgetRuleDao
  abstract fun recurringRuleDao(): RecurringRuleDao

  companion object {
    const val DATABASE_NAME = "obsidian_vault.db"

    @Volatile
    private var INSTANCE: ObsidianDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        ensureSeededCategories(db)
      }
    }

    fun getInstance(context: Context): ObsidianDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
      }
    }

    fun closeAndClearInstance() {
      synchronized(this) {
        INSTANCE?.close()
        INSTANCE = null
      }
    }

    private fun buildDatabase(appContext: Context): ObsidianDatabase {
      return Room.databaseBuilder(appContext, ObsidianDatabase::class.java, DATABASE_NAME)
        .addMigrations(MIGRATION_1_2)
        .addCallback(object : Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            seedStructuralDefaults(db)
          }
        })
        .build()
    }

    private fun seedStructuralDefaults(db: SupportSQLiteDatabase) {
      db.beginTransaction()
      try {
        val cashValues = ContentValues().apply {
          put("name", "Cash Wallet")
          put("institution", "On Hand")
          put("type", AccountType.CASH.name)
          put("balance", 0.0)
          put("annualRatePercent", 0.0)
          put("tenureMonths", 0)
          put("notes", "Default physical cash wallet")
          put("createdAt", System.currentTimeMillis())
        }
        db.insert("accounts", SQLiteDatabase.CONFLICT_REPLACE, cashValues)
        ensureSeededCategories(db)
        db.setTransactionSuccessful()
      } finally {
        db.endTransaction()
      }
    }

    /**
     * Ensures Expense, Income, Investment, Debt, and Transfer categories each have
     * 6 individual sub-categories without duplicating existing category names.
     */
    fun ensureSeededCategories(db: SupportSQLiteDatabase) {
      fun findCategoryId(name: String, parentId: Long?, scope: TransactionType): Long? {
        val sql = if (parentId == null) {
          "SELECT id FROM categories WHERE name = ? AND parentId IS NULL AND scope = ? LIMIT 1"
        } else {
          "SELECT id FROM categories WHERE name = ? AND parentId = ? AND scope = ? LIMIT 1"
        }
        val args = if (parentId == null) {
          arrayOf(name, scope.name)
        } else {
          arrayOf(name, parentId.toString(), scope.name)
        }
        return db.query(sql, args).use { cursor ->
          if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
      }

      fun ensureCat(
        name: String,
        parentId: Long?,
        scope: TransactionType,
        iconKey: String,
        colorHex: Long,
        isMilestone: Boolean = false,
      ): Long {
        findCategoryId(name, parentId, scope)?.let { return it }
        val cv = ContentValues().apply {
          put("name", name)
          if (parentId != null) put("parentId", parentId) else putNull("parentId")
          put("scope", scope.name)
          put("iconKey", iconKey)
          put("colorHex", colorHex)
          put("isMilestoneDefault", if (isMilestone) 1 else 0)
        }
        return db.insert("categories", SQLiteDatabase.CONFLICT_REPLACE, cv)
      }

      // --- EXPENSE CATEGORIES (6 sub-categories each) ---
      val food = ensureCat("Food & Dining", null, TransactionType.EXPENSE, "restaurant", 0xFF2EE6A8)
      listOf("Groceries", "Dining Out", "Coffee & Snacks", "Food Delivery", "Bakery & Sweets", "Beverages").forEach {
        ensureCat(it, food, TransactionType.EXPENSE, "restaurant", 0xFF2EE6A8)
      }

      val housing = ensureCat("Housing & Bills", null, TransactionType.EXPENSE, "home", 0xFF5BA4FF)
      listOf("Rent / Mortgage", "Electricity & Water", "Internet & Mobile", "Cooking Gas", "Society Maintenance", "Home Repairs").forEach {
        ensureCat(it, housing, TransactionType.EXPENSE, "home", 0xFF5BA4FF)
      }

      val transport = ensureCat("Transport", null, TransactionType.EXPENSE, "directions_car", 0xFF38BDF8)
      listOf("Fuel & Charging", "Metro & Cab", "Vehicle Maintenance", "Parking & Tolls", "Auto & Bus", "Vehicle Insurance").forEach {
        ensureCat(it, transport, TransactionType.EXPENSE, "directions_car", 0xFF38BDF8)
      }

      val travel = ensureCat("Travel & Trips", null, TransactionType.EXPENSE, "flight", 0xFFA78BFA)
      listOf("Flights & Rail", "Hotels & Stays", "Local Experiences", "Trip Dining", "Visa & Insurance", "Souvenirs & Gear").forEach {
        ensureCat(it, travel, TransactionType.EXPENSE, "flight", 0xFFA78BFA)
      }

      val events = ensureCat("Important Purchases & Events", null, TransactionType.EXPENSE, "celebration", 0xFFF6B94D, true)
      listOf("Electronics & Gear", "Home Appliances", "Celebrations & Gifts", "Medical Milestones", "Annual Subscriptions", "Education & Courses").forEach { sub ->
        ensureCat(sub, events, TransactionType.EXPENSE, "celebration", 0xFFF6B94D, true)
      }

      val lifestyle = ensureCat("Health & Lifestyle", null, TransactionType.EXPENSE, "favorite", 0xFFF472B6)
      listOf("Pharmacy & Doctor", "Fitness & Sports", "Clothing & Personal", "Diagnostics & Labs", "Salon & Grooming", "Movies & Leisure").forEach {
        ensureCat(it, lifestyle, TransactionType.EXPENSE, "favorite", 0xFFF472B6)
      }

      // --- INCOME CATEGORIES (6 sub-categories each) ---
      val salaryIncome = ensureCat("Employment & Salary", null, TransactionType.INCOME, "payments", 0xFF2EE6A8)
      listOf("Monthly Salary", "Performance Bonus", "Reimbursements", "Overtime Pay", "Allowances", "Stipend").forEach {
        ensureCat(it, salaryIncome, TransactionType.INCOME, "work", 0xFF2EE6A8)
      }

      val businessIncome = ensureCat("Business & Freelance", null, TransactionType.INCOME, "business_center", 0xFF38BDF8)
      listOf("Client Retainer", "Consulting Fee", "Project Milestone", "Product Sales", "Commission", "Royalty Income").forEach {
        ensureCat(it, businessIncome, TransactionType.INCOME, "business_center", 0xFF38BDF8)
      }

      val passiveIncome = ensureCat("Passive & Returns", null, TransactionType.INCOME, "trending_up", 0xFFF6B94D)
      listOf("Dividends & Interest", "Bank Interest", "FD / Bond Payout", "Rental Income", "Capital Gains", "Cashback & Rewards").forEach {
        ensureCat(it, passiveIncome, TransactionType.INCOME, "trending_up", 0xFFF6B94D)
      }

      // --- INVESTMENT CATEGORIES (6 sub-categories each) ---
      val marketInvest = ensureCat("Market & Mutual Funds", null, TransactionType.INVESTMENT_CONTRIBUTION, "trending_up", 0xFF5BA4FF)
      listOf("Mutual Funds / SIP", "Equities & ETFs", "Index Funds", "Sovereign Gold / ETF", "Global / US Equity", "Thematic Portfolio").forEach {
        ensureCat(it, marketInvest, TransactionType.INVESTMENT_CONTRIBUTION, "trending_up", 0xFF5BA4FF)
      }

      val fixedInvest = ensureCat("Fixed & Retirement", null, TransactionType.INVESTMENT_CONTRIBUTION, "savings", 0xFF2EE6A8)
      listOf("Fixed Deposit / PPF", "NPS Contribution", "Debt Mutual Funds", "Government Bonds", "Recurring Deposit", "EPF / VPF").forEach {
        ensureCat(it, fixedInvest, TransactionType.INVESTMENT_CONTRIBUTION, "savings", 0xFF2EE6A8)
      }

      val altInvest = ensureCat("Real & Reserve Assets", null, TransactionType.INVESTMENT_CONTRIBUTION, "account_balance", 0xFFA78BFA)
      listOf("Physical Gold", "Real Estate / REIT", "Emergency Liquid Fund", "Treasury Bills", "Digital Assets", "Angel / Private").forEach {
        ensureCat(it, altInvest, TransactionType.INVESTMENT_CONTRIBUTION, "account_balance", 0xFFA78BFA)
      }

      // --- DEBT & TRANSFER CATEGORIES (6 sub-categories each) ---
      val debt = ensureCat("Debt Repayment", null, TransactionType.DEBT_PAYMENT, "credit_score", 0xFFFB7185)
      listOf("Loan EMI", "Credit Card Settlement", "Home Loan Principal", "Vehicle Loan", "Education Loan", "Personal Loan").forEach {
        ensureCat(it, debt, TransactionType.DEBT_PAYMENT, "credit_score", 0xFFFB7185)
      }

      val transfer = ensureCat("Account Transfer", null, TransactionType.TRANSFER, "swap_horiz", 0xFF38BDF8)
      listOf("Self Transfer", "Wallet Top-Up", "Savings Sweep", "Joint Account", "Cash Withdrawal", "Cash Deposit").forEach {
        ensureCat(it, transfer, TransactionType.TRANSFER, "swap_horiz", 0xFF38BDF8)
      }
    }
  }
}
