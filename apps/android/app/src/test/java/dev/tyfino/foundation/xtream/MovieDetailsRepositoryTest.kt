package dev.tyfino.foundation.xtream

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieDetailsRepositoryTest {
    @Test
    fun freshCacheAvoidsNetworkAndPublishesOnlyOwnedMovie() = runBlocking {
        val accountStore = FakeAccountStore(account("a", 1))
        val store = FakeStore().apply { values["a" to "42"] = snapshot("a", "42", "Cached", 9_500) }
        val api = FakeApi("Network")
        val repository = MovieDetailsRepository(accountStore, api, store, FixedClock(10_000))
        val destination = requireNotNull(repository.open("42"))
        val states = mutableListOf<MovieDetailsState>()

        repository.details(destination, publish = states::add)

        assertEquals(0, api.calls)
        assertEquals("Cached", (states.single() as MovieDetailsState.Content).details.name)
    }

    @Test
    fun accountChangeSuppressesLatePublicationAndCacheCommit() = runBlocking {
        val accountStore = FakeAccountStore(account("a", 1))
        val store = FakeStore()
        val api = object : XtreamMovieDetailsApi {
            override suspend fun details(account: SavedXtreamAccount, movieId: String): MovieDetailsResult {
                accountStore.value = this@MovieDetailsRepositoryTest.account("b", 1)
                return MovieDetailsResult.Success(details("a", "42", "Late"))
            }
        }
        val repository = MovieDetailsRepository(accountStore, api, store, FixedClock(10_000))
        val destination = requireNotNull(repository.open("42"))
        val states = mutableListOf<MovieDetailsState>()

        repository.details(destination, publish = states::add)

        assertEquals(listOf(MovieDetailsState.Loading), states)
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun refreshFailureKeepsCachedDetailsAsStaleContent() = runBlocking {
        val accountStore = FakeAccountStore(account("a", 1))
        val store = FakeStore().apply { values["a" to "42"] = snapshot("a", "42", "Cached", 1) }
        val api = object : XtreamMovieDetailsApi {
            override suspend fun details(account: SavedXtreamAccount, movieId: String) =
                MovieDetailsResult.Failure(MovieDetailsFailure.Timeout)
        }
        val repository = MovieDetailsRepository(accountStore, api, store, FixedClock(30_000_000))
        val destination = requireNotNull(repository.open("42"))
        val states = mutableListOf<MovieDetailsState>()

        repository.details(destination, publish = states::add)

        assertTrue(states.first() is MovieDetailsState.Content)
        assertEquals(MovieDetailsFailure.Timeout, (states.last() as MovieDetailsState.StaleContent).failure)
    }

    private fun account(id: String, generation: Long) = SavedXtreamAccount(
        id, generation, ProviderEndpoint("https://provider.example", false), "user", "password", false,
    )

    private fun details(accountId: String, movieId: String, name: String) = MovieDetails(
        accountId, movieId, name, null, null, null, null, null, null, null, null, null,
    )

    private fun snapshot(accountId: String, movieId: String, name: String, refreshedAt: Long) =
        MovieDetailsSnapshot(1, refreshedAt, details(accountId, movieId, name))

    private class FakeAccountStore(var value: SavedXtreamAccount?) : XtreamAccountStore {
        override fun load() = value
        override fun save(account: SavedXtreamAccount) { value = account }
        override fun clear() { value = null }
    }

    private class FakeStore : MovieDetailsStore {
        val values = mutableMapOf<Pair<String, String>, MovieDetailsSnapshot>()
        override fun load(accountId: String, movieId: String) = values[accountId to movieId]
        override fun replace(accountId: String, movieId: String, snapshot: MovieDetailsSnapshot) {
            values[accountId to movieId] = snapshot
        }
        override fun clearAccount(accountId: String) { values.keys.removeAll { it.first == accountId } }
    }

    private class FakeApi(private val name: String) : XtreamMovieDetailsApi {
        var calls = 0
        override suspend fun details(account: SavedXtreamAccount, movieId: String): MovieDetailsResult {
            calls++
            return MovieDetailsResult.Success(
                MovieDetails(account.accountId, movieId, name, null, null, null, null, null, null, null, null, null),
            )
        }
    }

    private class FixedClock(private val wall: Long) : CatalogClock {
        override fun wallTimeMillis() = wall
        override fun elapsedTimeMillis() = wall
    }
}
