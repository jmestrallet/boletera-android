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
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(true)) }
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
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, iv)) }
            authorize(cipher, "Entrar a STM con huella") { authorized ->
                if (authorized == null) result(null, null)
                else try {
                    val bytes = authorized.doFinal(encrypted)
                    try { val data = JSONObject(String(bytes, Charsets.UTF_8)); result(data.getString("document"), data.getString("password")) }
                    finally { bytes.fill(0) }
                } catch (_: Exception) { result(null, null) }
            }
        } catch (_: Exception) { result(null, null) }
    }

    fun forget() {
        prompt?.cancelAuthentication()
        prefs.edit().clear().commit()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) }
    }
    fun cancel() { prompt?.cancelAuthentication(); prompt = null }
}
