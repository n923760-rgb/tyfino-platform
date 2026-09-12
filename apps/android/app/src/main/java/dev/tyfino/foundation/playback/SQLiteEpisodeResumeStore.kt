package dev.tyfino.foundation.playback

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SQLiteEpisodeResumeStore(context: Context) : EpisodeResumeStore {
    private val helper = EpisodeDatabase(context.applicationContext)

    override fun load(accountId: String, seriesId: String, episodeId: String): EpisodeResumeRecord? =
        synchronized(helper) {
            helper.readableDatabase.query(
                TABLE, COLUMNS,
                "$ACCOUNT_ID = ? AND $SERIES_ID = ? AND $EPISODE_ID = ?",
                arrayOf(accountId, seriesId, episodeId), null, null, null, "1",
            ).use { cursor -> if (cursor.moveToFirst()) cursor.record() else null }
        }

    override fun list(accountId: String, limit: Int): List<EpisodeResumeRecord> = synchronized(helper) {
        require(limit in 1..MAX_RECORDS_PER_ACCOUNT)
        helper.readableDatabase.query(
            TABLE, COLUMNS, "$ACCOUNT_ID = ?", arrayOf(accountId),
            null, null, "$UPDATED_AT_MILLIS DESC, $SERIES_ID ASC, $EPISODE_ID ASC", limit.toString(),
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.record()) } }
    }

    override fun upsert(record: EpisodeResumeRecord) = synchronized(helper) {
        validate(record)
        helper.writableDatabase.inTransaction {
            insertWithOnConflict(
                TABLE, null,
                ContentValues().apply {
                    put(ACCOUNT_ID, record.accountId)
                    put(SERIES_ID, record.providerSeriesId)
                    put(EPISODE_ID, record.providerEpisodeId)
                    put(POSITION_MILLIS, record.positionMillis)
                    record.durationMillis?.let { put(DURATION_MILLIS, it) } ?: putNull(DURATION_MILLIS)
                    put(UPDATED_AT_MILLIS, record.updatedAtEpochMillis)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            ).also { require(it != -1L) }
            execSQL(
                """DELETE FROM $TABLE WHERE $ACCOUNT_ID = ? AND rowid NOT IN (
                    SELECT rowid FROM $TABLE WHERE $ACCOUNT_ID = ?
                    ORDER BY $UPDATED_AT_MILLIS DESC, $SERIES_ID ASC, $EPISODE_ID ASC
                    LIMIT $MAX_RECORDS_PER_ACCOUNT
                )""".trimIndent(),
                arrayOf(record.accountId, record.accountId),
            )
        }
    }

    override fun delete(accountId: String, seriesId: String, episodeId: String) = synchronized(helper) {
        helper.writableDatabase.delete(
            TABLE, "$ACCOUNT_ID = ? AND $SERIES_ID = ? AND $EPISODE_ID = ?",
            arrayOf(accountId, seriesId, episodeId),
        )
        Unit
    }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.delete(TABLE, "$ACCOUNT_ID = ?", arrayOf(accountId))
        Unit
    }

    override fun clearOtherAccounts(accountId: String) = synchronized(helper) {
        helper.writableDatabase.delete(TABLE, "$ACCOUNT_ID != ?", arrayOf(accountId))
        Unit
    }

    private fun Cursor.record() = EpisodeResumeRecord(
        accountId = getString(getColumnIndexOrThrow(ACCOUNT_ID)),
        providerSeriesId = getString(getColumnIndexOrThrow(SERIES_ID)),
        providerEpisodeId = getString(getColumnIndexOrThrow(EPISODE_ID)),
        positionMillis = getLong(getColumnIndexOrThrow(POSITION_MILLIS)),
        durationMillis = getColumnIndexOrThrow(DURATION_MILLIS).let { if (isNull(it)) null else getLong(it) },
        updatedAtEpochMillis = getLong(getColumnIndexOrThrow(UPDATED_AT_MILLIS)),
    )

    private fun validate(record: EpisodeResumeRecord) {
        require(record.accountId.isBoundedId(128))
        require(record.providerSeriesId.isBoundedId(256))
        require(record.providerEpisodeId.isBoundedId(256))
        require(MovieResumePolicy.isValidPosition(record.positionMillis, record.durationMillis))
        require(record.updatedAtEpochMillis > 0L)
    }

    private fun String.isBoundedId(limit: Int) = isNotBlank() && codePointCount(0, length) <= limit

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try { block().also { setTransactionSuccessful() } } finally { endTransaction() }
    }

    private class EpisodeDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE $TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SERIES_ID TEXT NOT NULL,
                    $EPISODE_ID TEXT NOT NULL,
                    $POSITION_MILLIS INTEGER NOT NULL CHECK ($POSITION_MILLIS >= 0),
                    $DURATION_MILLIS INTEGER,
                    $UPDATED_AT_MILLIS INTEGER NOT NULL CHECK ($UPDATED_AT_MILLIS > 0),
                    PRIMARY KEY ($ACCOUNT_ID, $SERIES_ID, $EPISODE_ID),
                    CHECK ($DURATION_MILLIS IS NULL OR (
                        $DURATION_MILLIS > 0 AND $POSITION_MILLIS <= $DURATION_MILLIS
                    ))
                )""".trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX episode_resume_recent ON $TABLE ($ACCOUNT_ID, $UPDATED_AT_MILLIS DESC)",
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Missing forward-only episode resume migration: $oldVersion to $newVersion")
        }

        override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Episode resume database downgrade is unsupported")
        }
    }

    private companion object {
        const val DATABASE_NAME = "tyfino_episode_resume_v1.db"
        const val TABLE = "episode_resume"
        const val ACCOUNT_ID = "account_id"
        const val SERIES_ID = "series_id"
        const val EPISODE_ID = "episode_id"
        const val POSITION_MILLIS = "position_ms"
        const val DURATION_MILLIS = "duration_ms"
        const val UPDATED_AT_MILLIS = "updated_at_ms"
        const val MAX_RECORDS_PER_ACCOUNT = 200
        val COLUMNS = arrayOf(ACCOUNT_ID, SERIES_ID, EPISODE_ID, POSITION_MILLIS, DURATION_MILLIS, UPDATED_AT_MILLIS)
    }
}
