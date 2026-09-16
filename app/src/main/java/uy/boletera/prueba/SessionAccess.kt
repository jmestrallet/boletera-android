package uy.boletera.prueba

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Keeps an already-authorized STM access only for this process. Nothing is written to disk and
 * the plaintext exists only while a login command is being prepared.
 */
internal class SessionAccess {
    private var key: ByteArray? = null
    private var iv: ByteArray? = null
    private var ciphertext: ByteArray? = null

    val available: Boolean get() = key != null && iv != null && ciphertext != null

    fun remember(document: String, password: String) {
        clear()
        val documentBytes=document.toByteArray(Charsets.UTF_8)
        val passwordBytes=password.toByteArray(Charsets.UTF_8)
        val plain=ByteArray(documentBytes.size+1+passwordBytes.size)
        documentBytes.copyInto(plain)
        passwordBytes.copyInto(plain,documentBytes.size+1)
        try {
            val nextKey = ByteArray(32).also(SecureRandom()::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(nextKey, "AES"))
            }
            key = nextKey
            iv = cipher.iv.copyOf()
            ciphertext = cipher.doFinal(plain)
        } finally {
            documentBytes.fill(0);passwordBytes.fill(0);plain.fill(0)
        }
    }

    fun open(): Pair<String, String>? {
        val ownKey = key ?: return null
        val ownIv = iv ?: return null
        val ownCiphertext = ciphertext ?: return null
        val plain = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(ownKey, "AES"), GCMParameterSpec(128, ownIv))
            }.doFinal(ownCiphertext)
        } catch (_: Exception) { return null }
        return try {
            val separator = plain.indexOf(0.toByte())
            if (separator <= 0 || separator >= plain.lastIndex) null
            else String(plain, 0, separator, Charsets.UTF_8) to
                String(plain, separator + 1, plain.size - separator - 1, Charsets.UTF_8)
        } finally {
            plain.fill(0)
        }
    }

    fun clear() {
        key?.fill(0); iv?.fill(0); ciphertext?.fill(0)
        key = null; iv = null; ciphertext = null
    }
}
