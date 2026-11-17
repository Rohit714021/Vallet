package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountEntity
import com.example.data.local.AccountType
import com.example.data.local.BudgetPeriod
import com.example.data.local.BudgetRuleEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.ObsidianDatabase
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.prefs.CurrencyFormatter
import com.example.data.prefs.VaultCurrency
import com.example.data.security.BackupResult
import com.example.data.security.CryptoBackupManager
import com.example.data.security.RestoreResult
import com.example.domain.BudgetAnalyticsEngine
import com.example.domain.FinancialCalculator
import com.example.domain.TrendWindow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VaultDomainAndCryptoTest {

  private lateinit var context: Context
  private lateinit var db: ObsidianDatabase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, ObsidianDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `investment and debt calculators compute accurate compounding and EMI`() {
    val inv = FinancialCalculator.calculateInvestmentProjection(
      lumpsum = 100_000.0,
      monthlySip = 10_000.0,
      annualRatePercent = 12.0,
      years = 10,
    )
    assertEquals(10, inv.yearlyPoints.size)
    assertEquals(100_000.0 + 10_000.0 * 120, inv.totalInvested, 1.0)
    assertTrue(inv.futureValue > inv.totalInvested)

    val debtStandard = FinancialCalculator.calculateDebtPayoff(
      principal = 500_000.0,
      annualRatePercent = 10.0,
      tenureMonths = 60,
      extraMonthlyPayment = 0.0,
    )
    val debtAccelerated = FinancialCalculator.calculateDebtPayoff(
      principal = 500_000.0,
      annualRatePercent = 10.0,
      tenureMonths = 60,
      extraMonthlyPayment = 5_000.0,
    )
    assertTrue(debtStandard.baseMonthlyEmi in 10600.0..10700.0)
    assertTrue(debtAccelerated.actualMonthsToPayoff < debtStandard.actualMonthsToPayoff)
    assertTrue(debtAccelerated.interestSaved > 0.0)
  }

  @Test
  fun `budget analytics computes daily monthly yearly percent and travel events`() {
    val now = System.currentTimeMillis()
    val travelCat = CategoryEntity(id = 10L, name = "Travel & Trips", scope = TransactionType.EXPENSE)
    val hotelSub = CategoryEntity(id = 11L, name = "Hotels", parentId = 10L, scope = TransactionType.EXPENSE)
    val eventCat = CategoryEntity(id = 20L, name = "Important Purchases & Events", scope = TransactionType.EXPENSE, isMilestoneDefault = true)
    val cats = mapOf(10L to travelCat, 11L to hotelSub, 20L to eventCat)

    val txs = listOf(
      TransactionEntity(
        id = 1L,
        amount = 4500.0,
        type = TransactionType.EXPENSE,
        fromLabel = "HDFC Bank",
        toLabel = "Taj Resort",
        categoryId = 10L,
        subCategoryId = 11L,
        timestamp = now,
        isTravel = true,
        locationName = "Goa",
        tripName = "Winter Break",
        travelStartDate = now - 86_400_000L,
        travelEndDate = now,
      ),
      TransactionEntity(
        id = 2L,
        amount = 80_000.0,
        type = TransactionType.EXPENSE,
        fromLabel = "ICICI Bank",
        toLabel = "Apple Store",
        categoryId = 20L,
        timestamp = now,
        isImportantPurchase = true,
        eventTitle = "Workstation Laptop",
      ),
    )

    val rules = listOf(
      BudgetRuleEntity(id = 1L, name = "Daily Cap", period = BudgetPeriod.DAILY, limitAmount = 50_000.0, alertThresholdPercent = 80),
      BudgetRuleEntity(id = 2L, name = "Monthly Cap", period = BudgetPeriod.MONTHLY, limitAmount = 100_000.0, alertThresholdPercent = 80),
      BudgetRuleEntity(id = 3L, name = "Yearly Cap", period = BudgetPeriod.YEARLY, limitAmount = 500_000.0, alertThresholdPercent = 80),
    )

    val paces = BudgetAnalyticsEngine.computePeriodPaces(txs, rules, now)
    assertEquals(3, paces.size)
    assertTrue(paces.first { it.period == BudgetPeriod.DAILY }.percentSpent > 100f)
    assertEquals(84.5f, paces.first { it.period == BudgetPeriod.MONTHLY }.percentSpent, 0.5f)

    val alerts = BudgetAnalyticsEngine.evaluateBudgetRules(txs, rules, cats, now)
    assertEquals(2, alerts.count { it.isAlertTriggered })

    val events = BudgetAnalyticsEngine.buildYearlyImportantEvents(txs, cats)
    assertEquals(1, events.size)
    assertEquals(80_000.0, events.first().totalAmount, 0.1)

    val travels = BudgetAnalyticsEngine.buildTravelLocationSummaries(txs, cats)
    assertEquals(1, travels.size)
    assertEquals("Goa", travels.first().locationName)
    assertEquals(4500.0, travels.first().totalAmount, 0.1)

    val monthlySeries = BudgetAnalyticsEngine.buildTimeSeries(txs, TrendWindow.MONTHLY, now)
    assertEquals(12, monthlySeries.size)
  }

  @Test
  fun `currency formatting defaults cleanly to INR`() {
    val formatted = CurrencyFormatter.format(125000.0, VaultCurrency.INR, showDecimals = false)
    assertTrue(formatted.contains("₹"))
    assertTrue(formatted.contains("1,25,000"))
  }

  @Test
  fun `room database persists accounts and encrypted backup rejects wrong passphrase`() = runTest {
    val accountId = db.accountDao().insertAccount(
      AccountEntity(
        name = "HDFC Salary",
        institution = "HDFC Bank",
        type = AccountType.BANK,
        balance = 150_000.0,
      )
    )
    assertTrue(accountId > 0L)
    val all = db.accountDao().getAllAccounts().first()
    assertEquals(1, all.size)

    // Test CryptoBackupManager against a real file-backed DB instance
    val fileDb = ObsidianDatabase.getInstance(context)
    fileDb.accountDao().insertAccount(
      AccountEntity(name = "SBI Vault", institution = "SBI", type = AccountType.BANK, balance = 50_000.0)
    )
    val backupFile = File(context.cacheDir, "test_backup.enc")
    val backupUri = Uri.fromFile(backupFile)
    val crypto = CryptoBackupManager(context)

    val exportRes = crypto.exportEncryptedDatabase(backupUri, "StrongVaultPass99", fileDb)
    assertTrue(exportRes is BackupResult.Success)
    assertTrue(backupFile.exists())

    val wrongRestore = crypto.restoreEncryptedDatabase(backupUri, "WrongPass") {}
    assertTrue(wrongRestore is RestoreResult.Failure)

    val goodRestore = crypto.restoreEncryptedDatabase(backupUri, "StrongVaultPass99") {
      ObsidianDatabase.closeAndClearInstance()
    }
    assertTrue(goodRestore is RestoreResult.Success)
    ObsidianDatabase.closeAndClearInstance()
  }

  @Test
  fun `seeded expense income and investment categories each have 6 sub-categories`() = runTest {
    val fileDb = ObsidianDatabase.getInstance(context)
    val allCats = fileDb.categoryDao().getAllCategories().first()
    val parentsByScope = listOf(
      TransactionType.EXPENSE,
      TransactionType.INCOME,
      TransactionType.INVESTMENT_CONTRIBUTION,
    )
    for (scope in parentsByScope) {
      val parents = allCats.filter { it.parentId == null && it.scope == scope }
      assertTrue("Expected parents for $scope", parents.isNotEmpty())
      for (parent in parents) {
        val subs = allCats.filter { it.parentId == parent.id }
        assertEquals("Expected 6 sub-categories for ${parent.name}", 6, subs.size)
        // Verify choosing any sub-category maps cleanly back to its parent category
        for (sub in subs) {
          assertEquals(parent.id, sub.parentId)
        }
      }
    }
    ObsidianDatabase.closeAndClearInstance()
  }
}
