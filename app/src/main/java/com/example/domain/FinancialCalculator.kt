package com.example.domain

import kotlin.math.max
import kotlin.math.pow

data class InvestmentYearlyPoint(
  val year: Int,
  val investedCapital: Double,
  val totalValue: Double,
  val wealthGained: Double,
)

data class InvestmentProjection(
  val futureValue: Double,
  val totalInvested: Double,
  val totalGain: Double,
  val returnMultiple: Double,
  val yearlyPoints: List<InvestmentYearlyPoint>,
)

data class DebtYearlyPoint(
  val year: Int,
  val remainingBalance: Double,
  val cumulativePrincipalPaid: Double,
  val cumulativeInterestPaid: Double,
)

data class DebtPayoffResult(
  val baseMonthlyEmi: Double,
  val totalInterestStandard: Double,
  val totalInterestActual: Double,
  val interestSaved: Double,
  val actualMonthsToPayoff: Int,
  val monthsSaved: Int,
  val totalPayableActual: Double,
  val yearlySchedule: List<DebtYearlyPoint>,
)

object FinancialCalculator {

  fun calculateInvestmentProjection(
    lumpsum: Double,
    monthlySip: Double,
    annualRatePercent: Double,
    years: Int,
  ): InvestmentProjection {
    val safeYears = years.coerceIn(1, 50)
    val safeLumpsum = max(0.0, lumpsum)
    val safeSip = max(0.0, monthlySip)
    val monthlyRate = max(0.0, annualRatePercent) / 100.0 / 12.0

    var balance = safeLumpsum
    var totalInvested = safeLumpsum
    val yearlyPoints = mutableListOf<InvestmentYearlyPoint>()

    for (year in 1..safeYears) {
      for (month in 1..12) {
        balance = (balance + safeSip) * (1.0 + monthlyRate)
        totalInvested += safeSip
      }
      yearlyPoints.add(
        InvestmentYearlyPoint(
          year = year,
          investedCapital = totalInvested,
          totalValue = balance,
          wealthGained = (balance - totalInvested).coerceAtLeast(0.0),
        )
      )
    }

    val gain = (balance - totalInvested).coerceAtLeast(0.0)
    val multiple = if (totalInvested > 0.0) balance / totalInvested else 0.0
    return InvestmentProjection(
      futureValue = balance,
      totalInvested = totalInvested,
      totalGain = gain,
      returnMultiple = multiple,
      yearlyPoints = yearlyPoints,
    )
  }

  fun calculateDebtPayoff(
    principal: Double,
    annualRatePercent: Double,
    tenureMonths: Int,
    extraMonthlyPayment: Double = 0.0,
  ): DebtPayoffResult {
    val safePrincipal = max(0.0, principal)
    val safeMonths = tenureMonths.coerceIn(1, 480)
    val safeExtra = max(0.0, extraMonthlyPayment)
    val monthlyRate = max(0.0, annualRatePercent) / 100.0 / 12.0

    if (safePrincipal == 0.0) {
      return DebtPayoffResult(0.0, 0.0, 0.0, 0.0, 0, 0, 0.0, emptyList())
    }

    val baseEmi = if (monthlyRate == 0.0) {
      safePrincipal / safeMonths
    } else {
      val factor = (1.0 + monthlyRate).pow(safeMonths.toDouble())
      safePrincipal * monthlyRate * factor / (factor - 1.0)
    }

    val totalInterestStandard = (baseEmi * safeMonths - safePrincipal).coerceAtLeast(0.0)

    var remaining = safePrincipal
    var cumPrincipal = 0.0
    var cumInterest = 0.0
    var monthCount = 0
    val yearlySchedule = mutableListOf<DebtYearlyPoint>()
    val maxGuardMonths = safeMonths * 2

    while (remaining > 0.01 && monthCount < maxGuardMonths) {
      monthCount++
      val interestForMonth = remaining * monthlyRate
      val plannedPayment = baseEmi + safeExtra
      val actualPayment = minOf(remaining + interestForMonth, plannedPayment)
      val principalPaid = (actualPayment - interestForMonth).coerceAtLeast(0.0)

      remaining = (remaining - principalPaid).coerceAtLeast(0.0)
      cumPrincipal += principalPaid
      cumInterest += interestForMonth

      if (monthCount % 12 == 0 || remaining <= 0.01) {
        val yearIndex = ((monthCount - 1) / 12) + 1
        yearlySchedule.add(
          DebtYearlyPoint(
            year = yearIndex,
            remainingBalance = remaining,
            cumulativePrincipalPaid = cumPrincipal,
            cumulativeInterestPaid = cumInterest,
          )
        )
      }
    }

    val interestSaved = (totalInterestStandard - cumInterest).coerceAtLeast(0.0)
    val monthsSaved = (safeMonths - monthCount).coerceAtLeast(0)

    return DebtPayoffResult(
      baseMonthlyEmi = baseEmi,
      totalInterestStandard = totalInterestStandard,
      totalInterestActual = cumInterest,
      interestSaved = interestSaved,
      actualMonthsToPayoff = monthCount,
      monthsSaved = monthsSaved,
      totalPayableActual = safePrincipal + cumInterest,
      yearlySchedule = yearlySchedule,
    )
  }

  fun projectSingleHoldingInOneYear(balance: Double, annualRatePercent: Double): Double {
    return balance * (1.0 + annualRatePercent / 100.0)
  }

  fun formatTenure(months: Int): String {
    val yrs = months / 12
    val rem = months % 12
    return when {
      yrs > 0 && rem > 0 -> "${yrs}y ${rem}m"
      yrs > 0 -> "${yrs} yr${if (yrs > 1) "s" else ""}"
      else -> "${rem} mo"
    }
  }
}
