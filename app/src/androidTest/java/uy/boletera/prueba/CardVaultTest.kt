package uy.boletera.prueba

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The card vault with a real fingerprint prompt. Needs an emulator with a fingerprint enrolled; the host touches the
 * sensor (`adb emu finger touch 1`) while it runs. A sample card only: never a real one.
 */
@RunWith(AndroidJUnit4::class)
class CardVaultTest {
    @Test fun savesOpensAndForgetsOnlyWithTheFingerprint() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vault: CardVault
            scenario.onActivity { vault = CardVault(it); vault.forget() }
            val saved = CountDownLatch(1); var ok = false
            scenario.onActivity { vault.save("4517660012348842", "08/29", "123") { ok = it; saved.countDown() } }
            assertTrue("no fingerprint for saving", saved.await(40, TimeUnit.SECONDS))
            assertTrue(ok)
            scenario.onActivity { assertTrue(vault.exists); assertEquals("···· 8842", vault.label) }
            // What is kept in clear: the ciphertext and the last four digits, never the number, expiry or code.
            val prefs = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences("card_vault", android.content.Context.MODE_PRIVATE).all.values.joinToString(" ")
            assertFalse(prefs.contains("4517660012348842")); assertFalse(prefs.contains("08/29")); assertFalse(prefs.contains("\"123\""))
            val opened = CountDownLatch(1); var card: SavedCard? = null
            scenario.onActivity { vault.unlock { card = it; opened.countDown() } }
            assertTrue("no fingerprint for opening", opened.await(40, TimeUnit.SECONDS))
            assertEquals("4517660012348842", card?.pan); assertEquals("08/29", card?.expiry); assertEquals("123", card?.cvv)
            scenario.onActivity { vault.forget(); assertFalse(vault.exists); assertNull(vault.label) }
        }
    }
    /** Leaves a sample card saved (for trying Express in the demo by hand); run only when asked. */
    @Test fun keepSampleCardForDemo() {
        if (androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("keepCard") != "1") return
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val saved = CountDownLatch(1); var ok = false
            scenario.onActivity { CardVault(it).save("4517660012348842", "08/29", "123") { r -> ok = r; saved.countDown() } }
            assertTrue(saved.await(40, TimeUnit.SECONDS)); assertTrue(ok)
        }
    }
}
