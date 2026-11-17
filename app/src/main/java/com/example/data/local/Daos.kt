package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
  @Query("SELECT * FROM accounts ORDER BY type ASC, name ASC")
  fun getAllAccounts(): Flow<List<AccountEntity>>

  @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
  suspend fun getAccountById(id: Long): AccountEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAccount(account: AccountEntity): Long

  @Update
  suspend fun updateAccount(account: AccountEntity)

  @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
  suspend fun adjustBalance(accountId: Long, delta: Double)

  @Query("DELETE FROM accounts WHERE id = :id")
  suspend fun deleteAccountById(id: Long)
}

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY parentId ASC, name ASC")
  fun getAllCategories(): Flow<List<CategoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategory(category: CategoryEntity): Long

  @Query("DELETE FROM categories WHERE id = :id")
  suspend fun deleteCategoryById(id: Long)
}

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
  fun getAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
  suspend fun getTransactionById(id: Long): TransactionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(tx: TransactionEntity): Long

  @Update
  suspend fun updateTransaction(tx: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteTransactionById(id: Long)
}

@Dao
interface BudgetRuleDao {
  @Query("SELECT * FROM budget_rules ORDER BY period ASC, id ASC")
  fun getAllBudgetRules(): Flow<List<BudgetRuleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBudgetRule(rule: BudgetRuleEntity): Long

  @Update
  suspend fun updateBudgetRule(rule: BudgetRuleEntity)

  @Query("DELETE FROM budget_rules WHERE id = :id")
  suspend fun deleteBudgetRuleById(id: Long)
}

@Dao
interface RecurringRuleDao {
  @Query("SELECT * FROM recurring_rules ORDER BY nextDueTimestamp ASC")
  fun getAllRecurringRules(): Flow<List<RecurringRuleEntity>>

  @Query("SELECT * FROM recurring_rules WHERE isActive = 1 AND autoLog = 1 AND nextDueTimestamp <= :now")
  suspend fun getDueAutoRules(now: Long): List<RecurringRuleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecurringRule(rule: RecurringRuleEntity): Long

  @Update
  suspend fun updateRecurringRule(rule: RecurringRuleEntity)

  @Query("DELETE FROM recurring_rules WHERE id = :id")
  suspend fun deleteRecurringRuleById(id: Long)
}
