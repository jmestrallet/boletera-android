package uy.boletera.prueba

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/** Local choices, separated by account without persisting the login document. */
class JourneyPreferences(context: Context) {
    private val store = context.getSharedPreferences("journey_choices", Context.MODE_PRIVATE)
    init {
        // Retire only the old local review markers; these never represented confirmed payments.
        val edit = store.edit()
        store.all.keys.filter { it.endsWith(".pending") || it.endsWith(".resume") }.forEach { edit.remove(it) }
        edit.apply()
    }
    private var account: String? = null
    internal val accountKey: String? get() = account
    internal fun rememberVerifiedSession() {account?.let {store.edit().putString("session_account",it).apply()}}
    internal fun restoreSessionAccount(): Boolean {
        val saved=store.getString("session_account",null)?.takeIf {it.matches(Regex("[0-9a-f]{64}"))} ?: return false
        account=saved;return true
    }
    internal fun clearSessionAccount() {store.edit().remove("session_account").apply()}
    var successfulProvider: String?
        get() = account?.let { store.getString("$it.successProvider",null) }
        set(value) {account?.let {store.edit().putString("$it.successProvider",value).apply()}}
    var successfulPayer: String?
        get() = account?.let {store.getString("$it.successPayer",null)}
        set(value) {account?.let {store.edit().putString("$it.successPayer",value).apply()}}
    var successfulCardSuffix: String?
        get() = account?.let {store.getString("$it.successCardSuffix",null)}
        set(value) {account?.let {store.edit().putString("$it.successCardSuffix",value).apply()}}
    var successfulCardIdentity: String?
        get() = account?.let {store.getString("$it.successCardIdentity",null)}
        set(value) {account?.let {store.edit().putString("$it.successCardIdentity",value).apply()}}
    internal fun cardIdentity(number: String): String? = runCatching {
        val accountKey=account ?: return null
        val keys=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        val key=keys.getKey("boletera_choices_account",null) as SecretKey
        Mac.getInstance("HmacSHA256").run {init(key);doFinal("card:$accountKey:$number".toByteArray()).joinToString("") {"%02x".format(it)}}
    }.getOrNull()
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
    var payerProfileId: String?
        get() = account?.let { store.getString("$it.payer", null) }
        set(value) { account?.let { store.edit().putString("$it.payer", value).apply() } }
    fun forgetAll() {
        val edit = store.edit()
        store.all.keys.filter { it.endsWith(".card") || it.endsWith(".provider") || it.endsWith(".resume") || it.endsWith(".payer") || it.endsWith(".express") || it.contains(".success") }.forEach { edit.remove(it) }
        edit.remove("session_account").apply(); account = null
    }
}
