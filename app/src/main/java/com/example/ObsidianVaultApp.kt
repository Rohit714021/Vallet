package com.example

import android.app.Application
import com.example.data.local.VaultRepository
import com.example.data.prefs.VaultPreferences
import com.example.data.security.CryptoBackupManager
import com.example.domain.NotificationHelper

class ObsidianVaultApp : Application() {
  lateinit var preferences: VaultPreferences
    private set
  lateinit var cryptoBackupManager: CryptoBackupManager
    private set
  lateinit var repository: VaultRepository
    private set
  lateinit var notificationHelper: NotificationHelper
    private set

  override fun onCreate() {
    super.onCreate()
    preferences = VaultPreferences(this)
    cryptoBackupManager = CryptoBackupManager(this)
    repository = VaultRepository(this, cryptoBackupManager)
    notificationHelper = NotificationHelper(this)
  }
}
