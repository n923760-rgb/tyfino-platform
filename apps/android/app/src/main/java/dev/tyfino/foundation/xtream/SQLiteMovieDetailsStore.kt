package dev.tyfino.foundation.xtream

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal data class MovieDetailsSnapshot(
    val generation: Long,
    val refreshedAtEpochMillis: Long,
    val details: MovieDetails,
)

internal interface MovieDetailsStore {
    fun load(accountId: String, movieId: String): MovieDetailsSnapshot?
    fun replace(accountId: String, movieId: String, snapshot: MovieDetailsSnapshot)
    fun clearAccount(accountId: String)
}

internal class SQLiteMovieDetailsStore(context: Context) : MovieDetailsStore {
    private val helper = Database(context.applicationContext)

    override fun load(accountId: String, movieId: String): MovieDetailsSnapshot? = synchronized(helper) {
        helper.readableDatabase.query(
            TABLE, COLUMNS, "$ACCOUNT_ID = ? AND $MOVIE_ID = ?", arrayOf(accountId, movieId),
            null, null, null, "1",
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            fun text(name: String): String? {
                val index = cursor.getColumnIndexOrThrow(name)
                return if (cursor.isNull(index)) null else cursor.getString(index)
            }
            MovieDetailsSnapshot(
                cursor.getLong(cursor.getColumnIndexOrThrow(GENERATION)),
                cursor.getLong(cursor.getColumnIndexOrThrow(REFRESHED_AT)),
                MovieDetails(
                    accountId, movieId, text(NAME), text(PLOT), text(GENRE), text(RELEASE_DATE),
                    text(RATING), text(DURATION), text(CAST), text(DIRECTOR), text(POSTER), text(BACKDROP),
                ),
            )
        }
    }

    override fun replace(accountId: String, movieId: String, snapshot: MovieDetailsSnapshot) = synchronized(helper) {
        require(snapshot.details.accountId == accountId && snapshot.details.providerMovieId == movieId)
        helper.writableDatabase.insertWithOnConflict(
            TABLE, null, ContentValues().apply {
                put(ACCOUNT_ID, accountId); put(MOVIE_ID, movieId); put(GENERATION, snapshot.generation)
                put(REFRESHED_AT, snapshot.refreshedAtEpochMillis); put(NAME, snapshot.details.name)
                put(PLOT, snapshot.details.plot); put(GENRE, snapshot.details.genre)
                put(RELEASE_DATE, snapshot.details.releaseDate); put(RATING, snapshot.details.rating)
                put(DURATION, snapshot.details.duration); put(CAST, snapshot.details.cast)
                put(DIRECTOR, snapshot.details.director); put(POSTER, snapshot.details.posterUrl)
                put(BACKDROP, snapshot.details.backdropUrl)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        ).also { check(it != -1L) }
    }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.delete(TABLE, "$ACCOUNT_ID = ?", arrayOf(accountId))
        Unit
    }

    private class Database(context: Context) : SQLiteOpenHelper(context, "tyfino_movie_details_v1.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE $TABLE (
                $ACCOUNT_ID TEXT NOT NULL, $MOVIE_ID TEXT NOT NULL, $GENERATION INTEGER NOT NULL,
                $REFRESHED_AT INTEGER NOT NULL, $NAME TEXT, $PLOT TEXT, $GENRE TEXT, $RELEASE_DATE TEXT,
                $RATING TEXT, $DURATION TEXT, $CAST TEXT, $DIRECTOR TEXT, $POSTER TEXT, $BACKDROP TEXT,
                PRIMARY KEY ($ACCOUNT_ID, $MOVIE_ID))""".trimIndent())
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private companion object {
        const val TABLE = "movie_details"; const val ACCOUNT_ID = "account_id"; const val MOVIE_ID = "movie_id"
        const val GENERATION = "generation"; const val REFRESHED_AT = "refreshed_at"; const val NAME = "name"
        const val PLOT = "plot"; const val GENRE = "genre"; const val RELEASE_DATE = "release_date"
        const val RATING = "rating"; const val DURATION = "duration"; const val CAST = "cast_name"
        const val DIRECTOR = "director"; const val POSTER = "poster"; const val BACKDROP = "backdrop"
        val COLUMNS = arrayOf(GENERATION, REFRESHED_AT, NAME, PLOT, GENRE, RELEASE_DATE, RATING, DURATION, CAST, DIRECTOR, POSTER, BACKDROP)
    }
}
