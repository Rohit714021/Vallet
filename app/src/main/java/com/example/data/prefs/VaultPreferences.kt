package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "obsidian_vault_prefs")

data class VaultSettingsState(
  val currency: VaultCurrency = VaultCurrency.INR,
  val appLockEnabled: Boolean = false,
  val hasPinConfigured: Boolean = false,
  val lastBackupTimestamp: Long = 0L,
  val lastBackupFileName: String = "",
  val yearlyBackupReminderEnabled: Boolean = true,
  val budgetAlertNotificationsEnabled: Boolean = true,
)

class VaultPreferences(private val context: Context) {

  private object Keys {
    val CURRENCY_CODE = stringPreferencesKey("currency_code")
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    val PIN_HASH = stringPreferencesKey("pin_hash")
    val LAST_BACKUP_TS = longPreferencesKey("last_backup_ts")
    val LAST_BACKUP_NAME = stringPreferencesKey("last_backup_name")
    val YEARLY_REMINDER_ENABLED = booleanPreferencesKey("yearly_reminder_enabled")
    val BUDGET_NOTIFICATIONS_ENABLED = booleanPreferencesKey("budget_notifications_enabled")
  }

  val settingsFlow: Flow<VaultSettingsState> = context.vaultDataStore.data.map { prefs ->
    VaultSettingsState(
      currency = VaultCurrency.fromCode(prefs[Keys.CURRENCY_CODE]),
      appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
      hasPinConfigured = !prefs[Keys.PIN_HASH].isNullOrBlank(),
      lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TS] ?: 0L,
      lastBackupFileName = prefs[Keys.LAST_BACKUP_NAME] ?: "",
      yearlyBackupReminderEnabled = prefs[Keys.YEARLY_REMINDER_ENABLED] ?: true,
      budgetAlertNotificationsEnabled = prefs[Keys.BUDGET_NOTIFICATIONS_ENABLED] ?: true,
    )
  }

  suspend fun setCurrency(currency: VaultCurrency) {
    context.vaultDataStore.edit { it[Keys.CURRENCY_CODE] = currency.code }
  }

  suspend fun setAppLockEnabled(enabled: Boolean) {
    context.vaultDataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
  }

  suspend fun savePin(pin: String) {
    val hashed = hashPin(pin)
    context.vaultDataStore.edit {
      it[Keys.PIN_HASH] = hashed
      it[Keys.APP_LOCK_ENABLED] = true
    }
  }

  suspend fun clearPin() {
    context.vaultDataStore.edit { it.remove(Keys.PIN_HASH) }
  }

  fun verifyPinFlow(pin: String): Flow<Boolean> =
    context.vaultDataStore.data.map { prefs ->
      val stored = prefs[Keys.PIN_HASH] ?: return@map false
      stored == hashPin(pin)
    }

  suspend fun recordBackup(timestamp: Long, fileName: String) {
    context.vaultDataStore.edit {
      it[Keys.LAST_BACKUP_TS] = timestamp
      it[Keys.LAST_BACKUP_NAME] = fileName
    }
  }

  suspend fun setYearlyReminderEnabled(enabled: Boolean) {
    context.vaultDataStore.edit { it[Keys.YEARLY_REMINDER_ENABLED] = enabled }
  }

  suspend fun setBudgetNotificationsEnabled(enabled: Boolean) {
    context.vaultDataStore.edit { it[Keys.BUDGET_NOTIFICATIONS_ENABLED] = enabled }
  }

  private fun hashPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(("obsidian_pin_salt_$pin").toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
