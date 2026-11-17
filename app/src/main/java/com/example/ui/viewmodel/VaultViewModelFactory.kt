package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ObsidianVaultApp

class VaultViewModelFactory(private val app: ObsidianVaultApp) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return when {
      modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
        DashboardViewModel(app.repository, app.preferences, app.notificationHelper) as T
      modelClass.isAssignableFrom(AnalyticsViewModel::class.java) ->
        AnalyticsViewModel(app.repository, app.preferences) as T
      modelClass.isAssignableFrom(WealthViewModel::class.java) ->
        WealthViewModel(app.repository, app.preferences) as T
      modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
        SettingsViewModel(app.repository, app.preferences) as T
      else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
  }
}
