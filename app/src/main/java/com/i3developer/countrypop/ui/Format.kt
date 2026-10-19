package com.i3developer.countrypop.ui

import java.text.NumberFormat
import java.util.Locale

private val whole: NumberFormat get() = NumberFormat.getIntegerInstance()

fun formatFull(n: Long): String = whole.format(n)

fun formatCompact(n: Double): String = when {
    n >= 1e9 -> String.format(Locale.getDefault(), "%.2f B", n / 1e9)
    n >= 1e6 -> String.format(Locale.getDefault(), "%.1f M", n / 1e6)
    n >= 1e3 -> String.format(Locale.getDefault(), "%.0f K", n / 1e3)
    else -> String.format(Locale.getDefault(), "%.0f", n)
}

fun formatDensity(d: Double?): String = when {
    d == null -> "—"
    d >= 100 -> String.format(Locale.getDefault(), "%,.0f /km²", d)
    else -> String.format(Locale.getDefault(), "%.1f /km²", d)
}

fun formatArea(a: Double?): String = if (a == null) "—" else String.format(Locale.getDefault(), "%,.0f km²", a)

fun formatGrowth(g: Double?): String =
    if (g == null) "—" else String.format(Locale.getDefault(), "%+.2f%%", g * 100)
