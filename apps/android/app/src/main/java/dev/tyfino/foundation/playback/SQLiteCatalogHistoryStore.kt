package dev.tyfino.foundation.playback

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.tyfino.foundation.xtream.CatalogSection

/** Separate from favorites and resume; never stores stream URLs, titles, or credentials. */
internal class SQLiteCatalogHistoryStore(context: Context) : CatalogHistoryStore {
    private val helper = HistoryDatabase(context.applicationContext)

    override fun list(accountId: String, section: CatalogSection, limit: Int): List<CatalogHistoryRecord> =
        synchronized(helper) {
            require(limit in 1..MAX_HISTORY)
            helper.readableDatabase.query(
                TABLE, COLUMNS, "$ACCOUNT_ID = ? AND $SECTION = ?",
                arrayOf(accountId, section.name), null, null,
                "$STARTED_AT DESC, $ITEM_ID ASC", limit.toString(),
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.record()) } }
        }

    override fun upsert(record: CatalogHistoryRecord) = synchronized(helper) {
        require(record.section != CatalogSection.Series)
        require(record.accountId.bounded(128) && record.providerItemId.bounded(256))
        require(record.startedAtEpochMillis > 0L)
        helper.writableDatabase.inTransaction {
            insertWithOnConflict(
                TABLE, null,
                ContentValues().apply {
                    put(ACCOUNT_ID, record.accountId)
                    put(SECTION, record.section.name)
                    put(ITEM_ID, record.providerItemId)
                    put(STARTED_AT, record.startedAtEpochMillis)
                }, SQLiteDatabase.CONFLICT_REPLACE,
            ).also { require(it != -1L) }
            execSQL(
                """DELETE FROM $TABLE WHERE $ACCOUNT_ID = ? AND rowid NOT IN (
                    SELECT rowid FROM $TABLE WHERE $ACCOUNT_ID = ?
                    ORDER BY $STARTED_AT DESC, $SECTION ASC, $ITEM_ID ASC LIMIT $MAX_HISTORY
                )""".trimIndent(),
                arrayOf(record.accountId, record.accountId),
            )
        }
    }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.delete(TABLE, "$ACCOUNT_ID = ?", arrayOf(accountId))
        Unit
    }

    override fun clearOtherAccounts(accountId: String) = synchronized(helper) {
        helper.writableDatabase.delete(TABLE, "$ACCOUNT_ID != ?", arrayOf(accountId))
        Unit
    }

    private fun Cursor.record() = CatalogHistoryRecord(
        accountId = getString(getColumnIndexOrThrow(ACCOUNT_ID)),
        section = CatalogSection.valueOf(getString(getColumnIndexOrThrow(SECTION))),
        providerItemId = getString(getColumnIndexOrThrow(ITEM_ID)),
        startedAtEpochMillis = getLong(getColumnIndexOrThrow(STARTED_AT)),
    )

    private fun String.bounded(maxCodePoints: Int) = isNotBlank() && codePointCount(0, length) <= maxCodePoints

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try { block().also { setTransactionSuccessful() } } finally { endTransaction() }
    }

    private class HistoryDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE $TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SECTION TEXT NOT NULL CHECK ($SECTION IN ('Live', 'Movies')),
                    $ITEM_ID TEXT NOT NULL,
                    $STARTED_AT INTEGER NOT NULL CHECK ($STARTED_AT > 0),
                    PRIMARY KEY ($ACCOUNT_ID, $SECTION, $ITEM_ID)
                )""".trimIndent(),
            )
            database.execSQL("CREATE INDEX catalog_history_recent ON $TABLE ($ACCOUNT_ID, $STARTED_AT DESC)")
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Missing forward-only history migration: $oldVersion to $newVersion")
        }

        override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("History database downgrade is unsupported")
        }
    }

    private companion object {
        const val DATABASE_NAME = "tyfino_catalog_history_v1.db"
        const val TABLE = "catalog_history"
        const val ACCOUNT_ID = "account_id"
        const val SECTION = "section"
        const val ITEM_ID = "item_id"
        const val STARTED_AT = "started_at_ms"
        const val MAX_HISTORY = 100
        val COLUMNS = arrayOf(ACCOUNT_ID, SECTION, ITEM_ID, STARTED_AT)
    }
}
