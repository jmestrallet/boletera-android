package uy.boletera.prueba

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Locally intercepted provider pages and a controlled elapsed-time clock. No financial requests. */
class PaymentProgressTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var payment:EmbeddedPrexPayment
    private var now=1000L
    private val url="https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=PROGRESS-FIXTURE"
    private val summary="""<stepper-pago><confirmar-pago><div><b>Total:</b><p>260,00</p></div><div><b>Moneda:</b><p>UYU</p></div><button onclick="window.clicks++">Continuar</button></confirmar-pago></stepper-pago>"""
    private val card="""<stepper-pago><alta-tarjeta><form><input formcontrolname="nroTarjetaControl"><input formcontrolname="expiracionControl"><input formcontrolname="cvvControl"><button>Continuar</button></form></alta-tarjeta></stepper-pago>"""
    private fun setup(body:String,express:Boolean=false) {
        compose.runOnIdle {
            payment=EmbeddedPrexPayment(compose.activity){now}
            val delegate=payment.web.webViewClient
            payment.web.webViewClient=object:WebViewClient(){
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest)=WebResourceResponse("text/html","UTF-8",("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'><style>confirmar-pago,alta-tarjeta{display:block}input,button{display:block}</style><script>window.clicks=0</script>"+body).byteInputStream())
                override fun onPageStarted(v:WebView,u:String?,b:Bitmap?)=delegate.onPageStarted(v,u,b)
                override fun onPageFinished(v:WebView,u:String?)=delegate.onPageFinished(v,u)
                override fun onReceivedHttpError(v:WebView,r:WebResourceRequest,e:WebResourceResponse)=delegate.onReceivedHttpError(v,r,e)
            }
            val payer=if(express)PayerProfile("progress","Ficticia","Persona","Prueba","00000000","test@example.invalid","099123456") else null
            assertTrue(payment.open(url,payer,if(express)26000 else null,26000))
            compose.activity.setContent {BoleteraTheme("dark"){EmbeddedPrexScreen(payment,null,{})}}
        }
    }
    private fun js(source:String):String {
        val latch=CountDownLatch(1);val result=AtomicReference<String>()
        compose.runOnIdle{payment.web.evaluateJavascript(source){result.set(it);latch.countDown()}}
        assertTrue(latch.await(5,TimeUnit.SECONDS));return result.get()
    }
    private fun httpFailure(requestUrl:String=url) {
        compose.runOnIdle {
            val request=object:WebResourceRequest {
                override fun getUrl()=Uri.parse(requestUrl)
                override fun isForMainFrame()=true
                override fun isRedirect()=false
                override fun hasGesture()=false
                override fun getMethod()="GET"
                override fun getRequestHeaders()=emptyMap<String,String>()
            }
            payment.web.webViewClient.onReceivedHttpError(payment.web,request,WebResourceResponse("text/html","UTF-8",500,"Failure",emptyMap(),"Error ficticio".byteInputStream()))
        }
    }
    @Test fun stalledExpressExposesTheSameRequestWithoutSubmittingAgainAndLateProgressRecovers() {
        setup(summary,true)
        try {
            compose.waitUntil(15000){payment.nativeStage=="summary"&&payment.completionSubmitted}
            assertEquals("1",js("window.clicks"))
            compose.runOnIdle{now+=31000}
            compose.waitUntil(5000){payment.slowStep}
            compose.onNodeWithText("Revisar esta solicitud").assertIsDisplayed()
            assertEquals("1",js("window.clicks"))
            compose.runOnIdle{payment.advance()}
            assertEquals("1",js("window.clicks"))
            js("document.body.innerHTML="+org.json.JSONObject.quote(card))
            compose.waitUntil(5000){payment.nativeStage=="card"&&!payment.slowStep}
            compose.onNodeWithText("Tu tarjeta").assertIsDisplayed()
            compose.onNodeWithText("Revisar esta solicitud").assertDoesNotExist()
        } finally {compose.runOnIdle{payment.destroy()}}
    }
    @Test fun pageWithoutRecognizedContentOffersAnExitInsteadOfAnEndlessSpinner() {
        setup("<p>Cargando proveedor ficticio</p>")
        try {
            compose.waitUntil(10000){!payment.busy}
            compose.runOnIdle{now+=31000}
            compose.waitUntil(5000){payment.slowStep}
            compose.onNodeWithText("Revisar esta solicitud").assertIsDisplayed()
            compose.onNodeWithText("Volver al saldo").assertIsDisplayed()
            compose.onNodeWithText("Estamos recuperando tu solicitud.").assertDoesNotExist()
        } finally {compose.runOnIdle{payment.destroy()}}
    }
    @Test fun staleAndTransientHttpFailuresDoNotMaskProgressButPersistentFailuresRemainVisible() {
        setup(summary)
        try {
            compose.waitUntil(10000){payment.nativeStage=="summary"}
            httpFailure(url+"-old")
            compose.runOnIdle{now+=3000}
            assertTrue(payment.message.isBlank())
            httpFailure()
            js("document.body.innerHTML="+org.json.JSONObject.quote(card))
            compose.waitUntil(5000){payment.nativeStage=="card"}
            compose.runOnIdle{now+=3000}
            compose.waitForIdle();assertTrue(payment.message.isBlank())
            httpFailure();compose.runOnIdle{now+=3000}
            compose.waitUntil(5000){payment.message.isNotBlank()}
            compose.onNodeWithText("Enviar error al desarrollador").assertExists()
        } finally {compose.runOnIdle{payment.destroy()}}
    }
}
