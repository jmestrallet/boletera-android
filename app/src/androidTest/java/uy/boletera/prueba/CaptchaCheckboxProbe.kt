package uy.boletera.prueba

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** Research harness only. Live Google demo needs -e liveCaptcha true; never opens a payment. */
class CaptchaCheckboxProbe {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var web: WebView
    private var attempted = false
    private fun js(code: String): String {
        val value=AtomicReference<String?>(null)
        compose.runOnIdle {web.evaluateJavascript(code) {value.set(it)}}
        compose.waitUntil(5000) {value.get()!=null}
        return value.get()!!
    }
    private fun create(client: WebViewClient, url: String) {
        compose.runOnIdle {
            web=WebView(compose.activity).apply {
                settings.javaScriptEnabled=true;settings.domStorageEnabled=true
                settings.allowFileAccess=false;settings.allowContentAccess=false
                settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.userAgentString=EmbeddedPrexPayment.compatibilityIdentity(settings.userAgentString)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this,false)
                webViewClient=client
            }
            compose.activity.findViewById<ViewGroup>(android.R.id.content).addView(web,ViewGroup.LayoutParams(-1,-1))
            web.loadUrl(url)
        }
    }
    private fun close() = compose.runOnIdle {
        (web.parent as? ViewGroup)?.removeView(web);web.destroy()
    }
    private val geometry = """(()=>{
      const visible=e=>{const r=e.getBoundingClientRect();return r.width>0&&r.height>0&&getComputedStyle(e).visibility!=='hidden'};
      const frames=[...document.querySelectorAll('iframe')].filter(visible);
      if(frames.some(f=>{try{return /\/recaptcha\/(api2|enterprise)\/bframe$/.test(new URL(f.src).pathname)}catch{return false}}))return null;
      const anchors=frames.filter(f=>{try{const u=new URL(f.src);return u.protocol==='https:'&&['www.google.com','www.recaptcha.net'].includes(u.hostname)&&/^\/recaptcha\/(api2|enterprise)\/anchor$/.test(u.pathname)&&u.searchParams.get('size')==='normal'}catch{return false}});
      if(anchors.length!==1)return null;
      const f=anchors[0],r=f.getBoundingClientRect();
      if(Math.abs(r.width-304)>2||Math.abs(r.height-78)>2||r.x<0||r.y<0||r.right>innerWidth||r.bottom>innerHeight)return null;
      if(document.elementFromPoint(r.x+28,r.y+39)!==f)return null;
      return JSON.stringify({x:r.x+28,y:r.y+39,viewport:innerWidth});
    })()"""
    private fun tapOnce(): Boolean {
        if(attempted)return false
        val raw=JSONTokener(js(geometry)).nextValue() as? String?:return false
        val point=JSONObject(raw)
        compose.runOnIdle {
            assertTrue(web.isAttachedToWindow && web.isShown && web.hasWindowFocus())
            val scale=web.width/point.getDouble("viewport")
            val x=(point.getDouble("x")*scale).toFloat();val y=(point.getDouble("y")*scale).toFloat()
            attempted=true
            val time=SystemClock.uptimeMillis()
            MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,x,y,0).also {web.dispatchTouchEvent(it);it.recycle()}
            MotionEvent.obtain(time,time+60,MotionEvent.ACTION_UP,x,y,0).also {web.dispatchTouchEvent(it);it.recycle()}
        }
        return true
    }
    @Test fun nativeTouchReachesCrossOriginCheckboxButAnIframeClickDoesNot() {
        val client=object: WebViewClient() {
            override fun shouldInterceptRequest(view: WebView,request: WebResourceRequest): WebResourceResponse {
                val html=if(request.url.host=="www.google.com") """
                  <style>body{margin:0}button{position:absolute;left:14px;top:25px;width:28px;height:28px}</style>
                  <button onclick="parent.postMessage({clicked:true,trusted:event.isTrusted},'https://pasarelaspe.sistarbanc.com.uy')">✓</button>
                  <script>parent.postMessage({ready:true},'https://pasarelaspe.sistarbanc.com.uy')</script>
                """ else """
                  <style>iframe{border:0;margin:24px}</style>
                  <stepper-pago><alta-cliente><angular-recaptcha><iframe src="https://www.google.com/recaptcha/api2/anchor?size=normal" width="304" height="78"></iframe></angular-recaptcha></alta-cliente></stepper-pago>
                  <script>window.hits=0;window.fixtureReady=false;addEventListener('message',e=>{if(e.origin!=='https://www.google.com')return;if(e.data.ready)window.fixtureReady=true;if(e.data.clicked){hits++;window.trusted=e.data.trusted}})</script>
                """
                return WebResourceResponse("text/html","UTF-8",("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'>"+html).byteInputStream())
            }
        }
        create(client,"https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-CHECKBOX")
        try {
            compose.waitUntil(15000) {js("window.fixtureReady===true")=="true"}
            assertEquals("true",js("(()=>{try{document.querySelector('iframe').contentWindow.document;return false}catch{return true}})()"))
            js("document.querySelector('iframe').click()")
            assertEquals("0",js("window.hits"))
            val code=compose.activity.assets.open("prex-checkbox.js").bufferedReader().use {it.readText()}
            js(code)
            lateinit var tapper:CheckboxTapper
            val geometryValue=JSONObject(JSONTokener(js(geometry)).nextValue() as String)
            compose.runOnIdle {
                val width=web.width
                (web.parent as ViewGroup).removeView(web)
                val host=PaymentPageHost(compose.activity,web).apply {
                    viewportWidth=width;viewportHeight=1600
                    cssViewportWidth=geometryValue.getDouble("viewport").toFloat()
                    crop=CaptchaRect((geometryValue.getDouble("x")-28).toFloat(),(geometryValue.getDouble("y")-39).toFloat(),304f,78f)
                    nativeHidden=false
                }
                compose.activity.findViewById<ViewGroup>(android.R.id.content).addView(host,ViewGroup.LayoutParams(600,160))
            }
            compose.waitForIdle()
            // A real gesture arriving while the paint callback is pending cancels the automation.
            compose.runOnIdle {
                val cancelled=CheckboxTapper(web){true}
                cancelled.request("payer");cancelled.userTouched()
                assertEquals(0,cancelled.attempts)
            }
            js("true")
            assertEquals("0",js("window.hits"))
            compose.runOnIdle {
                tapper=CheckboxTapper(web){true}
                web.setOnTouchListener {_,e->if(e.actionMasked==MotionEvent.ACTION_DOWN)tapper.userTouched();false}
                tapper.request("payer")
            }
            compose.waitUntil(5000) {js("window.hits")=="1"}
            assertEquals("true",js("window.trusted"))
            compose.runOnIdle {tapper.request("payer");assertEquals(1,tapper.attempts)}
            assertEquals("1",js("window.hits"))
            compose.runOnIdle {tapper.cancel();tapper.request("payer");assertEquals(1,tapper.attempts)}
        } finally {close()}
    }
    @Test fun liveGoogleDemoReceivesOneAttemptAndKeepsAnyImagesManual() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveCaptcha")=="true")
        create(object:WebViewClient(){},"https://www.google.com/recaptcha/api2/demo")
        try {
            compose.waitUntil(30000) {js("document.querySelectorAll('iframe[src*=\"/anchor\"]').length>0")=="true"}
            js("document.querySelector('iframe[src*=\"/anchor\"]').scrollIntoView({block:'center'})")
            compose.waitUntil(10000) {js(geometry)!="null"}
            // Wait for a committed paint, not just the iframe element in the outer document.
            val painted=java.util.concurrent.CountDownLatch(1)
            compose.runOnIdle {web.postVisualStateCallback(1,object:WebView.VisualStateCallback(){override fun onComplete(id:Long){painted.countDown()}})}
            assertTrue(painted.await(10,java.util.concurrent.TimeUnit.SECONDS))
            assertTrue(tapOnce())
            val result=AtomicReference("waiting")
            compose.waitUntil(20000) {
                // Only a boolean is returned. No response token is copied, logged or submitted.
                val raw=js("(()=>{if([...document.querySelectorAll('textarea[name=g-recaptcha-response]')].some(e=>e.value.length>0))return 'accepted';if([...document.querySelectorAll('iframe')].some(e=>e.src.includes('/bframe')&&e.getBoundingClientRect().width>0&&e.getBoundingClientRect().height>0&&getComputedStyle(e).visibility!=='hidden'))return 'challenge';return 'waiting'})()")
                result.set(JSONTokener(raw).nextValue() as String)
                result.get()!="waiting"
            }
            assertFalse(tapOnce())
            val instrumentation=InstrumentationRegistry.getInstrumentation()
            val screenshot=instrumentation.uiAutomation.takeScreenshot()
            java.io.File(compose.activity.getExternalFilesDir(null),"captcha-checkbox-live.png").outputStream().use {screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
            screenshot.recycle()
            val report="Google public demo, Android WebView, application identity, third-party cookies disabled. One native attempt. Outcome: ${result.get()}. No image/audio interaction, no form submission. Not a measurement on STM/Prex."
            java.io.File(compose.activity.getExternalFilesDir(null),"captcha-checkbox-live.txt").writeText(report)
            android.util.Log.i("BoleteraCaptchaProbe",report)
        } finally {close()}
    }
}
