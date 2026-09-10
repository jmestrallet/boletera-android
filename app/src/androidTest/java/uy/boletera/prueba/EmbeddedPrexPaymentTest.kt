package uy.boletera.prueba

import android.webkit.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONTokener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Local intercepted HTML only: no payment provider or account is contacted. */
@RunWith(AndroidJUnit4::class)
class EmbeddedPrexPaymentTest {
    @Test fun reopeningKeepsThePageAndNeverReplaysASubmittedForm() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val loads = AtomicInteger()
        val completed = CountDownLatch(1)
        lateinit var payment: EmbeddedPrexPayment
        val link = "https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-RETAINED"
        val payer = PayerProfile("retained-payer", "Prex de prueba", "Persona", "Ficticia", "00000000", "persona@example.invalid", "000000000")
        instrumentation.runOnMainSync {
            payment = EmbeddedPrexPayment(instrumentation.targetContext)
            val delegate = payment.web.webViewClient
            payment.web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    loads.incrementAndGet()
                    val html = """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><title>Laboratorio de pago</title>
                      <form id="lab"><input id="titular" formcontrolname="nombreControl"><input formcontrolname="documentoControl"><input formcontrolname="correoControl">
                      <input formcontrolname="nroTarjetaControl"><input formcontrolname="expiracionControl"><input formcontrolname="cvvControl"><button>Continuar ficticio</button></form>
                      <script>window.sent=0;document.querySelector('form').onsubmit=e=>{e.preventDefault();window.sent++;document.title='Enviado una vez';}</script>"""
                    return WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
                }
                override fun onPageStarted(view: WebView, url: String?, icon: android.graphics.Bitmap?) = delegate.onPageStarted(view, url, icon)
                override fun onPageFinished(view: WebView, url: String?) { delegate.onPageFinished(view, url); completed.countDown() }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = delegate.shouldOverrideUrlLoading(view, request)
            }
            assertFalse(payment.open("https://otro.example/v2/confirmarPago?id=x"))
            assertTrue(payment.open(link, payer))
        }
        fun js(source: String): String {
            val latch = CountDownLatch(1)
            val result = AtomicReference<String>()
            instrumentation.runOnMainSync { payment.web.evaluateJavascript(source) { result.set(it); latch.countDown() } }
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            return result.get()
        }
        try {
            assertTrue(completed.await(10, TimeUnit.SECONDS))
            assertEquals("true", js("!!window.BoleteraNative && !!window.BoleteraVerification"))
            assertEquals("\"Persona Ficticia\"", js("document.querySelector('#titular').value"))
            assertEquals("\"00000000\"", js("document.querySelector('[formcontrolname=documentoControl]').value"))
            assertEquals("\"\"", js("document.querySelector('[formcontrolname=nroTarjetaControl]').value"))
            js("document.querySelector('#titular').value='Persona ficticia';document.querySelector('#lab').requestSubmit();")
            val requestsBefore = loads.get()
            instrumentation.runOnMainSync {
                repeat(3) { assertTrue(payment.open(link, payer)) }
                assertFalse(payment.open(link, payer.copy(id = "another-payer")))
                assertFalse(payment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=ANOTHER-SYNTHETIC"))
            }
            assertEquals("Persona ficticia", JSONTokener(js("document.querySelector('#titular').value")).nextValue())
            assertEquals("1", js("window.sent"))
            assertEquals(requestsBefore, loads.get())
            assertEquals("\"Enviado una vez\"", js("document.title"))
            instrumentation.runOnMainSync {
                assertEquals(android.view.View.IMPORTANT_FOR_AUTOFILL_YES, payment.web.importantForAutofill)
                assertFalse(payment.web.settings.userAgentString.contains("; wv"))
            }
            val conflicting = "<form><input formcontrolname='nombreControl' value='Otro titular'><input formcontrolname='documentoControl'><input formcontrolname='correoControl'><input formcontrolname='nroTarjetaControl'><input formcontrolname='expiracionControl'><input formcontrolname='cvvControl' value='FICTICIO'></form>"
            js("document.querySelector('form').outerHTML=${org.json.JSONObject.quote(conflicting)}")
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            var noticed = false
            while (!noticed && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync { noticed = payment.payerConflict }
                if (!noticed) Thread.sleep(50)
            }
            assertTrue("La app debe advertir los datos preexistentes distintos", noticed)
            assertEquals("\"Otro titular\"", js("document.querySelector('[formcontrolname=nombreControl]').value"))
            instrumentation.runOnMainSync { payment.applyChosenPayer() }
            assertEquals("\"Persona Ficticia\"", js("document.querySelector('[formcontrolname=nombreControl]').value"))
            assertEquals("\"FICTICIO\"", js("document.querySelector('[formcontrolname=cvvControl]').value"))
        } finally { instrumentation.runOnMainSync { payment.destroy() } }
    }
}
