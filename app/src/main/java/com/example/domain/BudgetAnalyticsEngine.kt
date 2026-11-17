package com.example.domain

import com.example.data.local.BudgetPeriod
import com.example.data.local.BudgetRuleEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BudgetProgressSummary(
  val rule: BudgetRuleEntity,
  val categoryName: String?,
  val spentAmount: Double,
  val percentSpent: Float,
  val remainingAmount: Double,
  val isAlertTriggered: Boolean,
  val isOverBudget: Boolean,
)

data class PeriodBudgetPace(
  val period: BudgetPeriod,
  val spentAmount: Double,
  val limitAmount: Double,
  val percentSpent: Float,
  val hasConfiguredLimit: Boolean,
)

data class TimeBucketPoint(
  val label: String,
  val expenseTotal: Double,
  val incomeTotal: Double,
  val comparisonExpenseTotal: Double = 0.0,
)

data class SubCategoryBreakdown(
  val subCategoryId: Long?,
  val name: String,
  val amount: Double,
  val count: Int,
  val sharePercent: Float,
)

data class CategoryBreakdown(
  val categoryId: Long,
  val name: String,
  val colorHex: Long,
  val amount: Double,
  val sharePercent: Float,
  val subCategories: List<SubCategoryBreakdown>,
)

data class YearlyEventGroup(
  val year: Int,
  val totalAmount: Double,
  val items: List<ImportantPurchaseItem>,
)

data class ImportantPurchaseItem(
  val transaction: TransactionEntity,
  val categoryName: String,
  val subCategoryName: String,
)

data class TravelTripSummary(
  val tripKey: String,
  val tripName: String,
  val locationName: String,
  val startTimestamp: Long,
  val endTimestamp: Long,
  val totalAmount: Double,
  val transactionCount: Int,
  val dailyAverage: Double,
  val transactions: List<TransactionEntity>,
)

enum class TrendWindow(val label: String) {
  DAILY("Daily (14d)"),
  MONTHLY("Monthly (12m)"),
  YEARLY("Yearly (YoY)"),
}

object BudgetAnalyticsEngine {

  fun computePeriodPaces(
    transactions: List<TransactionEntity>,
    rules: List<BudgetRuleEntity>,
    nowMs: Long = System.currentTimeMillis(),
  ): List<PeriodBudgetPace> {
    return BudgetPeriod.entries.map { period ->
      val (start, end) = periodBounds(period, nowMs)
      val spent = transactions
        .asSequence()
        .filter { it.type == TransactionType.EXPENSE && it.timestamp in start..end }
        .sumOf { it.amount }

      val overallRule = rules.firstOrNull { it.isActive && it.period == period && it.categoryId == null }
      val limit = overallRule?.limitAmount
        ?: rules.filter { it.isActive && it.period == period }.sumOf { it.limitAmount }

      val pct = if (limit > 0.0) ((spent / limit) * 100.0).toFloat() else 0f
      PeriodBudgetPace(
        period = period,
        spentAmount = spent,
        limitAmount = limit,
        percentSpent = pct,
        hasConfiguredLimit = limit > 0.0,
      )
    }
  }

  fun evaluateBudgetRules(
    transactions: List<TransactionEntity>,
    rules: List<BudgetRuleEntity>,
    categoriesById: Map<Long, CategoryEntity>,
    nowMs: Long = System.currentTimeMillis(),
  ): List<BudgetProgressSummary> {
    return rules.filter { it.isActive }.map { rule ->
      val (start, end) = periodBounds(rule.period, nowMs)
      val spent = transactions
        .asSequence()
        .filter { tx ->
          tx.type == TransactionType.EXPENSE &&
            tx.timestamp in start..end &&
            (rule.categoryId == null || tx.categoryId == rule.categoryId || tx.subCategoryId == rule.categoryId)
        }
        .sumOf { it.amount }

      val pct = if (rule.limitAmount > 0.0) ((spent / rule.limitAmount) * 100.0).toFloat() else 0f
      val catName = rule.categoryId?.let { categoriesById[it]?.name }
      BudgetProgressSummary(
        rule = rule,
        categoryName = catName,
        spentAmount = spent,
        percentSpent = pct,
        remainingAmount = (rule.limitAmount - spent),
        isAlertTriggered = rule.limitAmount > 0.0 && pct >= rule.alertThresholdPercent,
        isOverBudget = rule.limitAmount > 0.0 && spent > rule.limitAmount,
      )
    }
  }

  fun buildTimeSeries(
    transactions: List<TransactionEntity>,
    window: TrendWindow,
    nowMs: Long = System.currentTimeMillis(),
  ): List<TimeBucketPoint> {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val incomes = transactions.filter { it.type == TransactionType.INCOME }

    return when (window) {
      TrendWindow.DAILY -> {
        val dayFmt = SimpleDateFormat("dd MMM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
          timeInMillis = nowMs
          setToStartOfDay()
          add(Calendar.DAY_OF_YEAR, -13)
        }
        (0 until 14).map {
          val dayStart = cal.timeInMillis
          val dayEnd = dayStart + 86_400_000L - 1
          val prevWeekStart = dayStart - 7 * 86_400_000L
          val prevWeekEnd = dayEnd - 7 * 86_400_000L
          val exp = expenses.filter { t -> t.timestamp in dayStart..dayEnd }.sumOf { t -> t.amount }
          val inc = incomes.filter { t -> t.timestamp in dayStart..dayEnd }.sumOf { t -> t.amount }
          val prevExp = expenses.filter { t -> t.timestamp in prevWeekStart..prevWeekEnd }.sumOf { t -> t.amount }
          val label = dayFmt.format(Date(dayStart))
          cal.add(Calendar.DAY_OF_YEAR, 1)
          TimeBucketPoint(label = label, expenseTotal = exp, incomeTotal = inc, comparisonExpenseTotal = prevExp)
        }
      }
      TrendWindow.MONTHLY -> {
        val monthFmt = SimpleDateFormat("MMM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
          timeInMillis = nowMs
          set(Calendar.DAY_OF_MONTH, 1)
          setToStartOfDay()
          add(Calendar.MONTH, -11)
        }
        (0 until 12).map {
          val mStart = cal.timeInMillis
          val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
          val mEnd = nextCal.timeInMillis - 1

          val prevYrStart = (cal.clone() as Calendar).apply { add(Calendar.YEAR, -1) }.timeInMillis
          val prevYrEnd = (nextCal.clone() as Calendar).apply { add(Calendar.YEAR, -1) }.timeInMillis - 1

          val exp = expenses.filter { t -> t.timestamp in mStart..mEnd }.sumOf { t -> t.amount }
          val inc = incomes.filter { t -> t.timestamp in mStart..mEnd }.sumOf { t -> t.amount }
          val prevYrExp = expenses.filter { t -> t.timestamp in prevYrStart..prevYrEnd }.sumOf { t -> t.amount }
          val label = monthFmt.format(Date(mStart))
          cal.add(Calendar.MONTH, 1)
          TimeBucketPoint(label = label, expenseTotal = exp, incomeTotal = inc, comparisonExpenseTotal = prevYrExp)
        }
      }
      TrendWindow.YEARLY -> {
        val curYear = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.YEAR)
        val years = ((curYear - 4)..curYear).toList()
        years.map { yr ->
          val calStart = Calendar.getInstance().apply {
            set(yr, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
          }
          val calEnd = Calendar.getInstance().apply {
            set(yr + 1, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
          }
          val yStart = calStart.timeInMillis
          val yEnd = calEnd.timeInMillis - 1
          val exp = expenses.filter { t -> t.timestamp in yStart..yEnd }.sumOf { t -> t.amount }
          val inc = incomes.filter { t -> t.timestamp in yStart..yEnd }.sumOf { t -> t.amount }
          TimeBucketPoint(label = yr.toString(), expenseTotal = exp, incomeTotal = inc)
        }
      }
    }
  }

  fun buildCategoryBreakdowns(
    transactions: List<TransactionEntity>,
    categoriesById: Map<Long, CategoryEntity>,
  ): List<CategoryBreakdown> {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    val grandTotal = expenses.sumOf { it.amount }
    if (grandTotal <= 0.0) return emptyList()

    return expenses
      .groupBy { it.categoryId }
      .map { (catId, txs) ->
        val cat = categoriesById[catId]
        val catTotal = txs.sumOf { it.amount }
        val subs = txs.groupBy { it.subCategoryId }.map { (subId, subTxs) ->
          val subName = subId?.let { categoriesById[it]?.name } ?: "General"
          val subTotal = subTxs.sumOf { it.amount }
          SubCategoryBreakdown(
            subCategoryId = subId,
            name = subName,
            amount = subTotal,
            count = subTxs.size,
            sharePercent = if (catTotal > 0.0) ((subTotal / catTotal) * 100.0).toFloat() else 0f,
          )
        }.sortedByDescending { it.amount }

        CategoryBreakdown(
          categoryId = catId,
          name = cat?.name ?: "Uncategorized",
          colorHex = cat?.colorHex ?: 0xFF2EE6A8,
          amount = catTotal,
          sharePercent = ((catTotal / grandTotal) * 100.0).toFloat(),
          subCategories = subs,
        )
      }
      .sortedByDescending { it.amount }
  }

  fun buildYearlyImportantEvents(
    transactions: List<TransactionEntity>,
    categoriesById: Map<Long, CategoryEntity>,
  ): List<YearlyEventGroup> {
    val importantTxs = transactions.filter { tx ->
      val catMilestone = categoriesById[tx.categoryId]?.isMilestoneDefault == true
      val subMilestone = tx.subCategoryId?.let { categoriesById[it]?.isMilestoneDefault } == true
      tx.isImportantPurchase || catMilestone || subMilestone
    }

    val cal = Calendar.getInstance()
    return importantTxs
      .groupBy { tx ->
        cal.timeInMillis = tx.timestamp
        cal.get(Calendar.YEAR)
      }
      .entries
      .sortedByDescending { it.key }
      .map { (year, txs) ->
        val items = txs.sortedByDescending { it.timestamp }.map { tx ->
          ImportantPurchaseItem(
            transaction = tx,
            categoryName = categoriesById[tx.categoryId]?.name ?: "Milestone",
            subCategoryName = tx.subCategoryId?.let { categoriesById[it]?.name } ?: "General",
          )
        }
        YearlyEventGroup(
          year = year,
          totalAmount = txs.sumOf { it.amount },
          items = items,
        )
      }
  }

  fun buildTravelLocationSummaries(
    transactions: List<TransactionEntity>,
    categoriesById: Map<Long, CategoryEntity>,
  ): List<TravelTripSummary> {
    val travelCatIds = categoriesById.values
      .filter { it.name.contains("Travel", ignoreCase = true) || it.name.contains("Flight", ignoreCase = true) || it.name.contains("Hotel", ignoreCase = true) }
      .map { it.id }
      .toSet()

    val travelTxs = transactions.filter { tx ->
      tx.isTravel || tx.locationName.isNotBlank() || tx.tripName.isNotBlank() ||
        tx.categoryId in travelCatIds || (tx.subCategoryId in travelCatIds)
    }

    return travelTxs
      .groupBy { tx ->
        val loc = tx.locationName.trim().ifBlank { "Unspecified Location" }
        val trip = tx.tripName.trim().ifBlank { loc }
        "$trip|$loc"
      }
      .map { (key, txs) ->
        val parts = key.split("|")
        val tripName = parts.getOrElse(0) { "Trip" }
        val locationName = parts.getOrElse(1) { "Location" }
        val start = txs.minOf { it.travelStartDate ?: it.timestamp }
        val end = txs.maxOf { it.travelEndDate ?: it.timestamp }
        val days = (((end - start) / 86_400_000L) + 1).coerceAtLeast(1L)
        val total = txs.sumOf { it.amount }
        TravelTripSummary(
          tripKey = key,
          tripName = tripName,
          locationName = locationName,
          startTimestamp = start,
          endTimestamp = end,
          totalAmount = total,
          transactionCount = txs.size,
          dailyAverage = total / days.toDouble(),
          transactions = txs.sortedByDescending { it.timestamp },
        )
      }
      .sortedByDescending { it.endTimestamp }
  }

  fun periodBounds(period: BudgetPeriod, nowMs: Long = System.currentTimeMillis()): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
    when (period) {
      BudgetPeriod.DAILY -> {
        cal.setToStartOfDay()
        val start = cal.timeInMillis
        val end = start + 86_400_000L - 1
        return start to end
      }
      BudgetPeriod.MONTHLY -> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.setToStartOfDay()
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1
        return start to end
      }
      BudgetPeriod.YEARLY -> {
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.setToStartOfDay()
        val start = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        val end = cal.timeInMillis - 1
        return start to end
      }
    }
  }

  private fun Calendar.setToStartOfDay() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
}
