package com.example.domain

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

class NotificationHelper(private val context: Context) {

  companion object {
    const val CHANNEL_BUDGET_ALERTS = "obsidian_budget_alerts"
    const val CHANNEL_VAULT_REMINDERS = "obsidian_vault_reminders"
  }

  init {
    createChannels()
  }

  private fun createChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val budgetChannel = NotificationChannel(
        CHANNEL_BUDGET_ALERTS,
        "Budget Alerts",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Alerts when daily, monthly, yearly, or category budgets cross configured thresholds."
      }
      val reminderChannel = NotificationChannel(
        CHANNEL_VAULT_REMINDERS,
        "Yearly Backup Reminders",
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = "Annual reminders to store an encrypted database backup in Google Drive."
      }
      manager.createNotificationChannel(budgetChannel)
      manager.createNotificationChannel(reminderChannel)
    }
  }

  fun postBudgetAlert(title: String, message: String, notificationId: Int) {
    if (!hasNotificationPermission()) return
    val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(context).notify(notificationId, notification)
  }

  fun postYearlyBackupReminder() {
    if (!hasNotificationPermission()) return
    val notification = NotificationCompat.Builder(context, CHANNEL_VAULT_REMINDERS)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle("Yearly Encrypted Backup Due")
      .setContentText("Save an AES-256 encrypted copy of your Obsidian Vault database to Google Drive.")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()
    NotificationManagerCompat.from(context).notify(9001, notification)
  }

  fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }
}
