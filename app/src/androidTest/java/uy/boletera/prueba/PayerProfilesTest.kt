package uy.boletera.prueba

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PayerProfilesTest {
    @Test fun encryptedProfilesRemainSeparateAndUnreadableDataIsNeverOverwritten() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "payer_profiles_instrumentation"
        val records = PayerProfiles(context, name)
        records.forget()
        val first = PayerProfile("test-one", "Prex ficticia uno", "Persona", "Ficticia", "00000000", "one@example.invalid", "000000000")
        val second = first.copy(id = "test-two", label = "Prex ficticia dos", document = "11111111", email = "two@example.invalid")
        try {
            assertTrue(records.write(listOf(first, second)))
            assertEquals(listOf(first, second), PayerProfiles(context, name).read())
            val rawStore = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val encrypted = rawStore.getString("encrypted", "")!!
            assertFalse(encrypted.contains("example.invalid"))
            assertFalse(encrypted.contains("Prex ficticia"))
            val damaged = JSONObject(encrypted).put("data", "AAAA").toString()
            assertTrue(rawStore.edit().putString("encrypted", damaged).commit())
            try { records.write(listOf(first)); fail("No debe sobrescribir los perfiles ilegibles") } catch (_: Exception) { }
            assertEquals(damaged, rawStore.getString("encrypted", null))
        } finally {
            records.forget()
            context.deleteSharedPreferences(name)
            java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("boletera_payers_$name") }
        }
    }

}
