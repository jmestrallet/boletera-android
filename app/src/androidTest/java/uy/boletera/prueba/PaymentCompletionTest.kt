package uy.boletera.prueba

import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Every request is intercepted with fictitious fixtures. No external payment or credentials. */
class PaymentCompletionTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var engine:StmEngine
    private val hits=ConcurrentHashMap<String,AtomicInteger>()
    private val stm="https://stm.gub.uy/app/mistm/cuenta/pages/"
    private var startAtCard=false
    private var cardSuffix="1234"
    private var pendingResult=false
    private val testPayer=PayerProfile("express-completion","Mi Prex","Persona","Ficticia","00000000","persona@example.invalid","099123456")
    private fun pairs(vararg rows:Pair<String,String>)=rows.joinToString(""){(a,b)->"<div><p>$a:</p><p>$b</p></div>"}
    private fun response(path:String):String=when(path) {
        "/v2/confirmarPago" -> if(startAtCard) """<stepper-pago><alta-tarjeta><form><input formcontrolname="nroTarjetaControl"><input formcontrolname="expiracionControl"><input formcontrolname="cvvControl"><button type="button" onclick="this.disabled=true;location.href='/v2/final'">Continuar</button></form></alta-tarjeta></stepper-pago>""" else finalMarkup()
        "/v2/final" -> finalMarkup()
        "/v2/resultadoPago" -> """<resultado-pago><h4>Comprobante de Pago</h4><b>${if(pendingResult)"El pago se encuentra en estado: Pendiente" else "El pago se realizó con éxito"}</b>${pairs("Número de autorización" to "TEST-123","Fecha de Pago" to "01/01/2026 10:00:00","Comercio" to "STM Recargas","Transacción" to "TEST-901","TOTAL" to "$ 564,00")}<a onclick="location.href='${stm}resultado.xhtml'">Volver a la página inicial</a></resultado-pago>"""
        "/app/mistm/cuenta/pages/resultado.xhtml" -> """<h1>¡Recarga exitosa!</h1><p>Recarga Confirmada.</p><button onclick="location.href='${stm}principal.xhtml'">CONTINUAR</button>"""
        "/app/mistm/cuenta/pages/principal.xhtml" -> """<div>Operativa <label>Saldo disponible*:</label> $ 824</div><button onclick="location.href='${stm}recarga1.xhtml'">Recargar</button>"""
        "/app/mistm/cuenta/pages/tarjetas.xhtml" -> """<div id="form1:tablaTarjetas_data"><table><tbody><tr onclick="location.href='${stm}principal.xhtml'"><td><span>ABCD1234</span><span>Operativa</span></td></tr></tbody></table></div>"""
        "/app/mistm/cuenta/pages/recarga1.xhtml" -> """<p>Tu recarga mínima deberá ser de $ 260.</p><label>Saldo actual *</label><input id="recarga1:saldoActual" value="$ 824"><input id="recarga1:monto_input"><button onclick="location.href='${stm}recarga2.xhtml'">CONTINUAR</button>"""
        else -> "<p>Destino inesperado en la prueba.</p>"
    }
    private fun finalMarkup()="""<stepper-pago><finalizar-pago><form>${pairs("Comercio" to "STM Recargas","Cliente" to "999","Transacción" to "TEST-901","Medio de Pago" to "PREX **** $cardSuffix","Importe" to "$ 564,00")}<button type="button" onclick="this.disabled=true;location.href='/v2/resultadoPago'">Confirmar</button></form></finalizar-pago></stepper-pago>"""
    private fun intercept(web:WebView) {
        val delegate=web.webViewClient
        web.webViewClient=object:WebViewClient() {
            override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                val path=request.url.path.orEmpty()
                if(request.isForMainFrame)hits.computeIfAbsent(path){AtomicInteger()}.incrementAndGet()
                val html="""<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><style>stepper-pago,alta-tarjeta,finalizar-pago,resultado-pago{display:block}p{margin:8px}button,a{display:block;padding:12px}</style>${response(path)}"""
                return WebResourceResponse("text/html","UTF-8",html.byteInputStream())
            }
            override fun onPageStarted(view:WebView,url:String?,icon:Bitmap?)=delegate.onPageStarted(view,url,icon)
            override fun onPageFinished(view:WebView,url:String?)=delegate.onPageFinished(view,url)
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=delegate.shouldOverrideUrlLoading(view,request)
        }
    }
    private fun openExpress() {
        startAtCard=true
        compose.runOnIdle {
            engine=MainActivity::class.java.getDeclaredField("engine").apply{isAccessible=true}.get(compose.activity) as StmEngine
            intercept(engine.web);intercept(engine.prexPayment.web);engine.forgetChoices()
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            choices.useAccount("00000000");choices.card="ABCD1234";choices.provider="1033"
            PaymentJournal(compose.activity).clear(choices.accountKey!!)
            choices.successfulProvider="1033";choices.successfulPayer=testPayer.id
            choices.successfulCardSuffix="1111";choices.successfulCardIdentity=choices.cardIdentity("4111111111111111")
            StmEngine::class.java.getDeclaredField("accountVerified").apply{isAccessible=true}.setBoolean(engine,true)
            assertTrue(engine.savePayer(testPayer))
            @Suppress("UNCHECKED_CAST") val backing=StmEngine::class.java.getDeclaredField("state\$delegate").apply{isAccessible=true}.get(engine) as MutableState<UiState>
            backing.value=UiState(stage="embeddedPrex",selectedCard="ABCD1234",cards=listOf(CardInfo("ABCD1234",true,"Operativa")),balance=26000,minimum=26000,
                activePayment=ActivePayment("ABCD1234","1033",56400,System.currentTimeMillis(),testPayer.id))
            assertTrue(engine.prexPayment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-EXPRESS",
                testPayer,expressAmount=56400,expectedAmount=56400,expectedCardIdentity=choices.successfulCardIdentity,expectedCardSuffix="1111"))
        }
        compose.waitUntil(15000){engine.prexPayment.nativeStage=="card"&&engine.prexPayment.canContinue}
    }
    private fun enterCard(number:String) {
        compose.onNodeWithText("Número de tarjeta").performTextInput(number)
        compose.onNodeWithText("Vencimiento").performTextInput("12/39")
        compose.onNodeWithText("CVV").performTextInput("123")
        compose.onNodeWithText("CVV").performImeAction()
    }
    @Test fun knownExpressCardConfirmsOnceAndReturnsToFreshBalanceWithoutMoreTaps() {
        cardSuffix="1111";openExpress()
        compose.runOnIdle {engine.pause();engine.resume();assertTrue(engine.prexPayment.expressJourney)}
        enterCard("4111111111111111")
        compose.waitUntil(20000){engine.state.stage=="balance"&&!engine.state.busy&&engine.state.minimum==26000L}
        compose.runOnIdle {
            assertEquals(82400L,engine.state.balance);assertFalse(engine.state.paymentNeedsReview)
            assertEquals(1,hits["/v2/final"]?.get());assertEquals(1,hits["/v2/resultadoPago"]?.get())
            assertEquals(0,hits["/app/mistm/cuenta/pages/recarga2.xhtml"]?.get()?:0)
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            assertEquals("1111",choices.successfulCardSuffix)
            assertEquals(choices.cardIdentity("4111111111111111"),choices.successfulCardIdentity)
            assertNull(PaymentJournal(compose.activity).read(choices.accountKey!!))
        }
        shot("express30-return-home.png")
        compose.runOnIdle {engine.forgetChoices()}
    }
    @Test fun aDifferentCardRequiresConfirmationAndPendingResultSurvivesARefresh() {
        cardSuffix="4444";pendingResult=true;openExpress();enterCard("5555555555554444")
        compose.waitUntil(15000){engine.prexPayment.nativeStage=="finalConfirmation"&&engine.prexPayment.canContinue}
        compose.runOnIdle {assertEquals(0,hits["/v2/resultadoPago"]?.get()?:0)}
        compose.onNodeWithText("Confirmar pago").performClick()
        compose.waitUntil(15000){engine.prexPayment.nativeStage=="paymentPending"}
        compose.runOnIdle {engine.refresh()}
        compose.waitUntil(20000){engine.state.stage=="balance"&&!engine.state.busy&&engine.state.minimum==26000L}
        compose.runOnIdle {
            assertTrue(engine.state.paymentNeedsReview);assertFalse(engine.expressAvailable)
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            assertEquals("pending",PaymentJournal(compose.activity).read(choices.accountKey!!)?.phase)
            assertEquals("1111",choices.successfulCardSuffix)
            assertEquals(1,hits["/v2/resultadoPago"]?.get())
        }
        compose.onNodeWithText("Recargar boletera").assertIsNotEnabled()
        shot("express30-pending-home.png")
        compose.runOnIdle {engine.acknowledgePaymentReviewed();engine.forgetChoices()}
    }
    private fun shot(name:String) {
        compose.waitForIdle();android.os.SystemClock.sleep(300)
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    @Test fun explicitConfirmationReceiptStmSuccessAndFreshNativeHomeWithoutAnotherPayment() {
        compose.runOnIdle {compose.activity.getSharedPreferences("appearance",0).edit().putString("theme","dark").commit()}
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            engine=MainActivity::class.java.getDeclaredField("engine").apply{isAccessible=true}.get(compose.activity) as StmEngine
            intercept(engine.web);intercept(engine.prexPayment.web)
            engine.forgetChoices()
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            choices.useAccount("00000000");choices.card="ABCD1234";choices.provider="1033"
            StmEngine::class.java.getDeclaredField("accountVerified").apply{isAccessible=true}.setBoolean(engine,true)
            @Suppress("UNCHECKED_CAST")
            val backing=StmEngine::class.java.getDeclaredField("state\$delegate").apply{isAccessible=true}.get(engine) as MutableState<UiState>
            backing.value=UiState(stage="embeddedPrex",selectedCard="ABCD1234",cards=listOf(CardInfo("ABCD1234",true,"Operativa")),balance=26000,minimum=26000,activePayment=ActivePayment("ABCD1234","1033",56400,System.currentTimeMillis()))
            assertTrue(engine.prexPayment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-TAIL",expectedAmount=56400))
        }
        compose.waitUntil(15000){engine.prexPayment.nativeStage=="finalConfirmation"&&engine.prexPayment.canContinue}
        assertEquals(0,hits["/v2/resultadoPago"]?.get()?:0)
        compose.onNodeWithText("Confirmá tu pago").assertIsDisplayed()
        shot("payment-final-confirmation.png")
        compose.onNodeWithText("Confirmar pago").performClick()
        compose.runOnIdle {engine.prexPayment.advance("finalConfirmation")} // A late tap from the old screen cannot act on a new stage.
        compose.waitUntil(15000){engine.prexPayment.nativeStage=="receipt"}
        assertEquals(1,hits["/v2/resultadoPago"]?.get())
        compose.onNodeWithText("Pago confirmado").assertIsDisplayed()
        shot("payment-receipt.png")
        compose.onNodeWithText("Ver comprobante y detalle").performScrollTo().performClick()
        compose.onNodeWithText("TEST-123").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Volver a mi boletera").performClick()
        compose.waitUntil(20000){engine.state.stage=="balance"&&!engine.state.busy&&engine.state.minimum==26000L}
        compose.onNodeWithText("Ver mi saldo").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(82400L,engine.state.balance)
            assertEquals("ABCD1234",engine.state.selectedCard)
            assertNull(engine.state.activePayment)
            assertEquals(1,hits["/v2/confirmarPago"]?.get())
            assertEquals(1,hits["/v2/resultadoPago"]?.get())
            assertEquals(1,hits["/app/mistm/cuenta/pages/resultado.xhtml"]?.get())
            assertEquals(0,hits["/app/mistm/cuenta/pages/recarga2.xhtml"]?.get()?:0)
        }
        compose.onNodeWithText("$ 824").assertExists()
        shot("payment-return-home.png")
        compose.runOnIdle {engine.forgetChoices()}
    }
}
