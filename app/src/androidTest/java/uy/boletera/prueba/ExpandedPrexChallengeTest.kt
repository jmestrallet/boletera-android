package uy.boletera.prueba

import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Original browser/iframes, but ALL responses and touches are fictitious. No real CAPTCHA is solved. */
class ExpandedPrexChallengeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var payment: EmbeddedPrexPayment
    private fun js(code:String):String {
        val result=AtomicReference<String>()
        compose.runOnIdle {payment.web.evaluateJavascript(code){result.set(it)}}
        compose.waitUntil(5000){result.get()!=null}
        return result.get()
    }
    private fun touch(fraction:Float) {
        compose.runOnIdle {
            val host=payment.web.parent as ViewGroup
            assertTrue(host.width>0 && host.height>0)
            val now=android.os.SystemClock.uptimeMillis()
            val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,host.width/2f,host.height*fraction,0)
            val up=MotionEvent.obtain(now,now+60,MotionEvent.ACTION_UP,host.width/2f,host.height*fraction,0)
            try {host.dispatchTouchEvent(down);host.dispatchTouchEvent(up)} finally {down.recycle();up.recycle()}
        }
    }
    private fun check(stage:String) {
        compose.runOnIdle {
            payment=EmbeddedPrexPayment(compose.activity)
            val delegate=payment.web.webViewClient
            payment.web.webViewClient=object:WebViewClient() {
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse {
                    val html=if(request.url.host=="www.google.com") {
                        if(request.url.path!!.endsWith("bframe")) """<meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0;height:100%;font:18px sans-serif}button{display:block;width:100%;border:0;color:white}#heading{height:15%;background:#1458c8}main{height:70%;display:grid;grid-template-columns:repeat(3,1fr);gap:4px;background:white}main div{background:#91c694}#footer{height:15%;background:#133e28}</style><button id="heading" onclick="parent.postMessage('fixture-heading','*')">CONSIGNA DE PRUEBA<br>Seleccioná las figuras ficticias</button><main>${"<div>DEMO</div>".repeat(9)}</main><button id="footer" onclick="parent.postMessage('fixture-footer','*')">VERIFICAR · DEMO LOCAL</button>"""
                        else """<style>html,body{margin:0;background:#f00080;height:100%}</style>CASILLA INICIAL FICTICIA"""
                    } else {
                        val tag=if(stage=="card")"alta-tarjeta" else "alta-cliente"
                        val fields=if(stage=="card")listOf("nroTarjetaControl","expiracionControl","cvvControl") else listOf("nombreControl","apellidoControl","documentoControl","emailControl","celularControl")
                        """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0}iframe{border:0}alta-cliente,alta-tarjeta,angular-recaptcha{display:block}input{display:block}</style><stepper-pago><$tag>${fields.joinToString(""){"<input formcontrolname='$it'>"}}<angular-recaptcha><iframe width="304" height="78" src="https://www.google.com/recaptcha/api2/anchor?size=normal" onload="window.anchorLoads=(window.anchorLoads||0)+1"></iframe></angular-recaptcha><button type="button">Continuar</button></$tag></stepper-pago><script>window.initialAnchor=document.querySelector('iframe');window.hits=[];addEventListener('message',e=>{if(e.origin==='https://www.google.com'&&String(e.data).startsWith('fixture-'))window.hits.push(e.data)})</script>"""
                    }
                    return WebResourceResponse("text/html","UTF-8",html.byteInputStream())
                }
                override fun onPageStarted(v:WebView,u:String?,b:Bitmap?)=delegate.onPageStarted(v,u,b)
                override fun onPageFinished(v:WebView,u:String?)=delegate.onPageFinished(v,u)
            }
            compose.activity.setContent {BoleteraTheme("dark") {EmbeddedPrexScreen(payment,null,{})}}
            assertTrue(payment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-TALL-CHALLENGE"))
        }
        try {
            compose.waitUntil(15000){payment.nativeStage==stage && payment.challenge!=null && !payment.busy}
            compose.waitUntil(5000){(payment.challenge?.y?:999f)<2f}
            if(stage=="card")compose.onNodeWithText("Número de tarjeta").performScrollTo().performTextInput("4111111111111111")
            js("""(()=>{const overlay=document.createElement('div');overlay.id='fixture-overlay';overlay.style='position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);z-index:2000000000';overlay.innerHTML='<iframe width="304" height="680" src="https://www.google.com/recaptcha/api2/bframe?fixture=1" onload="window.challengeLoads=(window.challengeLoads||0)+1"></iframe>';document.body.append(overlay);return true})()""")
            compose.waitUntil(10000){payment.expandedChallenge && payment.challenge?.height==680f}
            val complete=AtomicReference(false)
            compose.waitUntil(5000){
                compose.activity.runOnUiThread {payment.web.evaluateJavascript("(()=>{const r=document.querySelector('iframe[src*=bframe]').getBoundingClientRect();return r.top>=-1 && r.bottom<=innerHeight+1 && Math.abs(r.y-${payment.challenge?.y?:-9999f})<1 && Math.abs(r.x-${payment.challenge?.x?:-9999f})<1})()"){complete.set(it=="true")}}
                complete.get()
            }
            compose.waitForIdle()
            assertEquals("The positioned checkbox must not cover the challenge instruction", "true",js("(()=>{const f=document.querySelector('iframe[src*=bframe]'),r=f.getBoundingClientRect();return document.elementFromPoint(r.x+r.width/2,r.y+20)===f})()"))
            compose.onNodeWithText("Continuar a la tarjeta").assertDoesNotExist()
            compose.onNodeWithText("Número de tarjeta").assertDoesNotExist()
            touch(0.05f);touch(0.95f)
            val touched=AtomicReference(false)
            try {
                compose.waitUntil(5000){compose.activity.runOnUiThread {payment.web.evaluateJavascript("window.hits.includes('fixture-heading')&&window.hits.includes('fixture-footer')"){touched.set(it=="true")}};touched.get()}
            } catch(error:Throwable) {
                val shape=js("JSON.stringify({hits:window.hits,viewport:[innerWidth,innerHeight],frame:document.querySelector('iframe[src*=bframe]').getBoundingClientRect().toJSON()})")
                throw AssertionError("Synthetic challenge $shape, crop=${payment.challenge}, host=${payment.web.parent}, scale=${payment.web.scaleX}, web=${payment.web.width}x${payment.web.height}, xy=${payment.web.left},${payment.web.top}",error)
            }
            assertEquals("1",js("window.anchorLoads"));assertEquals("1",js("window.challengeLoads"))
            assertEquals("true",js("document.querySelector('iframe')===window.initialAnchor"))
            compose.runOnIdle {assertEquals(0,compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)}
            compose.waitForIdle()
            val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            assertNotNull(screenshot)
            File(compose.activity.getExternalFilesDir(null),"expanded-challenge-$stage.png").outputStream().use {screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
            screenshot.recycle()
            js("document.getElementById('fixture-overlay').remove();true")
            compose.waitUntil(5000){!payment.expandedChallenge}
            if(stage=="card")compose.onNodeWithText("Número de tarjeta").assertTextContains("4111 1111 1111 1111")
            else compose.onNodeWithText("Continuar a la tarjeta").assertExists()
        } finally {compose.runOnIdle {payment.destroy()}}
    }
    @Test fun tallChallengeAtPayerKeepsInstructionFooterAndCapture()=check("payer")
    @Test fun tallChallengeAtCardKeepsInstructionFooterAndTemporaryFields()=check("card")
}
