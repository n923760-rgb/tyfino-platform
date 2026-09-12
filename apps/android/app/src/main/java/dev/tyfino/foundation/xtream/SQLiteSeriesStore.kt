package dev.tyfino.foundation.xtream

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SQLiteSeriesStore(context: Context) : SeriesStore {
    private val helper = SeriesDatabase(context.applicationContext)

    override fun load(accountId: String, seriesId: String): SeriesSnapshot? = synchronized(helper) {
        val database = helper.readableDatabase
        val metadata = database.query(
            SNAPSHOT_TABLE,
            SNAPSHOT_COLUMNS,
            "$ACCOUNT_ID = ? AND $SERIES_ID = ?",
            arrayOf(accountId, seriesId),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            SnapshotMetadata(
                generation = cursor.long(GENERATION),
                refreshedAtMillis = cursor.long(REFRESHED_AT),
                skippedEntries = cursor.int(SKIPPED_ENTRIES),
                seasonMismatchCount = cursor.int(SEASON_MISMATCH_COUNT),
                summary = SeriesSummary(
                    name = cursor.nullableText(DISPLAY_NAME),
                    plot = cursor.nullableText(PLOT),
                    genre = cursor.nullableText(GENRE),
                    releaseDate = cursor.nullableText(RELEASE_DATE),
                    rating = cursor.nullableText(RATING),
                    cast = cursor.nullableText(CAST),
                    director = cursor.nullableText(DIRECTOR),
                    coverUrl = cursor.nullableText(COVER_URL),
                    backdropUrl = cursor.nullableText(BACKDROP_URL),
                ),
            )
        } ?: return@synchronized null
        val generationSelection = "$ACCOUNT_ID = ? AND $SERIES_ID = ? AND $GENERATION = ?"
        val generationArguments = arrayOf(accountId, seriesId, metadata.generation.toString())
        val seasons = database.query(
            SEASON_TABLE,
            SEASON_COLUMNS,
            generationSelection,
            generationArguments,
            null,
            null,
            "$SEASON_NUMBER ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SeriesSeason(
                            accountId = accountId,
                            providerSeriesId = seriesId,
                            seasonNumber = cursor.int(SEASON_NUMBER),
                            displayLabel = cursor.text(DISPLAY_LABEL),
                            overview = cursor.nullableText(OVERVIEW),
                            airDate = cursor.nullableText(AIR_DATE),
                            coverUrl = cursor.nullableText(COVER_URL),
                            providerOrder = cursor.int(PROVIDER_ORDER),
                            episodeCount = cursor.int(EPISODE_COUNT),
                        ),
                    )
                }
            }
        }
        val episodes = database.query(
            EPISODE_TABLE,
            EPISODE_COLUMNS,
            generationSelection,
            generationArguments,
            null,
            null,
            "$SEASON_NUMBER ASC, CASE WHEN $EPISODE_NUMBER IS NULL THEN 1 ELSE 0 END, " +
                "$EPISODE_NUMBER ASC, $PROVIDER_ORDER ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SeriesEpisode(
                            accountId = accountId,
                            providerSeriesId = seriesId,
                            providerEpisodeId = cursor.text(EPISODE_ID),
                            seasonNumber = cursor.int(SEASON_NUMBER),
                            episodeNumber = cursor.nullableInt(EPISODE_NUMBER),
                            title = cursor.nullableText(DISPLAY_NAME),
                            containerExtension = cursor.nullableText(CONTAINER_EXTENSION),
                            providerOrder = cursor.int(PROVIDER_ORDER),
                            plot = cursor.nullableText(PLOT),
                            duration = cursor.nullableText(DURATION),
                            releaseDate = cursor.nullableText(RELEASE_DATE),
                            rating = cursor.nullableText(RATING),
                        ),
                    )
                }
            }
        }
        SeriesSnapshot(
            generation = metadata.generation,
            refreshedAtEpochMillis = metadata.refreshedAtMillis,
            details = SeriesDetailsCandidate(
                accountId = accountId,
                providerSeriesId = seriesId,
                summary = metadata.summary,
                seasons = seasons,
                episodes = episodes,
                skippedEntries = metadata.skippedEntries,
                seasonMismatchCount = metadata.seasonMismatchCount,
            ),
        )
    }

    override fun replace(
        accountId: String,
        seriesId: String,
        snapshot: SeriesSnapshot,
    ) = synchronized(helper) {
        require(snapshot.details.accountId == accountId)
        require(snapshot.details.providerSeriesId == seriesId)
        require(snapshot.details.seasons.all { it.accountId == accountId && it.providerSeriesId == seriesId })
        require(snapshot.details.episodes.all { it.accountId == accountId && it.providerSeriesId == seriesId })
        helper.writableDatabase.inTransaction {
            arrayOf(SEASON_TABLE, EPISODE_TABLE, SNAPSHOT_TABLE).forEach { table ->
                delete(table, "$ACCOUNT_ID = ? AND $SERIES_ID = ?", arrayOf(accountId, seriesId))
            }
            val summary = snapshot.details.summary
            insertOrThrow(
                SNAPSHOT_TABLE,
                null,
                ContentValues().apply {
                    put(ACCOUNT_ID, accountId)
                    put(SERIES_ID, seriesId)
                    put(GENERATION, snapshot.generation)
                    put(REFRESHED_AT, snapshot.refreshedAtEpochMillis)
                    put(SKIPPED_ENTRIES, snapshot.details.skippedEntries)
                    put(SEASON_MISMATCH_COUNT, snapshot.details.seasonMismatchCount)
                    put(DISPLAY_NAME, summary.name)
                    put(PLOT, summary.plot)
                    put(GENRE, summary.genre)
                    put(RELEASE_DATE, summary.releaseDate)
                    put(RATING, summary.rating)
                    put(CAST, summary.cast)
                    put(DIRECTOR, summary.director)
                    put(COVER_URL, summary.coverUrl)
                    put(BACKDROP_URL, summary.backdropUrl)
                },
            )
            snapshot.details.seasons.forEach { season ->
                insertOrThrow(
                    SEASON_TABLE,
                    null,
                    ContentValues().apply {
                        put(ACCOUNT_ID, accountId)
                        put(SERIES_ID, seriesId)
                        put(SEASON_NUMBER, season.seasonNumber)
                        put(DISPLAY_LABEL, season.displayLabel)
                        put(OVERVIEW, season.overview)
                        put(AIR_DATE, season.airDate)
                        put(COVER_URL, season.coverUrl)
                        put(PROVIDER_ORDER, season.providerOrder)
                        put(EPISODE_COUNT, season.episodeCount)
                        put(GENERATION, snapshot.generation)
                    },
                )
            }
            snapshot.details.episodes.forEach { episode ->
                insertOrThrow(
                    EPISODE_TABLE,
                    null,
                    ContentValues().apply {
                        put(ACCOUNT_ID, accountId)
                        put(SERIES_ID, seriesId)
                        put(EPISODE_ID, episode.providerEpisodeId)
                        put(SEASON_NUMBER, episode.seasonNumber)
                        put(EPISODE_NUMBER, episode.episodeNumber)
                        put(DISPLAY_NAME, episode.title)
                        put(CONTAINER_EXTENSION, episode.containerExtension)
                        put(PROVIDER_ORDER, episode.providerOrder)
                        put(PLOT, episode.plot)
                        put(DURATION, episode.duration)
                        put(RELEASE_DATE, episode.releaseDate)
                        put(RATING, episode.rating)
                        put(GENERATION, snapshot.generation)
                    },
                )
            }
        }
    }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            arrayOf(SEASON_TABLE, EPISODE_TABLE, SNAPSHOT_TABLE).forEach { table ->
                delete(table, "$ACCOUNT_ID = ?", arrayOf(accountId))
            }
        }
    }

    override fun clearOtherAccounts(accountId: String) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            arrayOf(SEASON_TABLE, EPISODE_TABLE, SNAPSHOT_TABLE).forEach { table ->
                delete(table, "$ACCOUNT_ID != ?", arrayOf(accountId))
            }
        }
    }

    private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column))
    private fun Cursor.nullableText(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }
    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.nullableInt(column: String): Int? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getInt(index)
    }
    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try {
            block().also { setTransactionSuccessful() }
        } finally {
            endTransaction()
        }
    }

    private data class SnapshotMetadata(
        val generation: Long,
        val refreshedAtMillis: Long,
        val skippedEntries: Int,
        val seasonMismatchCount: Int,
        val summary: SeriesSummary,
    )

    private class SeriesDatabase(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION,
    ) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE $SNAPSHOT_TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SERIES_ID TEXT NOT NULL,
                    $GENERATION INTEGER NOT NULL,
                    $REFRESHED_AT INTEGER NOT NULL,
                    $SKIPPED_ENTRIES INTEGER NOT NULL,
                    $SEASON_MISMATCH_COUNT INTEGER NOT NULL,
                    $DISPLAY_NAME TEXT,
                    $PLOT TEXT,
                    $GENRE TEXT,
                    $RELEASE_DATE TEXT,
                    $RATING TEXT,
                    $CAST TEXT,
                    $DIRECTOR TEXT,
                    $COVER_URL TEXT,
                    $BACKDROP_URL TEXT,
                    PRIMARY KEY ($ACCOUNT_ID, $SERIES_ID)
                )""".trimIndent(),
            )
            database.execSQL(
                """CREATE TABLE $SEASON_TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SERIES_ID TEXT NOT NULL,
                    $SEASON_NUMBER INTEGER NOT NULL,
                    $DISPLAY_LABEL TEXT NOT NULL,
                    $OVERVIEW TEXT,
                    $AIR_DATE TEXT,
                    $COVER_URL TEXT,
                    $PROVIDER_ORDER INTEGER NOT NULL,
                    $EPISODE_COUNT INTEGER NOT NULL,
                    $GENERATION INTEGER NOT NULL,
                    PRIMARY KEY ($ACCOUNT_ID, $SERIES_ID, $SEASON_NUMBER)
                )""".trimIndent(),
            )
            database.execSQL(
                """CREATE TABLE $EPISODE_TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SERIES_ID TEXT NOT NULL,
                    $EPISODE_ID TEXT NOT NULL,
                    $SEASON_NUMBER INTEGER NOT NULL,
                    $EPISODE_NUMBER INTEGER,
                    $DISPLAY_NAME TEXT,
                    $CONTAINER_EXTENSION TEXT,
                    $PROVIDER_ORDER INTEGER NOT NULL,
                    $PLOT TEXT,
                    $DURATION TEXT,
                    $RELEASE_DATE TEXT,
                    $RATING TEXT,
                    $GENERATION INTEGER NOT NULL,
                    PRIMARY KEY ($ACCOUNT_ID, $SERIES_ID, $EPISODE_ID)
                )""".trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX series_episode_order ON $EPISODE_TABLE " +
                    "($ACCOUNT_ID, $SERIES_ID, $GENERATION, $SEASON_NUMBER, $EPISODE_NUMBER, $PROVIDER_ORDER)",
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            check(oldVersion < newVersion)
            database.execSQL("DROP TABLE IF EXISTS $SEASON_TABLE")
            database.execSQL("DROP TABLE IF EXISTS $EPISODE_TABLE")
            database.execSQL("DROP TABLE IF EXISTS $SNAPSHOT_TABLE")
            onCreate(database)
        }
    }

    private companion object {
        const val DATABASE_NAME = "tyfino_series_v1.db"
        const val DATABASE_VERSION = 1
        const val SNAPSHOT_TABLE = "series_snapshots"
        const val SEASON_TABLE = "series_seasons"
        const val EPISODE_TABLE = "series_episodes"
        const val ACCOUNT_ID = "account_id"
        const val SERIES_ID = "series_id"
        const val EPISODE_ID = "episode_id"
        const val GENERATION = "generation"
        const val REFRESHED_AT = "refreshed_at"
        const val SKIPPED_ENTRIES = "skipped_entries"
        const val SEASON_MISMATCH_COUNT = "season_mismatch_count"
        const val DISPLAY_NAME = "display_name"
        const val DISPLAY_LABEL = "display_label"
        const val PLOT = "plot"
        const val OVERVIEW = "overview"
        const val GENRE = "genre"
        const val RELEASE_DATE = "release_date"
        const val AIR_DATE = "air_date"
        const val RATING = "rating"
        const val CAST = "cast_members"
        const val DIRECTOR = "director"
        const val COVER_URL = "cover_url"
        const val BACKDROP_URL = "backdrop_url"
        const val SEASON_NUMBER = "season_number"
        const val EPISODE_NUMBER = "episode_number"
        const val CONTAINER_EXTENSION = "container_extension"
        const val PROVIDER_ORDER = "provider_order"
        const val EPISODE_COUNT = "episode_count"
        const val DURATION = "duration"

        val SNAPSHOT_COLUMNS = arrayOf(
            GENERATION,
            REFRESHED_AT,
            SKIPPED_ENTRIES,
            SEASON_MISMATCH_COUNT,
            DISPLAY_NAME,
            PLOT,
            GENRE,
            RELEASE_DATE,
            RATING,
            CAST,
            DIRECTOR,
            COVER_URL,
            BACKDROP_URL,
        )
        val SEASON_COLUMNS = arrayOf(
            SEASON_NUMBER,
            DISPLAY_LABEL,
            OVERVIEW,
            AIR_DATE,
            COVER_URL,
            PROVIDER_ORDER,
            EPISODE_COUNT,
        )
        val EPISODE_COLUMNS = arrayOf(
            EPISODE_ID,
            SEASON_NUMBER,
            EPISODE_NUMBER,
            DISPLAY_NAME,
            CONTAINER_EXTENSION,
            PROVIDER_ORDER,
            PLOT,
            DURATION,
            RELEASE_DATE,
            RATING,
        )
    }
}
