package uy.boletera.prueba

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Entire journey is intercepted locally. No network, account or real credentials. */
@RunWith(AndroidJUnit4::class)
class CardFlowRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun protectedUrlLoginAndDelayedCardsKeepAccessUntilRowsAreActuallyRead() {
        lateinit var engine: StmEngine
        var authenticated = false
        val networkRequests = java.util.concurrent.atomic.AtomicInteger()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val chromeLinks = mutableListOf<String?>()
        val monitor = object : android.app.Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: android.content.Intent?): android.app.Instrumentation.ActivityResult? {
                if (intent?.`package` != "com.android.chrome") return null
                chromeLinks.add(intent.dataString)
                return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        val preferences = JourneyPreferences(compose.activity).apply { useAccount("00000000"); acknowledgePayment() }
        compose.runOnIdle {
            engine = MainActivity::class.java.getDeclaredField("engine").apply { isAccessible = true }.get(compose.activity) as StmEngine
            engine.forgetChoices()
            val delegate = engine.web.webViewClient
            engine.web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    networkRequests.incrementAndGet()
                    val root = "https://stm.gub.uy/app/mistm/cuenta/pages/"
                    val html = when (request.url.path) {
                        "/app/mistm/cuenta/" -> if (authenticated) "<button onclick=\"location.href='https://mi.iduruguay.gub.uy/login'\">INGRESAR CON USUARIO GUB.UY</button>" else "<script>location.href='${root}tarjetas.xhtml'</script>"
                        "/app/mistm/cuenta/pages/tarjetas.xhtml" -> if (!authenticated && request.url.getQueryParameter("authenticated") != "1") {
                            "<button onclick=\"location.href='https://mi.iduruguay.gub.uy/login'\">INGRESAR CON USUARIO GUB.UY</button>"
                        } else {
                            val rows = """<table><tbody><tr onclick="location.href='${root}principal.xhtml'"><td><span>ABCD1234</span><span>Operativa</span></td></tr><tr><td><span>DEAD5678</span><span>Pte. Anular (Caducidad G.U.)</span></td></tr></tbody></table>"""
                            "<div id='form1:tablaTarjetas_data'></div><script>setTimeout(()=>{document.getElementById('form1:tablaTarjetas_data').innerHTML=${org.json.JSONObject.quote(rows)}},2500)</script>"
                        }
                        "/login" -> """<form><input id="documento"><button type="button">Continuar</button></form><form><input type="password"><button type="button" onclick="if(document.querySelector('input[type=password]').value==='synthetic-offline-only') location.href='https://ih.montevideo.gub.uy/commonauth'">Continuar</button></form>"""
                        "/commonauth" -> { authenticated = true; "<script>location.href='${root}tarjetas.xhtml?authenticated=1'</script>" }
                        "/app/mistm/cuenta/pages/principal.xhtml" -> """
                            <p>Saldo disponible*: ${'$'} -304</p><button onclick="location.href='${root}recarga1.xhtml'">Recargar</button>
                        """.trimIndent()
                        "/app/mistm/cuenta/pages/recarga1.xhtml" -> """
                            <p>Tu recarga mínima deberá ser de ${'$'} 564 .</p><label>Saldo actual *</label><input id="recarga1:saldoActual" value="${'$'} -304"><input id="recarga1:monto_input"><button onclick="location.href='${root}recarga2.xhtml'">CONTINUAR</button>
                        """.trimIndent()
                        "/app/mistm/cuenta/pages/recarga2.xhtml" -> "<div class='banco' id='id-1033'>Prex</div><div class='banco' id='id-1002'>BROU</div><button>Continuar</button>"
                        else -> ""
                    }
                    return WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
                }
                override fun onPageStarted(view: WebView, url: String?, icon: android.graphics.Bitmap?) = delegate.onPageStarted(view, url, icon)
                override fun onPageFinished(view: WebView, url: String?) = delegate.onPageFinished(view, url)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = delegate.shouldOverrideUrlLoading(view, request)
            }
            engine.connect("00000000", "synthetic-offline-only")
        }
        try {
            compose.waitUntil(20000) { engine.state.stage == "cards" && engine.state.cards.size == 2 }
            compose.runOnIdle {
                assertTrue(engine.state.cards[0].active)
                assertFalse(engine.state.cards[1].active)
                engine.chooseCard("ABCD1234")
            }
            compose.waitUntil(20000) { engine.state.stage == "balance" && engine.state.minimum == 56400L }
            compose.runOnIdle {
                assertEquals(-30400L, engine.state.balance)
                assertEquals("ABCD1234", engine.state.selectedCard)
                assertFalse(engine.state.busy)
                engine.prepare(56400)
            }
            compose.waitUntil(20000) { engine.state.stage == "paymentBoundary" }
            compose.runOnIdle {
                assertEquals(2, engine.state.providers.size)
                engine.chooseProvider("1033")
                assertEquals("1033", engine.state.selectedProvider)
                engine.connect("00000000", "synthetic-offline-only")
            }
            // A new login automatically selects the remembered operational card.
            compose.waitUntil(20000) { engine.state.stage == "balance" && engine.state.minimum == 56400L }
            compose.runOnIdle {
                assertEquals("ABCD1234", engine.state.selectedCard)
                engine.prepare(56400)
            }
            compose.waitUntil(20000) { engine.state.stage == "paymentBoundary" }
            compose.runOnIdle {
                assertEquals("1033", engine.state.selectedProvider)
                engine.changeCard()
            }
            try {
                compose.waitUntil(20000) { engine.state.stage == "cards" && !engine.state.busy }
            } catch (error: Throwable) {
                throw AssertionError("Change card: stage=${engine.state.stage}, busy=${engine.state.busy}, reference=${engine.state.diagnostic}", error)
            }
            compose.runOnIdle {
                engine.chooseCard("DEAD5678")
                assertEquals("cards", engine.state.stage)
            }
            val originalLink = "https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-FLOW-RESUME"
            assertTrue(preferences.beginPayment(PendingPayment("ABCD1234", "1033", 56400, 123456)))
            assertTrue(preferences.rememberPrexLink(originalLink))
            compose.runOnIdle {
                engine.connect("00000000", "synthetic-offline-only")
                assertFalse(engine.state.canReopenPrex)
                engine.reopenPrexPayment()
                assertTrue(chromeLinks.isEmpty()) // A document alone, before successful login, cannot reopen a saved payment.
                engine.acknowledgePayment()
                assertNotNull("An unverified login must not clear this account's pending payment", preferences.pending)
                assertNull("Pending payment details stay hidden until login is complete", engine.state.pendingPayment)
            }
            compose.waitUntil(20000) { engine.state.stage == "balance" && engine.state.minimum == 56400L && !engine.state.busy }
            compose.runOnIdle { assertTrue(engine.state.canReopenPrex); assertNotNull(engine.state.pendingPayment) }
            val requestsBefore = networkRequests.get()
            compose.onNodeWithText("Volver al pago de Prex").performScrollTo().performClick()
            compose.onNodeWithText("Volver a la solicitud anterior").assertIsDisplayed()
            compose.onNodeWithText("Abrir Prex").performClick()
            compose.waitUntil(5000) { engine.state.stage == "paymentReview" }
            compose.runOnIdle {
                assertEquals(listOf(originalLink), chromeLinks)
                assertEquals(requestsBefore, networkRequests.get()) // No STM request or provider re-submission on reopen.
                assertEquals(123456L, engine.state.pendingPayment?.createdAt)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            preferences.acknowledgePayment()
            compose.runOnIdle {
                engine.cancel()
                engine.forgetChoices()
            }
        }
    }
}
