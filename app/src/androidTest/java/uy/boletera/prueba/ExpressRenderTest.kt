package uy.boletera.prueba

import android.graphics.Bitmap
import android.webkit.*
import androidx.activity.compose.setContent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** All network requests return a synthetic, delayed payer form. No real payment. */
class ExpressRenderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var payment: EmbeddedPrexPayment
    private val url = "https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-RENDER"
    private val payer = PayerProfile("render", "Prueba", "Persona", "Ficticia", "00000000", "persona@example.invalid", "099123456")
    private fun js(script: String): String {
        val value = AtomicReference<String?>(null)
        compose.runOnIdle { payment.web.evaluateJavascript(script) { value.set(it) } }
        compose.waitUntil(5000) { value.get() != null }
        return value.get()!!
    }
    @Test fun delayedValidationWaitsWhileHiddenAndContinuesOnceWhenReopened() {
        compose.runOnIdle {
            payment = EmbeddedPrexPayment(compose.activity)
            val delegate = payment.web.webViewClient
            payment.web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    val fields = listOf("nombreControl", "apellidoControl", "documentoControl", "emailControl", "celularControl")
                        .joinToString("") { "<input formcontrolname='$it'>" }
                    val html = """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1">
                        <style>stepper-pago,alta-cliente{display:block}input,button{display:block;min-height:40px}</style>
                        <stepper-pago><alta-cliente><form class="ng-pending">$fields
                        <button type="button" onclick="window.clicks++">Continuar</button>
                        </form></alta-cliente></stepper-pago><script>window.clicks=0</script>"""
                    return WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
                }
                override fun onPageStarted(v: WebView, u: String?, icon: Bitmap?) = delegate.onPageStarted(v,u,icon)
                override fun onPageFinished(v: WebView, u: String?) = delegate.onPageFinished(v,u)
            }
            compose.activity.setContent { AndroidView(factory = { payment.web }) }
            assertTrue(payment.open(url,payer,expressAmount=56400))
        }
        try {
            compose.waitUntil(15000) { payment.nativeStage == "payer" && payment.expressPhase == "advancing" }
            assertEquals("0",js("window.clicks"))
            compose.runOnIdle { payment.hide();payment.resume() }
            js("document.querySelector('form').className='ng-valid';setTimeout(()=>window.renderSettled=true,300)")
            compose.waitUntil(5000) { js("window.renderSettled===true") == "true" }
            assertEquals("0",js("window.clicks"))
            compose.runOnIdle { assertTrue(payment.open(url,payer,expressAmount=56400)) }
            compose.waitUntil(5000) { js("window.clicks") == "1" }
            js("document.querySelector('form').className='ng-valid changed';setTimeout(()=>window.rechecked=true,300)")
            compose.waitUntil(5000) { js("window.rechecked===true") == "true" }
            assertEquals("1",js("window.clicks"))
        } finally { compose.runOnIdle { payment.destroy() } }
    }
}
