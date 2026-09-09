package uy.boletera.prueba

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JourneyPreferencesTest {
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
