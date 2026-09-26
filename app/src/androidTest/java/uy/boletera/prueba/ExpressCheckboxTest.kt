package uy.boletera.prueba

import android.graphics.Bitmap
import android.webkit.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.platform.LocalView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** Original Express UI and polling, with every request intercepted and no real account or payment. */
class ExpressCheckboxTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var payment:EmbeddedPrexPayment
    private lateinit var autofillHost:android.view.View
    private fun js(code:String):String {
        val value=AtomicReference<String?>(null)
        compose.runOnIdle {payment.web.evaluateJavascript(code){value.set(it)}}
        compose.waitUntil(5000){value.get()!=null};return value.get()!!
    }
    private fun setup(express:Boolean,card:Boolean=false,knownProvider:Boolean=false) {
        compose.runOnIdle {
            payment=EmbeddedPrexPayment(compose.activity)
            val delegate=payment.web.webViewClient
            payment.web.webViewClient=object:WebViewClient() {
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                    val root=if(card)"alta-tarjeta" else "alta-cliente"
                    val fields=if(card)listOf("nroTarjetaControl","expiracionControl","cvvControl") else listOf("nombreControl","apellidoControl","documentoControl","emailControl","celularControl")
                    val body=if(request.url.host=="www.google.com") """
                      <style>body{margin:0}button{position:absolute;left:14px;top:25px;width:28px;height:28px}</style>
                      <button onclick="parent.postMessage('checkbox','https://pasarelaspe.sistarbanc.com.uy')">✓</button>
                    """ else """
                      <style>body{margin:0}iframe{border:0}alta-cliente,alta-tarjeta,angular-recaptcha{display:block}</style>
                      ${if(knownProvider)"<script type='application/json' src='/v2/main-es2015.c4dd4374250f3678bcc8.js'></script><app-root ng-version='11.2.14'>" else ""}
                      <stepper-pago><$root><form class="ng-valid">
                      ${fields.joinToString(""){ "<input formcontrolname='$it'>" }}
                      <button type="button" onclick="window.continues++">Continuar</button></form>
                      <angular-recaptcha><re-captcha><iframe width="304" height="78" src="https://www.google.com/recaptcha/api2/anchor?size=normal"></iframe><textarea name="g-recaptcha-response" style="display:none"></textarea></re-captcha></angular-recaptcha>
                      </$root></stepper-pago>
                      ${if(knownProvider)"</app-root>" else ""}
                      <script>window.hits=0;window.continues=0;addEventListener('message',e=>{if(e.origin==='https://www.google.com'&&e.data==='checkbox'){window.hits++;document.querySelector('textarea').value='synthetic-provider-pending'}})</script>
                      ${if(knownProvider) """<script>
                        function bindFixture(host,selector){class Component{};Component['\u0275cmp']={type:Component,selectors:[[selector]]};const instance=new Component(),child=[];child[0]=host;child[1]={};child[8]=instance;const parent=[];parent[1]={components:[20]};parent[20]=child;host.__ngContext__=parent;return instance}
                        bindFixture(document.querySelector('app-root'),'app-root');
                        const verificationFixture=bindFixture(document.querySelector('angular-recaptcha'),'angular-recaptcha');
                        verificationFixture.recaptchaSuccess=false;verificationFixture.recaptchaRef={elementRef:{nativeElement:document.querySelector('re-captcha')}};
                        window.acceptProvider=()=>verificationFixture.recaptchaSuccess=true;
                      </script>""" else ""}
                    """
                    return WebResourceResponse("text/html","UTF-8",("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'>"+body).byteInputStream())
                }
                override fun onPageStarted(v:WebView,u:String?,b:Bitmap?)=delegate.onPageStarted(v,u,b)
                override fun onPageFinished(v:WebView,u:String?)=delegate.onPageFinished(v,u)
            }
            val profile=PayerProfile("checkbox","Prueba","Persona","Ficticia","00000000","test@example.invalid","099123456")
            assertTrue(payment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-CHECKBOX",profile,if(express)26000L else null,26000L))
            compose.activity.setContent {autofillHost=LocalView.current;BoleteraTheme("dark"){EmbeddedPrexScreen(payment,null,{})}}
        }
    }
    private fun fillCard(includeCvv:Boolean=true) {
        val values=android.util.SparseArray<android.view.autofill.AutofillValue>()
        (listOf("Número de tarjeta" to "4111111111111111","Vencimiento" to "12/39")+if(includeCvv)listOf("CVV" to "123") else emptyList()).forEach {(label,value)->
            values.put(compose.onNodeWithText(label).fetchSemanticsNode().id,android.view.autofill.AutofillValue.forText(value))
        }
        compose.runOnIdle {autofillHost.autofill(values)}
    }
    private fun waitForThreeSnapshots() {
        js("window.snapshots=0;const originalSnapshot=window.BoleteraNative.snapshot;window.BoleteraNative.snapshot=()=>{window.snapshots++;return originalSnapshot()}")
        compose.waitUntil(7000){js("window.snapshots>=3")=="true"}
    }
    @Test fun expressAttemptsOnceAndNeverTreatsCheckboxResponseAsProviderApproval() {
        setup(true)
        try {
            compose.waitUntil(15000){payment.nativeStage=="payer" && payment.challenge!=null}
            compose.waitUntil(10000){js("window.hits")=="1"}
            assertEquals("0",js("window.continues"))
            compose.runOnIdle {payment.pause();payment.resume()}
            js("window.snapshots=0;const old=window.BoleteraNative.snapshot;window.BoleteraNative.snapshot=()=>{window.snapshots++;return old()}")
            compose.waitUntil(7000){js("window.snapshots>=3")=="true"}
            assertEquals("1",js("window.hits"));assertEquals("0",js("window.continues"))
        } finally {compose.runOnIdle{payment.destroy()}}
    }
    @Test fun ordinaryChargeLeavesTheCheckboxUntouched() {
        setup(false)
        try {
            compose.waitUntil(15000){payment.nativeStage=="payer" && payment.challenge!=null}
            js("window.snapshots=0;const old=window.BoleteraNative.snapshot;window.BoleteraNative.snapshot=()=>{window.snapshots++;return old()}")
            compose.waitUntil(7000){js("window.snapshots>=3")=="true"}
            assertEquals("0",js("window.hits"));assertEquals("0",js("window.continues"))
        } finally {compose.runOnIdle{payment.destroy()}}
    }
    @Test fun cardCheckboxWaitsForCompleteAutofillAndVisibleVerification() {
        setup(true,card=true)
        try {
            compose.waitUntil(15000){payment.nativeStage=="card" && payment.challenge!=null}
            js("window.snapshots=0;const old=window.BoleteraNative.snapshot;window.BoleteraNative.snapshot=()=>{window.snapshots++;return old()}")
            compose.waitUntil(7000){js("window.snapshots>=3")=="true"}
            assertEquals("0",js("window.hits"))
            fillCard()
            compose.waitUntil(10000){js("window.hits")=="1"}
            assertEquals("0",js("window.continues"))
            assertEquals("card",payment.nativeStage)
        } finally {compose.runOnIdle{payment.destroy()}}
    }
    @Test fun acceptedPayerContinuesOnceAfterServerSignalAndOnlyWhenResumed() {
        setup(true,knownProvider=true)
        try {
            compose.waitUntil(15000){payment.nativeStage=="payer" && payment.challenge!=null}
            compose.waitUntil(10000){js("window.hits")=="1"}
            waitForThreeSnapshots();assertEquals("0",js("window.continues"))
            compose.runOnIdle {payment.pause()}
            js("window.acceptProvider();window.BoleteraExpress.tick()")
            assertEquals("0",js("window.continues"))
            compose.runOnIdle {payment.resume()}
            compose.waitUntil(10000){js("window.continues")=="1"}
            waitForThreeSnapshots();assertEquals("1",js("window.continues"))
        } finally {compose.runOnIdle{payment.destroy()}}
    }
    @Test fun acceptedCardAutofillContinuesOnceWithoutManualContinue() {
        setup(true,card=true,knownProvider=true)
        try {
            compose.waitUntil(15000){payment.nativeStage=="card" && payment.challenge!=null}
            fillCard()
            compose.waitUntil(10000){js("window.hits")=="1"}
            waitForThreeSnapshots();assertEquals("0",js("window.continues"))
            compose.runOnIdle {payment.pause()}
            js("window.acceptProvider()")
            assertEquals("0",js("window.continues"))
            compose.runOnIdle {payment.resume()}
            compose.waitUntil(10000){js("window.continues")=="1"}
            waitForThreeSnapshots();assertEquals("1",js("window.continues"))
        } finally {compose.runOnIdle{payment.destroy()}}
    }
    @Test fun partialAutofillAndDoneContinueOnceAfterProviderAcceptance() {
        setup(true,card=true,knownProvider=true)
        try {
            compose.waitUntil(15000){payment.nativeStage=="card" && payment.challenge!=null}
            fillCard(includeCvv=false)
            compose.onNodeWithText("CVV").performTextInput("123")
            waitForThreeSnapshots();assertEquals("0",js("window.continues"))
            compose.onNodeWithText("CVV").performImeAction()
            compose.waitUntil(10000){js("window.hits")=="1"}
            waitForThreeSnapshots();assertEquals("0",js("window.continues"))
            js("window.acceptProvider()")
            compose.waitUntil(10000){js("window.continues")=="1"}
            waitForThreeSnapshots();assertEquals("1",js("window.continues"))
        } finally {compose.runOnIdle{payment.destroy()}}
    }
}
