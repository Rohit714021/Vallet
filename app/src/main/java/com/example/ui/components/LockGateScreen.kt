package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultSurface

@Composable
fun LockGateScreen(
  hasPinConfigured: Boolean,
  onTriggerBiometric: () -> Unit,
  onVerifyPin: suspend (String) -> Boolean,
  onUnlocked: () -> Unit,
) {
  var enteredPin by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(enteredPin) {
    if (enteredPin.length == 4) {
      val valid = onVerifyPin(enteredPin)
      if (valid) {
        onUnlocked()
      } else {
        pinError = "Incorrect Vault PIN"
        enteredPin = ""
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(listOf(ObsidianBg, VaultSurface, ObsidianBg))
      )
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(ElectricMint.copy(alpha = 0.14f))
          .border(1.5.dp, ElectricMint.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Vault Locked",
          tint = ElectricMint,
          modifier = Modifier.size(34.dp),
        )
      }

      Text(
        text = stringResource(R.string.lock_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(R.string.lock_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Button(
        onClick = onTriggerBiometric,
        colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBg),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("unlock_biometric_button"),
      ) {
        Icon(Icons.Default.Fingerprint, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.lock_unlock_button), style = MaterialTheme.typography.titleMedium)
      }

      if (hasPinConfigured) {
        Spacer(Modifier.height(8.dp))
        Text("Or Enter 4-Digit Vault PIN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          repeat(4) { idx ->
            val filled = idx < enteredPin.length
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (filled) ElectricMint else VaultCard)
                .border(1.dp, if (filled) ElectricMint else VaultBorder, CircleShape),
            )
          }
        }

        pinError?.let {
          Text(it, color = CoralCrimson, style = MaterialTheme.typography.bodySmall)
        }

        val rows = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("", "0", "DEL"),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
              row.forEach { key ->
                if (key.isEmpty()) {
                  Box(modifier = Modifier.size(64.dp))
                } else {
                  Box(
                    modifier = Modifier
                      .size(64.dp)
                      .minimumInteractiveComponentSize()
                      .clip(CircleShape)
                      .background(VaultCard)
                      .border(1.dp, VaultBorder, CircleShape)
                      .clickable {
                        pinError = null
                        if (key == "DEL") {
                          if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        } else if (enteredPin.length < 4) {
                          enteredPin += key
                        }
                      },
                    contentAlignment = Alignment.Center,
                  ) {
                    if (key == "DEL") {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete digit",
                        tint = MaterialTheme.colorScheme.onSurface,
                      )
                    } else {
                      Text(key, style = MaterialTheme.typography.headlineMedium)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
