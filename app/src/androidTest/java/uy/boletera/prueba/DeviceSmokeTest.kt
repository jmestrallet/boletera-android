package uy.boletera.prueba

import android.graphics.Bitmap
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class DeviceSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun nativeWelcomeHasNoOfficialPageAndStartsWithoutSavedSecrets() {
        compose.onNodeWithText("Tu próxima carga,\nsin las vueltas.").assertIsDisplayed()
        compose.onNodeWithText("Documento uruguayo").assertIsDisplayed()
        compose.runOnIdle {
            val vault = AccessVault(compose.activity)
            assertFalse(vault.exists)
            assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
    }

    @Test fun actualAndroidWebViewReadsOfflineFixtureAndNeverSubmitsPayment() {
        val ready = CountDownLatch(1)
        val result = AtomicReference<String>()
        var browser: WebView? = null
        compose.runOnIdle {
            browser = WebView(compose.activity).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        val script = compose.activity.assets.open("stm-adapter.js").bufferedReader().use { it.readText() }
                        view.evaluateJavascript(script + "\nJSON.stringify(window.BoleteraAdapter.snapshot())") {
                            result.set(it); ready.countDown()
                        }
                    }
                    override fun shouldInterceptRequest(view: WebView, request: android.webkit.WebResourceRequest) =
                        android.webkit.WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                }
                loadDataWithBaseURL("https://stm.gub.uy/app/mistm/cuenta/pages/recarga1.xhtml", """
                    <html><body><p>Tu recarga mínima deberá ser de ${'$'} 564 .</p><label>Saldo actual *</label>
                    <input id="recarga1:saldoActual" value="${'$'} -304"><input id="recarga1:monto_input"><button>CONTINUAR</button></body></html>
                """.trimIndent(), "text/html", "UTF-8", null)
            }
        }
        assertTrue("WebView did not complete fixture", ready.await(20, TimeUnit.SECONDS))
        val snapshot = org.json.JSONObject(org.json.JSONTokener(result.get()).nextValue() as String)
        assertEquals("amount", snapshot.getString("stage"))
        assertEquals(56400, snapshot.getLong("minimum"))
        assertEquals(-30400, snapshot.getLong("balance"))
        compose.runOnIdle { browser?.destroy() }
    }

    @Test fun unsupportedBiometricsDoNotSavePlaintextFallback() {
        compose.runOnIdle {
            val vault = AccessVault(compose.activity)
            org.junit.Assume.assumeFalse(vault.available)
            var called = false
            vault.save("00000000", "synthetic-not-a-real-password") { saved -> called = true; assertFalse(saved) }
            assertTrue(called)
            assertFalse(vault.exists)
            assertTrue(compose.activity.getSharedPreferences("access_vault", 0).all.isEmpty())
        }
    }

    @Test fun captureEmptyNativeWelcomeForVisualReview() {
        // Isolated test only: no credentials/account data exist. Production always uses FLAG_SECURE.
        compose.runOnIdle { compose.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), "welcome-test.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.runOnIdle { compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
