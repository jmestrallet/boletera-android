package uy.boletera.prueba

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only ciphertext + random IV persist. Key use is cryptographically bound to EACH biometric prompt. */
class AccessVault(private val activity: FragmentActivity) {
    private val prefs = activity.getSharedPreferences("access_vault", Context.MODE_PRIVATE)
    private val alias = "boletera.access.v1"
    private var prompt: BiometricPrompt? = null
    val exists get() = prefs.contains("ciphertext") && prefs.contains("iv")
    /** Set when Android dropped the key (a fingerprint was added or removed, or the lock screen changed) or the saved access could not be read; it was cleared. */
    var invalidated = false
        private set
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
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(spec.build()) }.generateKey()
    }

    /** The saved key, or null when Android no longer has it (lock screen removed or changed): the saved access can never open again. */
    private fun existingKey(): SecretKey? = try {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getKey(alias, null) as? SecretKey
    } catch (_: java.security.UnrecoverableKeyException) { null }

    private fun authorize(cipher: Cipher, title: String, result: (Cipher?) -> Unit) {
        if (!available) { result(null); return }
        prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(authentication: BiometricPrompt.AuthenticationResult) {
                prompt = null
                result(authentication.cryptoObject?.cipher)
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { prompt = null; result(null) }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title)
            .setSubtitle("Autoriza el acceso guardado a STM en este celular")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancelar").build()
        prompt?.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    fun save(document: String, password: String, result: (Boolean) -> Unit) {
        val bytes = JSONObject().put("document", document).put("password", password).toString().toByteArray(Charsets.UTF_8)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            // A key dropped by a fingerprint change can never encrypt again: replace it with a fresh one.
            try { cipher.init(Cipher.ENCRYPT_MODE, key(true)) }
            // The access sealed with the old key can no longer be opened by the new one: it goes too, so a cancelled save leaves nothing broken.
            catch (_: android.security.keystore.KeyPermanentlyInvalidatedException) { if (IsLab) prefs.edit().clear().commit(); deleteKey(); cipher.init(Cipher.ENCRYPT_MODE, key(true)) }
            invalidated = false
            authorize(cipher, "Guardar acceso con huella") { authorized ->
                try {
                    if (authorized == null) result(false)
                    else {
                        val encrypted = authorized.doFinal(bytes)
                        result(prefs.edit().putString("iv", Base64.encodeToString(authorized.iv, Base64.NO_WRAP))
                            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
                    }
                } catch (_: Exception) { result(false) }
                finally { bytes.fill(0) }
            }
        } catch (_: Exception) { bytes.fill(0); result(false) }
    }

    fun unlock(result: (String?, String?) -> Unit) {
        try {
            val iv = Base64.decode(prefs.getString("iv", null), Base64.NO_WRAP)
            val encrypted = Base64.decode(prefs.getString("ciphertext", null), Base64.NO_WRAP)
            // Without its key (the phone's lock screen was removed or changed) the saved access can never open again:
            // it is forgotten and said once, instead of a fingerprint that fails at every opening.
            val secret: SecretKey? = if (IsLab) existingKey() else key(false)
            if (secret == null) { forget(); invalidated = true; result(null, null); return }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            try { cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, iv)) }
            catch (_: android.security.keystore.KeyPermanentlyInvalidatedException) { forget(); invalidated = true; result(null, null); return }
            authorize(cipher, "Entrar a STM con huella") { authorized ->
                if (authorized == null) result(null, null)
                else {
                    // The answer is given outside the try: an error inside the caller's own code must not read as a failed unlock.
                    var document: String? = null; var password: String? = null; var unreadable = false
                    try {
                        val bytes = authorized.doFinal(encrypted)
                        try { val data = JSONObject(String(bytes, Charsets.UTF_8)); document = data.getString("document"); password = data.getString("password") }
                        finally { bytes.fill(0) }
                    } catch (_: javax.crypto.BadPaddingException) { unreadable = true }
                    catch (_: org.json.JSONException) { unreadable = true }
                    catch (_: Exception) {}
                    // A saved access that decrypts to nothing (sealed with another key) would fail at every opening.
                    if (unreadable && IsLab) { forget(); invalidated = true }
                    result(document, password)
                }
            }
        } catch (_: Exception) { result(null, null) }
    }

    fun forget() {
        prompt?.cancelAuthentication()
        prefs.edit().clear().commit()
        deleteKey()
    }
    private fun deleteKey() { KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) } }
    fun cancel() { prompt?.cancelAuthentication(); prompt = null }
}
