package uy.boletera.prueba

import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** A real WebView keeps the amount page visible while its first request is pending. */
class ExpressPreparationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var engine: StmEngine
    private val root = "https://stm.gub.uy/app/mistm/cuenta/pages/"
    private val providerVisits = AtomicInteger()
    private fun js(script: String): String {
        val result = AtomicReference<String?>(null)
        compose.runOnIdle { engine.web.evaluateJavascript(script) { result.set(it) } }
        compose.waitUntil(5000) { result.get() != null }
        return result.get()!!
    }
    private fun pendingPreparation(express: Boolean) {
        compose.runOnIdle {
            engine = MainActivity::class.java.getDeclaredField("engine").apply { isAccessible=true }.get(compose.activity) as StmEngine
            engine.forgetChoices()
            val choices = StmEngine::class.java.getDeclaredField("choices").apply { isAccessible=true }.get(engine) as JourneyPreferences
            choices.useAccount("00000000"); choices.card="ABCD1234"; choices.provider="1033"
            val delegate = engine.web.webViewClient
            engine.web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    val html = when(request.url.path) {
                        "/app/mistm/cuenta/" -> "<script>location.href='${root}tarjetas.xhtml'</script>"
                        "/app/mistm/cuenta/pages/tarjetas.xhtml" -> """<div id="form1:tablaTarjetas_data"><table><tbody><tr onclick="location.href='${root}principal.xhtml'"><td><span>ABCD1234</span><span>Operativa</span></td></tr></tbody></table></div>"""
                        "/app/mistm/cuenta/pages/principal.xhtml" -> """<p>Saldo disponible*: $ 824</p><button onclick="location.href='${root}recarga1.xhtml'">Recargar</button>"""
                        "/app/mistm/cuenta/pages/recarga1.xhtml" -> """<p>Tu recarga mínima deberá ser de $ 260.</p><input id="recarga1:saldoActual" value="$ 824"><input id="recarga1:monto_input"><button onclick="window.amountClicks++;window.releaseAmount=()=>location.href='${root}recarga2.xhtml'">CONTINUAR</button><script>window.amountClicks=0</script>"""
                        "/app/mistm/cuenta/pages/recarga2.xhtml" -> {
                            providerVisits.incrementAndGet()
                            "<p>Elegí un medio de pago</p><div class='banco' id='id-1002'>BROU</div>"
                        }
                        else -> "<p>Destino inesperado</p>"
                    }
                    return WebResourceResponse("text/html","UTF-8",("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'>"+html).byteInputStream())
                }
                override fun onPageStarted(v: WebView,u: String?,icon: Bitmap?) = delegate.onPageStarted(v,u,icon)
                override fun onPageFinished(v: WebView,u: String?) = delegate.onPageFinished(v,u)
                override fun shouldOverrideUrlLoading(v: WebView,r: WebResourceRequest) = delegate.shouldOverrideUrlLoading(v,r)
            }
            engine.connect("00000000","synthetic-offline-only")
        }
        try {
            compose.waitUntil(20000) { engine.state.stage=="balance" && engine.state.minimum==26000L && !engine.state.busy }
            js("window.snapshotCount=0;const originalAdapter=window.BoleteraAdapter;window.BoleteraAdapter={...originalAdapter,snapshot:()=>{window.snapshotCount++;return originalAdapter.snapshot()}}")
            compose.runOnIdle {
                assertTrue(engine.savePayer(PayerProfile("pending","Prueba","Persona","Ficticia","00000000","persona@example.invalid","099123456")))
                if(express) engine.startExpress() else engine.prepare(26000)
                assertTrue(engine.state.busy)
            }
            compose.waitUntil(7000) { js("window.snapshotCount>=3 && window.amountClicks===1")=="true" }
            compose.runOnIdle {
                assertTrue("Polling the old amount page must keep the pending request busy",engine.state.busy)
                assertEquals(express,engine.expressPreparing)
                // Stale gestures cannot replace or cancel the request that already started.
                engine.prepare(52000); engine.startExpress(); engine.refresh(); engine.changeCard()
                assertEquals(26000L,engine.state.amount)
                assertEquals(express,engine.expressPreparing)
            }
            assertEquals("1",js("window.amountClicks"))
            assertEquals(0,providerVisits.get())
            js("window.releaseAmount()")
            compose.waitUntil(10000) { engine.state.stage=="paymentBoundary" && !engine.state.busy }
            assertEquals(1,providerVisits.get())
            compose.runOnIdle { assertNull(engine.state.activePayment) }
        } finally { compose.runOnIdle { engine.cancel();engine.forgetChoices() } }
    }
    @Test fun expressWaitsForItsOriginalRequestDespiteRepeatedSnapshots() = pendingPreparation(true)
    @Test fun ordinaryChargeCannotSubmitAgainWhileTheOriginalRequestIsPending() = pendingPreparation(false)
}
