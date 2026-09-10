package uy.boletera.prueba

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class JourneyPreferencesTest {
    @Test fun retiredReviewMarkersAreRemovedWhileFavoritesSurvive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val choices = JourneyPreferences(context).apply { useAccount("11111111") }
        choices.card = "TESTCARD"; choices.provider = "1033"; choices.payerProfileId = "test-payer"
        val store = context.getSharedPreferences("journey_choices", 0)
        store.edit().putString("legacy.pending", "broken-or-abandoned").putString("legacy.resume", "old-link").commit()
        val restored = JourneyPreferences(context).apply { useAccount("11111111") }
        try {
            assertFalse(store.all.keys.any { it.endsWith(".pending") || it.endsWith(".resume") })
            assertEquals("TESTCARD", restored.card)
            assertEquals("1033", restored.provider)
            assertEquals("test-payer", restored.payerProfileId)
            restored.useAccount("22222222")
            assertNull(restored.card)
            assertNull(restored.provider)
        } finally { restored.forgetAll() }
    }
}
