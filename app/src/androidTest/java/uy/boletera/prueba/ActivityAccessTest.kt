package uy.boletera.prueba

import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Official page shapes observed live; all account values invented and every request intercepted. */
class ActivityAccessTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun homeOpensIdentityRequirementAndReturnsWithoutStartingAnyPayment() {
        lateinit var engine:StmEngine
        val hits=ConcurrentHashMap<String,AtomicInteger>()
        val root="https://stm.gub.uy/app/mistm/cuenta/pages/"
        compose.runOnIdle {
            engine=MainActivity::class.java.getDeclaredField("engine").apply{isAccessible=true}.get(compose.activity) as StmEngine
            engine.forgetChoices()
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            choices.useAccount("00000000");choices.card="ABCD1234"
            val delegate=engine.web.webViewClient
            engine.web.webViewClient=object:WebViewClient() {
                override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse {
                    val path=r.url.path.orEmpty()
                    if(r.isForMainFrame)hits.computeIfAbsent(path){AtomicInteger()}.incrementAndGet()
                    val body=when {
                        path.endsWith("tarjetas.xhtml") -> """<table><tbody><tr onclick="location.href='${root}principal.xhtml'"><td>ABCD1234 Operativa</td></tr></tbody></table>"""
                        path.endsWith("principal.xhtml") -> """<p>ABCD1234 Operativa</p><p>Saldo disponible: $730</p><button onclick="location.href='${root}recarga1.xhtml'">Recargar</button><button onclick="location.href='${root}usuarioNoValidado.xhtml'">Movimientos</button>"""
                        path.endsWith("recarga1.xhtml") -> """<p>Recarga mínima $260</p><input id="recarga1:saldoActual" value="$730"><input id="recarga1:monto_input"><button onclick="location.href='${root}recarga2.xhtml'">Continuar</button>"""
                        path.endsWith("usuarioNoValidado.xhtml") -> "<h2>Tu usuario no tiene garantía de identidad nivel 2 o superior.</h2>"
                        else -> """<button onclick="location.href='${root}tarjetas.xhtml'">INGRESAR CON USUARIO GUB.UY</button>"""
                    }
                    return WebResourceResponse("text/html","UTF-8",("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'>"+body).byteInputStream())
                }
                override fun onPageStarted(v:WebView,u:String?,b:Bitmap?)=delegate.onPageStarted(v,u,b)
                override fun onPageFinished(v:WebView,u:String?)=delegate.onPageFinished(v,u)
            }
            engine.connect("00000000","synthetic-only")
        }
        compose.waitUntil(20000){engine.state.stage=="balance"&&!engine.state.busy&&engine.state.minimum!=null}
        val priorChargePages=hits[root.removePrefix("https://stm.gub.uy")+"recarga1.xhtml"]?.get()?:0
        compose.onNodeWithText("Actividad").performScrollTo().performClick()
        compose.waitUntil(15000){engine.state.activity.access==ActivityAccess.IDENTITY_REQUIRED}
        compose.onNodeWithText("Habilitá tus movimientos").assertIsDisplayed()
        assertEquals(priorChargePages,hits["/app/mistm/cuenta/pages/recarga1.xhtml"]?.get()?:0)
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.waitUntil(15000){engine.state.stage=="balance"&&!engine.state.busy}
        assertEquals("ABCD1234",engine.state.selectedCard)
        assertNull(engine.state.amount)
        assertEquals(0,hits["/app/mistm/cuenta/pages/recarga2.xhtml"]?.get()?:0)
    }
}
