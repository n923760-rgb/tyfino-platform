package dev.tyfino.foundation.xtream

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogIdentityIndexMigrationTest {
    @Test fun versionOneCacheSurvivesIdentityIndexMigration() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(base.cacheDir, "catalog-migration-${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = File(directory, name)
        }
        val path = context.getDatabasePath("tyfino_catalog_v1.db")
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
                database.execSQL("CREATE TABLE catalog_snapshots (account_id TEXT NOT NULL, section TEXT NOT NULL, " +
                    "category_id TEXT NOT NULL, generation INTEGER NOT NULL, refreshed_at INTEGER NOT NULL, " +
                    "PRIMARY KEY (account_id, section, category_id))")
                database.execSQL("CREATE TABLE catalog_categories (account_id TEXT NOT NULL, section TEXT NOT NULL, " +
                    "provider_id TEXT NOT NULL, display_name TEXT NOT NULL, provider_order INTEGER NOT NULL, " +
                    "generation INTEGER NOT NULL, PRIMARY KEY (account_id, section, provider_id))")
                database.execSQL("CREATE TABLE catalog_items (account_id TEXT NOT NULL, section TEXT NOT NULL, " +
                    "category_id TEXT NOT NULL, provider_id TEXT NOT NULL, display_name TEXT NOT NULL, " +
                    "provider_order INTEGER NOT NULL, artwork_url TEXT, rating TEXT, release_year TEXT, " +
                    "container_extension TEXT, generation INTEGER NOT NULL, " +
                    "PRIMARY KEY (account_id, section, category_id, provider_id))")
                database.execSQL("CREATE INDEX catalog_item_order ON catalog_items " +
                    "(account_id, section, category_id, generation, provider_order)")
                database.execSQL("INSERT INTO catalog_snapshots VALUES ('account-a', 'Movies', 'category', 1, 1000)")
                database.execSQL("INSERT INTO catalog_categories VALUES ('account-a', 'Movies', 'category', 'Movies', 0, 1)")
                database.execSQL("INSERT INTO catalog_items VALUES " +
                    "('account-a', 'Movies', 'category', 'movie-1', 'Saved movie', 0, NULL, NULL, NULL, 'mp4', 1)")
                database.version = 1
            }

            val store = SQLiteCatalogStore(context)
            assertEquals("Saved movie", store.loadItems("account-a", CatalogSection.Movies, "category")
                ?.records?.single()?.name)
            assertEquals(listOf("movie-1"), store.loadItemsByProviderIds(
                "account-a", CatalogSection.Movies, setOf("movie-1"),
            ).map { it.providerId })
            assertTrue(store.loadItemsByProviderIds("account-b", CatalogSection.Movies, setOf("movie-1")).isEmpty())

            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                assertEquals(2, database.version)
                val sql = "EXPLAIN QUERY PLAN SELECT * FROM catalog_items WHERE " +
                    "account_id = ? AND section = ? AND provider_id IN (?)"
                database.rawQuery(sql, arrayOf("account-a", "Movies", "movie-1")).use { cursor ->
                    val details = buildList {
                        while (cursor.moveToNext()) add(cursor.getString(cursor.columnCount - 1))
                    }
                    assertTrue("Identity lookup should use the new index: $details",
                        details.any { it.contains("catalog_item_identity") })
                }
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
