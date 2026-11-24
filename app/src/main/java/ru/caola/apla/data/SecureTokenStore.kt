package ru.caola.apla.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts access/refresh tokens with separate Android Keystore AES/GCM keys. */
class SecureTokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("caola_secure", Context.MODE_PRIVATE)

    fun write(value: String) = writeValue("access", "caola_api_access_key", value)
    fun read(): String {
        readValue("access", "caola_api_access_key").takeIf { it.isNotBlank() }?.let { return it }
        // Migrate tokens saved by v4 using the original preference keys and Keystore alias.
        val legacy = try {
            val ivValue = prefs.getString("token_iv", null) ?: return ""
            val encryptedValue = prefs.getString("token_ciphertext", null) ?: return ""
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = keyStore.getKey("caola_api_token_key", null) as? SecretKey ?: return ""
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(ivValue, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(encryptedValue, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) { "" }
        if (legacy.isNotBlank()) write(legacy)
        return legacy
    }
    fun writeRefresh(value: String) = writeValue("refresh", "caola_api_refresh_key", value)
    fun readRefresh(): String = readValue("refresh", "caola_api_refresh_key")

    private fun writeValue(slot: String, alias: String, value: String) {
        if (value.isBlank()) {
            prefs.edit().remove("${slot}_iv").remove("${slot}_ciphertext").apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(alias))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("${slot}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${slot}_ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    private fun readValue(slot: String, alias: String): String {
        val ivValue = prefs.getString("${slot}_iv", null) ?: return ""
        val encryptedValue = prefs.getString("${slot}_ciphertext", null) ?: return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(alias), GCMParameterSpec(128, Base64.decode(ivValue, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(encryptedValue, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }
}
