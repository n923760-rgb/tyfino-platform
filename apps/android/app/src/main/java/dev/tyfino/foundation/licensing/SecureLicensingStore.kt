package dev.tyfino.foundation.licensing

import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.os.SystemClock
import androidx.core.content.edit
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

internal interface LicensingStore {
    fun installationId(): String
    fun snapshot(): EntitlementSnapshot?
    fun save(snapshot: EntitlementSnapshot)
    fun clearEntitlement()
}

internal class AndroidLicenseClock(private val context: Context) : LicenseClock {
    override fun wallTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
    override fun bootCount(): Int = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    }.getOrDefault(-1)
}

internal class SecureLicensingStore(context: Context) : LicensingStore {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun installationId(): String = synchronized(this) {
        readPayload()?.optString(INSTALLATION_ID)?.takeIf { it.length in 32..128 }
            ?: UUID.randomUUID().toString().replace("-", "").also { id ->
                writePayload(JSONObject().put(INSTALLATION_ID, id))
            }
    }

    override fun snapshot(): EntitlementSnapshot? = synchronized(this) {
        val payload = readPayload() ?: return@synchronized null
        val token = payload.optString(SESSION_TOKEN).takeIf(String::isNotBlank) ?: return@synchronized null
        val kind = runCatching { EntitlementKind.valueOf(payload.getString(KIND)) }.getOrNull()
            ?: return@synchronized null
        EntitlementSnapshot(
            kind = kind,
            startsAtMillis = payload.getLong(STARTS_AT),
            expiresAtMillis = payload.optLongOrNull(EXPIRES_AT),
            offlineValidUntilMillis = payload.getLong(OFFLINE_UNTIL),
            serverTimeMillis = payload.getLong(SERVER_TIME),
            refreshAfterMillis = payload.getLong(REFRESH_AFTER),
            sessionToken = token,
            verifiedElapsedRealtimeMillis = payload.getLong(VERIFIED_ELAPSED),
            verifiedBootCount = payload.getInt(VERIFIED_BOOT),
        )
    }

    override fun save(snapshot: EntitlementSnapshot) = synchronized(this) {
        val payload = JSONObject()
            .put(INSTALLATION_ID, installationId())
            .put(KIND, snapshot.kind.name)
            .put(STARTS_AT, snapshot.startsAtMillis)
            .put(EXPIRES_AT, snapshot.expiresAtMillis ?: JSONObject.NULL)
            .put(OFFLINE_UNTIL, snapshot.offlineValidUntilMillis)
            .put(SERVER_TIME, snapshot.serverTimeMillis)
            .put(REFRESH_AFTER, snapshot.refreshAfterMillis)
            .put(SESSION_TOKEN, snapshot.sessionToken)
            .put(VERIFIED_ELAPSED, snapshot.verifiedElapsedRealtimeMillis)
            .put(VERIFIED_BOOT, snapshot.verifiedBootCount)
        writePayload(payload)
    }

    override fun clearEntitlement() = synchronized(this) {
        val id = readPayload()?.optString(INSTALLATION_ID)?.takeIf(String::isNotBlank) ?: installationId()
        writePayload(JSONObject().put(INSTALLATION_ID, id))
    }

    private fun readPayload(): JSONObject? {
        val encoded = preferences.getString(PAYLOAD, null) ?: return null
        return runCatching {
            val parts = encoded.split('.', limit = 2)
            require(parts.size == 2)
            val decoder = android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
            val iv = android.util.Base64.decode(parts[0], decoder)
            val encrypted = android.util.Base64.decode(parts[1], decoder)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        }.getOrElse {
            preferences.edit { remove(PAYLOAD) }
            null
        }
    }

    private fun writePayload(payload: JSONObject) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encoder = android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        val iv = android.util.Base64.encodeToString(cipher.iv, encoder)
        val encrypted = android.util.Base64.encodeToString(
            cipher.doFinal(payload.toString().toByteArray(Charsets.UTF_8)), encoder,
        )
        preferences.edit { putString(PAYLOAD, "$iv.$encrypted") }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private fun JSONObject.optLongOrNull(name: String): Long? =
        if (isNull(name) || !has(name)) null else getLong(name)

    private companion object {
        const val PREFERENCES = "tyfino_licensing_protected"
        const val PAYLOAD = "payload"
        const val KEY_ALIAS = "tyfino_licensing_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val INSTALLATION_ID = "installationId"
        const val SESSION_TOKEN = "sessionToken"
        const val KIND = "kind"
        const val STARTS_AT = "startsAt"
        const val EXPIRES_AT = "expiresAt"
        const val OFFLINE_UNTIL = "offlineUntil"
        const val SERVER_TIME = "serverTime"
        const val REFRESH_AFTER = "refreshAfter"
        const val VERIFIED_ELAPSED = "verifiedElapsed"
        const val VERIFIED_BOOT = "verifiedBoot"
    }
}
