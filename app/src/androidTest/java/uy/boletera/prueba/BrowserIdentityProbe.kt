package uy.boletera.prueba

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Offline compatibility measurement. No provider page, account, CAPTCHA or transaction. */
@RunWith(AndroidJUnit4::class)
class BrowserIdentityProbe {
    @Test fun compareNativeAndChromeShapedIdentityOffline() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Reproduces the general UA and capability predicates inspected in the public gateway
        // bundle. Not a claim to reproduce server checks or every social-app-specific pattern.
        val script = """
            (() => {
              const ua = navigator.userAgent.toLowerCase();
              const uaPattern = [/; wv\)/, /android.*applewebkit.*version\/\d+\.\d+.*mobile safari/,
                /android.*chrome\/.*mobile safari.*wv/, /mobile.*safari.*wv/,
                /chrome.*mobile.*safari.*wv/, /webkit.*mobile.*safari.*wv/].some(r => r.test(ua));
              const uaMarker = ['; wv)', 'wv)', 'wv', 'webview', 'webviewclient', 'webviewcore'].some(s => ua.includes(s));
              const chromeWithoutApi = ua.includes('chrome') && !window.chrome;
              const googleApp = ['gsa/', 'gsa_'].some(s => ua.includes(s));
              const iosEmbedded = /iphone|ipad|ipod/.test(ua) && !ua.includes('safari') && ua.includes('mobile') && !ua.includes('crios') && !ua.includes('gsa') && !googleApp;
              const otherApp = ['bing','duckduckgo','pinterest','linkedin','reddit','tumblr','uber'].some(s => ua.includes(s));
              const capabilities = {webgl:!!window.WebGLRenderingContext, indexedDB:!!window.indexedDB,
                serviceWorker:'serviceWorker' in navigator, notification:'Notification' in window,
                localStorage:!!window.localStorage, sessionStorage:!!window.sessionStorage};
              return JSON.stringify({ua:navigator.userAgent, uaPattern, uaMarker, chromeWithoutApi,
                iosEmbedded, otherApp, webViewFlag:uaPattern||uaMarker||chromeWithoutApi||iosEmbedded||otherApp,
                capabilities, enoughCapabilities:Object.values(capabilities).filter(Boolean).length >= 4,
                embedded:window.self !== window.top, hostname:location.hostname});
            })()
        """.trimIndent()
        fun measure(identity: String): JSONObject {
            val finished = CountDownLatch(1)
            val result = AtomicReference<String>()
            var view: WebView? = null
            instrumentation.runOnMainSync {
                view = WebView(instrumentation.targetContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.blockNetworkLoads = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    if (identity != "native") {
                        val chromeShaped = settings.userAgentString
                            .replace("; wv", "").replace("Version/4.0 ", "")
                        settings.userAgentString = if (identity == "chromium")
                            chromeShaped.replace("Chrome/", "Chromium/") else chromeShaped
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(web: WebView, url: String) {
                            web.evaluateJavascript(script) { raw -> result.set(raw); finished.countDown() }
                        }
                    }
                    loadDataWithBaseURL("https://localhost/identity-lab", "<!doctype html><title>Laboratorio sin red</title>", "text/html", "UTF-8", null)
                }
            }
            try {
                assertTrue("No terminó la medición local", finished.await(20, TimeUnit.SECONDS))
                return JSONObject(JSONTokener(result.get()).nextValue() as String)
            } finally { instrumentation.runOnMainSync { view?.destroy() } }
        }
        val original = measure("native")
        val changed = measure("chrome")
        val chromium = measure("chromium")
        assertTrue("El control base debe identificar el WebView", original.getBoolean("webViewFlag"))
        assertFalse("El cambio debe quitar los marcadores explícitos", changed.getBoolean("uaMarker"))
        assertEquals("localhost", changed.getString("hostname"))
        assertFalse(changed.getBoolean("embedded"))
        // Report observations instead of assuming that changing UA bypasses every check.
        println("BOLETERA_IDENTITY_ORIGINAL=" + original)
        println("BOLETERA_IDENTITY_CHANGED=" + changed)
        println("BOLETERA_IDENTITY_CHROMIUM=" + chromium)
    }
}
