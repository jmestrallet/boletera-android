package uy.boletera.prueba

import android.view.ViewGroup
import android.webkit.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/** Explicit research probe of public static assets. API calls, other hosts and all POSTs are blocked. */
class GatewayVerificationProbe {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun currentPublicGatewayRuntimeSupportsTheBoundComponentReader() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveGateway")=="true")
        lateinit var web:WebView
        fun js(source:String):String {
            val result=AtomicReference<String?>(null)
            compose.runOnIdle {web.evaluateJavascript(source){result.set(it)}}
            compose.waitUntil(5000){result.get()!=null};return result.get()!!
        }
        compose.runOnIdle {
            val engine=MainActivity::class.java.getDeclaredField("engine").apply {isAccessible=true}.get(compose.activity) as StmEngine
            engine.cancel()
            web=WebView(compose.activity).apply {
                settings.javaScriptEnabled=true;settings.domStorageEnabled=true
                settings.allowFileAccess=false;settings.allowContentAccess=false
                settings.userAgentString=EmbeddedPrexPayment.compatibilityIdentity(settings.userAgentString)
                webViewClient=object:WebViewClient() {
                    override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse? {
                        if(r.method=="GET"&&r.url.scheme=="https"&&r.url.host=="pasarelaspe.sistarbanc.com.uy"&&r.url.path.orEmpty().startsWith("/v2/"))return null
                        return WebResourceResponse("application/json","UTF-8",503,"Blocked by read-only probe",emptyMap(),"{}".byteInputStream())
                    }
                }
            }
            compose.activity.findViewById<ViewGroup>(android.R.id.content).addView(web,ViewGroup.LayoutParams(-1,-1))
            web.loadUrl("https://pasarelaspe.sistarbanc.com.uy/v2/")
        }
        try {
            compose.waitUntil(45000){js("document.querySelector('app-root')?.getAttribute('ng-version')||null")!="null"}
            assertEquals("\"11.2.14\"",js("document.querySelector('app-root').getAttribute('ng-version')"))
            js(compose.activity.assets.open("prex-provider-verification.js").bufferedReader().use {it.readText()})
            assertEquals("true",js("window.BoleteraProviderVerification.supported()"))
            assertEquals("false",js("window.BoleteraProviderVerification.accepted(document.querySelector('app-root'))"))
            java.io.File(compose.activity.getExternalFilesDir(null),"gateway-verification-runtime.txt").writeText(
                "Public gateway static page: Angular 11.2.14 and inspected main bundle. Bound app-root instance found. No verified CAPTCHA asserted. All API requests, non-gateway requests and non-GET methods blocked. This proves runtime binding, not acceptance in a real recharge.")
        } finally {compose.runOnIdle {(web.parent as? ViewGroup)?.removeView(web);web.destroy()}}
    }
}
