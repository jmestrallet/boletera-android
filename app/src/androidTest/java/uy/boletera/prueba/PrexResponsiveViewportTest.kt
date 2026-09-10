package uy.boletera.prueba

import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/** Every URL is intercepted. The clickable iframe is a local colored button, NOT a real CAPTCHA. */
@RunWith(AndroidJUnit4::class)
class PrexResponsiveViewportTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun retainedWidgetAdaptsToWindowWidthAndExpandedChallenge() {
        lateinit var payment: EmbeddedPrexPayment
        var windowWidth by mutableStateOf(340f)
        compose.runOnIdle {
            payment = EmbeddedPrexPayment(compose.activity)
            val delegate = payment.web.webViewClient
            payment.web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    val html = if (request.url.host == "www.google.com") {
                        """<meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0}button{height:100vh;width:100%;border:0;background:cyan}</style><button onclick="parent.postMessage('fixture-touched','*')">LOCAL TEST</button>"""
                    } else {
                        """<meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0}iframe{border:0}alta-cliente,angular-recaptcha{display:block}</style><stepper-pago><alta-cliente><div style="height:1800px"></div><angular-recaptcha><iframe width="304" height="78" src="https://www.google.com/recaptcha/api2/anchor?size=normal" onload="window.fixtureLoads=(window.fixtureLoads||0)+1"></iframe></angular-recaptcha><button type="button">Continuar</button></alta-cliente></stepper-pago><script>window.fixtureClicks=0;window.originalFrame=document.querySelector('iframe');addEventListener('message',e=>{if(e.origin==='https://www.google.com'&&e.data==='fixture-touched')window.fixtureClicks++})</script>"""
                    }
                    return WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
                }
                override fun onPageStarted(view: WebView, url: String?, icon: android.graphics.Bitmap?) = delegate.onPageStarted(view, url, icon)
                override fun onPageFinished(view: WebView, url: String?) = delegate.onPageFinished(view, url)
            }
            compose.activity.setContent {
                val density = LocalDensity.current.density
                val cap = payment.challenge
                val ratio = if (payment.cssViewportWidth > 0) windowWidth / payment.cssViewportWidth else 1f
                val scale = if (cap == null) 1f else minOf(ratio, windowWidth / cap.width, 400f / cap.height)
                Box(Modifier.fillMaxSize()) {
                    PaymentBrowserView(payment, cap == null, cap, (windowWidth*density).roundToInt(), (550*density).roundToInt(),
                        if (cap == null) Modifier.size(1.dp) else Modifier.width((cap.width*scale).dp).height((cap.height*scale).dp))
                }
            }
            assertTrue(payment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-RESPONSIVE"))
        }
        fun js(code: String): String {
            val result = AtomicReference<String>()
            compose.runOnIdle { payment.web.evaluateJavascript(code) { result.set(it) } }
            compose.waitUntil(5000) { result.get() != null }
            return result.get()
        }
        fun touch() {
            compose.runOnIdle {
                val host = payment.web.parent as ViewGroup
                assertTrue(host.width > 0 && host.height > 0)
                val now = android.os.SystemClock.uptimeMillis()
                val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,host.width/2f,host.height/2f,0)
                val up=MotionEvent.obtain(now,now+60,MotionEvent.ACTION_UP,host.width/2f,host.height/2f,0)
                try { host.dispatchTouchEvent(down); host.dispatchTouchEvent(up) } finally { down.recycle(); up.recycle() }
            }
        }
        try {
            compose.waitUntil(15000) { payment.nativeStage == "payer" && payment.challenge != null }
            compose.runOnIdle { payment.positionVerification() }
            compose.waitUntil(5000) { (payment.challenge?.y ?: 999f) < 2f }
            for ((index, width) in listOf(340f, 280f, 380f).withIndex()) {
                compose.runOnIdle { windowWidth = width }
                compose.waitUntil(5000) { kotlin.math.abs(payment.cssViewportWidth-width) <= 2f }
                touch()
                val expected=index+1
                val count=AtomicReference(0)
                compose.waitUntil(4000) {
                    compose.activity.runOnUiThread { payment.web.evaluateJavascript("window.fixtureClicks") { count.set(it.toIntOrNull() ?: 0) } }
                    count.get() == expected
                }
                assertEquals("true",js("document.querySelector('iframe')===window.originalFrame"))
                assertEquals("1",js("window.fixtureLoads"))
            }
            js("""(()=>{const f=document.createElement('iframe');f.width=304;f.height=320;f.src='https://www.google.com/recaptcha/api2/bframe?fixture=1';document.body.prepend(f);return true})()""")
            compose.waitUntil(5000) { payment.expandedChallenge && payment.challenge?.height == 320f }
            compose.runOnIdle { payment.positionVerification() }
            touch()
            val count=AtomicReference(0)
            compose.waitUntil(4000) {
                compose.activity.runOnUiThread { payment.web.evaluateJavascript("window.fixtureClicks") { count.set(it.toIntOrNull() ?: 0) } }
                count.get() == 4
            }
            compose.runOnIdle { payment.restoreVerification() }
            assertEquals("false",js("document.querySelector('angular-recaptcha').hasAttribute('data-boletera-verification')"))
        } finally { compose.runOnIdle { payment.destroy() } }
    }
}
