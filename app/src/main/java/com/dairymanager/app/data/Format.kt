package com.dairymanager.app.data

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared formatting. Dates are stored in Room as ISO strings ("yyyy-MM-dd"):
 * they sort lexicographically, so period queries are plain string comparisons
 * and no timezone conversion is ever involved.
 */
object Format {

    private val inr = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun money(amount: Double): String = inr.format(amount)

    fun litres(litres: Double): String = "%.2f L".format(Locale.ENGLISH, litres)

    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
        isLenient = false
    }
    private val display = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    fun today(): String = iso.format(Date())

    fun firstDayOfMonth(): String = today().substring(0, 8) + "01"

    fun monthPrefix(): String = today().substring(0, 7)

    fun displayDate(isoDate: String): String = try {
        display.format(iso.parse(isoDate)!!)
    } catch (e: Exception) {
        isoDate
    }

    fun isValidDate(value: String): Boolean = try {
        iso.parse(value) != null
    } catch (e: Exception) {
        false
    }

    /** Money is rounded to paise at write time, so displayed sums never drift. */
    fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}
