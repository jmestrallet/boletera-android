package uy.boletera.prueba

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** One unresolved operation per account. No card credentials, cookies or replayable payment URL. */
internal data class PaymentRecord(
    val card: String, val provider: String, val amount: Long, val startedAt: Long,
    val balanceBefore: Long?, val phase: String = "authorizing", val transaction: String = ""
) {
    fun json() = JSONObject().put("card",card).put("provider",provider).put("amount",amount)
        .put("startedAt",startedAt).put("balanceBefore",balanceBefore ?: JSONObject.NULL)
        .put("phase",phase).put("transaction",transaction)
    companion object {
        fun from(json: JSONObject) = PaymentRecord(json.getString("card"),json.getString("provider"),
            json.getLong("amount"),json.getLong("startedAt"),if(json.isNull("balanceBefore"))null else json.getLong("balanceBefore"),
            json.getString("phase"),json.optString("transaction")).also {
                require(it.amount>0 && it.phase in setOf("reviewing","authorizing","pending","confirmed","credited","rejected"))
            }
    }
}

internal class PaymentJournal(context: Context) {
    private val store=context.getSharedPreferences("payment_journal",Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val keys=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        return keys.getKey("boletera_payment_journal",null) as? SecretKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("boletera_payment_journal",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generateKey()
        }
    }
    fun read(account: String): PaymentRecord? {
        val raw=store.getString(account,null) ?: return null
        val json=JSONObject(raw)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(json.getString("iv"),Base64.NO_WRAP)))
        cipher.updateAAD(account.toByteArray())
        return PaymentRecord.from(JSONObject(String(cipher.doFinal(Base64.decode(json.getString("data"),Base64.NO_WRAP)),Charsets.UTF_8)))
    }
    fun write(account: String, record: PaymentRecord): Boolean = runCatching {
        read(account) // Never overwrite an unreadable record.
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());cipher.updateAAD(account.toByteArray())
        val json=JSONObject().put("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP))
            .put("data",Base64.encodeToString(cipher.doFinal(record.json().toString().toByteArray()),Base64.NO_WRAP))
        store.edit().putString(account,json.toString()).commit()
    }.getOrDefault(false)
    fun clear(account: String): Boolean = store.edit().remove(account).commit()
}
