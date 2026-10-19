package com.i3developer.countrypop.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.ln

/** One country with its annual population series (index 0 = [firstYear], -1 = no value). */
class Country(
    val iso3: String,
    val iso2: String,
    val name: String,
    val region: String,
    val income: String,
    val capital: String,
    val areaKm2: Double?,
    val areaYear: Int?,
    val firstYear: Int,
    val values: LongArray,
) {
    private val latestIndex: Int = values.indexOfLast { it >= 0 }

    val latestYear: Int? get() = if (latestIndex >= 0) firstYear + latestIndex else null
    val latest: Long? get() = if (latestIndex >= 0) values[latestIndex] else null

    /** People per square kilometre of land area, using the latest population. */
    val density: Double?
        get() {
            val p = latest ?: return null
            val a = areaKm2?.takeIf { it > 0 } ?: return null
            return p / a
        }

    /** Growth between the last two consecutive years, as a fraction (0.01 = 1%). */
    val growth: Double?
        get() {
            if (latestIndex < 1) return null
            val prev = values[latestIndex - 1]
            if (prev <= 0) return null
            return values[latestIndex].toDouble() / prev - 1.0
        }

    /**
     * Average yearly growth over the last three years, as a fraction (0.01 = 1%). Smoother than [growth],
     * which uses a single year. Null when there is no figure three years before the latest one.
     */
    val avgGrowth3: Double?
        get() {
            if (latestIndex < 3) return null
            val base = values[latestIndex - 3]
            if (base <= 0) return null
            return (values[latestIndex].toDouble() / base).pow(1.0 / 3.0) - 1.0
        }

    /**
     * Continuous yearly growth rate implied by [growth]. Null when there is no growth figure or it is
     * extreme (more than 10% either way), because a one-off jump should not be extrapolated.
     */
    private val ratePerYear: Double?
        get() {
            val g = growth ?: return null
            if (g <= -MAX_RATE || g >= MAX_RATE) return null
            return ln(1.0 + g)
        }

    /**
     * Estimated population at [nowMillis]. World Bank figures are mid-year (1 July) values, so the latest
     * figure is projected forward from 1 July of its year using last year's growth. This is an
     * estimate, not a measurement. Null when it cannot be computed or the figure is too old to project.
     */
    fun estimateAt(nowMillis: Long): Double? {
        val p = latest ?: return null
        val year = latestYear ?: return null
        val k = ratePerYear ?: return null
        val midYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(year, Calendar.JULY, 1)
        }.timeInMillis
        val years = (nowMillis - midYear) / MILLIS_PER_YEAR
        if (years < 0 || years > MAX_PROJECT_YEARS) return null
        return p * exp(k * years)
    }

    /** Estimated net change in people per day, based on [estimateAt]. */
    fun estimatedChangePerDay(nowMillis: Long): Double? {
        val now = estimateAt(nowMillis) ?: return null
        val k = ratePerYear ?: return null
        return now * (exp(k / 365.25) - 1.0)
    }

    val growthYears: Pair<Int, Int>? get() = if (latestIndex >= 1) (firstYear + latestIndex - 1) to (firstYear + latestIndex) else null

    /** (year, population) pairs for every year that has a value. */
    fun series(): List<Pair<Int, Long>> =
        values.indices.filter { values[it] >= 0 }.map { (firstYear + it) to values[it] }

    val regionLabel: String get() = shortRegion(region)

    private companion object {
        const val MILLIS_PER_YEAR = 365.25 * 24 * 3600 * 1000
        const val MAX_PROJECT_YEARS = 5.0
        const val MAX_RATE = 0.10
    }

    /** Flag emoji built from the ISO 3166 alpha-2 code. */
    val flag: String
        get() {
            if (iso2.length != 2 || !iso2.all { it in 'A'..'Z' }) return "🌐"
            val base = 0x1F1E6 - 'A'.code
            return String(Character.toChars(base + iso2[0].code)) + String(Character.toChars(base + iso2[1].code))
        }
}

/** The World Bank's region names are long; this shortens the longest for display. */
fun shortRegion(region: String): String =
    if (region == "Middle East, North Africa, Afghanistan & Pakistan") "MENA, Afghanistan & Pakistan" else region

class Dataset(
    val countries: List<Country>,
    val firstYear: Int,
    val lastYear: Int,
    /** "lastupdated" date reported by the World Bank API, e.g. 2026-10-08. */
    val sourceUpdated: String,
    /** True when this data was refreshed from the API rather than bundled. */
    val refreshed: Boolean,
) {
    private val byIso = countries.associateBy { it.iso3 }
    fun find(iso3: String): Country? = byIso[iso3]
    val regions: List<String> get() = countries.map { it.region }.distinct().sorted()
}
