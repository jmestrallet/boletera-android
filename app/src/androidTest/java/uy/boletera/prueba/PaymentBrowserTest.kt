package uy.boletera.prueba

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PaymentBrowserTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun brouRelayIsLoopbackOnlySingleUseAndNeverPostsFromThisTest() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val destination = AtomicReference<Uri>()
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent?): Instrumentation.ActivityResult? {
                if (intent?.`package` == "com.android.chrome") {
                    destination.set(intent.data)
                    return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
                }
                return null
            }
        }
        // Block the Chrome intent: only a local socket reads the synthetic HTML. No bank request.
        instrumentation.addMonitor(monitor)
        var browser: PaymentBrowser? = null
        val fields = PaymentPolicy.brouFields.associateWith { "synthetic" }.toMutableMap().apply {
            put("Monto", "26000"); put("Moneda", "98"); put("codComercio", "synthetic\"<&")
            put("urlVueltaOK", "https://spf.sistarbanc.com.uy/spfws/UrlVueltaOK")
            put("urlVueltaERROR", "https://spf.sistarbanc.com.uy/spfws/UrlVueltaERROR")
            put("urlCONTROL", "https://spf.sistarbanc.com.uy/spfe/RetornoBROU.jsp")
        }
        try {
            compose.runOnIdle {
                browser = PaymentBrowser(compose.activity)
                org.junit.Assume.assumeTrue(browser!!.available())
                assertFalse(browser!!.openBrou(PaymentPolicy.BROU_ACTION, fields, 260))
                assertTrue(browser!!.openBrou(PaymentPolicy.BROU_ACTION, fields, 26000))
            }
            val url = destination.get()
            assertNotNull(url)
            assertEquals("127.0.0.1", url.host)
            fun get(path: String): String = Socket("127.0.0.1", url.port).use { socket ->
                socket.soTimeout = 5000
                socket.getOutputStream().write("GET $path HTTP/1.1\r\nHost: 127.0.0.1:${url.port}\r\nConnection: close\r\n\r\n".toByteArray())
                socket.getInputStream().bufferedReader().readText()
            }
            assertTrue(get("/wrong-token").startsWith("HTTP/1.1 404"))
            val response = get(url.path!!)
            assertTrue(response.startsWith("HTTP/1.1 200"))
            assertTrue(response.contains("Cache-Control: no-store"))
            assertTrue(response.contains("synthetic&quot;&lt;&amp;"))
            assertTrue(response.contains("action=\"${PaymentPolicy.BROU_ACTION}\""))
            try { get(url.path!!); fail("Relay accepted another request") } catch (_: java.io.IOException) { }
        } finally {
            instrumentation.removeMonitor(monitor)
            browser?.close()
        }
    }
}
