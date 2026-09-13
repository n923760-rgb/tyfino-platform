package dev.tyfino.foundation.xtream

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface XtreamAccountStore {
    fun load(): SavedXtreamAccount?
    fun save(account: SavedXtreamAccount)
    fun clear()
}

internal interface XtreamAccountPortfolioStore {
    fun loadPortfolio(): XtreamAccountPortfolio
    fun savePortfolio(portfolio: XtreamAccountPortfolio)
    fun clearPortfolio()
}

internal class SecureXtreamAccountStore(context: Context) :
    XtreamAccountStore,
    XtreamAccountPortfolioStore {
    private val persistence = XtreamAccountPortfolioPersistence(
        SecureXtreamPayloads(context.applicationContext),
    )

    override fun load(): SavedXtreamAccount? = loadPortfolio().activeAccount

    override fun save(account: SavedXtreamAccount) {
        savePortfolio(requireNotNull(XtreamAccountPortfolio.single(account)))
    }

    override fun clear() = clearPortfolio()

    override fun loadPortfolio(): XtreamAccountPortfolio = synchronized(this) {
        persistence.load()
    }

    override fun savePortfolio(portfolio: XtreamAccountPortfolio) = synchronized(this) {
        persistence.save(portfolio)
    }

    override fun clearPortfolio() = synchronized(this) {
        persistence.clear()
    }
}

internal class XtreamAccountPortfolioPersistence(
    private val payloads: XtreamEncryptedPayloads,
) {
    fun load(): XtreamAccountPortfolio {
        val current = try {
            payloads.read(PORTFOLIO_PAYLOAD)
        } catch (_: Exception) {
            return failClosed()
        }
        if (current != null) {
            return XtreamAccountPortfolioCodec.decode(current) ?: failClosed()
        }
        val legacy = try {
            payloads.read(LEGACY_PAYLOAD)
        } catch (_: Exception) {
            return failClosed()
        } ?: return XtreamAccountPortfolio.Empty
        val migrated = XtreamAccountPortfolioCodec.decodeLegacy(legacy) ?: return failClosed()
        try {
            save(migrated)
        } catch (_: Exception) {
            return migrated
        }
        runCatching { payloads.remove(LEGACY_PAYLOAD) }
        return migrated
    }

    fun save(portfolio: XtreamAccountPortfolio) {
        val previous = payloads.read(PORTFOLIO_PAYLOAD)
        try {
            val encoded = XtreamAccountPortfolioCodec.encode(portfolio)
            payloads.write(PORTFOLIO_PAYLOAD, encoded)
            check(XtreamAccountPortfolioCodec.decode(requireNotNull(payloads.read(PORTFOLIO_PAYLOAD))) == portfolio)
        } catch (failure: Exception) {
            runCatching {
                if (previous == null) payloads.remove(PORTFOLIO_PAYLOAD)
                else payloads.write(PORTFOLIO_PAYLOAD, previous)
            }
            throw failure
        }
    }

    fun clear() {
        val portfolioRemoval = runCatching { payloads.remove(PORTFOLIO_PAYLOAD) }
        val legacyRemoval = runCatching { payloads.remove(LEGACY_PAYLOAD) }
        portfolioRemoval.getOrThrow()
        legacyRemoval.getOrThrow()
    }

    private fun failClosed(): XtreamAccountPortfolio {
        runCatching { clear() }
        return XtreamAccountPortfolio.Empty
    }

    internal companion object {
        const val PORTFOLIO_PAYLOAD = "portfolio_payload_v2"
        const val LEGACY_PAYLOAD = "payload"
    }
}

internal interface XtreamEncryptedPayloads {
    fun read(name: String): String?
    fun write(name: String, plaintext: String)
    fun remove(name: String)
}

@SuppressLint("ApplySharedPref", "UseKtx")
internal class SecureXtreamPayloads(context: Context) : XtreamEncryptedPayloads {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun read(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        require(encoded.length <= MAX_ENCRYPTED_CHARACTERS)
        val parts = encoded.split('.', limit = 2)
        require(parts.size == 2)
        val flags = android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        val iv = android.util.Base64.decode(parts[0], flags)
        val encrypted = android.util.Base64.decode(parts[1], flags)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        val plaintext = cipher.doFinal(encrypted)
        require(plaintext.size <= MAX_PLAINTEXT_BYTES)
        return String(plaintext, Charsets.UTF_8)
    }

    override fun write(name: String, plaintext: String) {
        val bytes = plaintext.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_PLAINTEXT_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val flags = android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
        val iv = android.util.Base64.encodeToString(cipher.iv, flags)
        val encrypted = android.util.Base64.encodeToString(
            cipher.doFinal(bytes),
            flags,
        )
        check(preferences.edit().putString(name, "$iv.$encrypted").commit())
    }

    override fun remove(name: String) {
        check(preferences.edit().remove(name).commit())
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

    private companion object {
        const val PREFERENCES = "tyfino_xtream_account_protected"
        const val KEY_ALIAS = "tyfino_xtream_account_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val MAX_PLAINTEXT_BYTES = 65_536
        const val MAX_ENCRYPTED_CHARACTERS = 131_072
    }
}
