package dev.tyfino.foundation.ui.screen

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.tyfino.foundation.R
import dev.tyfino.foundation.playback.CatalogHistoryRecord
import dev.tyfino.foundation.playback.CatalogHistoryRepository
import dev.tyfino.foundation.playback.MovieResumeRepository
import dev.tyfino.foundation.playback.SQLiteCatalogHistoryStore
import dev.tyfino.foundation.playback.SQLiteMovieResumeStore
import dev.tyfino.foundation.ui.theme.TyfinoTheme
import dev.tyfino.foundation.xtream.CatalogCategory
import dev.tyfino.foundation.xtream.CatalogClock
import dev.tyfino.foundation.xtream.CatalogFavoritesRepository
import dev.tyfino.foundation.xtream.CatalogItem
import dev.tyfino.foundation.xtream.CatalogRepository
import dev.tyfino.foundation.xtream.CatalogSection
import dev.tyfino.foundation.xtream.CatalogSnapshot
import dev.tyfino.foundation.xtream.FavoriteRecord
import dev.tyfino.foundation.xtream.ProviderEndpoint
import dev.tyfino.foundation.xtream.SavedXtreamAccount
import dev.tyfino.foundation.xtream.SQLiteCatalogStore
import dev.tyfino.foundation.xtream.SQLiteFavoriteStore
import dev.tyfino.foundation.xtream.XtreamAccountStore
import dev.tyfino.foundation.xtream.XtreamCatalogApi
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogFilteredSearchTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changingFiltersAndRemovingAFavoriteRequeriesTheUnchangedEnglishSearch() {
        exerciseFilters(arabic = false)
    }

    @Test fun changingFiltersAndRemovingAFavoriteRequeriesTheUnchangedArabicSearch() {
        exerciseFilters(arabic = true)
    }

    private fun exerciseFilters(arabic: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(if (arabic) "ar" else "en"))
        }).resources
        val accountId = "filtered-ui-${UUID.randomUUID()}"
        val accounts = object : XtreamAccountStore {
            private var account: SavedXtreamAccount? = SavedXtreamAccount(
                accountId, 1L, ProviderEndpoint("https://fixture.invalid", false), "fixture", "fixture", false,
            )
            override fun load() = account
            override fun save(account: SavedXtreamAccount) { this.account = account }
            override fun clear() { account = null }
        }
        val catalog = SQLiteCatalogStore(context)
        val favorites = SQLiteFavoriteStore(context)
        val history = SQLiteCatalogHistoryStore(context)
        val resumes = SQLiteMovieResumeStore(context)
        val prefix = if (arabic) "فيلم مطابق" else "Match"
        fun title(index: Int) = "$prefix ${index.toString().padStart(3, '0')}"
        val providerRequests = AtomicInteger()
        val api = object : XtreamCatalogApi {
            override suspend fun categories(account: SavedXtreamAccount, section: CatalogSection): Nothing {
                providerRequests.incrementAndGet()
                error("Cached search must not request provider categories")
            }
            override suspend fun items(account: SavedXtreamAccount, section: CatalogSection, categoryId: String): Nothing {
                providerRequests.incrementAndGet()
                error("Cached search must not request provider items")
            }
        }
        val clock = object : CatalogClock {
            override fun wallTimeMillis() = 1_000L
            override fun elapsedTimeMillis() = 1_000L
        }
        val repository = CatalogRepository(accounts, api, catalog, clock)
        val visible = mutableStateOf(true)
        val resumeRepository = MovieResumeRepository(accounts, resumes)
        val favoritesRepository = CatalogFavoritesRepository(accounts, repository, favorites)
        val historyRepository = CatalogHistoryRepository(accounts, repository, history)
        try {
            catalog.replaceCategories(accountId, CatalogSection.Movies, CatalogSnapshot(
                1L, 1_000L, listOf(CatalogCategory("category", "Category", 0)),
            ))
            catalog.replaceItems(accountId, CatalogSection.Movies, "category", CatalogSnapshot(
                1L, 1_000L, (0..119).map { index ->
                    CatalogItem("item-$index", "category", title(index), index, null, null, null, "mp4")
                },
            ))
            favorites.upsert(FavoriteRecord(accountId, CatalogSection.Movies, "item-90", 1_000L))
            history.upsert(CatalogHistoryRecord(accountId, CatalogSection.Movies, "item-91", 1_000L))
            compose.setContent {
                CompositionLocalProvider(
                    LocalResources provides resources,
                    LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    TyfinoTheme {
                        if (visible.value) CatalogScreen(
                            section = CatalogSection.Movies,
                            repository = repository,
                            resumeRepository = resumeRepository,
                            onPlay = {},
                            favoritesRepository = favoritesRepository,
                            historyRepository = historyRepository,
                        )
                    }
                }
            }
            compose.onNodeWithTag("catalog-search").performScrollTo()
                .performTextReplacement(if (arabic) "مطابق" else "Match")
            awaitText(title(0))
            compose.onNodeWithTag("catalog-favorites-filter").performScrollTo().performClick()
            awaitText(title(90))
            compose.onNodeWithText(title(0), useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithTag("catalog-history-filter").performScrollTo().performClick()
            awaitText(title(91))
            compose.onNodeWithText(title(90), useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithTag("catalog-all-filter").performScrollTo().performClick()
            awaitText(title(0))
            compose.onNodeWithTag("catalog-favorites-filter").performScrollTo().performClick()
            awaitText(title(90))
            compose.onNodeWithTag("catalog-favorite-toggle").performScrollTo().performClick()
            awaitText(resources.getString(R.string.catalog_search_empty))
            compose.onNodeWithText(title(90), useUnmergedTree = true).assertDoesNotExist()
            assertEquals(0, providerRequests.get())
        } finally {
            // Dispose the real screen before clearing its isolated synthetic account partition.
            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()
            catalog.clearAccount(accountId)
            favorites.clearAccount(accountId)
            history.clearAccount(accountId)
            resumes.clearAccount(accountId)
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(8_000) {
            compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
    }
}
