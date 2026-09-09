package uy.boletera.prueba

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Entire journey is intercepted locally. No network, account or real credentials. */
@RunWith(AndroidJUnit4::class)
class CardFlowRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun hiddenEngineReadsCompactCardLabelsAndReachesBalanceAcrossNavigation() {
        lateinit var engine: StmEngine
        compose.runOnIdle {
            engine = StmEngine(compose.activity)
            val delegate = engine.web.webViewClient
            engine.web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    val root = "https://stm.gub.uy/app/mistm/cuenta/pages/"
                    val html = when (request.url.path) {
                        "/app/mistm/cuenta/" -> "<script>location.href='${root}tarjetas.xhtml'</script>"
                        "/app/mistm/cuenta/pages/tarjetas.xhtml" -> """
                            <table><tbody><tr onclick="location.href='${root}principal.xhtml'"><td><span>ABCD1234</span><span>Operativa</span></td></tr><tr><td><span>DEAD5678</span><span>Pte. Anular (Caducidad G.U.)</span></td></tr></tbody></table>
                        """.trimIndent()
                        "/app/mistm/cuenta/pages/principal.xhtml" -> """
                            <p>Saldo disponible*: ${'$'} -304</p><button onclick="location.href='${root}recarga1.xhtml'">Recargar</button>
                        """.trimIndent()
                        "/app/mistm/cuenta/pages/recarga1.xhtml" -> """
                            <p>Tu recarga mínima deberá ser de ${'$'} 564 .</p><label>Saldo actual *</label><input id="recarga1:saldoActual" value="${'$'} -304"><input id="recarga1:monto_input"><button>CONTINUAR</button>
                        """.trimIndent()
                        else -> ""
                    }
                    return WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
                }
                override fun onPageStarted(view: WebView, url: String?, icon: android.graphics.Bitmap?) = delegate.onPageStarted(view, url, icon)
                override fun onPageFinished(view: WebView, url: String?) = delegate.onPageFinished(view, url)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = delegate.shouldOverrideUrlLoading(view, request)
            }
            compose.activity.findViewById<ViewGroup>(android.R.id.content).addView(engine.host, ViewGroup.LayoutParams(1, 1))
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
            }
        } finally {
            compose.runOnIdle {
                (engine.host.parent as? ViewGroup)?.removeView(engine.host)
                engine.destroy()
            }
        }
    }
}
