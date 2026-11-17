package com.example.data.prefs

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

enum class VaultCurrency(
  val code: String,
  val symbol: String,
  val displayName: String,
  val locale: Locale,
) {
  INR("INR", "₹", "Indian Rupee (INR)", Locale.Builder().setLanguage("en").setRegion("IN").build()),
  USD("USD", "$", "US Dollar (USD)", Locale.US),
  EUR("EUR", "€", "Euro (EUR)", Locale.GERMANY),
  GBP("GBP", "£", "British Pound (GBP)", Locale.UK),
  JPY("JPY", "¥", "Japanese Yen (JPY)", Locale.JAPAN);

  companion object {
    fun fromCode(code: String?): VaultCurrency =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
  }
}

object CurrencyFormatter {
  fun format(amount: Double, currency: VaultCurrency, showDecimals: Boolean = true): String {
    if (currency == VaultCurrency.INR) {
      return formatIndianRupee(amount, showDecimals)
    }
    val formatter = NumberFormat.getCurrencyInstance(currency.locale)
    formatter.currency = Currency.getInstance(currency.code)
    val maxDigits = if (currency == VaultCurrency.JPY || !showDecimals) 0 else 2
    formatter.maximumFractionDigits = maxDigits
    formatter.minimumFractionDigits = if (currency == VaultCurrency.JPY || !showDecimals) 0 else 2
    return formatter.format(amount)
  }

  private fun formatIndianRupee(amount: Double, showDecimals: Boolean): String {
    val sign = if (amount < 0) "-" else ""
    val absVal = abs(amount)
    val whole = if (showDecimals) absVal.toLong() else absVal.roundToLong()
    val frac = if (showDecimals) ((absVal - whole) * 100.0).roundToLong().coerceIn(0, 99) else 0L

    val digits = whole.toString()
    val grouped = if (digits.length <= 3) {
      digits
    } else {
      val lastThree = digits.takeLast(3)
      val prefix = digits.dropLast(3)
      val pairs = prefix.reversed().chunked(2).joinToString(",").reversed()
      "$pairs,$lastThree"
    }

    val decimalPart = if (showDecimals) String.format(Locale.US, ".%02d", frac) else ""
    return "${sign}₹$grouped$decimalPart"
  }

  fun formatCompact(amount: Double, currency: VaultCurrency): String {
    val absVal = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
      currency == VaultCurrency.INR && absVal >= 10_000_000 ->
        String.format(Locale.US, "%s%s%.2fCr", sign, currency.symbol, absVal / 10_000_000)
      currency == VaultCurrency.INR && absVal >= 100_000 ->
        String.format(Locale.US, "%s%s%.2fL", sign, currency.symbol, absVal / 100_000)
      absVal >= 1_000_000 ->
        String.format(Locale.US, "%s%s%.2fM", sign, currency.symbol, absVal / 1_000_000)
      absVal >= 1_000 ->
        String.format(Locale.US, "%s%s%.1fK", sign, currency.symbol, absVal / 1_000)
      else -> format(amount, currency, showDecimals = false)
    }
  }
}
