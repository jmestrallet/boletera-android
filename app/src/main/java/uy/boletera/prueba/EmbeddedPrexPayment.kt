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
class EmbeddedPrexPayment(context: Context, private val clock: () -> Long = android.os.SystemClock::elapsedRealtime) {
    var nativeStage by mutableStateOf("loading")
        private set
    var summaryRows by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    var completionNotice by mutableStateOf("")
        private set
    var completionSubmitted by mutableStateOf(false)
        private set
    var returningToWallet by mutableStateOf(false)
        private set
    private var expectedAmount: Long? = null
    private var finalSubmitted = false
    var payerValues by mutableStateOf<List<String>>(emptyList())
        private set
    var canContinue by mutableStateOf(false)
        private set
    var cardBusy by mutableStateOf(false)
        private set
    var cardError by mutableStateOf(false)
        private set
    var expressPhase by mutableStateOf("off")
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
    var slowStep by mutableStateOf(false)
        private set
    private var progressStage = ""
    private var progressSince = 0L
    private var pendingFailure: String? = null
    private var failureSince = 0L
    private var failureStage = ""
    private var transientFailureShown = false
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
    private var expressAmount: Long? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val profileStatus: Runnable = object : Runnable {
        override fun run() {
            if (destroyed || !visible || originalLink == null) return
            checkProgress()
            val version = navigationVersion
            web.evaluateJavascript("(()=>{const phase=window.BoleteraExpress?.tick()||'off';return window.BoleteraNative?JSON.stringify({...window.BoleteraNative.snapshot(),expressPhase:phase}):null;})()") { raw ->
                if (!destroyed && visible && version == navigationVersion) try {
                    val decoded = org.json.JSONTokener(raw).nextValue() as? String
                    if (decoded != null) {
                        val state = org.json.JSONObject(decoded)
                        nativeStage = state.optString("stage", "loading")
                        completionNotice = state.optString("completionNotice")
                        completionSubmitted = state.optBoolean("submitted")
                        val rows = state.optJSONArray("rows")
                        summaryRows = if (rows == null) emptyList() else (0 until rows.length()).map {
                            rows.getJSONArray(it).let { row -> row.getString(0) to row.getString(1) }
                        }
                        val values = state.optJSONArray("values")
                        payerValues = if (values == null) emptyList() else (0 until values.length()).map(values::getString)
                        canContinue = state.optBoolean("canContinue")
                        cardBusy = state.optBoolean("cardBusy")
                        cardError = state.optBoolean("cardError")
                        expressPhase = state.optString("expressPhase","off")
                        challenge = state.optJSONObject("challenge")?.let { r -> CaptchaRect(r.getDouble("x").toFloat(), r.getDouble("y").toFloat(), r.getDouble("width").toFloat(), r.getDouble("height").toFloat()) }
                        expandedChallenge = state.optBoolean("expanded")
                        cssViewportWidth = state.optDouble("viewportWidth", 0.0).toFloat()
                        if(nativeStage=="stmSuccess" && canContinue) advance()
                        if(nativeStage!=progressStage) {
                            progressStage=nativeStage;progressSince=clock();slowStep=false
                            if(nativeStage!=failureStage && nativeStage in listOf("summary","payer","card","finalConfirmation","receipt","paymentRejected","paymentPending","stmSuccess","returnBalance","sessionExpired")) {
                                pendingFailure=null
                                if(transientFailureShown) {message="";transientFailureShown=false}
                            }
                        }
                    }
                } catch (_: Exception) { nativeStage = "loading" }
            }
            web.evaluateJavascript("if(location.origin==='https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/') && window.top===window.self) { window.BoleteraPayer ? window.BoleteraPayer.status() : 'waiting'; } else { 'waiting'; }") { raw ->
                if (!destroyed && visible && version == navigationVersion) {
                    val status = try { org.json.JSONTokener(raw).nextValue() as? String } catch (_: Exception) { null }
                    payerConflict = status == "conflict"
                    payerNotice = when (status) {
                        "conflict" -> "Prex muestra datos distintos de los que elegiste. Podés conservarlos o usar los que seleccionaste."
                        "foreign-document" -> "Los datos guardados usan cédula, pero Prex muestra otro tipo de documento. Revisá esa elección."
                        else -> ""
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }
    private val completionScript = context.assets.open("prex-completion.js").bufferedReader().use { it.readText() }
    private val returnScript = listOf("stm-session.js", "stm-payment-return.js").joinToString("\n") { name -> context.assets.open(name).bufferedReader().use { it.readText() } }
    private val nativeScript = context.assets.open("prex-native.js").bufferedReader().use { it.readText() }
    private val cardScript = context.assets.open("prex-card.js").bufferedReader().use { it.readText() }
    private val expressScript = context.assets.open("prex-express.js").bufferedReader().use { it.readText() }
    private val verificationScript = context.assets.open("prex-verification.js").bufferedReader().use { it.readText() }
    private val payerScript = context.assets.open("prex-payer.js").bufferedReader().use { it.readText() }
    val web: WebView = WebView(context).apply {
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
        @Suppress("DEPRECATION")
        settings.saveFormData = false
        isSaveEnabled = false
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
                progressStage="loading";progressSince=clock();slowStep=false;pendingFailure=null;transientFailureShown=false
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
                    view.evaluateJavascript(verificationScript + "\n" + cardScript + "\n" + nativeScript + "\n" + completionScript + "\n" + payerScript + "\n" + expressScript, null)
                    view.evaluateJavascript("window.BoleteraCompletion?.configure(${expectedAmount ?: "null"},$finalSubmitted)",null)
                    payer?.let { profile ->
                        view.evaluateJavascript("if(location.origin==='https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/') && window.top===window.self) { window.BoleteraPayer && window.BoleteraPayer.use(${profile.json()}); }", null)
                        expressAmount?.let { amount ->
                            expressAmount=null // One document only; navigation never restarts the shortcut.
                            view.evaluateJavascript("window.BoleteraExpress && window.BoleteraExpress.start($amount,${profile.json()})",null)
                        }
                    }
                } else {
                    view.evaluateJavascript(returnScript + ";window.BoleteraNative ? window.BoleteraNative.snapshot().stage : 'original'") { raw ->
                        if (!destroyed && url == view.url) {
                            nativeStage = try { org.json.JSONTokener(raw).nextValue() as? String ?: "original" } catch (_: Exception) { "original" }
                            handler.removeCallbacks(profileStatus)
                            if (visible) handler.post(profileStatus)
                        }
                    }
                }
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                fail("No se pudo verificar la conexión segura. No continuamos con este paso.")
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && request.url.toString()==view.url) deferFailure("Se interrumpió la conexión. Conservamos la solicitud para revisar su resultado.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, error: WebResourceResponse) {
                if (request.isForMainFrame && request.url.toString()==view.url) deferFailure("El proveedor no pudo mostrar este paso. La solicitud sigue por revisar.")
            }
        }
    }

    fun open(url: String, profile: PayerProfile? = null, expressAmount: Long? = null, expectedAmount: Long? = null): Boolean {
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
        this.expectedAmount = expectedAmount ?: expressAmount
        this.expressAmount = expressAmount
        expressPhase = if(expressAmount!=null)"advancing" else "off"
        payer = profile
        payerLabel = profile?.label.orEmpty()
        currentHost = android.net.Uri.parse(url).host.orEmpty()
        busy = true
        message = ""
        progressStage="loading";progressSince=clock();slowStep=false;pendingFailure=null;transientFailureShown=false
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
        if (destroyed || !visible || !canContinue || nativeStage !in listOf("summary", "payer", "finalConfirmation", "receipt", "paymentRejected", "paymentPending", "stmSuccess")) return
        stopExpress()
        val expected = nativeStage
        if(expected in listOf("receipt","paymentRejected","paymentPending","stmSuccess"))returningToWallet=true
        if (expected == "finalConfirmation") {
            if (finalSubmitted) return
            finalSubmitted = true // Conservative lock survives navigation and reopening this journey.
        }
        canContinue = false
        completionSubmitted = true
        progressSince=clock();slowStep=false
        web.evaluateJavascript("window.BoleteraNative && window.BoleteraNative.advance(${org.json.JSONObject.quote(expected)})", null)
    }

    fun positionVerification() {
        if (!destroyed && visible && nativeStage in listOf("payer","card")) web.evaluateJavascript("window.BoleteraNative && window.BoleteraNative.showVerification()", null)
    }

    fun submitCard(pan: String, expiry: String, cvv: String) {
        if(destroyed || !visible || nativeStage!="card" || cardBusy || !canContinue)return
        cardBusy=true;cardError=false;canContinue=false
        progressSince=clock();slowStep=false
        val version=navigationVersion
        // Literal escaping only; never log this command or retain its arguments in engine state.
        web.evaluateJavascript("window.BoleteraCard && window.BoleteraCard.submit(${org.json.JSONObject.quote(pan)},${org.json.JSONObject.quote(expiry)},${org.json.JSONObject.quote(cvv)})") { raw ->
            if(!destroyed && version==navigationVersion && raw!="true") { cardBusy=false;cardError=true }
        }
    }

    fun restoreVerification() {
        if (!destroyed) web.evaluateJavascript("window.BoleteraNative && window.BoleteraNative.restoreVerification()", null)
    }

    fun stopExpress() {
        expressAmount=null
        if(destroyed)return
        expressPhase="manual"
        web.evaluateJavascript("window.BoleteraExpress && window.BoleteraExpress.stop()",null)
    }

    fun editPayer(profile: PayerProfile): Boolean {
        if (destroyed || !visible || nativeStage != "payer" || profile.id != payer?.id || !profile.valid()) return false
        payer = profile
        stopExpress()
        web.evaluateJavascript("if(location.origin==='https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/')) { window.BoleteraPayer && window.BoleteraPayer.updateForThisPayment(${profile.json()}); }", null)
        return true
    }

    fun hide() { visible = false; handler.removeCallbacks(profileStatus) }

    private fun fail(text: String) { busy = false; message = text }
    private fun deferFailure(text: String) {
        if(pendingFailure==null) {failureSince=clock();failureStage=nativeStage}
        pendingFailure=text
    }
    private fun checkProgress() {
        if(pendingFailure!=null && clock()-failureSince>=2000) {
            val failure=pendingFailure!!;pendingFailure=null;transientFailureShown=true;fail(failure)
        }
        val waiting=busy || nativeStage=="loading" || expressPhase=="advancing" || cardBusy || completionSubmitted
        if(waiting && challenge==null && clock()-progressSince>=30000 && !slowStep && message.isBlank()) {
            slowStep=true;stopExpress()
        }
    }

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
        expectedAmount = null
        finalSubmitted = false
        completionNotice = ""
        completionSubmitted = false
        returningToWallet = false
        payerValues = emptyList()
        canContinue = false
        cardBusy = false
        cardError = false
        expressAmount = null
        expressPhase = "off"
        challenge = null
        expandedChallenge = false
        payer = null
        payerLabel = ""
        payerNotice = ""
        payerConflict = false
        busy = false
        message = ""
        slowStep=false;pendingFailure=null;transientFailureShown=false;progressStage="";progressSince=0L
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
