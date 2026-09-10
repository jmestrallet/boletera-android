package uy.boletera.prueba

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class AutomaticUnlockTest {
    private fun engine(activity: MainActivity) = MainActivity::class.java.getDeclaredField("engine").apply { isAccessible = true }.get(activity) as StmEngine

    @Test fun savedAccessIsAttemptedOncePerLaunchAndFailureDoesNotLoop() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("access_vault", 0)
        // Intentionally unreadable fixture: exercises automatic invocation without a fingerprint or network login.
        assertTrue(prefs.edit().putString("iv", "invalid-fixture").putString("ciphertext", "invalid-fixture").commit())
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { activity ->
                    assertTrue(engine(activity).state.message.startsWith("No se desbloqueó"))
                    assertEquals("welcome", engine(activity).state.stage)
                    engine(activity).dismissNotice(engine(activity).state.message)
                    engine(activity).logout()
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { assertFalse(engine(it).state.message.startsWith("No se desbloqueó")) }
                scenario.recreate()
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { assertFalse(engine(it).state.message.startsWith("No se desbloqueó")) }
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity { assertTrue(engine(it).state.message.startsWith("No se desbloqueó")) }
            }
            assertEquals("invalid-fixture", prefs.getString("ciphertext", null))
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun noSavedAccessLeavesManualLoginUntouched() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("access_vault", 0)
        prefs.edit().clear().commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                assertFalse(engine(it).state.hasSavedAccess)
                assertEquals("", engine(it).state.message)
                assertEquals("welcome", engine(it).state.stage)
            }
        }
    }
}
