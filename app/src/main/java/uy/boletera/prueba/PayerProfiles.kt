package uy.boletera.prueba

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Identifies a payer profile chosen by the user, never a verified bank card or its credentials. */
data class PayerProfile(
    val id: String, val label: String, val givenName: String, val familyName: String,
    val document: String, val email: String, val phone: String,
    val documentType: String = "CI"
) {
    fun valid() = Regex("[A-Za-z0-9-]{1,80}").matches(id) &&
        listOf(label, givenName, familyName).all { it.isNotBlank() && it.length <= 80 } &&
        (if (documentType == "CI") Regex("[0-9]{7,8}").matches(document)
        else documentType == "PAS" && document.isNotBlank() && document.length <= 40 && document.none { it.isISOControl() }) && email.length <= 120 &&
        Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email) && Regex("[0-9]{9,15}").matches(phone)
    fun json() = JSONObject().put("id", id).put("label", label).put("givenName", givenName)
        .put("familyName", familyName).put("document", document).put("email", email).put("phone", phone).put("documentType", documentType)
    companion object {
        fun from(value: JSONObject) = PayerProfile(value.getString("id"), value.getString("label"),
            value.getString("givenName"), value.getString("familyName"), value.getString("document"),
            value.getString("email"), value.getString("phone"), value.optString("documentType", "CI")).also { require(it.valid()) }
    }
}

/** Encrypts ordinary payer details locally. Card number, expiration and CVV have no fields here. */
class PayerProfiles(context: Context, private val storageName: String = "payer_profiles") {
    private val store = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)
    private val alias = "boletera_payers_$storageName"
    private val aad = "boletera-payer-profiles:v1:$storageName".toByteArray(Charsets.UTF_8)
    private fun key(): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (keys.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    fun read(): List<PayerProfile> {
        val raw = store.getString("encrypted", null) ?: return emptyList()
        val value = JSONObject(raw)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(value.getString("iv"), Base64.NO_WRAP)))
        cipher.updateAAD(aad)
        val rows = JSONArray(String(cipher.doFinal(Base64.decode(value.getString("data"), Base64.NO_WRAP)), Charsets.UTF_8))
        require(rows.length() <= 20)
        return (0 until rows.length()).map { PayerProfile.from(rows.getJSONObject(it)) }.also { profiles ->
            require(profiles.map { it.id }.distinct().size == profiles.size)
        }
    }
    fun write(profiles: List<PayerProfile>): Boolean {
        require(profiles.size <= 20 && profiles.all { it.valid() } && profiles.map { it.id }.distinct().size == profiles.size)
        // Do not replace unreadable existing records with a new, silently empty collection.
        read()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(aad)
        val encrypted = cipher.doFinal(JSONArray(profiles.map { it.json() }).toString().toByteArray(Charsets.UTF_8))
        return store.edit().putString("encrypted", JSONObject()
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).toString()).commit()
    }
    fun forget(): Boolean = store.edit().clear().commit()
}
