package dev.tyfino.foundation.xtream

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Dedicated EPG database; no credentials, authenticated URLs, or raw provider responses. */
internal class SQLiteLiveEpgStore(context: Context) : LiveEpgStore {
    private val helper = EpgDatabase(context.applicationContext)

    override fun retainOwner(accountId: String, accountGeneration: Long) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            delete(SNAPSHOT, "$ACCOUNT_ID != ? OR $ACCOUNT_GENERATION != ?",
                arrayOf(accountId, accountGeneration.toString()))
            removeOrphanPrograms()
        }
    }

    override fun load(accountId: String, channelId: String, nowEpochMillis: Long): LiveEpgSnapshot? =
        synchronized(helper) {
            val database = helper.writableDatabase
            database.inTransaction {
            delete(PROGRAM, "$ACCOUNT_ID = ? AND $CHANNEL_ID = ? AND $ENDS_AT <= ?",
                arrayOf(accountId, channelId, nowEpochMillis.toString()))
            val metadata = database.query(SNAPSHOT,
                arrayOf(GENERATION, REFRESHED_AT), "$ACCOUNT_ID = ? AND $CHANNEL_ID = ?",
                arrayOf(accountId, channelId), null, null, null, "1",
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getLong(0) to cursor.getLong(1)
            } ?: return@inTransaction null
            val programs = database.query(PROGRAM,
                arrayOf(TITLE, DESCRIPTION, STARTS_AT, ENDS_AT),
                "$ACCOUNT_ID = ? AND $CHANNEL_ID = ?",
                arrayOf(accountId, channelId), null, null,
                "$STARTS_AT ASC, $ENDS_AT ASC", "101",
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(LiveEpgProgram(accountId, channelId, cursor.nullable(0), cursor.nullable(1),
                            cursor.getLong(2), cursor.getLong(3)))
                    }
                }
            }
            require(programs.size <= MAX_ENTRIES)
            database.execSQL("UPDATE $SNAPSHOT SET $ACCESSED_AT = ? WHERE $ACCOUNT_ID = ? AND $CHANNEL_ID = ?",
                arrayOf(nowEpochMillis, accountId, channelId))
            LiveEpgSnapshot(metadata.first, metadata.second, programs)
            }
        }

    override fun replace(accountId: String, channelId: String, accountGeneration: Long,
        snapshot: LiveEpgSnapshot, nowEpochMillis: Long) = synchronized(helper) {
        require(accountId.bounded(128) && channelId.bounded(256))
        require(snapshot.generation > 0 && snapshot.refreshedAtEpochMillis > 0)
        require(snapshot.programs.size <= MAX_ENTRIES)
        require(snapshot.programs.all { entry ->
            entry.accountId == accountId && entry.channelId == channelId &&
                entry.endEpochMillis > entry.startEpochMillis &&
                listOfNotNull(entry.title, entry.description).all {
                    it.codePointCount(0, it.length) <= 512
                }
        })
        helper.writableDatabase.inTransaction {
            delete(PROGRAM, "$ACCOUNT_ID = ? AND $CHANNEL_ID = ?", arrayOf(accountId, channelId))
            delete(SNAPSHOT, "$ACCOUNT_ID = ? AND $CHANNEL_ID = ?", arrayOf(accountId, channelId))
            insertOrThrow(SNAPSHOT, null, ContentValues().apply {
                put(ACCOUNT_ID, accountId)
                put(CHANNEL_ID, channelId)
                put(ACCOUNT_GENERATION, accountGeneration)
                put(GENERATION, snapshot.generation)
                put(REFRESHED_AT, snapshot.refreshedAtEpochMillis)
                put(ACCESSED_AT, nowEpochMillis)
            })
            snapshot.programs.filter { it.endEpochMillis > nowEpochMillis }.forEachIndexed { index, entry ->
                insertOrThrow(PROGRAM, null, ContentValues().apply {
                    put(ACCOUNT_ID, accountId)
                    put(CHANNEL_ID, channelId)
                    put(ORDINAL, index)
                    put(TITLE, entry.title)
                    put(DESCRIPTION, entry.description)
                    put(STARTS_AT, entry.startEpochMillis)
                    put(ENDS_AT, entry.endEpochMillis)
                })
            }
            execSQL("""DELETE FROM $SNAPSHOT WHERE $ACCOUNT_ID = ? AND $CHANNEL_ID NOT IN (
                SELECT $CHANNEL_ID FROM $SNAPSHOT WHERE $ACCOUNT_ID = ?
                ORDER BY $ACCESSED_AT DESC, $CHANNEL_ID ASC LIMIT $MAX_CHANNELS
            )""".trimIndent(), arrayOf(accountId, accountId))
            removeOrphanPrograms()
        }
    }

    override fun clearAccount(accountId: String) = synchronized(helper) {
        helper.writableDatabase.inTransaction {
            delete(PROGRAM, "$ACCOUNT_ID = ?", arrayOf(accountId))
            delete(SNAPSHOT, "$ACCOUNT_ID = ?", arrayOf(accountId))
        }
    }

    private fun SQLiteDatabase.removeOrphanPrograms() {
        execSQL("""DELETE FROM $PROGRAM WHERE NOT EXISTS (
            SELECT 1 FROM $SNAPSHOT WHERE $SNAPSHOT.$ACCOUNT_ID = $PROGRAM.$ACCOUNT_ID
            AND $SNAPSHOT.$CHANNEL_ID = $PROGRAM.$CHANNEL_ID
        )""".trimIndent())
    }

    private fun Cursor.nullable(index: Int): String? = if (isNull(index)) null else getString(index)
    private fun String.bounded(limit: Int) = isNotBlank() && codePointCount(0, length) <= limit

    private inline fun <T> SQLiteDatabase.inTransaction(block: SQLiteDatabase.() -> T): T {
        beginTransaction()
        return try { block().also { setTransactionSuccessful() } } finally { endTransaction() }
    }

    private class EpgDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL("""CREATE TABLE $SNAPSHOT (
                $ACCOUNT_ID TEXT NOT NULL,
                $CHANNEL_ID TEXT NOT NULL,
                $ACCOUNT_GENERATION INTEGER NOT NULL,
                $GENERATION INTEGER NOT NULL CHECK ($GENERATION > 0),
                $REFRESHED_AT INTEGER NOT NULL CHECK ($REFRESHED_AT > 0),
                $ACCESSED_AT INTEGER NOT NULL,
                PRIMARY KEY ($ACCOUNT_ID, $CHANNEL_ID)
            )""".trimIndent())
            database.execSQL("""CREATE TABLE $PROGRAM (
                $ACCOUNT_ID TEXT NOT NULL,
                $CHANNEL_ID TEXT NOT NULL,
                $ORDINAL INTEGER NOT NULL,
                $TITLE TEXT,
                $DESCRIPTION TEXT,
                $STARTS_AT INTEGER NOT NULL,
                $ENDS_AT INTEGER NOT NULL CHECK ($ENDS_AT > $STARTS_AT),
                PRIMARY KEY ($ACCOUNT_ID, $CHANNEL_ID, $ORDINAL)
            )""".trimIndent())
            database.execSQL("CREATE INDEX epg_program_channel ON $PROGRAM ($ACCOUNT_ID, $CHANNEL_ID, $STARTS_AT)")
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Missing forward-only EPG migration: $oldVersion to $newVersion")
        }

        override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("EPG database downgrade is unsupported")
        }
    }

    private companion object {
        const val DATABASE_NAME = "tyfino_live_epg_v1.db"
        const val SNAPSHOT = "epg_snapshots"
        const val PROGRAM = "epg_programs"
        const val ACCOUNT_ID = "account_id"
        const val CHANNEL_ID = "channel_id"
        const val ACCOUNT_GENERATION = "account_generation"
        const val GENERATION = "generation"
        const val REFRESHED_AT = "refreshed_at_ms"
        const val ACCESSED_AT = "accessed_at_ms"
        const val ORDINAL = "ordinal"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val STARTS_AT = "starts_at_ms"
        const val ENDS_AT = "ends_at_ms"
        const val MAX_ENTRIES = 100
        const val MAX_CHANNELS = 20
    }
}
