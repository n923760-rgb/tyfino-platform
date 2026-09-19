package dev.tyfino.foundation.playback

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Stores identities and start time only; never titles, artwork, stream URLs, or credentials. */
internal class SQLiteEpisodeHistoryStore(context: Context) : EpisodeHistoryStore {
    private val helper = Database(context.applicationContext)

    override fun list(accountId: String, limit: Int): List<EpisodeHistoryRecord> = synchronized(helper) {
        require(limit in 1..MAX_HISTORY)
        helper.readableDatabase.query(
            TABLE, COLUMNS, "$ACCOUNT_ID = ?", arrayOf(accountId), null, null,
            "$STARTED_AT DESC, $SERIES_ID ASC, $EPISODE_ID ASC", limit.toString(),
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.record()) } }
    }

    override fun upsert(record: EpisodeHistoryRecord) = synchronized(helper) {
        require(record.accountId.bounded(128) && record.providerSeriesId.bounded(256) &&
            record.providerEpisodeId.bounded(256) && record.startedAtEpochMillis > 0L)
        helper.writableDatabase.inTransaction {
            insertWithOnConflict(
                TABLE, null,
                ContentValues().apply {
                    put(ACCOUNT_ID, record.accountId)
                    put(SERIES_ID, record.providerSeriesId)
                    put(EPISODE_ID, record.providerEpisodeId)
                    put(STARTED_AT, record.startedAtEpochMillis)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            ).also { require(it != -1L) }
            execSQL(
                """DELETE FROM $TABLE WHERE $ACCOUNT_ID = ? AND rowid NOT IN (
                    SELECT rowid FROM $TABLE WHERE $ACCOUNT_ID = ?
                    ORDER BY $STARTED_AT DESC, $SERIES_ID ASC, $EPISODE_ID ASC LIMIT $MAX_HISTORY
                )""".trimIndent(),
                arrayOf(record.accountId, record.accountId),
            )
        }
    }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.delete(TABLE, "$ACCOUNT_ID = ?", arrayOf(accountId))
        Unit
    }

    private fun Cursor.record() = EpisodeHistoryRecord(
        getString(getColumnIndexOrThrow(ACCOUNT_ID)),
        getString(getColumnIndexOrThrow(SERIES_ID)),
        getString(getColumnIndexOrThrow(EPISODE_ID)),
        getLong(getColumnIndexOrThrow(STARTED_AT)),
    )

    private fun String.bounded(limit: Int) = isNotBlank() && codePointCount(0, length) <= limit

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try { block().also { setTransactionSuccessful() } } finally { endTransaction() }
    }

    private class Database(context: Context) : SQLiteOpenHelper(context, "tyfino_episode_history_v1.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE $TABLE (
                    $ACCOUNT_ID TEXT NOT NULL, $SERIES_ID TEXT NOT NULL, $EPISODE_ID TEXT NOT NULL,
                    $STARTED_AT INTEGER NOT NULL CHECK ($STARTED_AT > 0),
                    PRIMARY KEY ($ACCOUNT_ID, $SERIES_ID, $EPISODE_ID))""".trimIndent(),
            )
            db.execSQL("CREATE INDEX episode_history_recent ON $TABLE ($ACCOUNT_ID, $STARTED_AT DESC)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
            error("Missing forward-only episode history migration: $oldVersion to $newVersion")
        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) =
            error("Episode history database downgrade is unsupported")
    }

    private companion object {
        const val TABLE = "episode_history"; const val ACCOUNT_ID = "account_id"
        const val SERIES_ID = "series_id"; const val EPISODE_ID = "episode_id"
        const val STARTED_AT = "started_at_ms"; const val MAX_HISTORY = 100
        val COLUMNS = arrayOf(ACCOUNT_ID, SERIES_ID, EPISODE_ID, STARTED_AT)
    }
}
