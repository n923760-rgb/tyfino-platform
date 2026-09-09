package dev.tyfino.foundation.xtream

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SQLiteCatalogStore(context: Context) : CatalogStore {
    private val helper = CatalogDatabase(context.applicationContext)

    override fun loadCategories(
        accountId: String,
        section: CatalogSection,
    ): CatalogSnapshot<CatalogCategory>? = synchronized(helper) {
        val database = helper.readableDatabase
        val metadata = readMetadata(database, accountId, section, CATEGORIES_KEY) ?: return@synchronized null
        val records = database.query(
            CATEGORY_TABLE,
            arrayOf(PROVIDER_ID, DISPLAY_NAME, PROVIDER_ORDER),
            "$ACCOUNT_ID = ? AND $SECTION = ? AND $GENERATION = ?",
            arrayOf(accountId, section.name, metadata.generation.toString()),
            null,
            null,
            "$PROVIDER_ORDER ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CatalogCategory(
                            providerId = cursor.text(PROVIDER_ID),
                            name = cursor.text(DISPLAY_NAME),
                            providerOrder = cursor.getInt(cursor.getColumnIndexOrThrow(PROVIDER_ORDER)),
                        ),
                    )
                }
            }
        }
        CatalogSnapshot(metadata.generation, metadata.refreshedAtMillis, records)
    }

    override fun loadItems(
        accountId: String,
        section: CatalogSection,
        categoryId: String,
    ): CatalogSnapshot<CatalogItem>? = synchronized(helper) {
        val database = helper.readableDatabase
        val metadata = readMetadata(database, accountId, section, categoryId) ?: return@synchronized null
        val records = database.query(
            ITEM_TABLE,
            arrayOf(
                PROVIDER_ID,
                DISPLAY_NAME,
                PROVIDER_ORDER,
                ARTWORK_URL,
                RATING,
                RELEASE_YEAR,
                CONTAINER_EXTENSION,
            ),
            "$ACCOUNT_ID = ? AND $SECTION = ? AND $CATEGORY_ID = ? AND $GENERATION = ?",
            arrayOf(accountId, section.name, categoryId, metadata.generation.toString()),
            null,
            null,
            "$PROVIDER_ORDER ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CatalogItem(
                            providerId = cursor.text(PROVIDER_ID),
                            categoryId = categoryId,
                            name = cursor.text(DISPLAY_NAME),
                            providerOrder = cursor.getInt(cursor.getColumnIndexOrThrow(PROVIDER_ORDER)),
                            artworkUrl = cursor.nullableText(ARTWORK_URL),
                            rating = cursor.nullableText(RATING),
                            releaseYear = cursor.nullableText(RELEASE_YEAR),
                            containerExtension = cursor.nullableText(CONTAINER_EXTENSION),
                        ),
                    )
                }
            }
        }
        CatalogSnapshot(metadata.generation, metadata.refreshedAtMillis, records)
    }

    override fun replaceCategories(
        accountId: String,
        section: CatalogSection,
        snapshot: CatalogSnapshot<CatalogCategory>,
    ) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            delete(
                CATEGORY_TABLE,
                "$ACCOUNT_ID = ? AND $SECTION = ?",
                arrayOf(accountId, section.name),
            )
            snapshot.records.forEach { record ->
                insertOrThrow(
                    CATEGORY_TABLE,
                    null,
                    ContentValues().apply {
                        put(ACCOUNT_ID, accountId)
                        put(SECTION, section.name)
                        put(PROVIDER_ID, record.providerId)
                        put(DISPLAY_NAME, record.name)
                        put(PROVIDER_ORDER, record.providerOrder)
                        put(GENERATION, snapshot.generation)
                    },
                )
            }
            replaceMetadata(this, accountId, section, CATEGORIES_KEY, snapshot)
        }
    }

    override fun replaceItems(
        accountId: String,
        section: CatalogSection,
        categoryId: String,
        snapshot: CatalogSnapshot<CatalogItem>,
    ) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            delete(
                ITEM_TABLE,
                "$ACCOUNT_ID = ? AND $SECTION = ? AND $CATEGORY_ID = ?",
                arrayOf(accountId, section.name, categoryId),
            )
            snapshot.records.forEach { record ->
                require(record.categoryId == categoryId)
                insertOrThrow(
                    ITEM_TABLE,
                    null,
                    ContentValues().apply {
                        put(ACCOUNT_ID, accountId)
                        put(SECTION, section.name)
                        put(CATEGORY_ID, categoryId)
                        put(PROVIDER_ID, record.providerId)
                        put(DISPLAY_NAME, record.name)
                        put(PROVIDER_ORDER, record.providerOrder)
                        put(ARTWORK_URL, record.artworkUrl)
                        put(RATING, record.rating)
                        put(RELEASE_YEAR, record.releaseYear)
                        put(CONTAINER_EXTENSION, record.containerExtension)
                        put(GENERATION, snapshot.generation)
                    },
                )
            }
            replaceMetadata(this, accountId, section, categoryId, snapshot)
        }
    }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            arrayOf(CATEGORY_TABLE, ITEM_TABLE, SNAPSHOT_TABLE).forEach { table ->
                delete(table, "$ACCOUNT_ID = ?", arrayOf(accountId))
            }
        }
    }

    private fun readMetadata(
        database: SQLiteDatabase,
        accountId: String,
        section: CatalogSection,
        categoryId: String,
    ): SnapshotMetadata? = database.query(
        SNAPSHOT_TABLE,
        arrayOf(GENERATION, REFRESHED_AT),
        "$ACCOUNT_ID = ? AND $SECTION = ? AND $CATEGORY_ID = ?",
        arrayOf(accountId, section.name, categoryId),
        null,
        null,
        null,
        "1",
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        SnapshotMetadata(
            generation = cursor.getLong(cursor.getColumnIndexOrThrow(GENERATION)),
            refreshedAtMillis = cursor.getLong(cursor.getColumnIndexOrThrow(REFRESHED_AT)),
        )
    }

    private fun replaceMetadata(
        database: SQLiteDatabase,
        accountId: String,
        section: CatalogSection,
        categoryId: String,
        snapshot: CatalogSnapshot<*>,
    ) {
        database.insertWithOnConflict(
            SNAPSHOT_TABLE,
            null,
            ContentValues().apply {
                put(ACCOUNT_ID, accountId)
                put(SECTION, section.name)
                put(CATEGORY_ID, categoryId)
                put(GENERATION, snapshot.generation)
                put(REFRESHED_AT, snapshot.refreshedAtEpochMillis)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        ).also { require(it != -1L) }
    }

    private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column))

    private fun Cursor.nullableText(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try {
            block().also { setTransactionSuccessful() }
        } finally {
            endTransaction()
        }
    }

    private data class SnapshotMetadata(val generation: Long, val refreshedAtMillis: Long)

    private class CatalogDatabase(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION,
    ) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE $SNAPSHOT_TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SECTION TEXT NOT NULL,
                    $CATEGORY_ID TEXT NOT NULL,
                    $GENERATION INTEGER NOT NULL,
                    $REFRESHED_AT INTEGER NOT NULL,
                    PRIMARY KEY ($ACCOUNT_ID, $SECTION, $CATEGORY_ID)
                )""".trimIndent(),
            )
            database.execSQL(
                """CREATE TABLE $CATEGORY_TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SECTION TEXT NOT NULL,
                    $PROVIDER_ID TEXT NOT NULL,
                    $DISPLAY_NAME TEXT NOT NULL,
                    $PROVIDER_ORDER INTEGER NOT NULL,
                    $GENERATION INTEGER NOT NULL,
                    PRIMARY KEY ($ACCOUNT_ID, $SECTION, $PROVIDER_ID)
                )""".trimIndent(),
            )
            database.execSQL(
                """CREATE TABLE $ITEM_TABLE (
                    $ACCOUNT_ID TEXT NOT NULL,
                    $SECTION TEXT NOT NULL,
                    $CATEGORY_ID TEXT NOT NULL,
                    $PROVIDER_ID TEXT NOT NULL,
                    $DISPLAY_NAME TEXT NOT NULL,
                    $PROVIDER_ORDER INTEGER NOT NULL,
                    $ARTWORK_URL TEXT,
                    $RATING TEXT,
                    $RELEASE_YEAR TEXT,
                    $CONTAINER_EXTENSION TEXT,
                    $GENERATION INTEGER NOT NULL,
                    PRIMARY KEY ($ACCOUNT_ID, $SECTION, $CATEGORY_ID, $PROVIDER_ID)
                )""".trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX catalog_item_order ON $ITEM_TABLE " +
                    "($ACCOUNT_ID, $SECTION, $CATEGORY_ID, $GENERATION, $PROVIDER_ORDER)",
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            check(oldVersion < newVersion)
            database.execSQL("DROP TABLE IF EXISTS $CATEGORY_TABLE")
            database.execSQL("DROP TABLE IF EXISTS $ITEM_TABLE")
            database.execSQL("DROP TABLE IF EXISTS $SNAPSHOT_TABLE")
            onCreate(database)
        }
    }

    private companion object {
        const val DATABASE_NAME = "tyfino_catalog_v1.db"
        const val DATABASE_VERSION = 1
        const val SNAPSHOT_TABLE = "catalog_snapshots"
        const val CATEGORY_TABLE = "catalog_categories"
        const val ITEM_TABLE = "catalog_items"
        const val ACCOUNT_ID = "account_id"
        const val SECTION = "section"
        const val CATEGORY_ID = "category_id"
        const val PROVIDER_ID = "provider_id"
        const val DISPLAY_NAME = "display_name"
        const val PROVIDER_ORDER = "provider_order"
        const val ARTWORK_URL = "artwork_url"
        const val RATING = "rating"
        const val RELEASE_YEAR = "release_year"
        const val CONTAINER_EXTENSION = "container_extension"
        const val GENERATION = "generation"
        const val REFRESHED_AT = "refreshed_at"
        const val CATEGORIES_KEY = ""
    }
}
