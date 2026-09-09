package uy.boletera.prueba

import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Opt-in live probe. Only navigates public login pages: no document, password or recarga sent. */
@RunWith(AndroidJUnit4::class)
class PublicSiteProbe {
    @Test fun publicLoginIsRecognizedInsideAndroidWebView() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val finished = CountDownLatch(1)
        val lastStage = AtomicReference("not-loaded")
        var web: WebView? = null
        var done = false
        var sent = ""
        var diagnostic = ""
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        lateinit var poll: Runnable
        scenario.onActivity { activity ->
            val script = activity.assets.open("stm-adapter.js").bufferedReader().use { it.readText() }
            web = WebView(activity).apply {
                settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                settings.allowFileAccess = false; settings.allowContentAccess = false
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest) =
                        request.isForMainFrame && !NavigationPolicy.allowed(request.url.toString())
                    override fun onReceivedSslError(view: WebView, ssl: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                        ssl.cancel()
                        lastStage.set("TLS_REJECTED_${error.primaryError}")
                        done = true; finished.countDown()
                    }
                    override fun onReceivedError(view: WebView, request: android.webkit.WebResourceRequest, error: android.webkit.WebResourceError) {
                        if (request.isForMainFrame) { lastStage.set("NETWORK_ERROR_${error.errorCode}"); done = true; finished.countDown() }
                    }
                }
                val parent = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                parent.addView(this, android.view.ViewGroup.LayoutParams(1080, 1600))
                visibility = android.view.View.INVISIBLE
            }
            poll = object : Runnable {
                override fun run() {
                    if (done) return
                    web?.evaluateJavascript(script + "\nJSON.stringify(window.BoleteraAdapter.snapshot())") { raw ->
                        try {
                            val state = JSONObject(JSONTokener(raw).nextValue() as String)
                            val stage = state.optString("stage")
                            lastStage.set(stage)
                            if (stage == "unknown") web?.evaluateJavascript("JSON.stringify({host:location.hostname,path:location.pathname,title:document.title,buttons:[...document.querySelectorAll('button,a,input[type=submit]')].map(x=>(x.textContent||x.value||'').trim()).filter(Boolean).slice(0,25)})") { info ->
                                if (info != diagnostic) { diagnostic = info; android.util.Log.i("BoleteraPublicProbe", info) }
                            }
                            if (stage == "document" || !state.isNull("captcha")) { done = true; finished.countDown() }
                            else if (stage in setOf("start", "identity") && sent != stage) {
                                sent = stage
                                web?.evaluateJavascript("window.BoleteraAdapter.command('$stage', '')", null)
                            }
                        } catch (_: Exception) { /* transitional page */ }
                    }
                    handler.postDelayed(this, 1000)
                }
            }
            web?.loadUrl(StmEngine.START)
            handler.postDelayed(poll, 1000)
        }
        val completed = finished.await(90, TimeUnit.SECONDS)
        scenario.onActivity {
            done = true; handler.removeCallbacks(poll)
            web?.let { (it.parent as? android.view.ViewGroup)?.removeView(it); it.destroy() }
        }
        scenario.close()
        assertTrue("Live page was not recognized. Last safe stage: ${lastStage.get()}", completed)
        assertTrue("Public login blocked: ${lastStage.get()}", lastStage.get() in setOf("document", "identity", "verification", "password", "start"))
    }
}
