package uy.boletera.prueba

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
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
        return store.edit().putString("$scope.pending", JSONObject().put("card", value.card).put("provider", value.provider)
            .put("amount", value.amount).put("createdAt", value.createdAt).toString()).commit()
    }
    fun acknowledgePayment(): Boolean = account?.let { store.edit().remove("$it.pending").commit() } ?: false
    fun forgetAll() {
        val edit = store.edit()
        store.all.keys.filter { it.endsWith(".card") || it.endsWith(".provider") }.forEach { edit.remove(it) }
        edit.apply(); account = null
    }
}
