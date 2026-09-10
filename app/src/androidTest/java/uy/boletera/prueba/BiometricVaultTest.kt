package uy.boletera.prueba

import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Opt-in integration test. Authenticate only with an enrolled emulator finger; no STM requests. */
@RunWith(AndroidJUnit4::class)
class BiometricVaultTest {
    private fun phase(name: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("vaultPhase", name) })
    }

    @Test fun encryptedRoundTripCancellationAndForget() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("biometricProbe") == "true")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vault: AccessVault
            scenario.onActivity { activity ->
                vault = AccessVault(activity)
                assertTrue("Enroll an emulator fingerprint before running this test", vault.available)
                vault.forget()
            }
            try {
                val saved = CountDownLatch(1)
                var saveOk = false
                scenario.onActivity {
                    vault.save("00000000", "synthetic-biometric-password") { ok -> saveOk = ok; saved.countDown() }
                }
                phase("SAVE_TOUCH")
                assertTrue("Save callback timed out", saved.await(90, TimeUnit.SECONDS))
                assertTrue("Biometric encryption failed", saveOk)
                scenario.onActivity { activity ->
                    assertTrue(vault.exists)
                    val persisted = activity.getSharedPreferences("access_vault", 0).all
                    assertEquals(setOf("iv", "ciphertext"), persisted.keys)
                    assertFalse(persisted.toString().contains("synthetic-biometric-password"))
                    assertFalse(persisted.toString().contains("00000000"))
                    vault.cancel()
                }
                scenario.recreate()
                val unlocked = CountDownLatch(1)
                var restoredDocument: String? = null
                var restoredPassword: String? = null
                scenario.onActivity { activity ->
                    vault = AccessVault(activity)
                    assertTrue(vault.exists)
                    vault.unlock { document, password ->
                        restoredDocument = document; restoredPassword = password; unlocked.countDown()
                    }
                }
                phase("UNLOCK_TOUCH")
                assertTrue("Unlock callback timed out", unlocked.await(90, TimeUnit.SECONDS))
                assertEquals("00000000", restoredDocument)
                assertEquals("synthetic-biometric-password", restoredPassword)

                val cancelled = CountDownLatch(1)
                var cancellationReleasedCredentials = false
                scenario.onActivity {
                    vault.unlock { document, password ->
                        cancellationReleasedCredentials = document != null || password != null
                        cancelled.countDown()
                    }
                }
                phase("CANCEL_PROMPT")
                assertTrue("Cancellation callback timed out", cancelled.await(90, TimeUnit.SECONDS))
                assertFalse("Cancellation released credentials", cancellationReleasedCredentials)
                scenario.onActivity {
                    assertTrue("Cancellation must preserve saved access", vault.exists)
                    vault.forget()
                    assertFalse(vault.exists)
                    var called = false
                    vault.unlock { document, password ->
                        called = true; assertNull(document); assertNull(password)
                    }
                    assertTrue(called)
                }
                phase("COMPLETE")
            } finally {
                scenario.onActivity { vault.forget() }
            }
        }
    }
}
