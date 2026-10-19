package com.i3developer.countrypop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.i3developer.countrypop.AppViewModel
import com.i3developer.countrypop.BuildConfig
import com.i3developer.countrypop.data.Country
import com.i3developer.countrypop.data.Dataset
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToLong

// ---------------------------------------------------------------- shared pieces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = { actions() },
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}

@Composable
private fun CountryRow(country: Country, onClick: () -> Unit, trailing: String? = null, subtitle: String? = null) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Text(country.flag, style = MaterialTheme.typography.headlineSmall) },
        headlineContent = { Text(country.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle ?: country.regionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    trailing ?: country.latest?.let { formatCompact(it.toDouble()) }.orEmpty(),
                    fontWeight = FontWeight.SemiBold,
                )
                if (trailing == null) {
                    Text(
                        country.latestYear?.toString().orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

// ---------------------------------------------------------------- countries

private enum class SortKey(val label: String) {
    Name("Name"), Population("Population"), Area("Area"), Density("Density")
}

@Composable
fun CountriesScreen(vm: AppViewModel, onOpen: (String) -> Unit, onAbout: () -> Unit) {
    val data = vm.dataset
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortKey.Name) }
    var region by rememberSaveable { mutableStateOf<String?>(null) }

    val list = remember(data, query, sort, region) {
        data.countries
            .filter { region == null || it.region == region }
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            .let { l ->
                when (sort) {
                    SortKey.Name -> l.sortedBy { it.name }
                    SortKey.Population -> l.sortedByDescending { it.latest ?: -1L }
                    SortKey.Area -> l.sortedByDescending { it.areaKm2 ?: -1.0 }
                    SortKey.Density -> l.sortedByDescending { it.density ?: -1.0 }
                }
            }
    }

    Column(Modifier.fillMaxSize()) {
        AppBar("Country Population", actions = {
            if (vm.refreshing) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            } else {
                TextButton(onClick = vm::refresh) { Text("Refresh") }
            }
            IconButton(onClick = onAbout) { Icon(Icons.Default.Info, contentDescription = "About and sources") }
        })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text("Search countries") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SortKey.entries) { key ->
                FilterChip(selected = sort == key, onClick = { sort = key }, label = { Text(key.label) })
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(selected = region == null, onClick = { region = null }, label = { Text("All regions") }) }
            items(data.regions) { r ->
                FilterChip(selected = region == r, onClick = { region = r }, label = { Text(com.i3developer.countrypop.data.shortRegion(r)) })
            }
        }
        if (list.isEmpty()) {
            Text("No countries match.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.iso3 }) { c ->
                val sub = when (sort) {
                    SortKey.Area -> formatArea(c.areaKm2)
                    SortKey.Density -> formatDensity(c.density)
                    else -> c.regionLabel
                }
                CountryRow(c, onClick = { onOpen(c.iso3) }, subtitle = sub)
            }
        }
    }
}

// ---------------------------------------------------------------- detail

@Composable
fun DetailScreen(vm: AppViewModel, iso3: String, onBack: () -> Unit) {
    val data = vm.dataset
    val country = data.find(iso3)
    var range by rememberSaveable { mutableIntStateOf(1) } // 0 = 10 years, 1 = 30 years, 2 = all

    Column(Modifier.fillMaxSize()) {
        AppBar(country?.name ?: "Country", onBack = onBack, actions = {
            if (country != null) {
                val fav = country.iso3 in vm.favorites
                IconButton(onClick = { vm.toggleFavorite(country.iso3) }) {
                    Icon(
                        if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (fav) "Remove from favourites" else "Add to favourites",
                    )
                }
            }
        })
        if (country == null) {
            Text("This country is not in the dataset.", Modifier.padding(16.dp))
            return@Column
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("${country.flag}  ${country.name}", style = MaterialTheme.typography.titleLarge)
            Text(country.regionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(
                country.latest?.let { formatFull(it) } ?: "No data",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Total population, ${country.latestYear ?: "—"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            LiveEstimate(country)

            val all = country.series()
            val last = country.latestYear ?: data.lastYear
            val years = intArrayOf(10, 30, Int.MAX_VALUE)[range]
            val points = if (years == Int.MAX_VALUE) all else all.filter { it.first >= last - years }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10 years", "30 years", "All").forEachIndexed { i, label ->
                    FilterChip(selected = range == i, onClick = { range = i }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            PopulationChart(listOf(ChartSeries(country.name, MaterialTheme.colorScheme.primary, points)))
            Spacer(Modifier.height(16.dp))

            val growthText = country.growth?.let { g ->
                val y = country.growthYears
                formatGrowth(g) + if (y != null) "  (${y.first}→${y.second})" else ""
            } ?: "—"
            StatRow("Annual growth", growthText)
            StatRow("Average growth, 3 years", formatGrowth(country.avgGrowth3))
            StatRow("Land area", formatArea(country.areaKm2) + (country.areaYear?.let { " ($it)" } ?: ""))
            StatRow("Density", formatDensity(country.density))
            StatRow("Capital", country.capital.ifBlank { "—" })
            StatRow("Income group", country.income.ifBlank { "—" })
        }
    }
}

/** A ticking estimate of the population right now. Clearly labelled as an estimate, not an official count. */
@Composable
private fun LiveEstimate(country: Country) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(country.iso3) {
        while (true) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val estimate = country.estimateAt(now) ?: return
    val perDay = country.estimatedChangePerDay(now)
    val years = country.growthYears
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Estimated right now",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                formatFull(estimate.roundToLong()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            if (perDay != null) {
                val sign = if (perDay >= 0) "+" else "−"
                Text(
                    "Net change: about $sign${formatFull(abs(perDay).roundToLong())} people per day",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "An estimate worked out on your phone from the ${country.latestYear} figure and the net " +
                    "${formatGrowth(country.growth)} yearly growth (births minus deaths, plus migration)" +
                    (if (years != null) " between ${years.first} and ${years.second}" else "") +
                    ". It is not an official count. See About and sources.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun StatRow(label: String, value: String) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Medium)
        }
        HorizontalDivider()
    }
}

// ---------------------------------------------------------------- rankings

private enum class Ranking(val label: String) {
    Largest("Largest"), Smallest("Smallest"), Densest("Densest"), Growing("Fastest growing")
}

@Composable
fun RankingsScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val data = vm.dataset
    var ranking by rememberSaveable { mutableStateOf(Ranking.Largest) }
    val list = remember(data, ranking) {
        val l = data.countries
        when (ranking) {
            Ranking.Largest -> l.filter { it.latest != null }.sortedByDescending { it.latest }
            Ranking.Smallest -> l.filter { it.latest != null }.sortedBy { it.latest }
            Ranking.Densest -> l.filter { it.density != null }.sortedByDescending { it.density }
            Ranking.Growing -> l.filter { it.avgGrowth3 != null }.sortedByDescending { it.avgGrowth3 }
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppBar("Rankings")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Ranking.entries) { r ->
                FilterChip(selected = ranking == r, onClick = { ranking = r }, label = { Text(r.label) })
            }
        }
        Text(
            when (ranking) {
                Ranking.Largest -> "All countries, largest to smallest by total population"
                Ranking.Smallest -> "All countries, smallest to largest by total population"
                Ranking.Densest -> "All countries by people per km² of land area, highest first"
                Ranking.Growing -> "All countries by average yearly growth over the last 3 years (${data.lastYear - 3}–${data.lastYear}), highest first"
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(list.size) { i ->
                val c = list[i]
                val value = when (ranking) {
                    Ranking.Largest, Ranking.Smallest -> formatCompact((c.latest ?: 0L).toDouble())
                    Ranking.Densest -> formatDensity(c.density)
                    Ranking.Growing -> formatGrowth(c.avgGrowth3)
                }
                CountryRow(c, onClick = { onOpen(c.iso3) }, trailing = value, subtitle = "${i + 1}.  ${c.regionLabel}")
            }
        }
    }
}

// ---------------------------------------------------------------- compare

private val compareColors = listOf(Color(0xFF1E88E5), Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFB8C00))

@Composable
fun CompareScreen(vm: AppViewModel) {
    val data = vm.dataset
    val selected = vm.compare.mapNotNull { data.find(it) }
    var picking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppBar("Compare")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { picking = true }, enabled = selected.size < AppViewModel.MAX_COMPARE) {
                    Text("Add country")
                }
                Text("${selected.size} of ${AppViewModel.MAX_COMPARE}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Pick up to four countries to compare their population over time side by side.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selected.size) { i ->
                        val c = selected[i]
                        InputChip(
                            selected = true,
                            onClick = { vm.removeCompare(c.iso3) },
                            label = { Text("${c.flag} ${c.name}") },
                            trailingIcon = { Text("✕") },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                PopulationChart(
                    selected.mapIndexed { i, c -> ChartSeries(c.name, compareColors[i % compareColors.size], c.series()) },
                )
                Spacer(Modifier.height(16.dp))
                selected.forEachIndexed { i, c ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("●", color = compareColors[i % compareColors.size])
                            Spacer(Modifier.width(8.dp))
                            Text("${c.flag} ${c.name}", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Population ${c.latest?.let { formatFull(it) } ?: "—"} (${c.latestYear ?: "—"})",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Density ${formatDensity(c.density)}  ·  Growth ${formatGrowth(c.growth)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (picking) {
        CountryPicker(
            options = data.countries.filter { it.iso3 !in vm.compare },
            onPick = { vm.addCompare(it); picking = false },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun CountryPicker(options: List<Country>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(options, query) { options.filter { it.name.contains(query.trim(), ignoreCase = true) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add country") },
        text = {
            Column {
                OutlinedTextField(
                    query, { query = it },
                    singleLine = true,
                    label = { Text("Search countries") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(320.dp)) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No countries match your search.",
                                Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(filtered, key = { it.iso3 }) { c ->
                        ListItem(
                            modifier = Modifier.clickable { onPick(c.iso3) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = { Text(c.flag, style = MaterialTheme.typography.titleLarge) },
                            headlineContent = { Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        },
    )
}

// ---------------------------------------------------------------- favourites

@Composable
fun FavoritesScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    val data = vm.dataset
    val list = data.countries.filter { it.iso3 in vm.favorites }
    Column(Modifier.fillMaxSize()) {
        AppBar("Favourites")
        if (list.isEmpty()) {
            Text(
                "No favourites yet. Open a country and tap the heart to save it here.",
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.iso3 }) { c -> CountryRow(c, onClick = { onOpen(c.iso3) }) }
        }
    }
}

// ---------------------------------------------------------------- about

@Composable
fun AboutScreen(vm: AppViewModel, onBack: () -> Unit) {
    val data = vm.dataset
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize()) {
        AppBar("About and sources", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Section(
                "What this app is",
                "Country Population is a reference tool for looking up, ranking and comparing the total " +
                    "population of countries and territories. The official figures are annual, not live counts. " +
                    "A country page also shows a clearly labelled estimate that updates on screen (see below).",
            )
            Section(
                "Data and dates",
                "Population, total (indicator SP.POP.TOTL) and Land area in sq. km (AG.LND.TOTL.K2) from World Bank " +
                    "Open Data, which compiles them from officially recognised sources such as national statistical " +
                    "offices and United Nations population estimates.\n\n" +
                    "Population figures in this app run up to ${data.lastYear}. World Bank release date: ${data.sourceUpdated}. " +
                    "Land area is the latest year available for each country (shown on its page). " +
                    "Figures are estimates and are revised by the source from time to time.",
            )
            Section(
                "Estimated live counter",
                "The \"Estimated right now\" figure on a country page is not official and not measured. " +
                    "The app takes the latest World Bank figure (a mid-year value, 1 July) and projects it forward " +
                    "using the net growth between the last two years of data, at a constant rate. Net growth already " +
                    "counts deaths and migration as well as births. It is calculated on " +
                    "your device, needs no internet, and sends nothing. Real population changes with births, deaths " +
                    "and migration, so the estimate will differ from the true value and from other sources. " +
                    "It is hidden when growth is missing or extreme (over 10% a year), or when the data is more " +
                    "than five years old.",
            )
            Section(
                "Licence",
                "World Bank data is licensed under Creative Commons Attribution 4.0 (CC BY 4.0). " +
                    "This app is not affiliated with, or endorsed by, the World Bank.",
            )
            TextButton(onClick = { uri.openUri("https://creativecommons.org/licenses/by/4.0/") }) { Text("View licence") }
            Section(
                "Privacy",
                "No account, no ads and no analytics. The app collects no personal data and keeps your favourites " +
                    "only on this device. The optional Refresh button downloads the latest figures from api.worldbank.org; " +
                    "like any web request this reveals your IP address to that server.",
            )
            if (AppViewModel.PRIVACY_URL.isNotBlank()) {
                TextButton(onClick = { uri.openUri(AppViewModel.PRIVACY_URL) }) { Text("Privacy policy") }
            }
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
