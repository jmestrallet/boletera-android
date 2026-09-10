package uy.boletera.prueba

import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Fully intercepted identity/STM navigation. No real credentials, login attempts or payments. */
class LoginErrorTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var engine:StmEngine
    @Volatile private var reject=true
    @Volatile private var httpFailure:Pair<WebResourceRequest,WebResourceResponse>?=null
    private val submitted=AtomicInteger()
    private val root="https://stm.gub.uy/app/mistm/cuenta/pages/"
    private fun setup(status:Int,body:String,expected:String="blocked") {
        compose.runOnIdle {
            engine=MainActivity::class.java.getDeclaredField("engine").apply{isAccessible=true}.get(compose.activity) as StmEngine
            engine.forgetChoices()
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            choices.useAccount("00000000");choices.card="ABCD1234"
            val delegate=engine.web.webViewClient
            engine.web.webViewClient=object:WebViewClient() {
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                    val path=request.url.path.orEmpty()
                    val step=request.url.getQueryParameter("step")
                    var code=200
                    val html=when {
                        path=="/app/mistm/cuenta/"->"<button onclick=\"location.href='https://mi.iduruguay.gub.uy/login'\">INGRESAR CON USUARIO GUB.UY</button>"
                        path=="/login" && step=="sent"->{submitted.incrementAndGet();if(reject){code=status;body}else "<script>location.href='${root}tarjetas.xhtml'</script>"}
                        path=="/login" && step=="password"->"<form><input type='password'><button type='button' onclick=\"location.href='/login?step=sent'\">Continuar</button></form>"
                        path=="/login"->"<form><input id='documento'><button type='button' onclick=\"location.href='/login?step=password'\">Continuar</button></form>"
                        path.endsWith("tarjetas.xhtml")->"<table><tbody><tr onclick=\"location.href='${root}principal.xhtml'\"><td>ABCD1234 Operativa</td></tr></tbody></table>"
                        path.endsWith("principal.xhtml")->"<p>Saldo disponible: $ 824</p><button onclick=\"location.href='${root}recarga1.xhtml'\">Recargar</button>"
                        path.endsWith("recarga1.xhtml")->"<p>Tu recarga mínima deberá ser de $ 260.</p><input id='recarga1:saldoActual' value='$ 824'>"
                        else->""
                    }
                    val response=WebResourceResponse("text/html","UTF-8",code,if(code==200)"OK" else "Error",emptyMap(),("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'>$html").byteInputStream())
                    if(code>=400 && request.isForMainFrame)httpFailure=request to response
                    return response
                }
                override fun onPageStarted(v:WebView,u:String?,b:Bitmap?)=delegate.onPageStarted(v,u,b)
                override fun onPageFinished(v:WebView,u:String?) {
                    httpFailure?.takeIf{it.first.url.toString()==u}?.let{httpFailure=null;delegate.onReceivedHttpError(v,it.first,it.second)}
                    delegate.onPageFinished(v,u)
                }
                override fun onReceivedHttpError(v:WebView,r:WebResourceRequest,e:WebResourceResponse)=delegate.onReceivedHttpError(v,r,e)
            }
            engine.savedAccess(true)
            engine.connect("00000000","synthetic-rejected-password")
        }
        compose.waitUntil(if(expected=="accessHelp")35000 else 20000){engine.state.stage==expected}
    }
    private fun checkRejection(status:Int) {
        setup(status,"<form><input type='password'><button>Continuar</button><div class='error-message'>Documento o contraseña incorrectos</div></form>")
        compose.onNodeWithText("DATOS DE ACCESO").assertIsDisplayed()
        compose.onNodeWithText("Revisá tus datos").assertIsDisplayed()
        compose.onNodeWithText("El servicio\nno respondió").assertDoesNotExist()
        compose.onNodeWithText("Corregir datos").performScrollTo().assertIsDisplayed()
        assertEquals(1,submitted.get())
        compose.runOnIdle {
            for(field in listOf("document","password"))assertNull(StmEngine::class.java.getDeclaredField(field).apply{isAccessible=true}.get(engine))
        }
        compose.waitForIdle();android.os.SystemClock.sleep(300)
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"login-rejected-$status.png").outputStream().use{shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
        reject=false
        compose.onNodeWithText("Corregir datos").performClick()
        compose.onNodeWithText("Documento uruguayo").performScrollTo().performTextInput("00000000")
        compose.onNodeWithText("Contraseña de gub.uy").performScrollTo().performTextInput("synthetic-corrected-password")
        compose.onNodeWithText("Ingresar",useUnmergedTree=true).performScrollTo().performClick()
        compose.waitUntil(20000){engine.state.stage=="balance" && engine.state.minimum==26000L && !engine.state.busy}
        assertEquals(2,submitted.get())
        compose.runOnIdle {engine.forgetChoices()}
    }
    @Test fun visibleRejectionOffersManualCorrectionAndSuccessfulRetry()=checkRejection(200)
    @Test fun rejectionBodyTakesPrecedenceOverHttpError()=checkRejection(401)
    @Test fun serverFailureDoesNotClaimWrongPassword() {
        setup(503,"<div role='alert'>El servicio no está disponible.</div>")
        compose.onNodeWithText("SERVICIO").assertIsDisplayed()
        compose.onNodeWithText("DATOS DE ACCESO").assertDoesNotExist()
        assertEquals(1,submitted.get())
        compose.runOnIdle {engine.forgetChoices()}
    }
    @Test fun firstAccessConsentIsNeverAcceptedAndOffersPublicStmBrowserThenRetry() {
        setup(200,"<h1>Autorización para STM</h1><p>Compartir datos de Usuario gub.uy</p><button onclick='window.approved=true'>Autorizar</button>","accessHelp")
        compose.onNodeWithText("Completá el acceso\nen la web de STM").assertIsDisplayed()
        compose.onNodeWithText("PASO INTERRUMPIDO").assertDoesNotExist()
        assertEquals(1,submitted.get())
        val latch=java.util.concurrent.CountDownLatch(1)
        var approved=""
        compose.runOnIdle {
            engine.web.evaluateJavascript("Boolean(window.approved)"){approved=it;latch.countDown()}
            assertNull(StmEngine::class.java.getDeclaredField("password").apply{isAccessible=true}.get(engine))
        }
        assertTrue(latch.await(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals("false",approved)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val intents=java.util.concurrent.CopyOnWriteArrayList<android.content.Intent>()
        val monitor=object:android.app.Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent:android.content.Intent?):android.app.Instrumentation.ActivityResult? {
                if(intent?.action==android.content.Intent.ACTION_VIEW) {
                    intents.add(android.content.Intent(intent));return android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED,null)
                }
                return null
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.onNodeWithText("Abrir STM en el navegador").performScrollTo().performClick()
            assertEquals("https://stm.gub.uy/app/mistm/cuenta/",intents.single().data.toString())
            assertTrue(intents.single().extras?.isEmpty!=false)
            assertEquals("accessHelp",engine.state.stage);assertEquals(1,submitted.get())
            compose.waitForIdle();android.os.SystemClock.sleep(300)
            val shot=instrumentation.uiAutomation.takeScreenshot()
            File(compose.activity.getExternalFilesDir(null),"first-stm-access.png").outputStream().use{shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
            reject=false // Fixture represents consent completed independently on the provider's website.
            compose.onNodeWithText("Volver a ingresar").performScrollTo().performClick()
            compose.onNodeWithText("Ingresar manualmente").performScrollTo().performClick()
            compose.onNodeWithText("Documento uruguayo").performScrollTo().performTextInput("00000000")
            compose.onNodeWithText("Contraseña de gub.uy").performScrollTo().performTextInput("synthetic-password")
            compose.onNodeWithText("Ingresar",useUnmergedTree=true).performScrollTo().performClick()
            compose.waitUntil(20000){engine.state.stage=="balance"&&engine.state.minimum==26000L&&!engine.state.busy}
            assertEquals(2,submitted.get())
        } finally {instrumentation.removeMonitor(monitor);compose.runOnIdle{engine.forgetChoices()}}
    }
    @Test fun firstAccessInstructionsAreAvailableBeforeEnteringCredentials() {
        compose.onNodeWithText("¿Es tu primer ingreso a STM?").performScrollTo().performClick()
        compose.onNodeWithText("Primer ingreso a STM").assertIsDisplayed()
        compose.onNodeWithText("Abrir STM").assertIsDisplayed()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Primer ingreso a STM").assertDoesNotExist()
    }
}
