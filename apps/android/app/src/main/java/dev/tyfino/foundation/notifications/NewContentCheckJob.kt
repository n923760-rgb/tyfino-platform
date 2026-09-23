package dev.tyfino.foundation.notifications

import android.app.job.JobParameters
import android.app.job.JobService
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.HttpXtreamCatalogApi
import dev.tyfino.foundation.xtream.HttpXtreamSeriesApi
import dev.tyfino.foundation.xtream.SQLiteCatalogStore
import dev.tyfino.foundation.xtream.SQLiteSeriesStore
import dev.tyfino.foundation.xtream.SecureXtreamAccountStore
import dev.tyfino.foundation.xtream.SeriesDetailsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Bounded Wi-Fi check of previously opened categories/series; no whole-provider scan. */
class NewContentCheckJob : JobService() {
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val work = SupervisorJob()
        running = work
        CoroutineScope(Dispatchers.IO + work).launch {
            try {
                val notifier = NewContentNotifier(applicationContext)
                if (!notifier.isEnabled() || !notifier.canNotify()) return@launch
                val accounts = SecureXtreamAccountStore(applicationContext)
                val owner = accounts.load() ?: return@launch
                val catalogStore = SQLiteCatalogStore(applicationContext)
                val seriesStore = SQLiteSeriesStore(applicationContext)
                val catalog = CatalogRepository(accounts, HttpXtreamCatalogApi(applicationContext), catalogStore,
                    onNewMovies = notifier::movies)
                val series = SeriesDetailsRepository(accounts, HttpXtreamSeriesApi(applicationContext), seriesStore,
                    onNewEpisodes = notifier::episodes)
                for (categoryId in catalogStore.monitoredMovieCategories(owner.accountId, 2)) {
                    if (!work.isActive || !notifier.isEnabled() || !owns(accounts, owner.accountId, owner.generation)) break
                    catalog.items(CatalogSection.Movies, categoryId, forceRefresh = true) { }
                }
                for (seriesId in seriesStore.monitoredSeriesIds(owner.accountId, 2)) {
                    if (!work.isActive || !notifier.isEnabled() || !owns(accounts, owner.accountId, owner.generation)) break
                    val destination = series.open(seriesId) ?: continue
                    try { series.details(destination, forceRefresh = true) { } }
                    finally { series.close(destination) }
                }
            } catch (_: RuntimeException) {
                // A provider or local storage error should never take down the app process.
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        running = null
        return false
    }

    private fun owns(store: SecureXtreamAccountStore, id: String, generation: Long): Boolean =
        store.load()?.let { it.accountId == id && it.generation == generation } == true
}
