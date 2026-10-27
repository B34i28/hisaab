package com.example.util

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun createStoredPinHash(pin: String): String {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        return "$salt$$hash"
    }

    fun verifyPin(enteredPin: String, storedPinHash: String?): Boolean {
        val stored = storedPinHash ?: return false
        if (stored.isEmpty()) return false

        // Support legacy plain-text migration if existing stored PIN was plain text (e.g. "1234")
        if (!stored.contains("$")) {
            return enteredPin == stored
        }

        val parts = stored.split("$")
        if (parts.size != 2) return false

        val salt = parts[0]
        val expectedHash = parts[1]
        val actualHash = hashPin(enteredPin, salt)

        return actualHash == expectedHash
    }
}
