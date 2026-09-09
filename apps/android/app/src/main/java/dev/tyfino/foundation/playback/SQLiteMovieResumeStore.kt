package dev.tyfino.foundation.playback

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SQLiteMovieResumeStore(context: Context) : MovieResumeStore {
    private val helper = MovieResumeDatabase(context.applicationContext)

    override fun load(
        accountId: String,
        providerItemId: String,
    ): MovieResumeRecord? = synchronized(helper) {
        helper.readableDatabase.query(
            TABLE,
            COLUMNS,
            "$ACCOUNT_ID = ? AND $SECTION = ? AND $PROVIDER_ITEM_ID = ?",
            arrayOf(accountId, MOVIES_SECTION, providerItemId),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.record() else null
        }
    }

    override fun list(accountId: String, limit: Int): List<MovieResumeRecord> =
        synchronized(helper) {
            require(limit in 1..MAX_RECORDS_PER_ACCOUNT)
            helper.readableDatabase.query(
                TABLE,
                COLUMNS,
                "$ACCOUNT_ID = ? AND $SECTION = ?",
                arrayOf(accountId, MOVIES_SECTION),
                null,
                null,
                "$UPDATED_AT_MILLIS DESC",
                limit.toString(),
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.record())
                }
            }
        }

    override fun upsert(record: MovieResumeRecord) = synchronized(helper) {
        validate(record)
        helper.writableDatabase.inTransaction {
            insertWithOnConflict(
                TABLE,
                null,
                ContentValues().apply {
                    put(ACCOUNT_ID, record.accountId)
                    put(SECTION, MOVIES_SECTION)
                    put(PROVIDER_ITEM_ID, record.providerItemId)
                    put(POSITION_MILLIS, record.positionMillis)
                    record.durationMillis?.let { put(DURATION_MILLIS, it) }
                        ?: putNull(DURATION_MILLIS)
                    put(UPDATED_AT_MILLIS, record.updatedAtEpochMillis)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            ).also { require(it != -1L) }
            execSQL(
                """DELETE FROM $TABLE
                    WHERE $ACCOUNT_ID = ? AND $SECTION = ?
                    AND $PROVIDER_ITEM_ID NOT IN (
                        SELECT $PROVIDER_ITEM_ID FROM $TABLE
                        WHERE $ACCOUNT_ID = ? AND $SECTION = ?
                        ORDER BY $UPDATED_AT_MILLIS DESC
                        LIMIT $MAX_RECORDS_PER_ACCOUNT
                    )""".trimIndent(),
                arrayOf(
                    record.accountId,
                    MOVIES_SECTION,
                    record.accountId,
                    MOVIES_SECTION,
                ),
            )
        }
    }

    override fun delete(accountId: String, providerItemId: String) =
        synchronized(helper) {
            helper.writableDatabase.delete(
                TABLE,
                "$ACCOUNT_ID = ? AND $SECTION = ? AND $PROVIDER_ITEM_ID = ?",
                arrayOf(accountId, MOVIES_SECTION, providerItemId),
            )
            Unit
        }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            delete(
                TABLE,
                "$ACCOUNT_ID = ? AND $SECTION = ?",
                arrayOf(accountId, MOVIES_SECTION),
            )
        }
    }

    private fun Cursor.record() = MovieResumeRecord(
        accountId = getString(getColumnIndexOrThrow(ACCOUNT_ID)),
        providerItemId = getString(getColumnIndexOrThrow(PROVIDER_ITEM_ID)),
        positionMillis = getLong(getColumnIndexOrThrow(POSITION_MILLIS)),
        durationMillis = getColumnIndexOrThrow(DURATION_MILLIS).let { index ->
            if (isNull(index)) null else getLong(index)
        },
        updatedAtEpochMillis = getLong(getColumnIndexOrThrow(UPDATED_AT_MILLIS)),
    )

    private fun validate(record: MovieResumeRecord) {
        require(record.accountId.isNotBlank())
        require(record.accountId.codePointCount(0, record.accountId.length) <= 128)
        require(record.providerItemId.isNotBlank())
        require(record.providerItemId.codePointCount(0, record.providerItemId.length) <= 256)
        require(MovieResumePolicy.isValidPosition(record.positionMillis, record.durationMillis))
        require(record.updatedAtEpochMillis > 0L)
    }

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try {
            block().also { setTransactionSuccessful() }
        } finally {
            endTransaction()
        }
    }

    private class MovieResumeDatabase(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION,
    ) {
        override fun onConfigure(database: SQLiteDatabase) {
            database.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE $TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SECTION TEXT NOT NULL CHECK ($SECTION = '$MOVIES_SECTION'),
                    $PROVIDER_ITEM_ID TEXT NOT NULL,
                    $POSITION_MILLIS INTEGER NOT NULL CHECK ($POSITION_MILLIS >= 0),
                    $DURATION_MILLIS INTEGER,
                    $UPDATED_AT_MILLIS INTEGER NOT NULL CHECK ($UPDATED_AT_MILLIS > 0),
                    PRIMARY KEY ($ACCOUNT_ID, $SECTION, $PROVIDER_ITEM_ID),
                    CHECK ($DURATION_MILLIS IS NULL OR (
                        $DURATION_MILLIS > 0 AND $POSITION_MILLIS <= $DURATION_MILLIS
                    ))
                )""".trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX movie_resume_recent ON $TABLE " +
                    "($ACCOUNT_ID, $SECTION, $UPDATED_AT_MILLIS DESC)",
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            check(oldVersion < newVersion)
            error("Missing forward-only movie resume migration: $oldVersion to $newVersion")
        }

        override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Movie resume database downgrade is unsupported")
        }
    }

    private companion object {
        const val DATABASE_NAME = "tyfino_movie_resume_v1.db"
        const val DATABASE_VERSION = 1
        const val TABLE = "movie_resume"
        const val ACCOUNT_ID = "account_id"
        const val SECTION = "section"
        const val PROVIDER_ITEM_ID = "provider_item_id"
        const val POSITION_MILLIS = "position_ms"
        const val DURATION_MILLIS = "duration_ms"
        const val UPDATED_AT_MILLIS = "updated_at_ms"
        const val MOVIES_SECTION = "MOVIES"
        const val MAX_RECORDS_PER_ACCOUNT = 200
        val COLUMNS = arrayOf(
            ACCOUNT_ID,
            PROVIDER_ITEM_ID,
            POSITION_MILLIS,
            DURATION_MILLIS,
            UPDATED_AT_MILLIS,
        )
    }
}
