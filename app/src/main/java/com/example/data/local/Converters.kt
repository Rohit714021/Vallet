package com.example.data.local

import androidx.room.TypeConverter

class VaultConverters {
  @TypeConverter fun fromAccountType(v: AccountType): String = v.name
  @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

  @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
  @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

  @TypeConverter fun fromBudgetPeriod(v: BudgetPeriod): String = v.name
  @TypeConverter fun toBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

  @TypeConverter fun fromRecurrenceFrequency(v: RecurrenceFrequency): String = v.name
  @TypeConverter fun toRecurrenceFrequency(v: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(v)
}
