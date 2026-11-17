package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.prefs.CurrencyFormatter
import com.example.domain.TrendWindow
import com.example.ui.components.EmptyVaultState
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.SpendingComparisonChart
import com.example.ui.components.VaultCardBox
import com.example.ui.theme.ChartSlicePalette
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.AnalyticsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AnalyticsTab(val label: String) {
  TRENDS("Spending Trends"),
  CATEGORIES("Categories"),
  EVENTS("Yearly Events"),
  TRAVEL("Travel & Locations"),
}

@Composable
fun AnalyticsScreen(
  state: AnalyticsUiState,
  onSelectTrendWindow: (TrendWindow) -> Unit,
  onSelectCategory: (Long) -> Unit,
  onSelectEventYear: (Int?) -> Unit,
  onSelectEventSubCategory: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var activeTab by remember { mutableStateOf(AnalyticsTab.TRENDS) }
  val currency = state.settings.currency
  val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().testTag("analytics_screen"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text("Trends & Insights", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AnalyticsTab.entries.forEach { tab ->
            FilterChip(
              selected = activeTab == tab,
              onClick = { activeTab = tab },
              label = { Text(tab.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricMint.copy(alpha = 0.2f),
                selectedLabelColor = ElectricMint,
              ),
              modifier = Modifier.testTag("analytics_tab_${tab.name.lowercase()}"),
            )
          }
        }
      }

      when (activeTab) {
        AnalyticsTab.TRENDS -> {
          item {
            VaultCardBox {
              Text("Cash Flow Comparison Over Time", style = MaterialTheme.typography.titleLarge)
              Spacer(Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                TrendWindow.entries.forEach { win ->
                  FilterChip(
                    selected = state.selectedWindow == win,
                    onClick = { onSelectTrendWindow(win) },
                    label = { Text(win.label) },
                  )
                }
              }
              Spacer(Modifier.height(12.dp))
              SpendingComparisonChart(
                points = state.timeSeries,
                currency = currency,
                showComparisonSeries = state.selectedWindow != TrendWindow.YEARLY,
              )
            }
          }

          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              VaultCardBox(modifier = Modifier.weight(1f)) {
                Text("Total Tracked Spend", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                  CurrencyFormatter.formatCompact(state.totalTrackedSpend, currency),
                  style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.Bold),
                  color = CoralCrimson,
                )
              }
              VaultCardBox(modifier = Modifier.weight(1f)) {
                Text("Total Tracked Income", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                  CurrencyFormatter.formatCompact(state.totalTrackedIncome, currency),
                  style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize, fontWeight = FontWeight.Bold),
                  color = ElectricMint,
                )
              }
            }
          }
        }

        AnalyticsTab.CATEGORIES -> {
          if (state.categoryBreakdowns.isEmpty()) {
            item {
              EmptyVaultState(
                icon = Icons.Default.PieChart,
                title = "No Category Spend Yet",
                subtitle = "Log expenses with categories and sub-categories to unlock interactive ring drilldowns.",
              )
            }
          } else {
            item {
              VaultCardBox {
                Text("Spending by Category & Sub-Category", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                  InteractiveDonutChart(
                    slices = state.categoryBreakdowns,
                    selectedCategoryId = state.selectedCategoryId,
                    currency = currency,
                    onSelectCategory = onSelectCategory,
                  )
                }
              }
            }

            items(state.categoryBreakdowns, key = { it.categoryId }) { cat ->
              val isSelected = cat.categoryId == state.selectedCategoryId
              val color = Color(cat.colorHex).takeIf { it.alpha > 0f }
                ?: ChartSlicePalette[0]
              VaultCardBox(
                highlightedBorder = if (isSelected) color else VaultBorder,
                modifier = Modifier.clickable { onSelectCategory(cat.categoryId) },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                    Text(cat.name, style = MaterialTheme.typography.titleMedium)
                  }
                  Text(
                    "${CurrencyFormatter.format(cat.amount, currency, showDecimals = false)} (${cat.sharePercent.toInt()}%)",
                    style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                    color = color,
                  )
                }
                if (isSelected && cat.subCategories.isNotEmpty()) {
                  Spacer(Modifier.height(10.dp))
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cat.subCategories.forEach { sub ->
                      Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                          Text("${sub.name} (${sub.count})", style = MaterialTheme.typography.bodySmall)
                          Text(
                            CurrencyFormatter.format(sub.amount, currency, showDecimals = false),
                            style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                          )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                          progress = { (sub.sharePercent / 100f).coerceIn(0f, 1f) },
                          modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                          color = color,
                          trackColor = VaultBorder.copy(alpha = 0.4f),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        AnalyticsTab.EVENTS -> {
          if (state.yearlyEventGroups.isEmpty()) {
            item {
              EmptyVaultState(
                icon = Icons.Default.Star,
                title = "No Important Purchases / Events Yet",
                subtitle = "Toggle \"Important Purchase / Yearly Event\" or pick a milestone sub-category when logging expenses.",
              )
            }
          } else {
            item {
              val years = state.yearlyEventGroups.map { it.year }
              val subNames = state.yearlyEventGroups
                .flatMap { g -> g.items.map { it.subCategoryName } }
                .distinct()
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Yearly Important Purchases & Events", style = MaterialTheme.typography.titleLarge)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FilterChip(
                    selected = state.selectedEventYear == null,
                    onClick = { onSelectEventYear(null) },
                    label = { Text("All Years") },
                  )
                  years.forEach { yr ->
                    FilterChip(
                      selected = state.selectedEventYear == yr,
                      onClick = { onSelectEventYear(yr) },
                      label = { Text(yr.toString()) },
                    )
                  }
                }
                if (subNames.isNotEmpty()) {
                  Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    FilterChip(
                      selected = state.selectedEventSubCategory == null,
                      onClick = { onSelectEventSubCategory(null) },
                      label = { Text("All Sub-Categories") },
                    )
                    subNames.forEach { sub ->
                      FilterChip(
                        selected = state.selectedEventSubCategory == sub,
                        onClick = { onSelectEventSubCategory(sub) },
                        label = { Text(sub) },
                      )
                    }
                  }
                }
              }
            }

            val visibleGroups = state.yearlyEventGroups.filter {
              state.selectedEventYear == null || it.year == state.selectedEventYear
            }
            items(visibleGroups, key = { it.year }) { group ->
              val itemsFiltered = group.items.filter {
                state.selectedEventSubCategory == null || it.subCategoryName == state.selectedEventSubCategory
              }
              if (itemsFiltered.isNotEmpty()) {
                VaultCardBox(highlightedBorder = WarmAmber.copy(alpha = 0.4f)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${group.year} Milestones", style = MaterialTheme.typography.titleLarge, color = WarmAmber)
                    Text(
                      CurrencyFormatter.format(itemsFiltered.sumOf { it.transaction.amount }, currency, showDecimals = false),
                      style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.Bold),
                      color = WarmAmber,
                    )
                  }
                  Spacer(Modifier.height(10.dp))
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsFiltered.forEach { item ->
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(12.dp))
                          .background(VaultSurface)
                          .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            item.transaction.eventTitle.ifBlank { item.transaction.toLabel },
                            style = MaterialTheme.typography.titleMedium,
                          )
                          Text(
                            "${item.subCategoryName} • ${dateFmt.format(Date(item.transaction.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        Text(
                          CurrencyFormatter.format(item.transaction.amount, currency, showDecimals = false),
                          style = TabularNumberStyle.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        AnalyticsTab.TRAVEL -> {
          if (state.travelSummaries.isEmpty()) {
            item {
              EmptyVaultState(
                icon = Icons.Default.FlightTakeoff,
                title = "No Travel & Location Logs Yet",
                subtitle = "Enable \"Travel, Location & Trip Dates\" in the transaction form to track destination budgets.",
              )
            }
          } else {
            items(state.travelSummaries, key = { it.tripKey }) { trip ->
              VaultCardBox(highlightedBorder = SoftViolet.copy(alpha = 0.45f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Icon(Icons.Default.LocationOn, contentDescription = null, tint = SoftViolet, modifier = Modifier.size(18.dp))
                      Text(trip.tripName, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                      "${trip.locationName} · ${dateFmt.format(Date(trip.startTimestamp))} – ${dateFmt.format(Date(trip.endTimestamp))}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      CurrencyFormatter.format(trip.totalAmount, currency, showDecimals = false),
                      style = TabularNumberStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize, fontWeight = FontWeight.Bold),
                      color = SoftViolet,
                    )
                    Text(
                      "${CurrencyFormatter.formatCompact(trip.dailyAverage, currency)}/day · ${trip.transactionCount} entries",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }

      item { Spacer(Modifier.height(76.dp)) }
    }
  }
}
