package com.i3developer.countrypop

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.i3developer.countrypop.data.Dataset
import com.i3developer.countrypop.data.FavoritesStore
import com.i3developer.countrypop.data.PopulationRepository
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PopulationRepository(app)
    private val favoritesStore = FavoritesStore(app)

    var dataset: Dataset by mutableStateOf(repo.load())
        private set
    var favorites: Set<String> by mutableStateOf(favoritesStore.load())
        private set
    var refreshing by mutableStateOf(false)
        private set
    var message: String? by mutableStateOf(null)

    /** ISO3 codes selected on the Compare screen (max [MAX_COMPARE]). */
    var compare: List<String> by mutableStateOf(emptyList())
        private set

    fun toggleFavorite(iso3: String) {
        favorites = if (iso3 in favorites) favorites - iso3 else favorites + iso3
        favoritesStore.save(favorites)
    }

    fun addCompare(iso3: String) {
        if (iso3 !in compare && compare.size < MAX_COMPARE) compare = compare + iso3
    }

    fun removeCompare(iso3: String) {
        compare = compare - iso3
    }

    fun refresh() {
        if (refreshing) return
        viewModelScope.launch {
            refreshing = true
            repo.refresh(dataset)
                .onSuccess {
                    val same = repo.sameData(dataset, it)
                    dataset = it
                    message = if (same) {
                        "Already up to date. Latest figures are for ${it.lastYear}."
                    } else {
                        "Updated. Data now runs to ${it.lastYear}."
                    }
                }
                .onFailure { message = "Couldn't reach the data source. Showing saved figures." }
            refreshing = false
        }
    }

    companion object {
        const val MAX_COMPARE = 4

        /** Link to the hosted privacy policy. Leave empty until the page is published. */
        const val PRIVACY_URL = ""
    }
}
