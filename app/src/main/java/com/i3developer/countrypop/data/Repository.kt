package com.i3developer.countrypop.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * Loads the bundled World Bank snapshot and can optionally refresh it from the
 * World Bank Indicators API (SP.POP.TOTL). Nothing about the user is sent.
 */
class PopulationRepository(private val context: Context) {

    private val cacheFile get() = File(context.filesDir, "population_refreshed.json")

    fun load(): Dataset {
        if (cacheFile.exists()) {
            runCatching { return parse(cacheFile.readText(), refreshed = true) }
        }
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        return parse(text, refreshed = false)
    }

    suspend fun refresh(current: Dataset): Result<Dataset> = withContext(Dispatchers.IO) {
        runCatching {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val url = "https://api.worldbank.org/v2/country/all/indicator/SP.POP.TOTL" +
                "?format=json&per_page=20000&date=${current.firstYear}:$year"
            val body = download(url)
            val root = JSONArray(body)
            val updated = root.getJSONObject(0).optString("lastupdated", current.sourceUpdated)
            val rows = root.getJSONArray(1)

            val known = current.countries.associateBy { it.iso3 }
            val fresh = HashMap<String, HashMap<Int, Long>>()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val iso = row.optString("countryiso3code")
                if (iso !in known || row.isNull("value")) continue
                fresh.getOrPut(iso) { HashMap() }[row.getString("date").toInt()] = row.getLong("value")
            }
            // Refuse a response that does not look like the full dataset.
            require(fresh.size >= known.size * 9 / 10) { "Unexpected response from the data source" }

            val lastYear = fresh.values.maxOf { it.keys.max() }
            val merged = current.countries.map { c ->
                val series = fresh[c.iso3]
                if (series == null) {
                    c
                } else {
                    val values = LongArray(lastYear - current.firstYear + 1) { idx ->
                        series[current.firstYear + idx] ?: -1L
                    }
                    Country(c.iso3, c.iso2, c.name, c.region, c.income, c.capital, c.areaKm2, c.areaYear, c.firstYear, values)
                }
            }
            val dataset = Dataset(merged, current.firstYear, lastYear, updated, refreshed = true)
            cacheFile.writeText(toJson(dataset))
            dataset
        }
    }

    /** True when both datasets hold exactly the same figures (ignores how they were loaded). */
    fun sameData(a: Dataset, b: Dataset): Boolean =
        a.lastYear == b.lastYear && a.countries.size == b.countries.size &&
            a.countries.indices.all { i ->
                a.countries[i].iso3 == b.countries[i].iso3 && a.countries[i].values.contentEquals(b.countries[i].values)
            }

    private fun download(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.requestMethod = "GET"
            check(conn.responseCode == 200) { "Data source returned HTTP ${conn.responseCode}" }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(text: String, refreshed: Boolean): Dataset {
        val doc = JSONObject(text)
        val first = doc.getInt("firstYear")
        val arr = doc.getJSONArray("countries")
        val list = ArrayList<Country>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val p = o.getJSONArray("p")
            val values = LongArray(p.length()) { if (p.isNull(it)) -1L else p.getLong(it) }
            list += Country(
                iso3 = o.getString("i"),
                iso2 = o.optString("c"),
                name = o.getString("n"),
                region = o.optString("r"),
                income = o.optString("inc"),
                capital = o.optString("cap"),
                areaKm2 = if (o.isNull("a")) null else o.getDouble("a"),
                areaYear = if (o.isNull("ay")) null else o.getInt("ay"),
                firstYear = first,
                values = values,
            )
        }
        return Dataset(list, first, doc.getInt("lastYear"), doc.optString("sourceUpdated"), refreshed)
    }

    private fun toJson(d: Dataset): String {
        val countries = JSONArray()
        for (c in d.countries) {
            countries.put(
                JSONObject()
                    .put("i", c.iso3).put("c", c.iso2).put("n", c.name).put("r", c.region)
                    .put("inc", c.income).put("cap", c.capital)
                    .put("a", c.areaKm2 ?: JSONObject.NULL).put("ay", c.areaYear ?: JSONObject.NULL)
                    .put("p", JSONArray().also { arr -> c.values.forEach { arr.put(if (it < 0) JSONObject.NULL else it) } }),
            )
        }
        return JSONObject()
            .put("firstYear", d.firstYear).put("lastYear", d.lastYear)
            .put("sourceUpdated", d.sourceUpdated).put("countries", countries)
            .toString()
    }

    companion object {
        private const val ASSET = "population.json"
    }
}

/** Favourite countries, kept on the device only. */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    fun save(items: Set<String>) {
        prefs.edit().putStringSet(KEY, items).apply()
    }

    private companion object {
        const val KEY = "iso3"
    }
}
