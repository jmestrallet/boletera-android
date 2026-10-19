package uy.boletera.prueba

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The last balance STM reported, with its time, to show while connecting («Último saldo $ 1.240 · hoy 08:53»). */
internal data class LastBalance(val amount: Long, val at: Long)

/**
 * Keeps only the last balance and its time, encrypted with a key that never leaves the phone's keystore. It is shown
 * after the sign-in (never on the entry screen) and «Olvidar este celular» erases it.
 */
internal class LastBalanceStore(context: Context) {
    private val store = context.getSharedPreferences("lab_last_balance", Context.MODE_PRIVATE)
    private val alias = "boletera_last_balance"

    private fun key(): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keys.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    fun save(card: String, value: LastBalance) = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.doFinal("${value.amount};${value.at}".toByteArray())
        store.edit().putString("card", card)
            .putString("data", Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    /** The last balance of [card], or of the last card seen when [card] is still unknown (right after signing in). */
    fun read(card: String?): LastBalance? = runCatching {
        if (card != null && store.getString("card", null) != card) return null
        val (iv, sealed) = (store.getString("data", null) ?: return null).split(":").map { Base64.decode(it, Base64.NO_WRAP) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        val (amount, at) = String(cipher.doFinal(sealed)).split(";")
        LastBalance(amount.toLong(), at.toLong())
    }.getOrNull()

    fun clear() { store.edit().clear().apply() }
}
