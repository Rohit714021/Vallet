package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TransactionType
import com.example.data.local.VaultRepository
import com.example.data.prefs.VaultPreferences
import com.example.data.prefs.VaultSettingsState
import com.example.domain.BudgetAnalyticsEngine
import com.example.domain.CategoryBreakdown
import com.example.domain.TimeBucketPoint
import com.example.domain.TravelTripSummary
import com.example.domain.TrendWindow
import com.example.domain.YearlyEventGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
  val settings: VaultSettingsState = VaultSettingsState(),
  val selectedWindow: TrendWindow = TrendWindow.MONTHLY,
  val timeSeries: List<TimeBucketPoint> = emptyList(),
  val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
  val selectedCategoryId: Long? = null,
  val yearlyEventGroups: List<YearlyEventGroup> = emptyList(),
  val selectedEventYear: Int? = null,
  val selectedEventSubCategory: String? = null,
  val travelSummaries: List<TravelTripSummary> = emptyList(),
  val totalTrackedSpend: Double = 0.0,
  val totalTrackedIncome: Double = 0.0,
)

class AnalyticsViewModel(
  repository: VaultRepository,
  preferences: VaultPreferences,
) : ViewModel() {

  private val selectedWindow = MutableStateFlow(TrendWindow.MONTHLY)
  private val selectedCategoryId = MutableStateFlow<Long?>(null)
  private val selectedEventYear = MutableStateFlow<Int?>(null)
  private val selectedEventSubCategory = MutableStateFlow<String?>(null)

  private val dataFlow = combine(
    preferences.settingsFlow,
    repository.transactions,
    repository.categories,
  ) { settings, transactions, categories ->
    Triple(settings, transactions, categories)
  }

  private val filterFlow = combine(
    selectedWindow,
    selectedCategoryId,
    selectedEventYear,
    selectedEventSubCategory,
  ) { window, catId, yr, subCat ->
    AnalyticsFilters(window, catId, yr, subCat)
  }

  val uiState: StateFlow<AnalyticsUiState> = combine(
    dataFlow,
    filterFlow,
  ) { (settings, transactions, categories), filters ->
    val categoriesById = categories.associateBy { it.id }
    val series = BudgetAnalyticsEngine.buildTimeSeries(transactions, filters.window)
    val breakdowns = BudgetAnalyticsEngine.buildCategoryBreakdowns(transactions, categoriesById)
    val events = BudgetAnalyticsEngine.buildYearlyImportantEvents(transactions, categoriesById)
    val travels = BudgetAnalyticsEngine.buildTravelLocationSummaries(transactions, categoriesById)

    val activeCatId = filters.selectedCategoryId?.takeIf { id ->
      breakdowns.any { it.categoryId == id }
    } ?: breakdowns.firstOrNull()?.categoryId

    val totalSpend = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    AnalyticsUiState(
      settings = settings,
      selectedWindow = filters.window,
      timeSeries = series,
      categoryBreakdowns = breakdowns,
      selectedCategoryId = activeCatId,
      yearlyEventGroups = events,
      selectedEventYear = filters.selectedEventYear,
      selectedEventSubCategory = filters.selectedEventSubCategory,
      travelSummaries = travels,
      totalTrackedSpend = totalSpend,
      totalTrackedIncome = totalIncome,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = AnalyticsUiState(),
  )

  fun selectTrendWindow(window: TrendWindow) {
    selectedWindow.value = window
  }

  fun selectCategory(categoryId: Long) {
    selectedCategoryId.value = categoryId
  }

  fun selectEventYear(year: Int?) {
    selectedEventYear.value = year
  }

  fun selectEventSubCategory(subCategory: String?) {
    selectedEventSubCategory.value = subCategory
  }
}

private data class AnalyticsFilters(
  val window: TrendWindow,
  val selectedCategoryId: Long?,
  val selectedEventYear: Int?,
  val selectedEventSubCategory: String?,
)
