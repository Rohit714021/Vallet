package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.prefs.CurrencyFormatter
import com.example.data.prefs.VaultCurrency
import com.example.domain.PeriodBudgetPace
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultCardBox(
  modifier: Modifier = Modifier,
  highlightedBorder: Color = VaultBorder,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.verticalGradient(
          listOf(VaultCard, VaultSurfaceElevated)
        )
      )
      .border(1.dp, highlightedBorder, RoundedCornerShape(20.dp))
      .padding(16.dp),
    content = content,
  )
}

@Composable
fun BudgetPaceTile(
  pace: PeriodBudgetPace,
  currency: VaultCurrency,
  onClickConfigure: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accentColor = when {
    !pace.hasConfiguredLimit -> CoolSapphire
    pace.percentSpent >= 100f -> CoralCrimson
    pace.percentSpent >= 75f -> WarmAmber
    else -> ElectricMint
  }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(VaultSurface)
      .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
      .clickable(onClick = onClickConfigure)
      .padding(12.dp)
      .testTag("budget_pace_${pace.period.name.lowercase()}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = pace.period.label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = if (pace.hasConfiguredLimit) "${pace.percentSpent.toInt()}%" else "Set Cap",
        style = TabularNumberStyle.copy(
          fontSize = MaterialTheme.typography.labelLarge.fontSize,
          fontWeight = FontWeight.Bold,
        ),
        color = accentColor,
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    LinearProgressIndicator(
      progress = { if (pace.hasConfiguredLimit) (pace.percentSpent / 100f).coerceIn(0f, 1f) else 0f },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(999.dp)),
      color = accentColor,
      trackColor = VaultBorder.copy(alpha = 0.5f),
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = CurrencyFormatter.formatCompact(pace.spentAmount, currency),
      style = TabularNumberStyle.copy(
        fontSize = MaterialTheme.typography.titleMedium.fontSize,
        fontWeight = FontWeight.Bold,
      ),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
    )
    Text(
      text = if (pace.hasConfiguredLimit) {
        "of ${CurrencyFormatter.formatCompact(pace.limitAmount, currency)}"
      } else {
        "Tap to set limit"
      },
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

@Composable
fun TransactionRowCard(
  tx: TransactionEntity,
  categoriesById: Map<Long, CategoryEntity>,
  currency: VaultCurrency,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
  val cat = categoriesById[tx.categoryId]
  val sub = tx.subCategoryId?.let { categoriesById[it] }
  val amountColor = when (tx.type) {
    TransactionType.INCOME -> ElectricMint
    TransactionType.EXPENSE -> CoralCrimson
    TransactionType.INVESTMENT_CONTRIBUTION -> CoolSapphire
    TransactionType.DEBT_PAYMENT -> WarmAmber
    TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
  }
  val prefix = when (tx.type) {
    TransactionType.INCOME -> "+"
    TransactionType.EXPENSE -> "-"
    else -> ""
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(VaultSurface)
      .border(1.dp, VaultBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("transaction_item_${tx.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(amountColor.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = cat?.name?.take(1)?.uppercase() ?: tx.type.label.take(1),
        style = MaterialTheme.typography.titleMedium,
        color = amountColor,
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = tx.fromLabel.ifBlank { "Account" },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "to",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(13.dp),
        )
        Text(
          text = tx.toLabel.ifBlank { cat?.name ?: tx.type.label },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      val categoryLabel = buildString {
        append(cat?.name ?: tx.type.label)
        if (sub != null) append(" • ${sub.name}")
      }
      Text(
        text = "$categoryLabel  ·  ${dateFmt.format(Date(tx.timestamp))}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (tx.isTravel || tx.isImportantPurchase || tx.recurringRuleId != null || tx.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          if (tx.isImportantPurchase) {
            MiniTagChip(Icons.Default.Star, tx.eventTitle.ifBlank { "Milestone" }, WarmAmber)
          }
          if (tx.isTravel) {
            MiniTagChip(Icons.Default.FlightTakeoff, tx.locationName.ifBlank { tx.tripName.ifBlank { "Travel" } }, SoftViolet)
          }
          if (tx.recurringRuleId != null) {
            MiniTagChip(Icons.Default.Repeat, "Recurring", CoolSapphire)
          }
        }
      }
    }

    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = "$prefix${CurrencyFormatter.format(tx.amount, currency, showDecimals = false)}",
        style = TabularNumberStyle.copy(
          fontSize = MaterialTheme.typography.titleMedium.fontSize,
          fontWeight = FontWeight.Bold,
        ),
        color = amountColor,
      )
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize().size(32.dp).testTag("delete_tx_${tx.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete transaction",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun MiniTagChip(icon: ImageVector, label: String, color: Color) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(color.copy(alpha = 0.14f))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun EmptyVaultState(
  icon: ImageVector,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  VaultCardBox(modifier = modifier) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(ElectricMint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = ElectricMint, modifier = Modifier.size(26.dp))
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
