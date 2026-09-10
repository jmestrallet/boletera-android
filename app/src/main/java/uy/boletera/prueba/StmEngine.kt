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
    private val stageTrail = ArrayDeque<String>()
    private var interruptedAccess = false
    private var accountVerified = false
    private var debugDestination = ""
    private val choices = JourneyPreferences(context)
    private val payerStore = PayerProfiles(context)
    var payerProfiles by mutableStateOf<List<PayerProfile>>(emptyList())
        private set
    private var payerStoreAvailable = true
    private var choosingCard = false
    private var awaitingNavigation = false
    private val paymentBrowser = PaymentBrowser(context)
    val prexPayment = EmbeddedPrexPayment(context)
    private var paymentInFlight = false
    private var providerSubmitted = false
    private var handoffSent = false
    private var expressRequest: ExpressChoice? = null
    private var expressPayment = false
    val expressPreparing: Boolean get() = expressPayment && state.busy
    val web = WebView(context)
    val host = CaptchaHost(context, web)
    private val poll = object : Runnable {
        override fun run() { if (active && !destroyed) { inspect(); handler.postDelayed(this, 1100) } }
    }

    init {
        // Standard Android WebView inspector is available only in developer builds.
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
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
                if (paymentInFlight && state.selectedProvider == "1033" && PaymentPolicy.prexLink(request.url.toString())) {
                    openPrexPayment(request.url.toString()); return true
                }
                if (paymentInFlight && state.selectedProvider == "1002" && request.url.host == "ebanking.brou.com.uy") {
                    inspectPaymentPage(); return true
                }
                if (allowedPage(request.url.toString())) return false
                if (!active) return true
                if (BuildConfig.DEBUG) debugDestination = "${request.url.scheme}://${request.url.host}${request.url.path}"
                fail("Este paso necesita otra pantalla o proveedor. La prueba se detuvo sin abrir la web ni pagar.")
                return true
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                awaitingNavigation = false
                navigationGeneration++
                polling = false
                state = state.copy(captcha = null)
                host.crop = null
                if (handoffSent) return
                if (paymentInFlight && state.selectedProvider == "1033" && url != null && PaymentPolicy.prexLink(url)) {
                    view.stopLoading(); openPrexPayment(url); return
                }
                if (url != null && url != "about:blank" && !allowedPage(url)) {
                    view.stopLoading(); fail("Navegación no reconocida. No se enviaron credenciales desde la app.")
                }
            }
            override fun onPageFinished(view: WebView, url: String?) {
                if (url != null && allowedPage(url) && active) inspect()
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel(); fail("No se pudo verificar la conexión segura. No continuamos.")
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && active) fail("No se pudo conectar. Revisá tu conexión y el estado del pago antes de repetirlo.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame && active) fail("El servicio no respondió correctamente. La consulta se detuvo.")
            }
        }
    }

    fun savedAccess(value: Boolean) { state = state.copy(hasSavedAccess = value) }
    fun dismissNotice(message: String) { if (state.message == message) state = state.copy(message = "") }
    fun notice(message: String) { state = state.copy(message = message, busy = false) }

    fun connect(doc: String, pass: String) {
        if (state.busy) return
        if (!Regex("\\d{8}").matches(doc) || pass.isBlank()) { notice("Ingresá tu documento de 8 dígitos y contraseña."); return }
        clearSecrets()
        expressRequest = null
        prexPayment.reset()
        document = doc; password = pass
        accountVerified = false
        choices.useAccount(doc)
        choosingCard = false
        paymentInFlight = false; providerSubmitted = false; handoffSent = false
        interruptedAccess = false; stageTrail.clear()
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
        choices.card = id
        choosingCard = false
        state = state.copy(selectedCard = id, minimum = null, balance = null, amount = null)
        act("card", id)
    }

    fun changeCard() {
        if (state.busy) return
        choosingCard = true
        lastAction = ""; pageStage = ""; active = true
        state = state.copy(stage = "connecting", busy = true, amount = null, message = "")
        actionStarted = System.currentTimeMillis()
        awaitingNavigation = true; navigationGeneration++; polling = false
        web.loadUrl(CARDS)
        handler.removeCallbacks(poll); handler.post(poll)
    }

    /** Leaving a picker opened from the wallet is navigation, never cancellation of login. */
    fun returnFromCardPicker(): Boolean {
        if (!accountVerified || !choosingCard || state.selectedCard == null) return false
        choosingCard = false
        state = state.copy(busy = false)
        refresh()
        return true
    }

    fun forgetChoices(): Boolean {
        if (!payerStore.forget()) { notice("No pudimos borrar los datos de titular guardados. No se confirmó su eliminación."); return false }
        payerProfiles = emptyList()
        payerStoreAvailable = true
        choices.forgetAll()
        expressRequest = null
        return true
    }

    fun choosePayer(id: String) {
        if (!accountVerified || state.busy || state.activePayment != null || state.stage != "paymentBoundary") return
        if (payerProfiles.none { it.id == id }) return
        choices.payerProfileId = id
        state = state.copy(payerProfileId = id, message = "")
    }

    fun savePayer(profile: PayerProfile): Boolean {
        if (!accountVerified || state.busy || state.activePayment != null || !payerStoreAvailable || !profile.valid()) return false
        val updated = payerProfiles.filterNot { it.id == profile.id } + profile
        return try {
            if (!payerStore.write(updated)) false else {
                payerProfiles = updated
                choices.payerProfileId = profile.id
                state = state.copy(payerProfileId = profile.id, message = "")
                true
            }
        } catch (_: Exception) { notice("No se pudieron guardar los datos del titular en este celular."); false }
    }

    fun deletePayer(id: String): Boolean {
        if (!accountVerified || state.busy || !payerStoreAvailable || state.activePayment?.payerProfileId == id) return false
        return try {
            val updated = payerProfiles.filterNot { it.id == id }
            if (!payerStore.write(updated)) false else {
                payerProfiles = updated
                val selected = state.payerProfileId?.takeIf { candidate -> updated.any { it.id == candidate } } ?: updated.singleOrNull()?.id
                choices.payerProfileId = selected
                state = state.copy(payerProfileId = selected)
                true
            }
        } catch (_: Exception) { false }
    }

    fun chooseProvider(id: String) {
        if (state.busy || state.providers.none { it.id == id }) return
        choices.provider = id
        state = state.copy(selectedProvider = id)
    }

    fun beginPayment() {
        val provider = state.selectedProvider ?: return
        val amount = state.amount ?: return
        val card = state.selectedCard ?: return
        if (!accountVerified || paymentInFlight || state.stage != "paymentBoundary" || state.busy || provider !in PaymentPolicy.supported || !Amounts.valid(amount, state.minimum)) return
        if (provider == "1033" && payerProfiles.none { it.id == state.payerProfileId }) {
            notice("Elegí o guardá los datos para esta Prex antes de seguir.")
            return
        }
        if (provider == "1002" && !paymentBrowser.available()) { notice("Para pagar con eBROU necesitás Chrome instalado y habilitado."); return }
        val pending = ActivePayment(card, provider, amount, System.currentTimeMillis(), state.payerProfileId.takeIf { provider == "1033" })
        paymentInFlight = true; providerSubmitted = false; handoffSent = false
        active = true; lastAction = ""; actionStarted = System.currentTimeMillis()
        state = state.copy(stage = "openingPayment", busy = true, activePayment = pending, message = "")
        act("provider", provider)
        handler.removeCallbacks(poll); handler.post(poll)
    }

    private fun clearPaymentSession() {
        expressRequest = null
        expressPayment = false
        state = state.copy(activePayment = null)
        paymentBrowser.close(); paymentInFlight = false; handoffSent = false
        prexPayment.reset()
    }

    private fun allowedPage(url: String) = NavigationPolicy.allowed(url) || paymentInFlight && PaymentPolicy.gateway(url)

    private fun paymentOpened() {
        handoffSent = true; paymentInFlight = false; active = false
        handler.removeCallbacks(poll); web.stopLoading(); clearSecrets()
        state = state.copy(stage = "externalPayment", busy = false, message = "")
    }

    private fun openPrexPayment(url: String) {
        if (handoffSent || !paymentInFlight || state.selectedProvider != "1033") return
        if (prexPayment.open(url, payerProfiles.find { it.id == state.activePayment?.payerProfileId },
                expressAmount = state.activePayment?.amount?.takeIf { expressPayment })) {
            paymentOpened()
            state = state.copy(stage = "embeddedPrex")
        } else fail("No se pudo abrir esta solicitud de Prex. No se volvió a enviar la recarga.")
    }

    fun leavePrexPayment() {
        if (state.stage == "embeddedPrex") refresh()
    }

    private fun inspectPaymentPage() {
        if (!paymentInFlight || handoffSent) return
        if (state.selectedProvider == "1033" && PaymentPolicy.prexLink(web.url ?: "")) {
            openPrexPayment(web.url!!); return
        }
        if (state.selectedProvider != "1002") return
        // Only the original transaction handoff, never bank login, card fields, cookies or storage.
        val script = """
            (()=>{if(location.hostname!=='spf.sistarbanc.com.uy'||location.pathname!=='/spfe/PasajeBROU.jsp')return null;
              const f=document.forms[0];if(!f||f.method.toLowerCase()!=='post'||f.action!=='https://ebanking.brou.com.uy/multipagos/billetera')return null;
              if([...f.elements].some(e=>e.tagName==='INPUT'&&e.type!=='hidden'))return null;
              return JSON.stringify({action:f.action,fields:[...f.elements].filter(e=>e.name).map(e=>[e.name,e.value])});})()
        """.trimIndent()
        web.evaluateJavascript(script) { raw ->
            if (!paymentInFlight || handoffSent) return@evaluateJavascript
            try {
                val text = JSONTokener(raw ?: "null").nextValue() as? String ?: return@evaluateJavascript
                val data = JSONObject(text)
                val entries = data.getJSONArray("fields")
                val fields = (0 until entries.length()).associate { val pair = entries.getJSONArray(it); pair.getString(0) to pair.getString(1) }
                if (fields.size != entries.length()) { fail("El banco recibió un formato de solicitud distinto. No se repitió el pago."); return@evaluateJavascript }
                if (paymentBrowser.openBrou(data.getString("action"), fields, state.amount ?: 0)) paymentOpened()
                else fail("No pudimos traspasar la solicitud a eBROU con el monto esperado. No se volvió a enviar.")
            } catch (_: Exception) { fail("No pudimos preparar el acceso a eBROU. No se repitió el pago.") }
        }
    }

    fun refresh() {
        if (state.busy) return
        clearPaymentSession()
        clearSecrets(); lastAction = ""; pageStage = ""; active = true; paymentInFlight = false; handoffSent = false
        state = state.copy(stage = "connecting", busy = true, minimum = null, amount = null, captcha = null, message = "")
        actionStarted = System.currentTimeMillis()
        awaitingNavigation = true; navigationGeneration++; polling = false
        web.loadUrl(CARDS)
        handler.removeCallbacks(poll); handler.post(poll)
    }

    fun prepare(amount: Long) {
        expressRequest = null
        expressPayment = false
        if (state.busy || pageStage != "amount") return
        if (!Amounts.valid(amount, state.minimum)) { notice("El monto debe alcanzar el mínimo informado por STM."); return }
        state = state.copy(amount = amount)
        // JS re-reads the minimum immediately before submission. Never selects/submits a payment provider.
        act("amount", amount.toString())
    }

    private fun expressPayer() = payerProfiles.find { it.id==state.payerProfileId && it.valid() }
        ?: payerProfiles.singleOrNull()?.takeIf { it.valid() }

    val expressProviderName: String get() = when(choices.provider) {"1033"->"Prex";"1002"->"eBROU";else->"Medio guardado"}
    private val expressProviderReady: Boolean get() = when(choices.provider) {
        "1033" -> payerStoreAvailable && expressPayer()!=null
        "1002" -> paymentBrowser.available()
        else -> false
    }
    val expressAvailable: Boolean get() = accountVerified && expressProviderReady &&
        choices.card==state.selectedCard &&
        state.selectedCard!=null && state.cards.any { it.id==state.selectedCard && it.active } &&
        Amounts.valid(state.minimum,state.minimum)

    fun startExpress() {
        val card=state.selectedCard ?: return
        val amount=state.minimum ?: return
        if(!accountVerified || state.busy || state.stage!="balance" || pageStage!="amount" || state.activePayment!=null || expressRequest!=null) return
        if(!expressAvailable)return
        val provider=choices.provider ?: return
        val payer=if(provider=="1033")expressPayer() ?: return else null
        if(state.cards.none { it.id==card && it.active })return
        if(!Amounts.valid(amount,state.minimum))return
        expressRequest=ExpressChoice(card,provider,payer?.id)
        expressPayment=true
        state=state.copy(amount=amount)
        // The existing adapter checks the live minimum before submitting; authorization stays with the provider.
        act("amount",amount.toString())
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
        clearPaymentSession()
        clearSecrets(); active = false; accountVerified = false; paymentInFlight = false; handler.removeCallbacks(poll)
        web.stopLoading(); web.loadUrl("about:blank"); host.crop = null
        prexPayment.reset()
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

    fun pause() {
        expressRequest = null // Returning to the app never starts a provider automatically.
        expressPayment = false
        prexPayment.stopExpress()
        handler.removeCallbacks(poll)
        if (document != null || password != null) interruptedAccess = true
        clearSecrets()
    }
    fun resume() {
        if (state.stage == "externalPayment") { refresh(); return }
        if (active) { handler.removeCallbacks(poll); handler.post(poll) }
    }
    fun destroy() {
        destroyed = true; active = false; handler.removeCallbacksAndMessages(null); clearSecrets()
        (web.parent as? ViewGroup)?.removeView(web); web.destroy()
        paymentBrowser.close()
        prexPayment.destroy()
    }

    private fun clearSecrets() { document = null; password = null; secretsExpireAt = 0L }
    private fun fail(message: String) {
        expressRequest = null
        if (BuildConfig.DEBUG) captureDebugShape()
        active = false; paymentInFlight = false; handler.removeCallbacks(poll); clearSecrets(); host.crop = null; web.stopLoading()
        state = state.copy(stage = "blocked", busy = false, message = message, captcha = null)
    }

    /** Local developer build only: DOM structure, never input values, query strings, cookies or tokens. */
    private fun captureDebugShape() {
        val script = """
            JSON.stringify({host:location.hostname,path:location.pathname,ready:document.readyState,
              forms:[...document.forms].map(e=>{let u;try{u=new URL(e.action)}catch{};return {scheme:u?.protocol,host:u?.hostname,path:u?.pathname,method:e.method,target:e.target}}),
              inputs:[...document.querySelectorAll('input')].map(e=>({type:e.type,name:e.name,placeholder:e.placeholder,visible:e.getBoundingClientRect().width>0})),
              buttons:[...document.querySelectorAll('button')].map(e=>({text:(e.innerText||e.textContent||'').trim().replace(/[0-9]/g,'#'),disabled:e.disabled})),
              frames:[...document.querySelectorAll('iframe')].map(e=>{let u;try{u=new URL(e.src)}catch{};let r=e.getBoundingClientRect();return {host:u?.hostname,path:u?.pathname,x:r.x,y:r.y,width:r.width,height:r.height,visibility:getComputedStyle(e).visibility}}),
              errors:[...document.querySelectorAll('[role=alert],.error,.alert,.invalid-feedback')].map(e=>(e.innerText||'').replace(/[0-9]/g,'#').slice(0,200)).filter(Boolean)})
        """.trimIndent()
        web.evaluateJavascript(script) { raw ->
            try {
                val decoded = JSONTokener(raw ?: "null").nextValue() as? String ?: return@evaluateJavascript
                val shape = JSONObject(decoded).put("blockedDestination", debugDestination)
                java.io.File(context.cacheDir, "debug-dom-shape.json").writeText(shape.toString())
            } catch (_: Exception) { /* Debug evidence must not alter the user flow. */ }
        }
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
                if (paymentInFlight) { fail("El proveedor no aceptó el siguiente paso. Revisá el estado del pago antes de repetirlo."); return@evaluateJavascript }
                clearSecrets()
                expressRequest = null
                state = state.copy(busy = false, message = "La página cambió o no aceptó el paso. Volvé a consultar; no se repitió la operación.")
            }
        }
    }

    private fun inspect() {
        if (!active || destroyed || polling || !allowedPage(web.url ?: "")) return
        if (paymentInFlight && !NavigationPolicy.allowed(web.url ?: "")) {
            inspectPaymentPage()
            if (System.currentTimeMillis() - actionStarted > 45000) fail("El proveedor demoró demasiado. Revisá el pago antes de iniciar otro.")
            return
        }
        if (awaitingNavigation) {
            if (System.currentTimeMillis() - actionStarted > 35000) fail("No comenzó la nueva consulta. Volvé a ingresar.")
            return
        }
        if (secretsExpireAt != 0L && System.currentTimeMillis() > secretsExpireAt) {
            fail("El acceso quedó esperando demasiado tiempo. Por seguridad, ingresá nuevamente."); return
        }
        polling = true
        val generation = navigationGeneration
        val request = sessionRequest
        web.evaluateJavascript(adapter + "\nJSON.stringify({origin:location.origin,path:location.pathname,snapshot:window.BoleteraAdapter.snapshot()});") { raw ->
            polling = false
            if (!active || destroyed || request != sessionRequest || generation != navigationGeneration) return@evaluateJavascript
            try {
                val decoded = JSONTokener(raw ?: "null").nextValue() as? String ?: return@evaluateJavascript
                val sample = JSONObject(decoded)
                // WebView's reported URL can advance before the JavaScript document does.
                // An old about:blank/login snapshot must never block the next document or execute actions in it.
                if (!NavigationPolicy.snapshotMatches(sample.optString("origin"), web.url ?: "", sample.optString("path"))) {
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
        val knownStages = setOf("loading", "handoff", "start", "identity", "document", "password", "cards", "cardsLoading", "balance", "amount", "paymentBoundary", "signedOut", "unknown", "verification", "blocked")
        if (changed) {
            if (stageTrail.size >= 6) stageTrail.removeFirst()
            stageTrail.addLast(if (stage in knownStages) stage else "unknown")
        }
        val shape = if (stage == "cards" || stage == "cardsLoading")
            " · tabla=${if (data.optBoolean("tablePresent")) 1 else 0}, filas=${data.optInt("rowCount").coerceIn(0, 999)}, visibles=${data.optInt("visibleRowCount").coerceIn(0, 999)}, leídas=${(data.optJSONArray("cards")?.length() ?: 0).coerceIn(0, 999)}" else ""
        state = state.copy(diagnostic = stageTrail.joinToString(" → ") + shape)
        if (changed) { lastAction = ""; state = state.copy(busy = false); actionStarted = System.currentTimeMillis() }
        pageStage = stage
        val c = data.optJSONObject("captcha")
        val rect = c?.let { CaptchaRect(it.optDouble("x").toFloat(), it.optDouble("y").toFloat(), it.optDouble("width").toFloat(), it.optDouble("height").toFloat()) }
        if (rect != null && (!rect.x.isFinite() || !rect.y.isFinite() || !rect.width.isFinite() || !rect.height.isFinite() || rect.width < 20 || rect.height < 20 || rect.height > 1800)) {
            fail("El desafío no se pudo encuadrar. No mostraremos la página completa."); return
        }
        if (rect != null && !c.optBoolean("inViewport", true)) {
            host.crop = null
            state = state.copy(captcha = null)
            if (rect.width > c.optDouble("viewportWidth") + 1 || rect.height > c.optDouble("viewportHeight") + 1) {
                fail("La verificación no entra completa en este tamaño de pantalla. No se enviaron respuestas al desafío.")
            } else if (lastAction != "positionCaptcha") {
                act("positionCaptcha")
                state = state.copy(stage = "captcha", message = "Acomodando la verificación para que puedas completarla…")
            } else if (System.currentTimeMillis() - actionStarted > 3000) {
                fail("No pudimos encuadrar la verificación completa. Volvé a ingresar para intentarlo otra vez.")
            }
            return
        }
        if (rect != null && lastAction == "positionCaptcha") { lastAction = ""; state = state.copy(message = "") }
        host.crop = rect
        state = state.copy(captcha = rect)
        if (data.optBoolean("error")) {
            expressRequest = null
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
            "loading", "handoff" -> state = state.copy(stage = if (paymentInFlight) "openingPayment" else "connecting", busy = true)
            "start" -> if (password != null && lastAction != "start") act("start") else if (password == null) expired()
            "identity" -> if (password != null && lastAction != "identity") act("identity") else if (password == null) expired()
            "document" -> if (document != null && lastAction != "document") act("document", document!!) else if (document == null) expired()
            "password" -> if (password != null && lastAction != "password") {
                act("password", password!!)
            } else if (!state.busy && lastAction != "password") expired()
            "cards" -> {
                val cards = data.optJSONArray("cards") ?: return
                val list = (0 until cards.length()).map { cards.getJSONObject(it) }.map { CardInfo(it.getString("id"), it.getBoolean("active"), it.getString("status")) }
                if (list.isEmpty()) return // Never treat an unparsed/unfinished page as a completed login.
                clearSecrets()
                if (!accountVerified) {
                    try { payerProfiles = payerStore.read(); payerStoreAvailable = true }
                    catch (_: Exception) { payerProfiles = emptyList(); payerStoreAvailable = false }
                }
                accountVerified = true
                state = state.copy(stage = "cards", cards = list, activePayment = null,
                    payerProfileId = choices.payerProfileId?.takeIf { id -> payerProfiles.any { it.id == id } } ?: payerProfiles.singleOrNull()?.id,
                    message = if (payerStoreAvailable) "" else "No pudimos recuperar los perfiles de Prex guardados. Conservamos sus archivos sin sobrescribirlos.")
                val preferred = choices.card
                if (!choosingCard && !state.busy && lastAction != "card" && preferred != null) {
                    if (list.any { it.id == preferred && it.active }) chooseCard(preferred)
                    else {
                        choices.card = null
                        state = state.copy(message = "Tu boletera habitual ya no está habilitada. Elegí otra para continuar.")
                    }
                }
            }
            "cardsLoading" -> {
                state = state.copy(stage = "connecting", busy = true, message = "Esperando que STM termine de mostrar las boleteras…")
                if (System.currentTimeMillis() - actionStarted > 20000) {
                    fail("No pudimos leer la lista de boleteras. Esto no significa que no tengas ninguna. Pasame una captura con la referencia que aparece abajo.")
                }
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
                if (paymentInFlight) {
                    state = state.copy(stage = "openingPayment", busy = true)
                    if (!providerSubmitted && data.optString("selectedProvider") == state.selectedProvider && data.optBoolean("providerReady")) {
                        providerSubmitted = true
                        act("providerContinue", state.selectedProvider!!)
                    }
                    return
                }
                clearSecrets(); active = false; handler.removeCallbacks(poll)
                val providers = data.optJSONArray("providers")
                val list = if (providers == null) emptyList() else (0 until providers.length()).map {
                    val provider = providers.getJSONObject(it)
                    ProviderInfo(provider.getString("id"), provider.getString("name"))
                }
                val preferred = choices.provider?.takeIf { id -> list.any { it.id == id } }
                state = state.copy(stage = "paymentBoundary", busy = false,
                    providers = list, selectedProvider = preferred, message = "")
                val express=expressRequest
                expressRequest=null // Consume once, before initiating any provider navigation.
                if(express!=null) {
                    val valid=accountVerified && express.card==state.selectedCard &&
                        list.any { it.id==express.provider } &&
                        (express.provider!="1033" || payerProfiles.any { it.id==express.payerId && it.valid() })
                    if(valid) {
                        state=state.copy(selectedProvider=express.provider,payerProfileId=express.payerId)
                        beginPayment()
                    } else {
                        expressPayment=false
                        notice("STM cambió las opciones disponibles. Revisá esta recarga para continuar.")
                    }
                }
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
        state = state.copy(stage = "welcome", busy = false, message = if (interruptedAccess)
            "El ingreso se interrumpió al pasar la app a segundo plano. Volvé a entrar con huella o manualmente."
            else "La app volvió a la pantalla de acceso sin completar la consulta. Pasame una captura con la referencia de abajo.")
    }
    companion object {
        const val START = "https://stm.gub.uy/app/mistm/cuenta/"
        const val CARDS = "https://stm.gub.uy/app/mistm/cuenta/pages/tarjetas.xhtml"
    }
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
