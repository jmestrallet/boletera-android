package uy.boletera.prueba

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import org.json.JSONTokener

/** Browser automation is local, serial and restricted to the official origins. */
@SuppressLint("SetJavaScriptEnabled")
class StmEngine(private val context: Context) {
    var state by mutableStateOf(UiState())
        private set
    private val handler = Handler(Looper.getMainLooper())
    private val adapter = context.assets.open("stm-adapter.js").bufferedReader().use { it.readText() }
    private var document: String? = null
    private var password: String? = null
    private var lastAction = ""
    private var actionStarted = 0L
    private var active = false
    private var destroyed = false
    private var polling = false
    private var pageStage = ""
    private var navigationGeneration = 0
    private var secretsExpireAt = 0L
    private var sessionRequest = 0
    val web = WebView(context)
    val host = CaptchaHost(context, web)
    private val poll = object : Runnable {
        override fun run() { if (active && !destroyed) { inspect(); handler.postDelayed(this, 1100) } }
    }

    init {
        WebView.setWebContentsDebuggingEnabled(false)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            safeBrowsingEnabled = true
            @Suppress("DEPRECATION")
            saveFormData = false
        }
        // App-local cookies; no import from Chrome or account/session transfer.
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        web.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) { request?.deny() }
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?) = true // never log page data
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                if (NavigationPolicy.allowed(request.url.toString())) return false
                fail("Este paso necesita otra pantalla o proveedor. La prueba se detuvo sin abrir la web ni pagar.")
                return true
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                navigationGeneration++
                polling = false
                state = state.copy(captcha = null)
                host.crop = null
                if (url != null && url != "about:blank" && !NavigationPolicy.allowed(url)) {
                    view.stopLoading(); fail("Navegación no reconocida. No se enviaron credenciales desde la app.")
                }
            }
            override fun onPageFinished(view: WebView, url: String?) {
                if (url != null && NavigationPolicy.allowed(url) && active) inspect()
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel(); fail("No se pudo verificar la conexión segura. No continuamos.")
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) fail("No se pudo conectar con STM. Revisá tu conexión y volvé a consultar.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame) fail("El servicio no respondió correctamente. La consulta se detuvo.")
            }
        }
    }

    fun savedAccess(value: Boolean) { state = state.copy(hasSavedAccess = value) }
    fun notice(message: String) { state = state.copy(message = message, busy = false) }

    fun connect(doc: String, pass: String) {
        if (state.busy) return
        if (!Regex("\\d{8}").matches(doc) || pass.isBlank()) { notice("Ingresá tu documento de 8 dígitos y contraseña."); return }
        clearSecrets()
        document = doc; password = pass
        secretsExpireAt = System.currentTimeMillis() + 180000
        lastAction = ""; pageStage = ""; actionStarted = System.currentTimeMillis()
        state = UiState(stage = "connecting", busy = true, hasSavedAccess = state.hasSavedAccess)
        active = false
        handler.removeCallbacks(poll)
        // A new explicit login must never silently reuse a DIFFERENT person's old web session.
        val request = ++sessionRequest
        web.stopLoading(); web.loadUrl("about:blank")
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies {
            if (request == sessionRequest && !destroyed) {
                CookieManager.getInstance().flush()
                active = true; web.loadUrl(START); handler.post(poll)
            }
        }
    }

    fun chooseCard(id: String) {
        if (state.busy || state.cards.none { it.id == id && it.active }) return
        state = state.copy(selectedCard = id, minimum = null, balance = null, amount = null)
        act("card", id)
    }

    fun refresh() {
        if (state.busy) return
        clearSecrets(); lastAction = ""; pageStage = ""; active = true
        state = state.copy(busy = true, minimum = null, amount = null, captcha = null, message = "")
        actionStarted = System.currentTimeMillis()
        web.loadUrl(START)
        handler.removeCallbacks(poll); handler.post(poll)
    }

    fun prepare(amount: Long) {
        if (state.busy || pageStage != "amount") return
        if (!Amounts.valid(amount, state.minimum)) { notice("El monto debe alcanzar el mínimo informado por STM."); return }
        state = state.copy(amount = amount)
        // JS re-reads the minimum immediately before submission. Never selects/submits a payment provider.
        act("amount", amount.toString())
    }

    fun continueCaptcha() {
        if (!active || state.captcha == null) return
        lastAction = ""
        when (pageStage) {
            "document" -> document?.let { act("document", it) }
            "password" -> password?.let { act("password", it) }
            else -> notice("Terminá la verificación original. Si pide otro paso, esta prueba se detendrá.")
        }
    }

    fun cancel() {
        sessionRequest++
        clearSecrets(); active = false; handler.removeCallbacks(poll)
        web.stopLoading(); web.loadUrl("about:blank"); host.crop = null
        state = UiState(hasSavedAccess = state.hasSavedAccess)
    }

    fun logout() {
        cancel()
        state = state.copy(busy = true)
        // Remove this app's local session, not sessions in another browser or device.
        val request = sessionRequest
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            if (!destroyed && request == sessionRequest) state = state.copy(busy = false)
        }
        WebStorage.getInstance().deleteAllData()
        web.clearHistory(); web.clearCache(true)
        state = state.copy(message = "Sesión local eliminada. El acceso cifrado se conserva hasta que elijas olvidarlo.")
    }

    fun pause() { handler.removeCallbacks(poll); clearSecrets() }
    fun resume() {
        if (active) { handler.removeCallbacks(poll); handler.post(poll) }
    }
    fun destroy() {
        destroyed = true; active = false; handler.removeCallbacksAndMessages(null); clearSecrets()
        (web.parent as? ViewGroup)?.removeView(web); web.destroy()
    }

    private fun clearSecrets() { document = null; password = null; secretsExpireAt = 0L }
    private fun fail(message: String) {
        active = false; handler.removeCallbacks(poll); clearSecrets(); host.crop = null
        state = state.copy(stage = "blocked", busy = false, message = message, captcha = null)
    }
    private fun act(action: String, value: String = "") {
        if (!active || destroyed || !NavigationPolicy.allowed(web.url ?: "")) return
        if (lastAction == action && state.busy) return
        lastAction = action; actionStarted = System.currentTimeMillis()
        state = state.copy(busy = true, message = "")
        val generation = navigationGeneration
        val request = sessionRequest
        // JSONObject.quote escapes user input as a literal; it can never become executable code.
        val script = "window.BoleteraAdapter && window.BoleteraAdapter.command(${JSONObject.quote(action)}, ${JSONObject.quote(value)})"
        web.evaluateJavascript(script) { result ->
            if (active && !destroyed && request == sessionRequest && generation == navigationGeneration && result != "true") {
                clearSecrets()
                state = state.copy(busy = false, message = "La página cambió o no aceptó el paso. Volvé a consultar; no se repitió la operación.")
            }
        }
    }

    private fun inspect() {
        if (!active || destroyed || polling || !NavigationPolicy.allowed(web.url ?: "")) return
        if (secretsExpireAt != 0L && System.currentTimeMillis() > secretsExpireAt) {
            fail("El acceso quedó esperando demasiado tiempo. Por seguridad, ingresá nuevamente."); return
        }
        polling = true
        val generation = navigationGeneration
        val request = sessionRequest
        web.evaluateJavascript(adapter + "\nJSON.stringify({origin:location.origin,snapshot:window.BoleteraAdapter.snapshot()});") { raw ->
            polling = false
            if (!active || destroyed || request != sessionRequest || generation != navigationGeneration) return@evaluateJavascript
            try {
                val decoded = JSONTokener(raw ?: "null").nextValue() as? String ?: return@evaluateJavascript
                val sample = JSONObject(decoded)
                // WebView's reported URL can advance before the JavaScript document does.
                // An old about:blank/login snapshot must never block the next document or execute actions in it.
                if (!NavigationPolicy.snapshotMatches(sample.optString("origin"), web.url ?: "")) {
                    if (System.currentTimeMillis() - actionStarted > 35000) fail("No terminó de cargar el siguiente paso. Volvé a ingresar. Código: CAMBIO-PAGINA.")
                    return@evaluateJavascript
                }
                applySnapshot(sample.getJSONObject("snapshot"))
            } catch (_: Exception) { fail("No pudimos interpretar esta pantalla de STM. No se avanzó ni se pagó.") }
        }
    }

    private fun applySnapshot(data: JSONObject) {
        val stage = data.optString("stage", "unknown")
        val changed = stage != pageStage
        if (changed) { lastAction = ""; state = state.copy(busy = false); actionStarted = System.currentTimeMillis() }
        pageStage = stage
        val c = data.optJSONObject("captcha")
        val rect = c?.let { CaptchaRect(it.optDouble("x").toFloat(), it.optDouble("y").toFloat(), it.optDouble("width").toFloat(), it.optDouble("height").toFloat()) }
        if (rect != null && (!rect.width.isFinite() || !rect.height.isFinite() || rect.width < 20 || rect.height < 20 || rect.height > 1800)) {
            fail("El desafío no se pudo encuadrar. No mostraremos la página completa."); return
        }
        host.crop = rect
        state = state.copy(captcha = rect)
        if (data.optBoolean("error")) {
            clearSecrets(); active = false
            state = state.copy(stage = "blocked", busy = false, message = "El sitio no aceptó los datos o la verificación. Podés ingresar otra vez manualmente.")
            return
        }
        if (state.busy && System.currentTimeMillis() - actionStarted > 35000 && rect == null) {
            fail("El sitio demoró demasiado. La app no repetirá el paso automáticamente."); return
        }
        if (rect != null) {
            state = state.copy(stage = "captcha", busy = false)
            return
        }
        fun cents(key: String): Long? = if (data.has(key) && !data.isNull(key)) data.getLong(key) else null
        when (stage) {
            "start" -> if (password != null && lastAction != "start") act("start") else if (password == null) expired()
            "identity" -> if (password != null && lastAction != "identity") act("identity") else if (password == null) expired()
            "document" -> if (document != null && lastAction != "document") act("document", document!!) else if (document == null) expired()
            "password" -> if (password != null && lastAction != "password") {
                act("password", password!!)
            } else if (!state.busy && lastAction != "password") expired()
            "cards" -> {
                clearSecrets()
                val cards = data.optJSONArray("cards") ?: return
                val list = (0 until cards.length()).map { cards.getJSONObject(it) }.map { CardInfo(it.getString("id"), it.getBoolean("active"), it.getString("status")) }
                state = state.copy(stage = "cards", cards = list)
            }
            "balance" -> {
                clearSecrets()
                state = state.copy(stage = "balance", balance = cents("balance"), consultedAt = System.currentTimeMillis())
                if (lastAction != "minimum") act("minimum")
            }
            "amount" -> {
                state = state.copy(stage = "balance", balance = cents("balance"), minimum = cents("minimum"), consultedAt = System.currentTimeMillis())
                if (state.minimum == null || state.balance == null) {
                    fail("No pudimos leer el saldo o el mínimo con certeza. No se habilitó la recarga.")
                }
            }
            "paymentBoundary" -> {
                clearSecrets(); active = false; handler.removeCallbacks(poll)
                state = state.copy(stage = "paymentBoundary", busy = false,
                    message = "Llegamos a la selección de medio de pago. Esta APK todavía no puede cobrar con interfaz propia: no se seleccionó un proveedor ni se generó una solicitud de pago.")
            }
            "signedOut" -> expired()
            "unknown", "verification" -> if (System.currentTimeMillis() - actionStarted > 25000) {
                fail("Este acceso pide un paso que la prueba no reconoce. No vamos a mostrarte la página completa.")
            }
            "blocked" -> fail("Pantalla fuera del recorrido permitido.")
        }
    }
    private fun expired() {
        clearSecrets(); active = false; handler.removeCallbacks(poll)
        state = state.copy(stage = "welcome", busy = false, message = "STM pide ingresar nuevamente. Usá tu huella o ingresá manualmente.")
    }
    companion object { const val START = "https://stm.gub.uy/app/mistm/cuenta/" }
}

/** One original WebView, cropped with native clipping. No copied CAPTCHA, CSS rewrite or overlay taps. */
class CaptchaHost(context: Context, private val browser: WebView) : ViewGroup(context) {
    var crop: CaptchaRect? = null
        set(value) { if (field != value) { field = value; requestLayout() } }
    init { clipChildren = true; clipToPadding = true; addView(browser) }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val metrics = resources.displayMetrics
        browser.measure(MeasureSpec.makeMeasureSpec(metrics.widthPixels, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(metrics.heightPixels, MeasureSpec.EXACTLY))
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val density = resources.displayMetrics.density
        val rect = crop
        browser.visibility = if (rect == null) View.INVISIBLE else View.VISIBLE
        val scale = if (rect == null) 1f else minOf(1f, width / (rect.width * density), height / (rect.height * density))
        browser.pivotX = 0f; browser.pivotY = 0f
        browser.scaleX = scale; browser.scaleY = scale
        val x = ((rect?.x ?: 0f) * density * scale).toInt()
        val y = ((rect?.y ?: 0f) * density * scale).toInt()
        browser.layout(-x, -y, browser.measuredWidth - x, browser.measuredHeight - y)
    }
}
