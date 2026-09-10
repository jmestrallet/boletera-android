package uy.boletera.prueba

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Retains one original payment page. Opening the panel again never replays a POST. */
@SuppressLint("SetJavaScriptEnabled")
class EmbeddedPrexPayment(context: Context) {
    var nativeStage by mutableStateOf("loading")
        private set
    var summaryRows by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    var payerValues by mutableStateOf<List<String>>(emptyList())
        private set
    var canContinue by mutableStateOf(false)
        private set
    var challenge by mutableStateOf<CaptchaRect?>(null)
        private set
    var expandedChallenge by mutableStateOf(false)
        private set
    var cssViewportWidth by mutableStateOf(0f)
        private set
    val chosenPayer get() = payer
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var currentHost by mutableStateOf("pasarelaspe.sistarbanc.com.uy")
        private set
    var payerLabel by mutableStateOf("")
        private set
    var payerNotice by mutableStateOf("")
        private set
    var payerConflict by mutableStateOf(false)
        private set
    private var originalLink: String? = null
    private var destroyed = false
    private var payer: PayerProfile? = null
    private var visible = false
    private var navigationVersion = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val profileStatus = object : Runnable {
        override fun run() {
            if (destroyed || !visible || originalLink == null) return
            val version = navigationVersion
            web.evaluateJavascript("window.BoleteraNative ? JSON.stringify(window.BoleteraNative.snapshot()) : null") { raw ->
                if (!destroyed && visible && version == navigationVersion) try {
                    val decoded = org.json.JSONTokener(raw).nextValue() as? String
                    if (decoded != null) {
                        val state = org.json.JSONObject(decoded)
                        nativeStage = state.optString("stage", "loading")
                        val rows = state.optJSONArray("rows")
                        summaryRows = if (rows == null) emptyList() else (0 until rows.length()).map {
                            rows.getJSONArray(it).let { row -> row.getString(0) to row.getString(1) }
                        }
                        val values = state.optJSONArray("values")
                        payerValues = if (values == null) emptyList() else (0 until values.length()).map(values::getString)
                        canContinue = state.optBoolean("canContinue")
                        challenge = state.optJSONObject("challenge")?.let { r -> CaptchaRect(r.getDouble("x").toFloat(), r.getDouble("y").toFloat(), r.getDouble("width").toFloat(), r.getDouble("height").toFloat()) }
                        expandedChallenge = state.optBoolean("expanded")
                        cssViewportWidth = state.optDouble("viewportWidth", 0.0).toFloat()
                    }
                } catch (_: Exception) { nativeStage = "loading" }
            }
            web.evaluateJavascript("if(location.origin==='https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/') && window.top===window.self) { window.BoleteraPayer ? window.BoleteraPayer.status() : 'waiting'; } else { 'waiting'; }") { raw ->
                if (!destroyed && visible && version == navigationVersion) {
                    val status = try { org.json.JSONTokener(raw).nextValue() as? String } catch (_: Exception) { null }
                    payerConflict = status == "conflict"
                    payerNotice = when (status) {
                        "conflict" -> "La página trae datos distintos del perfil elegido. No los reemplazamos automáticamente."
                        "foreign-document" -> "Este perfil usa cédula uruguaya, pero la página indica otro tipo de documento. Revisá esa elección."
                        else -> ""
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }
    private val nativeScript = context.assets.open("prex-native.js").bufferedReader().use { it.readText() }
    private val verificationScript = context.assets.open("prex-verification.js").bufferedReader().use { it.readText() }
    private val payerScript = context.assets.open("prex-payer.js").bufferedReader().use { it.readText() }
    val web = WebView(context).apply {
        alpha = 0f
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.safeBrowsingEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.userAgentString = compatibilityIdentity(settings.userAgentString)
        // Keep the user's Android autofill provider, including Google; no app card vault.
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                if (message.message().startsWith("Acceso bloqueado:"))
                    fail("Sistarbanc rechazó este navegador. La solicitud se conserva para revisar su estado.")
                return true
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame || allowedDestination(request.url.toString())) return false
                fail("El proveedor pidió abrir otro destino. Conservamos esta solicitud; no se inició otra recarga.")
                return true
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                navigationVersion++
                view.alpha = 0f
                if (url == "about:blank" || url == null) return
                if (!allowedDestination(url)) {
                    view.stopLoading()
                    fail("Este destino todavía no está incorporado. La solicitud sigue por revisar.")
                    return
                }
                currentHost = android.net.Uri.parse(url).host.orEmpty()
                nativeStage = "loading"
                canContinue = false
                busy = true
                message = ""
            }
            override fun onPageFinished(view: WebView, url: String?) {
                if (url == null || url != view.url || !allowedDestination(url)) return
                busy = false
                if (PaymentPolicy.gateway(url)) {
                    view.evaluateJavascript(verificationScript + "\n" + nativeScript + "\n" + payerScript, null)
                    payer?.let { profile ->
                        view.evaluateJavascript("if(location.origin==='https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/') && window.top===window.self) { window.BoleteraPayer && window.BoleteraPayer.use(${profile.json()}); }", null)
                    }
                } else nativeStage = "original"
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                fail("No se pudo verificar la conexión segura. No continuamos con este paso.")
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) fail("Se interrumpió la conexión. Conservamos la solicitud para revisar su resultado.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, error: WebResourceResponse) {
                if (request.isForMainFrame) fail("El proveedor no pudo mostrar este paso. La solicitud sigue por revisar.")
            }
        }
    }

    fun open(url: String, profile: PayerProfile? = null): Boolean {
        if (destroyed || !PaymentPolicy.prexLink(url) || profile?.valid() == false) return false
        if (originalLink == url) {
            if (payer?.id != profile?.id) return false
            visible = true
            handler.removeCallbacks(profileStatus)
            handler.post(profileStatus)
            return true
        }
        if (originalLink != null) return false // Caller must explicitly finish/reset the previous journey.
        originalLink = url
        payer = profile
        payerLabel = profile?.label.orEmpty()
        currentHost = android.net.Uri.parse(url).host.orEmpty()
        busy = true
        message = ""
        visible = true
        web.loadUrl(url)
        handler.post(profileStatus)
        return true
    }

    fun applyChosenPayer() {
        if (destroyed || !visible || !payerConflict) return
        web.evaluateJavascript("if(location.origin==='https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/') && window.top===window.self) { window.BoleteraPayer && window.BoleteraPayer.applyChosenProfile(); }", null)
    }

    fun advance() {
        if (destroyed || !visible || !canContinue || nativeStage !in listOf("summary", "payer")) return
        val expected = nativeStage
        canContinue = false
        web.evaluateJavascript("window.BoleteraNative && window.BoleteraNative.advance(${org.json.JSONObject.quote(expected)})", null)
    }

    fun positionVerification() {
        if (!destroyed && visible && nativeStage == "payer") web.evaluateJavascript("window.BoleteraNative && window.BoleteraNative.showVerification()", null)
    }

    fun restoreVerification() {
        if (!destroyed) web.evaluateJavascript("window.BoleteraNative && window.BoleteraNative.restoreVerification()", null)
    }

    fun editPayer(profile: PayerProfile): Boolean {
        if (destroyed || !visible || nativeStage != "payer" || profile.id != payer?.id || !profile.valid()) return false
        payer = profile
        web.evaluateJavascript("if(location.origin==='https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/')) { window.BoleteraPayer && window.BoleteraPayer.updateForThisPayment(${profile.json()}); }", null)
        return true
    }

    fun hide() { visible = false; handler.removeCallbacks(profileStatus) }

    private fun fail(text: String) { busy = false; message = text }

    fun reset() {
        if (destroyed) return
        navigationVersion++
        hide()
        web.stopLoading()
        web.loadUrl("about:blank")
        web.clearHistory()
        originalLink = null
        nativeStage = "loading"
        summaryRows = emptyList()
        payerValues = emptyList()
        canContinue = false
        challenge = null
        expandedChallenge = false
        payer = null
        payerLabel = ""
        payerNotice = ""
        payerConflict = false
        busy = false
        message = ""
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        hide()
        (web.parent as? ViewGroup)?.removeView(web)
        web.stopLoading()
        web.destroy()
        originalLink = null
        payer = null
    }

    companion object {
        fun compatibilityIdentity(original: String): String = original
            .replace("; wv", "").replace("Version/4.0 ", "").replace("Chrome/", "Chromium/")
        fun allowedDestination(url: String) = PaymentPolicy.gateway(url) || NavigationPolicy.allowed(url)
    }
}
