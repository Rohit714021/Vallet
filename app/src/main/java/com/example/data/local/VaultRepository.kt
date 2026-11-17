package com.example.data.local

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.security.BackupResult
import com.example.data.security.CryptoBackupManager
import com.example.data.security.RestoreResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class VaultRepository(
  private val appContext: Context,
  private val cryptoBackupManager: CryptoBackupManager,
) {
  private val dbGeneration = MutableStateFlow(0)

  private fun db(): ObsidianDatabase = ObsidianDatabase.getInstance(appContext)

  val accounts: Flow<List<AccountEntity>> = dbGeneration.flatMapLatest {
    db().accountDao().getAllAccounts()
  }

  val categories: Flow<List<CategoryEntity>> = dbGeneration.flatMapLatest {
    db().categoryDao().getAllCategories()
  }

  val transactions: Flow<List<TransactionEntity>> = dbGeneration.flatMapLatest {
    db().transactionDao().getAllTransactions()
  }

  val budgetRules: Flow<List<BudgetRuleEntity>> = dbGeneration.flatMapLatest {
    db().budgetRuleDao().getAllBudgetRules()
  }

  val recurringRules: Flow<List<RecurringRuleEntity>> = dbGeneration.flatMapLatest {
    db().recurringRuleDao().getAllRecurringRules()
  }

  suspend fun saveAccount(account: AccountEntity): Long {
    val dao = db().accountDao()
    return if (account.id == 0L) {
      dao.insertAccount(account)
    } else {
      dao.updateAccount(account)
      account.id
    }
  }

  suspend fun deleteAccount(id: Long) {
    db().accountDao().deleteAccountById(id)
  }

  suspend fun addCategory(category: CategoryEntity): Long {
    return db().categoryDao().insertCategory(category)
  }

  suspend fun deleteCategory(id: Long) {
    db().categoryDao().deleteCategoryById(id)
  }

  suspend fun saveBudgetRule(rule: BudgetRuleEntity): Long {
    val dao = db().budgetRuleDao()
    return if (rule.id == 0L) {
      dao.insertBudgetRule(rule)
    } else {
      dao.updateBudgetRule(rule)
      rule.id
    }
  }

  suspend fun deleteBudgetRule(id: Long) {
    db().budgetRuleDao().deleteBudgetRuleById(id)
  }

  suspend fun saveRecurringRule(rule: RecurringRuleEntity): Long {
    val dao = db().recurringRuleDao()
    return if (rule.id == 0L) {
      dao.insertRecurringRule(rule)
    } else {
      dao.updateRecurringRule(rule)
      rule.id
    }
  }

  suspend fun deleteRecurringRule(id: Long) {
    db().recurringRuleDao().deleteRecurringRuleById(id)
  }

  suspend fun logTransaction(
    tx: TransactionEntity,
    recurringFrequency: RecurrenceFrequency? = null,
    autoLogRecurring: Boolean = false,
  ): Long {
    val database = db()
    return database.withTransaction {
      var effectiveRuleId = tx.recurringRuleId
      if (recurringFrequency != null && effectiveRuleId == null) {
        val nextDue = advanceTimestamp(tx.timestamp, recurringFrequency)
        effectiveRuleId = database.recurringRuleDao().insertRecurringRule(
          RecurringRuleEntity(
            title = tx.toLabel.ifBlank { tx.notes.ifBlank { "Recurring Payment" } },
            amount = tx.amount,
            type = tx.type,
            fromAccountId = tx.fromAccountId,
            fromLabel = tx.fromLabel,
            toAccountId = tx.toAccountId,
            toLabel = tx.toLabel,
            categoryId = tx.categoryId,
            subCategoryId = tx.subCategoryId,
            frequency = recurringFrequency,
            nextDueTimestamp = nextDue,
            autoLog = autoLogRecurring,
            isActive = true,
          )
        )
      }

      val finalTx = tx.copy(recurringRuleId = effectiveRuleId)
      val insertedId = database.transactionDao().insertTransaction(finalTx)
      applyBalanceEffect(database.accountDao(), finalTx, multiplier = 1.0)
      insertedId
    }
  }

  suspend fun deleteTransactionWithReversal(tx: TransactionEntity) {
    val database = db()
    database.withTransaction {
      applyBalanceEffect(database.accountDao(), tx, multiplier = -1.0)
      database.transactionDao().deleteTransactionById(tx.id)
    }
  }

  suspend fun settleRecurringRuleNow(rule: RecurringRuleEntity) {
    val database = db()
    database.withTransaction {
      val now = System.currentTimeMillis()
      val tx = TransactionEntity(
        amount = rule.amount,
        type = rule.type,
        fromAccountId = rule.fromAccountId,
        fromLabel = rule.fromLabel,
        toAccountId = rule.toAccountId,
        toLabel = rule.toLabel,
        categoryId = rule.categoryId,
        subCategoryId = rule.subCategoryId,
        timestamp = now,
        notes = "Recurring: ${rule.title}",
        recurringRuleId = rule.id,
      )
      database.transactionDao().insertTransaction(tx)
      applyBalanceEffect(database.accountDao(), tx, multiplier = 1.0)

      val nextDue = advanceTimestamp(
        maxOf(rule.nextDueTimestamp, now),
        rule.frequency,
      )
      database.recurringRuleDao().updateRecurringRule(rule.copy(nextDueTimestamp = nextDue))
    }
  }

  suspend fun processAutoDueRecurringRules() {
    val database = db()
    val now = System.currentTimeMillis()
    val dueRules = database.recurringRuleDao().getDueAutoRules(now)
    for (rule in dueRules) {
      database.withTransaction {
        var cursorDue = rule.nextDueTimestamp
        var safety = 0
        while (cursorDue <= now && safety < 12) {
          val tx = TransactionEntity(
            amount = rule.amount,
            type = rule.type,
            fromAccountId = rule.fromAccountId,
            fromLabel = rule.fromLabel,
            toAccountId = rule.toAccountId,
            toLabel = rule.toLabel,
            categoryId = rule.categoryId,
            subCategoryId = rule.subCategoryId,
            timestamp = cursorDue,
            notes = "Auto-logged recurring: ${rule.title}",
            recurringRuleId = rule.id,
          )
          database.transactionDao().insertTransaction(tx)
          applyBalanceEffect(database.accountDao(), tx, multiplier = 1.0)
          cursorDue = advanceTimestamp(cursorDue, rule.frequency)
          safety++
        }
        database.recurringRuleDao().updateRecurringRule(rule.copy(nextDueTimestamp = cursorDue))
      }
    }
  }

  suspend fun exportEncryptedDatabaseToUri(uri: Uri, passphrase: String): BackupResult {
    return cryptoBackupManager.exportEncryptedDatabase(uri, passphrase, db())
  }

  suspend fun restoreEncryptedDatabaseFromUri(uri: Uri, passphrase: String): RestoreResult {
    val result = cryptoBackupManager.restoreEncryptedDatabase(uri, passphrase) {
      ObsidianDatabase.closeAndClearInstance()
    }
    if (result is RestoreResult.Success) {
      dbGeneration.value = dbGeneration.value + 1
    }
    return result
  }

  private suspend fun applyBalanceEffect(
    accountDao: AccountDao,
    tx: TransactionEntity,
    multiplier: Double,
  ) {
    val amount = tx.amount * multiplier
    when (tx.type) {
      TransactionType.EXPENSE -> {
        tx.fromAccountId?.let { id ->
          val acc = accountDao.getAccountById(id)
          if (acc != null) {
            val delta = if (acc.type == AccountType.CREDIT_CARD) amount else -amount
            accountDao.adjustBalance(id, delta)
          }
        }
      }
      TransactionType.INCOME -> {
        tx.toAccountId?.let { id ->
          accountDao.adjustBalance(id, amount)
        }
      }
      TransactionType.TRANSFER -> {
        tx.fromAccountId?.let { accountDao.adjustBalance(it, -amount) }
        tx.toAccountId?.let { accountDao.adjustBalance(it, amount) }
      }
      TransactionType.INVESTMENT_CONTRIBUTION -> {
        tx.fromAccountId?.let { accountDao.adjustBalance(it, -amount) }
        tx.toAccountId?.let { accountDao.adjustBalance(it, amount) }
      }
      TransactionType.DEBT_PAYMENT -> {
        tx.fromAccountId?.let { accountDao.adjustBalance(it, -amount) }
        tx.toAccountId?.let { id ->
          val acc = accountDao.getAccountById(id)
          if (acc != null) {
            val delta = if (acc.type == AccountType.DEBT || acc.type == AccountType.CREDIT_CARD) -amount else amount
            accountDao.adjustBalance(id, delta)
          }
        }
      }
    }
  }

  private fun advanceTimestamp(fromMs: Long, freq: RecurrenceFrequency): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = fromMs }
    when (freq) {
      RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
      RecurrenceFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
      RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
      RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
    }
    return cal.timeInMillis
  }
}
