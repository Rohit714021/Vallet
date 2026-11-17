package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.prefs.CurrencyFormatter
import com.example.data.prefs.VaultCurrency
import com.example.domain.CategoryBreakdown
import com.example.domain.TimeBucketPoint
import com.example.ui.theme.ChartSlicePalette
import com.example.ui.theme.CoolSapphire
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.ElectricMint
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.WarmAmber
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun SpendingComparisonChart(
  points: List<TimeBucketPoint>,
  currency: VaultCurrency,
  showComparisonSeries: Boolean,
  modifier: Modifier = Modifier,
) {
  var selectedIndex by remember(points) { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }
  val animProgress by animateFloatAsState(
    targetValue = if (points.isEmpty()) 0f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "bar_anim",
  )

  val maxVal = remember(points, showComparisonSeries) {
    val peak = points.maxOfOrNull {
      max(it.expenseTotal, if (showComparisonSeries) max(it.comparisonExpenseTotal, it.incomeTotal) else it.incomeTotal)
    } ?: 0.0
    if (peak <= 0.0) 1.0 else peak * 1.12
  }

  Column(modifier = modifier.fillMaxWidth().testTag("spending_comparison_chart")) {
    val active = points.getOrNull(selectedIndex)
    if (active != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(VaultSurface)
          .border(1.dp, VaultBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = active.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Tap any bar to inspect",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          MetricDotReadout(
            label = "Spent",
            value = CurrencyFormatter.formatCompact(active.expenseTotal, currency),
            color = CoralCrimson,
          )
          MetricDotReadout(
            label = "Income",
            value = CurrencyFormatter.formatCompact(active.incomeTotal, currency),
            color = ElectricMint,
          )
          if (showComparisonSeries && active.comparisonExpenseTotal > 0.0) {
            MetricDotReadout(
              label = "Prior",
              value = CurrencyFormatter.formatCompact(active.comparisonExpenseTotal, currency),
              color = CoolSapphire,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(190.dp)
        .pointerInput(points) {
          detectTapGestures { offset ->
            if (points.isNotEmpty()) {
              val idx = ((offset.x / size.width) * points.size).toInt().coerceIn(0, points.lastIndex)
              selectedIndex = idx
            }
          }
        },
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.isEmpty()) return@Canvas
        val padBottom = 10.dp.toPx()
        val chartHeight = (size.height - padBottom).coerceAtLeast(1f)
        val groupWidth = size.width / points.size.toFloat()

        // Horizontal dashed gridlines
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        for (step in 1..3) {
          val y = chartHeight * (1f - step / 4f)
          drawLine(
            color = VaultBorder.copy(alpha = 0.6f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dash,
          )
        }

        points.forEachIndexed { index, pt ->
          val isSelected = index == selectedIndex
          val centerX = (index + 0.5f) * groupWidth
          val barW = (groupWidth * 0.28f).coerceIn(5.dp.toPx(), 16.dp.toPx())
          val gap = 2.dp.toPx()

          if (isSelected) {
            drawRoundRect(
              color = Color.White.copy(alpha = 0.05f),
              topLeft = Offset(index * groupWidth + 2.dp.toPx(), 0f),
              size = Size((groupWidth - 4.dp.toPx()).coerceAtLeast(1f), chartHeight),
              cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
          }

          // Expense bar
          val expH = ((pt.expenseTotal / maxVal).toFloat() * chartHeight * animProgress).coerceAtLeast(
            if (pt.expenseTotal > 0.0) 4.dp.toPx() else 0f
          )
          if (expH > 0f) {
            drawRoundRect(
              brush = Brush.verticalGradient(
                listOf(
                  if (isSelected) CoralCrimson else CoralCrimson.copy(alpha = 0.8f),
                  CoralCrimson.copy(alpha = 0.35f),
                )
              ),
              topLeft = Offset(centerX - barW - gap / 2f, chartHeight - expH),
              size = Size(barW, expH),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }

          // Income or Prior Comparison bar
          val secondaryVal = if (pt.incomeTotal > 0.0) pt.incomeTotal else pt.comparisonExpenseTotal
          val secondaryColor = if (pt.incomeTotal > 0.0) ElectricMint else CoolSapphire
          val secH = ((secondaryVal / maxVal).toFloat() * chartHeight * animProgress).coerceAtLeast(
            if (secondaryVal > 0.0) 4.dp.toPx() else 0f
          )
          if (secH > 0f) {
            drawRoundRect(
              brush = Brush.verticalGradient(
                listOf(
                  if (isSelected) secondaryColor else secondaryColor.copy(alpha = 0.75f),
                  secondaryColor.copy(alpha = 0.3f),
                )
              ),
              topLeft = Offset(centerX + gap / 2f, chartHeight - secH),
              size = Size(barW, secH),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }
        }
      }
    }

    // X-axis label strip (condensed to avoid crowding)
    if (points.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val step = if (points.size > 7) 3 else 1
        points.forEachIndexed { idx, pt ->
          if (idx % step == 0 || idx == points.lastIndex) {
            Text(
              text = pt.label,
              style = MaterialTheme.typography.labelSmall,
              color = if (idx == selectedIndex) ElectricMint else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetricDotReadout(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.End) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      text = value,
      style = TabularNumberStyle.copy(
        fontSize = MaterialTheme.typography.labelLarge.fontSize,
        fontWeight = FontWeight.Bold,
      ),
      color = color,
    )
  }
}

@Composable
fun InteractiveDonutChart(
  slices: List<CategoryBreakdown>,
  selectedCategoryId: Long?,
  currency: VaultCurrency,
  onSelectCategory: (Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  val total = remember(slices) { slices.sumOf { it.amount } }
  val sweepScale by animateFloatAsState(
    targetValue = if (slices.isEmpty()) 0f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "donut_sweep",
  )

  val selectedSlice = slices.firstOrNull { it.categoryId == selectedCategoryId } ?: slices.firstOrNull()

  Box(
    modifier = modifier
      .size(210.dp)
      .testTag("category_donut_chart")
      .pointerInput(slices) {
        detectTapGestures { offset ->
          if (slices.isEmpty() || total <= 0.0) return@detectTapGestures
          val cx = size.width / 2f
          val cy = size.height / 2f
          val dx = offset.x - cx
          val dy = offset.y - cy
          val dist = hypot(dx, dy)
          if (dist < size.width * 0.2f || dist > size.width * 0.52f) return@detectTapGestures

          var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
          if (angle < 0f) angle += 360f

          var acc = 0f
          for (slice in slices) {
            val sweep = ((slice.amount / total) * 360.0).toFloat()
            if (angle in acc..(acc + sweep)) {
              onSelectCategory(slice.categoryId)
              break
            }
            acc += sweep
          }
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
      if (slices.isEmpty() || total <= 0.0) {
        drawArc(
          color = VaultBorder,
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round),
        )
        return@Canvas
      }

      var startAngle = -90f
      slices.forEachIndexed { idx, slice ->
        val isSelected = slice.categoryId == selectedSlice?.categoryId
        val sweep = ((slice.amount / total) * 360.0).toFloat() * sweepScale
        val color = Color(slice.colorHex).takeIf { it.alpha > 0f }
          ?: ChartSlicePalette[idx % ChartSlicePalette.size]
        val strokeWidth = if (isSelected) 28.dp.toPx() else 21.dp.toPx()

        drawArc(
          color = if (isSelected) color else color.copy(alpha = 0.72f),
          startAngle = startAngle + 1.5f,
          sweepAngle = (sweep - 3f).coerceAtLeast(2f),
          useCenter = false,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
        )
        startAngle += sweep
      }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
      Text(
        text = selectedSlice?.name ?: "No Spend",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = CurrencyFormatter.formatCompact(selectedSlice?.amount ?: total, currency),
        style = TabularNumberStyle.copy(
          fontSize = MaterialTheme.typography.titleLarge.fontSize,
          fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (selectedSlice != null) {
        Text(
          text = "${selectedSlice.sharePercent.toInt()}% of spend",
          style = MaterialTheme.typography.labelSmall,
          color = ElectricMint,
        )
      }
    }
  }
}

@Composable
fun AssetDistributionBar(
  banksTotal: Double,
  investmentsTotal: Double,
  debtsTotal: Double,
  currency: VaultCurrency,
  modifier: Modifier = Modifier,
) {
  val grossSum = (banksTotal.coerceAtLeast(0.0) + investmentsTotal.coerceAtLeast(0.0) + debtsTotal.coerceAtLeast(0.0))
  val bankFrac = if (grossSum > 0) (banksTotal.coerceAtLeast(0.0) / grossSum).toFloat() else 0.34f
  val invFrac = if (grossSum > 0) (investmentsTotal.coerceAtLeast(0.0) / grossSum).toFloat() else 0.33f
  val debtFrac = if (grossSum > 0) (debtsTotal.coerceAtLeast(0.0) / grossSum).toFloat() else 0.33f

  Column(modifier = modifier.fillMaxWidth().testTag("asset_distribution_bar")) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(14.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(VaultBorder.copy(alpha = 0.4f)),
    ) {
      if (grossSum <= 0.0) return@Canvas
      val w = size.width
      val bW = w * bankFrac
      val iW = w * invFrac
      val dW = w * debtFrac

      if (bW > 0f) drawRect(color = CoolSapphire, topLeft = Offset.Zero, size = Size(bW, size.height))
      if (iW > 0f) drawRect(color = ElectricMint, topLeft = Offset(bW, 0f), size = Size(iW, size.height))
      if (dW > 0f) drawRect(color = CoralCrimson, topLeft = Offset(bW + iW, 0f), size = Size(dW, size.height))
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      DistributionLegendItem("Banks", CurrencyFormatter.formatCompact(banksTotal, currency), CoolSapphire)
      DistributionLegendItem("Investments", CurrencyFormatter.formatCompact(investmentsTotal, currency), ElectricMint)
      DistributionLegendItem("Debts", CurrencyFormatter.formatCompact(debtsTotal, currency), CoralCrimson)
    }
  }
}

@Composable
private fun DistributionLegendItem(label: String, amount: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
    Column {
      Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        amount,
        style = TabularNumberStyle.copy(
          fontSize = MaterialTheme.typography.labelLarge.fontSize,
          fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

data class DualSeriesPoint(
  val xLabel: String,
  val primaryValue: Double,
  val secondaryValue: Double,
)

@Composable
fun ProjectionLineChart(
  points: List<DualSeriesPoint>,
  primaryLabel: String,
  secondaryLabel: String,
  primaryColor: Color,
  secondaryColor: Color,
  currency: VaultCurrency,
  modifier: Modifier = Modifier,
) {
  var selectedIdx by remember(points) { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }
  val maxVal = remember(points) {
    val peak = points.maxOfOrNull { max(it.primaryValue, it.secondaryValue) } ?: 0.0
    if (peak <= 0.0) 1.0 else peak * 1.1
  }

  Column(modifier = modifier.fillMaxWidth().testTag("projection_line_chart")) {
    val active = points.getOrNull(selectedIdx)
    if (active != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(VaultSurface)
          .border(1.dp, VaultBorder, RoundedCornerShape(10.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(active.xLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          MetricDotReadout(primaryLabel, CurrencyFormatter.formatCompact(active.primaryValue, currency), primaryColor)
          MetricDotReadout(secondaryLabel, CurrencyFormatter.formatCompact(active.secondaryValue, currency), secondaryColor)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(175.dp)
        .pointerInput(points) {
          detectTapGestures { offset ->
            if (points.isNotEmpty()) {
              val idx = ((offset.x / size.width) * points.size).toInt().coerceIn(0, points.lastIndex)
              selectedIdx = idx
            }
          }
        },
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas
        val pad = 8.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2

        val primaryPath = Path()
        val secondaryPath = Path()

        points.forEachIndexed { idx, pt ->
          val x = pad + (idx.toFloat() / points.lastIndex.toFloat()) * w
          val y1 = pad + h - ((pt.primaryValue / maxVal).toFloat() * h)
          val y2 = pad + h - ((pt.secondaryValue / maxVal).toFloat() * h)

          if (idx == 0) {
            primaryPath.moveTo(x, y1)
            secondaryPath.moveTo(x, y2)
          } else {
            primaryPath.lineTo(x, y1)
            secondaryPath.lineTo(x, y2)
          }
        }

        drawPath(secondaryPath, color = secondaryColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawPath(primaryPath, color = primaryColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        val sel = points.getOrNull(selectedIdx)
        if (sel != null) {
          val x = pad + (selectedIdx.toFloat() / points.lastIndex.toFloat()) * w
          val y1 = pad + h - ((sel.primaryValue / maxVal).toFloat() * h)
          drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(x, pad),
            end = Offset(x, pad + h),
            strokeWidth = 1.dp.toPx(),
          )
          drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(x, y1))
          drawCircle(color = VaultSurface, radius = 2.5.dp.toPx(), center = Offset(x, y1))
        }
      }
    }
  }
}
