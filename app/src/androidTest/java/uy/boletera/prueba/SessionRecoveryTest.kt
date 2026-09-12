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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Auth and STM requests are intercepted locally; all identities and amounts are fictitious. */
class SessionRecoveryTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var engine:StmEngine
    @Volatile private var authenticated=false
    @Volatile private var ssoAlive=false
    @Volatile private var serverFailure:String?=null
    @Volatile private var interceptedHttpFailure:Pair<WebResourceRequest,WebResourceResponse>?=null
    private val hits=ConcurrentHashMap<String,AtomicInteger>()
    private val root="https://stm.gub.uy/app/mistm/cuenta/pages/"
    private val login="<button onclick=\"location.href='https://mi.iduruguay.gub.uy/login'\">INGRESAR CON USUARIO GUB.UY</button>"
    private fun html(path:String):String=when(path) {
        "/app/mistm/cuenta/" -> login
        "/login" -> if(ssoAlive) {authenticated=true;"<script>location.href='${root}tarjetas.xhtml'</script>"} else """<form><input id="documento"><button type="button">Continuar</button></form><form><input type="password"><button type="button" onclick="location.href='${root}tarjetas.xhtml?authenticated=1'">Continuar</button></form>"""
        "/app/mistm/cuenta/pages/tarjetas.xhtml" -> if(!authenticated)login else """<div id="form1:tablaTarjetas_data"><table><tbody><tr onclick="location.href='${root}principal.xhtml'"><td><span>ABCD1234</span><span>Operativa</span></td></tr></tbody></table></div>"""
        "/app/mistm/cuenta/pages/principal.xhtml" -> """<p>Saldo disponible*: $ 824</p><button onclick="location.href='${root}recarga1.xhtml'">Recargar</button>"""
        "/app/mistm/cuenta/pages/recarga1.xhtml" -> """<p>Tu recarga mínima deberá ser de $ 260.</p><input id="recarga1:saldoActual" value="$ 824"><input id="recarga1:monto_input"><button onclick="location.href='${root}recarga2.xhtml'">CONTINUAR</button>"""
        "/app/mistm/cuenta/pages/recarga2.xhtml" -> """<div class="banco" id="id-1033" onclick="this.classList.add('selected')">Prex</div><button onclick="location.href='${root}session-ended.xhtml'">Continuar</button>"""
        "/app/mistm/cuenta/pages/session-ended.xhtml" -> {authenticated=false;"<div role='alert'>Tu sesión ha expirado.</div>"}
        else -> "<p>Destino no esperado en el caso ficticio.</p>"
    }
    private fun setup() {
        compose.runOnIdle {
            engine=MainActivity::class.java.getDeclaredField("engine").apply{isAccessible=true}.get(compose.activity) as StmEngine
            engine.forgetChoices()
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            choices.useAccount("00000000");choices.card="ABCD1234"
            val delegate=engine.web.webViewClient
            engine.web.webViewClient=object:WebViewClient() {
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                    val path=request.url.path.orEmpty()
                    if(request.isForMainFrame)hits.computeIfAbsent(path){AtomicInteger()}.incrementAndGet()
                    if(path.endsWith("/tarjetas.xhtml") && serverFailure!=null) {
                        val body=serverFailure!!;serverFailure=null
                        val response=WebResourceResponse("text/html","UTF-8",500,"Internal Server Error",emptyMap(),body.byteInputStream())
                        interceptedHttpFailure=request to response
                        return response
                    }
                    if(request.url.getQueryParameter("authenticated")=="1")authenticated=true
                    return WebResourceResponse("text/html","UTF-8",("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'>"+html(path)).byteInputStream())
                }
                override fun onPageStarted(view:WebView,url:String?,icon:Bitmap?)=delegate.onPageStarted(view,url,icon)
                override fun onPageFinished(view:WebView,url:String?) {
                    // Intercepted responses do not reliably emit the network HTTP callback.
                    // Deliver the same callback before the completed body is inspected.
                    interceptedHttpFailure?.takeIf{it.first.url.toString()==url}?.let {
                        interceptedHttpFailure=null
                        delegate.onReceivedHttpError(view,it.first,it.second)
                    }
                    delegate.onPageFinished(view,url)
                }
                override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,errorResponse:WebResourceResponse)=delegate.onReceivedHttpError(view,request,errorResponse)
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest)=delegate.shouldOverrideUrlLoading(view,request)
            }
            engine.connect("00000000","synthetic-session-password")
        }
        home()
    }
    private fun home(){compose.waitUntil(20000){engine.state.stage=="balance"&&engine.state.minimum==26000L&&!engine.state.busy}}
    private fun expireMarkup(markup:String) {
        compose.runOnIdle {authenticated=false;engine.web.evaluateJavascript("document.body.innerHTML="+org.json.JSONObject.quote(markup),null)}
    }
    private fun shot(name:String) {
        compose.waitForIdle();android.os.SystemClock.sleep(300)
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }
    private fun signInAgain() {
        compose.onNodeWithText("Documento uruguayo").performScrollTo().performTextInput("00000000")
        compose.onNodeWithText("Contraseña de gub.uy").performScrollTo().performTextInput("synthetic-session-password")
        compose.onNodeWithText("Ingresar",useUnmergedTree=true).performScrollTo().performClick()
        home()
    }
    private fun prepareSavedSession() {
        val cookieReady=java.util.concurrent.atomic.AtomicBoolean(false)
        compose.runOnIdle {
            CookieManager.getInstance().setCookie(StmEngine.START,"BOLETERA_TEST_SESSION=synthetic; Path=/; Secure") {cookieReady.set(it)}
        }
        compose.waitUntil(5000){cookieReady.get()}
        compose.runOnIdle {engine.cancel();engine.savedAccess(true)}
    }
    @Test fun savedLiveSessionReturnsToHomeWithoutDecryptingCredentials() {
        setup();prepareSavedSession()
        val before=hits["/login"]?.get()?:0
        compose.runOnIdle {assertTrue(engine.trySavedSession())}
        home()
        compose.runOnIdle {
            assertEquals(before,hits["/login"]?.get()?:0)
            assertNull(StmEngine::class.java.getDeclaredField("document").apply{isAccessible=true}.get(engine))
            assertNull(StmEngine::class.java.getDeclaredField("password").apply{isAccessible=true}.get(engine))
            assertEquals(0,engine.state.accessRequestId);assertFalse(engine.state.sessionExpired)
            engine.forgetChoices()
        }
    }
    @Test fun aNewSignInAndFreshBalanceDoNotClearAnUnresolvedPayment() {
        setup()
        compose.runOnIdle {
            val choices=StmEngine::class.java.getDeclaredField("choices").apply{isAccessible=true}.get(engine) as JourneyPreferences
            assertTrue(PaymentJournal(compose.activity).write(choices.accountKey!!,PaymentRecord("ABCD1234","1033",56400,1000,26000,"pending","DEMO-PENDING")))
            engine.cancel();engine.connect("00000000","synthetic-session-password")
        }
        home()
        compose.runOnIdle {
            assertTrue(engine.state.paymentNeedsReview);engine.startExpress()
            assertNull(engine.state.activePayment);assertEquals("balance",engine.state.stage)
            assertEquals(0,hits["/app/mistm/cuenta/pages/recarga2.xhtml"]?.get()?:0)
            assertTrue(engine.acknowledgePaymentReviewed());engine.forgetChoices()
        }
    }
    @Test fun expiredSavedSessionRequestsUnlockOnceInsteadOfLoopingOrSendingCredentials() {
        setup();prepareSavedSession();authenticated=false;ssoAlive=false
        compose.runOnIdle {assertTrue(engine.trySavedSession())}
        compose.waitUntil(20000){engine.state.stage=="welcome"&&engine.state.sessionExpired}
        compose.runOnIdle {
            assertEquals(1,engine.state.accessRequestId);assertFalse(engine.trySavedSession())
            assertNull(StmEngine::class.java.getDeclaredField("document").apply{isAccessible=true}.get(engine))
            assertNull(StmEngine::class.java.getDeclaredField("password").apply{isAccessible=true}.get(engine))
            engine.forgetChoices()
        }
    }
    @Test fun expiredViewUsesExistingIdentitySessionWithoutAskingForCredentialsOrRepeatingCharge() {
        setup()
        val before=hits["/app/mistm/cuenta/pages/tarjetas.xhtml"]!!.get()
        ssoAlive=true
        expireMarkup("<div role='alert'>Sesión vencida</div>")
        compose.waitUntil(20000){(hits["/app/mistm/cuenta/pages/tarjetas.xhtml"]?.get()?:0)>before&&engine.state.stage=="balance"&&!engine.state.busy&&engine.state.minimum==26000L}
        compose.runOnIdle {assertEquals(0,engine.state.accessRequestId);assertFalse(engine.state.sessionExpired);assertEquals(82400L,engine.state.balance);assertNull(engine.state.activePayment)}
        assertEquals(0,hits["/app/mistm/cuenta/pages/recarga2.xhtml"]?.get()?:0)
        compose.runOnIdle {engine.forgetChoices()}
    }
    @Test fun loginOnAmountPageShowsSessionRecoveryAndManualLoginReturnsToFreshHome() {
        setup();ssoAlive=false
        expireMarkup(login)
        compose.waitUntil(20000){engine.state.stage=="welcome"&&engine.state.sessionExpired}
        val request=engine.state.accessRequestId
        compose.onNodeWithText("Tu sesión venció").assertIsDisplayed()
        compose.onNodeWithText("Enviar error al desarrollador").assertDoesNotExist()
        shot("session-expired-manual.png")
        compose.runOnIdle {engine.pause();engine.resume()}
        compose.runOnIdle {assertEquals(request,engine.state.accessRequestId);assertEquals("welcome",engine.state.stage)}
        signInAgain()
        compose.runOnIdle {assertEquals("ABCD1234",engine.state.selectedCard);assertEquals(82400L,engine.state.balance)}
        assertEquals(0,hits["/app/mistm/cuenta/pages/recarga2.xhtml"]?.get()?:0)
        compose.runOnIdle {engine.forgetChoices()}
    }
    @Test fun expiryAfterProviderSubmissionNeverResubmitsWhenTheUserSignsInAgain() {
        setup();ssoAlive=false
        compose.runOnIdle {engine.prepare(26000)}
        compose.waitUntil(15000){engine.state.stage=="paymentBoundary"}
        compose.runOnIdle {
            assertTrue(engine.savePayer(PayerProfile("session-test","Ficticia","Persona","Prueba","00000000","test@example.invalid","099123456")))
            engine.chooseProvider("1033");engine.beginPayment()
        }
        compose.waitUntil(20000){engine.state.stage=="welcome"&&engine.state.sessionExpired}
        compose.runOnIdle {assertTrue(engine.state.paymentNeedsReview);assertNull(engine.state.activePayment);assertNull(engine.state.amount)}
        signInAgain()
        assertEquals(1,hits["/app/mistm/cuenta/pages/recarga2.xhtml"]?.get())
        assertEquals(1,hits["/app/mistm/cuenta/pages/session-ended.xhtml"]?.get())
        compose.runOnIdle {assertEquals(82400L,engine.state.balance);assertNull(engine.state.activePayment);assertTrue(engine.state.paymentNeedsReview);engine.acknowledgePaymentReviewed();engine.forgetChoices()}
    }
    @Test fun httpSessionExpiryRecoversButAnUnrelatedServerFailureRemainsAnError() {
        setup();ssoAlive=true
        val before=hits["/app/mistm/cuenta/pages/tarjetas.xhtml"]!!.get()
        compose.runOnIdle {serverFailure="<div role='alert'>Sesión expirada</div>";engine.refresh()}
        compose.waitUntil(20000){(hits["/app/mistm/cuenta/pages/tarjetas.xhtml"]?.get()?:0)>=before+2&&engine.state.stage=="balance"&&!engine.state.busy}
        compose.runOnIdle {assertEquals(0,engine.state.accessRequestId);serverFailure="<p>El servicio no está disponible.</p>";engine.refresh()}
        try { compose.waitUntil(15000){engine.state.stage=="blocked"} } catch(e:Throwable) {
            throw AssertionError("HTTP failure state=${engine.state.stage}, busy=${engine.state.busy}, message=${engine.state.message}, hits=${hits.mapValues{it.value.get()}}",e)
        }
        compose.runOnIdle {assertFalse(engine.state.sessionExpired);assertEquals(0,engine.state.accessRequestId);engine.forgetChoices()}
    }
}
