package uy.boletera.prueba

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import org.json.JSONObject

/** Local choices, separated by account without persisting the login document. */
class JourneyPreferences(context: Context) {
    private val store = context.getSharedPreferences("journey_choices", Context.MODE_PRIVATE)
    private var account: String? = null
    fun useAccount(document: String) {
        account = try {
            val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "boletera_choices_account"
            val key = (keys.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").run {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build())
                generateKey()
            }
            Mac.getInstance("HmacSHA256").run { init(key); doFinal(document.toByteArray()).joinToString("") { "%02x".format(it) } }
        } catch (_: Exception) { null } // If unavailable, keep manual choices; never mix accounts.
    }
    var card: String?
        get() = account?.let { store.getString("$it.card", null) }
        set(value) { account?.let { store.edit().putString("$it.card", value).apply() } }
    var provider: String?
        get() = account?.let { store.getString("$it.provider", null) }
        set(value) { account?.let { store.edit().putString("$it.provider", value).apply() } }
    val pending: PendingPayment?
        get() {
            val raw = account?.let { store.getString("$it.pending", null) } ?: return null
            return try {
                val value = JSONObject(raw)
                PendingPayment(value.getString("card"), value.getString("provider"), value.getLong("amount"), value.getLong("createdAt"))
            } catch (_: Exception) { PendingPayment("", "", 0, 0) } // Corrupt evidence never enables another charge.
        }
    fun beginPayment(value: PendingPayment): Boolean {
        val scope = account ?: return false
        if (pending != null) return false
        return store.edit().remove("$scope.resume").putString("$scope.pending", JSONObject().put("card", value.card).put("provider", value.provider)
            .put("amount", value.amount).put("createdAt", value.createdAt).toString()).commit()
    }
    private fun resumeKey(): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "boletera_payment_resume"
        return (keys.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    private fun resumeContext(scope: String, payment: PendingPayment) =
        "$scope|${payment.card}|${payment.provider}|${payment.amount}|${payment.createdAt}".toByteArray(Charsets.UTF_8)

    /** Store only the original Prex link, encrypted and bound to this account and pending operation. */
    fun rememberPrexLink(url: String): Boolean {
        val scope = account ?: return false
        val payment = pending ?: return false
        if (payment.provider != "1033" || !PaymentPolicy.prexLink(url)) return false
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, resumeKey())
            cipher.updateAAD(resumeContext(scope, payment))
            val encrypted = cipher.doFinal(url.toByteArray(Charsets.UTF_8))
            store.edit().putString("$scope.resume", JSONObject()
                .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).toString()).commit()
        } catch (_: Exception) { false }
    }
    val pendingPrexLink: String?
        get() {
            val scope = account ?: return null
            val payment = pending ?: return null
            if (payment.provider != "1033") return null
            val raw = store.getString("$scope.resume", null) ?: return null
            return try {
                val value = JSONObject(raw)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, resumeKey(), GCMParameterSpec(128, Base64.decode(value.getString("iv"), Base64.NO_WRAP)))
                cipher.updateAAD(resumeContext(scope, payment))
                String(cipher.doFinal(Base64.decode(value.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8)
                    .takeIf(PaymentPolicy::prexLink)
            } catch (_: Exception) { null } // Losing the link never clears the duplicate-payment guard.
        }
    fun acknowledgePayment(): Boolean = account?.let { store.edit().remove("$it.pending").remove("$it.resume").commit() } ?: false
    fun forgetAll() {
        val edit = store.edit()
        store.all.keys.filter { it.endsWith(".card") || it.endsWith(".provider") || it.endsWith(".resume") }.forEach { edit.remove(it) }
        edit.apply(); account = null
    }
}
