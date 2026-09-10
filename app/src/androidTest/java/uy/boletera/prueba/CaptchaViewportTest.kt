package uy.boletera.prueba

import android.view.MotionEvent
import android.webkit.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Fake iframe and all URLs served locally. No Google challenge, token or CAPTCHA service is used. */
@RunWith(AndroidJUnit4::class)
class CaptchaViewportTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun offscreenOriginalFrameIsPositionedBeforeCroppingAndAcceptsNativeTouch() {
        lateinit var engine: StmEngine
        compose.runOnIdle {
            engine = MainActivity::class.java.getDeclaredField("engine").apply { isAccessible = true }.get(compose.activity) as StmEngine
            val delegate = engine.web.webViewClient
            engine.web.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                    val html = if (request.url.host == "www.google.com") {
                        """<meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0}button{height:100vh;width:100%;border:0;background:cyan}</style><button onclick="parent.postMessage('fixture-touched','*')">PRUEBA LOCAL</button>"""
                    } else if (request.url.host == "mi.iduruguay.gub.uy") {
                        """<meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0}iframe{border:0;margin-top:2500px;margin-left:10px}</style><input type="password"><iframe width="304" height="78" src="https://www.google.com/recaptcha/api2/anchor?size=normal"></iframe><div style="height:1000px"></div><script>window.fixtureTouched=false;addEventListener('message',e=>{if(e.origin==='https://www.google.com'&&e.data==='fixture-touched')window.fixtureTouched=true})</script>"""
                    } else """<button onclick="location.href='https://mi.iduruguay.gub.uy/login'">INGRESAR CON USUARIO GUB.UY</button>"""
                    return WebResourceResponse("text/html", "UTF-8", html.byteInputStream())
                }
                override fun onPageStarted(view: WebView, url: String?, icon: android.graphics.Bitmap?) = delegate.onPageStarted(view, url, icon)
                override fun onPageFinished(view: WebView, url: String?) = delegate.onPageFinished(view, url)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = delegate.shouldOverrideUrlLoading(view, request)
            }
            engine.connect("00000000", "synthetic-only")
        }
        fun script(code: String): String {
            val result = AtomicReference<String>()
            compose.runOnIdle { engine.web.evaluateJavascript(code) { result.set(it) } }
            compose.waitUntil(5000) { result.get() != null }
            return result.get()
        }
        try {
            compose.waitUntil(15000) { engine.state.stage == "captcha" && engine.state.captcha != null && !engine.state.busy }
            assertEquals("Original iframe is outside the WebView viewport; native translation alone produces an empty panel", "true",
                script("(()=>{const r=document.querySelector('iframe').getBoundingClientRect();return r.top>=-1&&r.bottom<=innerHeight+1})()"))
            compose.runOnIdle {
                val host = engine.host
                val now = android.os.SystemClock.uptimeMillis()
                val x = host.width / 2f; val y = host.height / 2f
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
                val up = MotionEvent.obtain(now, now + 60, MotionEvent.ACTION_UP, x, y, 0)
                try { host.dispatchTouchEvent(down); host.dispatchTouchEvent(up) } finally { down.recycle(); up.recycle() }
            }
            val touched = AtomicReference(false)
            try {
                compose.waitUntil(3000) {
                    compose.activity.runOnUiThread { engine.web.evaluateJavascript("window.fixtureTouched") { if (it == "true") touched.set(true) } }
                    touched.get()
                }
            } catch (error: Throwable) {
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                    .executeShellCommand("screencap -p /sdcard/Download/captcha-test.png").use { java.io.FileInputStream(it.fileDescriptor).readBytes() }
                val shape = script("JSON.stringify({dpr:devicePixelRatio,scroll:scrollY,viewport:[innerWidth,innerHeight],frame:document.querySelector('iframe').getBoundingClientRect().toJSON()})")
                throw AssertionError("Native host=${engine.host.width}x${engine.host.height}, webScale=${engine.web.scaleX}, webXY=${engine.web.left},${engine.web.top}, nativeScroll=${engine.web.scrollY}, shape=$shape", error)
            }
            script("""(()=>{window.fixtureTouched=false;const f=document.createElement('iframe');f.width=304;f.height=320;f.src='https://www.google.com/recaptcha/api2/bframe?fixture=1';document.body.append(f);return true})()""")
            compose.waitUntil(10000) { engine.state.captcha?.height == 320f && !engine.state.busy }
            val expandedShape = script("JSON.stringify({viewport:innerHeight,scroll:scrollY,frame:document.querySelector('iframe[src*=bframe]').getBoundingClientRect().toJSON()})")
            // Android's integer viewport and fractional scroll offsets differ by sub-pixel rounding.
            assertEquals(expandedShape, "true", script("(()=>{const r=document.querySelector('iframe[src*=bframe]').getBoundingClientRect();return r.top>=-1&&r.bottom<=innerHeight+1})()"))
            compose.runOnIdle {
                val host = engine.host
                val now = android.os.SystemClock.uptimeMillis()
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, host.width / 2f, host.height / 2f, 0)
                val up = MotionEvent.obtain(now, now + 60, MotionEvent.ACTION_UP, host.width / 2f, host.height / 2f, 0)
                try { host.dispatchTouchEvent(down); host.dispatchTouchEvent(up) } finally { down.recycle(); up.recycle() }
            }
            touched.set(false)
            compose.waitUntil(3000) {
                compose.activity.runOnUiThread { engine.web.evaluateJavascript("window.fixtureTouched") { if (it == "true") touched.set(true) } }
                touched.get()
            }
            assertEquals("false", script("document.querySelector('input').value.length>0"))
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("screencap -p /sdcard/Download/captcha-expanded-test.png").use { java.io.FileInputStream(it.fileDescriptor).readBytes() }
        } finally { compose.runOnIdle { engine.cancel() } }
    }
}
