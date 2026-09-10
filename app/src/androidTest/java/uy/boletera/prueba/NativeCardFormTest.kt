package uy.boletera.prueba

import android.graphics.Bitmap
import android.view.WindowManager
import android.webkit.*
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Fictitious card, intercepted page: no account, provider connection, or financial operation. */
@RunWith(AndroidJUnit4::class)
class NativeCardFormTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun validationClearingAndScreenshotsAllowed() {
        var submits=0
        var visible by mutableStateOf(true)
        compose.activity.setContent { BoleteraTheme {
            Surface { if(visible) NativeCardForm(false,true,false,false,{pan,expiry,cvv->
                assertEquals("4111111111111111",pan);assertEquals("12/39",expiry);assertEquals("123",cvv);submits++
            },{}) }
        } }
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Revisá el número de tarjeta.").assertExists();assertEquals(0,submits)
        compose.onNodeWithText("Número de tarjeta").performTextInput("4111111111111111")
        compose.onNodeWithText("Vencimiento").performTextInput("1239")
        compose.onNodeWithText("CVV").performTextInput("123")
        compose.onNodeWithText("Continuar").performClick()
        assertEquals(1,submits)
        compose.onNodeWithText("CVV").assertTextEquals("CVV","")
        compose.runOnIdle { assertEquals(0,compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE);visible=false }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0,compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE);visible=true }
        compose.onNodeWithText("Número de tarjeta").assertTextEquals("Número de tarjeta","")
        compose.onNodeWithText("CVV").assertTextEquals("CVV","")
        compose.onNodeWithText("Número de tarjeta").performTextInput("4111111111111111")
        compose.onNodeWithText("CVV").performTextInput("123")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("Número de tarjeta").assertTextEquals("Número de tarjeta","")
        compose.onNodeWithText("CVV").assertTextEquals("CVV","")
    }

    @Test fun realWebViewReceivesOneExplicitSubmission() {
        lateinit var payment: EmbeddedPrexPayment
        compose.runOnIdle {
            payment=EmbeddedPrexPayment(compose.activity)
            val delegate=payment.web.webViewClient
            payment.web.webViewClient=object: WebViewClient() {
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest)=WebResourceResponse("text/html","UTF-8",("""
                    <!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><style>alta-tarjeta{display:block}input{display:block}</style>
                    <stepper-pago><alta-tarjeta><form><input formcontrolname="nroTarjetaControl" maxlength="19"><input formcontrolname="expiracionControl" maxlength="5"><input formcontrolname="cvvControl" maxlength="4"><button type="button">Continuar</button></form></alta-tarjeta></stepper-pago>
                    <script>window.sent=0;window.correct=false;document.querySelector('button').onclick=()=>{window.sent++;const f=document.querySelectorAll('input');window.correct=f[0].value==='4111 1111 1111 1111'&&f[1].value==='12/39'&&f[2].value==='123';document.querySelector('button').disabled=true;};</script>
                """).byteInputStream())
                override fun onPageStarted(v:WebView,u:String?,b:Bitmap?)=delegate.onPageStarted(v,u,b)
                override fun onPageFinished(v:WebView,u:String?)=delegate.onPageFinished(v,u)
            }
            assertTrue(payment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=NATIVE-CARD-FIXTURE"))
            compose.activity.setContent { BoleteraTheme { EmbeddedPrexScreen(payment,null,{}) } }
        }
        fun js(source:String):String {
            val latch=CountDownLatch(1);val result=AtomicReference<String>()
            compose.runOnIdle { payment.web.evaluateJavascript(source){result.set(it);latch.countDown()} }
            assertTrue(latch.await(5,TimeUnit.SECONDS));return result.get()
        }
        try {
            compose.waitUntil(15000){payment.nativeStage=="card" && payment.canContinue}
            compose.onNodeWithText("Tu tarjeta").assertIsDisplayed()
            compose.onNodeWithText("Número de tarjeta").performTextInput("4111111111111111")
            compose.onNodeWithText("Vencimiento").performTextInput("1239")
            compose.onNodeWithText("CVV").performTextInput("123")
            compose.onNodeWithText("Continuar").performClick()
            compose.runOnIdle { payment.submitCard("4111111111111111","12/39","123") }
            assertEquals("1",js("window.sent"));assertEquals("true",js("window.correct"))
            assertEquals("false",js("JSON.stringify(window.BoleteraNative.snapshot()).includes('4111')"))
            compose.onNodeWithText("CVV").assertTextEquals("CVV","")
            assertEquals("false",js("window.BoleteraNative.advance('card')"))
            compose.runOnIdle { payment.hide() }
            assertEquals("1",js("window.sent"))
        } finally { compose.runOnIdle { payment.destroy() } }
    }

    @Test fun captureNativeFormLightAndDark() {
        var dark by mutableStateOf(false)
        compose.activity.setContent { BoleteraTheme(appearance=if(dark)"dark" else "light") {
            Surface { Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                AppTopBar(title="Pago con Prex",onBack={})
                NativeCardForm(false,true,false,false,{_,_,_->},{})
            } }
        } }
        for(name in listOf("light","dark")) {
            compose.runOnIdle { dark=name=="dark" };compose.waitForIdle()
            compose.onNodeWithText("Continuar").assertIsDisplayed()
            val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(compose.activity.getExternalFilesDir(null),"native-card-$name.png").outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
            compose.onNodeWithText("CVV").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Vencimiento").assertIsDisplayed()
            val fields=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(compose.activity.getExternalFilesDir(null),"native-card-fields-$name.png").outputStream().use {fields.compress(Bitmap.CompressFormat.PNG,100,it)};fields.recycle()
        }
    }
}
