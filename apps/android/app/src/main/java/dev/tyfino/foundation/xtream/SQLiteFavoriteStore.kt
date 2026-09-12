package dev.tyfino.foundation.xtream

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SQLiteFavoriteStore(context: Context) : FavoriteStore {
    private val helper = FavoriteDatabase(context.applicationContext)

    override fun list(accountId: String, section: CatalogSection, limit: Int): List<FavoriteRecord> =
        synchronized(helper) {
            require(limit in 1..MAX_RECORDS)
            helper.readableDatabase.query(
                TABLE, COLUMNS, "$ACCOUNT_ID = ? AND $SECTION = ?",
                arrayOf(accountId, section.name), null, null,
                "$UPDATED_AT DESC, $PROVIDER_ID ASC", limit.toString(),
            ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.record()) } }
        }

    override fun contains(accountId: String, section: CatalogSection, itemId: String): Boolean =
        synchronized(helper) {
            helper.readableDatabase.query(
                TABLE, arrayOf(PROVIDER_ID),
                "$ACCOUNT_ID = ? AND $SECTION = ? AND $PROVIDER_ID = ?",
                arrayOf(accountId, section.name, itemId), null, null, null, "1",
            ).use(Cursor::moveToFirst)
        }

    override fun upsert(record: FavoriteRecord) = synchronized(helper) {
        require(record.accountId.bounded(128) && record.providerItemId.bounded(256))
        require(record.updatedAtEpochMillis > 0L)
        helper.writableDatabase.inTransaction {
            insertWithOnConflict(
                TABLE, null,
                ContentValues().apply {
                    put(ACCOUNT_ID, record.accountId)
                    put(SECTION, record.section.name)
                    put(PROVIDER_ID, record.providerItemId)
                    put(UPDATED_AT, record.updatedAtEpochMillis)
                }, SQLiteDatabase.CONFLICT_REPLACE,
            ).also { require(it != -1L) }
            execSQL(
                """DELETE FROM $TABLE WHERE $ACCOUNT_ID = ? AND rowid NOT IN (
                    SELECT rowid FROM $TABLE WHERE $ACCOUNT_ID = ?
                    ORDER BY $UPDATED_AT DESC, $SECTION ASC, $PROVIDER_ID ASC LIMIT $MAX_RECORDS
                )""".trimIndent(),
                arrayOf(record.accountId, record.accountId),
            )
        }
    }

    override fun delete(accountId: String, section: CatalogSection, itemId: String) = synchronized(helper) {
        helper.writableDatabase.delete(
            TABLE, "$ACCOUNT_ID = ? AND $SECTION = ? AND $PROVIDER_ID = ?",
            arrayOf(accountId, section.name, itemId),
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

    private fun Cursor.record() = FavoriteRecord(
        accountId = getString(getColumnIndexOrThrow(ACCOUNT_ID)),
        section = CatalogSection.valueOf(getString(getColumnIndexOrThrow(SECTION))),
        providerItemId = getString(getColumnIndexOrThrow(PROVIDER_ID)),
        updatedAtEpochMillis = getLong(getColumnIndexOrThrow(UPDATED_AT)),
    )

    private fun String.bounded(limit: Int): Boolean = isNotBlank() && codePointCount(0, length) <= limit

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try { block().also { setTransactionSuccessful() } } finally { endTransaction() }
    }

    private class FavoriteDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE $TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SECTION TEXT NOT NULL,
                    $PROVIDER_ID TEXT NOT NULL,
                    $UPDATED_AT INTEGER NOT NULL CHECK ($UPDATED_AT > 0),
                    PRIMARY KEY ($ACCOUNT_ID, $SECTION, $PROVIDER_ID)
                )""".trimIndent(),
            )
            database.execSQL("CREATE INDEX favorite_recent ON $TABLE ($ACCOUNT_ID, $UPDATED_AT DESC)")
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Missing forward-only favorites migration: $oldVersion to $newVersion")
        }

        override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Favorites database downgrade is unsupported")
        }
    }

    private companion object {
        const val DATABASE_NAME = "tyfino_favorites_v1.db"
        const val TABLE = "catalog_favorites"
        const val ACCOUNT_ID = "account_id"
        const val SECTION = "section"
        const val PROVIDER_ID = "provider_id"
        const val UPDATED_AT = "updated_at_ms"
        const val MAX_RECORDS = 200
        val COLUMNS = arrayOf(ACCOUNT_ID, SECTION, PROVIDER_ID, UPDATED_AT)
    }
}
