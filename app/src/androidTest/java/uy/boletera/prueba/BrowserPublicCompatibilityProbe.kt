package uy.boletera.prueba

import android.webkit.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Opt-in public-page probe: no operation id, cookies, account, payment API or form actions. */
@RunWith(AndroidJUnit4::class)
class BrowserPublicCompatibilityProbe {
    @Test fun comparePublicConfirmationWithoutAnOperation() {
        org.junit.Assume.assumeTrue(
            "Public probe requires explicit opt-in",
            InstrumentationRegistry.getArguments().getString("publicIdentityProbe") == "true"
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // A new browser data directory prevents reuse of any application login/session.
        instrumentation.runOnMainSync { WebView.setDataDirectorySuffix("public-identity-" + System.currentTimeMillis()) }
        fun measure(identity: String): JSONObject {
            val finished = CountDownLatch(1)
            val blocked = AtomicBoolean(false)
            val initialized = AtomicBoolean(false)
            val error = AtomicReference("")
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val deniedRequests = java.util.concurrent.atomic.AtomicInteger()
            var view: WebView? = null
            instrumentation.runOnMainSync {
                view = WebView(instrumentation.targetContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    CookieManager.getInstance().setAcceptCookie(false)
                    if (identity == "chromium") settings.userAgentString = settings.userAgentString
                        .replace("; wv", "").replace("Version/4.0 ", "").replace("Chrome/", "Chromium/")
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            val text = message.message()
                            if (text.startsWith("Acceso bloqueado:")) blocked.set(true)
                            if (text == "initializeProtection") {
                                initialized.set(true)
                                handler.postDelayed({ finished.countDown() }, 500)
                            }
                            return true
                        }
                        override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(web: WebView, request: WebResourceRequest): WebResourceResponse? {
                            val uri = request.url
                            val path = uri.path ?: ""
                            val allowedPath = path == "/v2/confirmarPago" ||
                                Regex("/v2/[A-Za-z0-9._-]+\\.(js|css|ico)").matches(path) ||
                                (path.startsWith("/v2/assets/") && Regex(".*\\.(png|jpg|jpeg|svg|gif|woff|woff2|ttf|json)").matches(path))
                            if (request.method == "GET" && uri.scheme == "https" &&
                                uri.host == "pasarelaspe.sistarbanc.com.uy" && uri.query == null && allowedPath) return null
                            deniedRequests.incrementAndGet()
                            return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked by local test", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                        }
                        override fun onReceivedSslError(web: WebView, ssl: SslErrorHandler, problem: android.net.http.SslError) {
                            ssl.cancel(); error.set("TLS_REJECTED_" + problem.primaryError); finished.countDown()
                        }
                        override fun onReceivedError(web: WebView, request: WebResourceRequest, problem: WebResourceError) {
                            if (request.isForMainFrame) { error.set("NETWORK_ERROR_" + problem.errorCode); finished.countDown() }
                        }
                    }
                    loadUrl("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago")
                }
            }
            try {
                val completed = finished.await(25, TimeUnit.SECONDS)
                return JSONObject().put("identity", identity).put("completed", completed)
                    .put("protectionInitialized", initialized.get()).put("browserBlocked", blocked.get())
                    .put("error", error.get()).put("requestsDeniedLocally", deniedRequests.get())
            } finally { instrumentation.runOnMainSync { handler.removeCallbacksAndMessages(null); view?.stopLoading(); view?.destroy() } }
        }
        val native = measure("native")
        println("BOLETERA_PUBLIC_IDENTITY_NATIVE=" + native)
        assertTrue("No se ejecutó el control base: $native", native.getBoolean("protectionInitialized"))
        assertTrue("La prueba base no reprodujo el bloqueo: $native", native.getBoolean("browserBlocked"))
        val chromium = measure("chromium")
        println("BOLETERA_PUBLIC_IDENTITY_CHROMIUM=" + chromium)
        assertTrue("No se ejecutó la segunda medición: $chromium", chromium.getBoolean("protectionInitialized"))
        // No assertion claims a payment capability. Report the actual public detector outcome.
    }
}
