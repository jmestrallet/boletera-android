package uy.boletera.prueba

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JourneyPreferencesTest {
    @Test fun prexLinkSurvivesRestartEncryptedAndIsBoundToAccountAndOperation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = JourneyPreferences(context)
        val store = context.getSharedPreferences("journey_choices", 0)
        val link = "https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-RESUME-TOKEN"
        try {
            first.useAccount("33333333"); first.acknowledgePayment()
            assertFalse(first.rememberPrexLink(link))
            assertTrue(first.beginPayment(PendingPayment("TEST-A", "1033", 26000, 1234)))
            assertFalse(first.rememberPrexLink("https://example.invalid/?id=SYNTHETIC"))
            assertTrue(first.rememberPrexLink(link))
            val restored = JourneyPreferences(context).apply { useAccount("33333333") }
            assertEquals(link, restored.pendingPrexLink)
            assertFalse(store.all.toString().contains("SYNTHETIC-RESUME-TOKEN"))
            assertFalse(store.all.toString().contains("pasarelaspe"))
            val resumeA = store.all.keys.single { it.endsWith(".resume") }
            val cipherA = store.getString(resumeA, null)
            restored.useAccount("44444444"); restored.acknowledgePayment()
            assertTrue(restored.beginPayment(PendingPayment("TEST-A", "1033", 26000, 1234)))
            assertTrue(restored.rememberPrexLink(link))
            val resumeB = store.all.keys.single { it.endsWith(".resume") && it != resumeA }
            store.edit().putString(resumeB, cipherA).commit()
            assertNull(restored.pendingPrexLink) // Same operation metadata in another account cannot decrypt it.
            assertNotNull(restored.pending)
            restored.acknowledgePayment()
            first.acknowledgePayment()
            assertNull(first.pendingPrexLink)
            assertTrue(first.beginPayment(PendingPayment("TEST-A", "1033", 26000, 5678)))
            store.edit().putString(resumeA, cipherA).commit()
            assertNull(first.pendingPrexLink) // A new operation cannot reuse old encrypted state.
            assertNotNull(first.pending)
            assertTrue(first.rememberPrexLink(link))
            store.edit().putString(resumeA, "damaged").commit()
            assertNull(first.pendingPrexLink)
            assertNotNull(first.pending)
            assertTrue(first.rememberPrexLink(link))
            first.forgetAll(); first.useAccount("33333333")
            assertNull(first.pendingPrexLink)
            assertNotNull(first.pending)
            first.acknowledgePayment()
            assertTrue(first.beginPayment(PendingPayment("TEST-A", "1002", 26000, 9012)))
            assertFalse(first.rememberPrexLink(link))
        } finally {
            first.useAccount("33333333"); first.acknowledgePayment()
            first.useAccount("44444444"); first.acknowledgePayment(); first.forgetAll()
        }
    }

    @Test fun choicesSurviveRecreationStayAccountScopedAndCanBeForgotten() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = JourneyPreferences(context)
        first.forgetAll()
        try {
            first.useAccount("11111111")
            first.card = "TESTCARD"
            first.provider = "1033"
            assertTrue(first.beginPayment(PendingPayment("TESTCARD", "1033", 26000, 1)))
            assertFalse(first.beginPayment(PendingPayment("TESTCARD", "1033", 26000, 2)))
            val restored = JourneyPreferences(context)
            restored.useAccount("11111111")
            assertEquals("TESTCARD", restored.card)
            assertEquals("1033", restored.provider)
            assertEquals(26000L, restored.pending?.amount)
            restored.useAccount("22222222")
            assertNull(restored.card)
            assertNull(restored.provider)
            assertNull(restored.pending)
            val raw = context.getSharedPreferences("journey_choices", 0).all.toString()
            assertFalse(raw.contains("11111111"))
            restored.forgetAll()
            first.useAccount("11111111")
            assertNull(first.card)
            assertNull(first.provider)
            assertNotNull(first.pending) // Forgetting an access never cancels/forgets a pending bank operation.
            assertTrue(first.acknowledgePayment())
            assertNull(first.pending)
        } finally { first.useAccount("11111111"); first.acknowledgePayment(); first.forgetAll() }
    }
}
