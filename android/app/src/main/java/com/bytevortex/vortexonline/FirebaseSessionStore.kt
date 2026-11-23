package com.bytevortex.vortexonline

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps only the Firebase refresh token, encrypted with an Android Keystore AES-GCM key. */
class FirebaseSessionStore(context: Context) {
    data class SavedSession(val email: String, val refreshToken: String)

    private val prefs = context.getSharedPreferences("firebase_session", Context.MODE_PRIVATE)
    private val alias = "vortex_firebase_refresh_v1"

    @Synchronized fun save(email: String, refreshToken: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        prefs.edit()
            .putString("email", email)
            .putString("refresh", Base64.encodeToString(combined, Base64.NO_WRAP))
            .apply()
    }

    @Synchronized fun read(): SavedSession? {
        val email = prefs.getString("email", null) ?: return null
        val encoded = prefs.getString("refresh", null) ?: return null
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > 12) { "Saved session is incomplete." }
        val iv = combined.copyOfRange(0, 12)
        val payload = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        val refresh = cipher.doFinal(payload).toString(Charsets.UTF_8)
        return SavedSession(email, refresh)
    }

    @Synchronized fun clear() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
