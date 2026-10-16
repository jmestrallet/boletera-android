package uy.boletera.prueba

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** A card read from the vault, held only for the few seconds of one Express payment. */
internal class SavedCard(val pan: String, val expiry: String, val cvv: String)

/**
 * The card for Carga Express (the owner's choice, 16/10: «mínima interacción»). Number, expiry and code are encrypted
 * with a key that lives in the phone's secure hardware and opens only with a fingerprint, each time (like
 * [AccessVault]); a fingerprint added or removed drops it. Only the last four digits and the expiry are kept in clear,
 * to say which card it is. «Olvidar este celular» and Configuración erase it.
 */
internal class CardVault(private val activity: FragmentActivity) {
    private val prefs = activity.getSharedPreferences("card_vault", Context.MODE_PRIVATE)
    private val alias = "boletera.card.v1"
    private var prompt: BiometricPrompt? = null
    /** «···· 8842» (only the last four digits are ever kept in clear), or null with no card saved. Observable. */
    var label by mutableStateOf(prefs.getString("label", null)); private set
    val exists get() = prefs.contains("ciphertext") && prefs.contains("iv")
    val available get() = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create)
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        else @Suppress("DEPRECATION") spec.setUserAuthenticationValidityDurationSeconds(-1)
        // Only with the phone unlocked, and in the dedicated security chip (StrongBox) when the phone has one.
        if (Build.VERSION.SDK_INT >= 28) spec.setUnlockedDeviceRequired(true)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        if (Build.VERSION.SDK_INT >= 28 && activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
            try { generator.init(spec.setIsStrongBoxBacked(true).build()); return generator.generateKey() }
            catch (_: Exception) { spec.setIsStrongBoxBacked(false) }
        }
        generator.init(spec.build()); return generator.generateKey()
    }

    private fun authorize(cipher: Cipher, title: String, subtitle: String, result: (Cipher?) -> Unit) {
        if (!available) { result(null); return }
        prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(authentication: BiometricPrompt.AuthenticationResult) { prompt = null; result(authentication.cryptoObject?.cipher) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { prompt = null; result(null) }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).setNegativeButtonText("Cancelar").build()
        prompt?.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    /** [expiry] as typed («MM/AA»). */
    fun save(pan: String, expiry: String, cvv: String, result: (Boolean) -> Unit) {
        val bytes = JSONObject().put("pan", pan).put("expiry", expiry).put("cvv", cvv).toString().toByteArray(Charsets.UTF_8)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            try { cipher.init(Cipher.ENCRYPT_MODE, key(true)) }
            catch (_: android.security.keystore.KeyPermanentlyInvalidatedException) { deleteKey(); cipher.init(Cipher.ENCRYPT_MODE, key(true)) }
            authorize(cipher, "Guardar tarjeta para Carga Express", "Se guarda cifrada y solo se abre con tu huella") { authorized ->
                try {
                    if (authorized == null) result(false)
                    else {
                        val encrypted = authorized.doFinal(bytes)
                        val shown = "···· ${pan.takeLast(4)}"
                        val ok = prefs.edit().putString("iv", Base64.encodeToString(authorized.iv, Base64.NO_WRAP))
                            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)).putString("label", shown).commit()
                        if (ok) label = shown
                        result(ok)
                    }
                } catch (_: Exception) { result(false) } finally { bytes.fill(0) }
            }
        } catch (_: Exception) { bytes.fill(0); result(false) }
    }

    fun unlock(result: (SavedCard?) -> Unit) {
        try {
            val iv = Base64.decode(prefs.getString("iv", null), Base64.NO_WRAP)
            val encrypted = Base64.decode(prefs.getString("ciphertext", null), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            try { cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, iv)) }
            catch (_: android.security.keystore.KeyPermanentlyInvalidatedException) { forget(); result(null); return }
            authorize(cipher, "Carga Express", "Confirmá con tu huella para pagar con tu tarjeta guardada") { authorized ->
                if (authorized == null) result(null)
                else try {
                    val bytes = authorized.doFinal(encrypted)
                    try { val d = JSONObject(String(bytes, Charsets.UTF_8)); result(SavedCard(d.getString("pan"), d.getString("expiry"), d.getString("cvv"))) }
                    finally { bytes.fill(0) }
                } catch (_: Exception) { result(null) }
            }
        } catch (_: Exception) { result(null) }
    }

    fun forget() {
        prompt?.cancelAuthentication()
        prefs.edit().clear().commit(); label = null
        deleteKey()
    }
    private fun deleteKey() { KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) } }
}

/** The vault for the screens, and the card opened for the Express in course (cleared as soon as it is sent). */
internal object LabExpressCard {
    var vault: CardVault? = null
    var pending: SavedCard? = null
}
