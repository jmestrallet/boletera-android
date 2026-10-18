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

private var rendererRestartAt = 0L
/**
 * Android can end the pages' renderer on its own (low memory, with the app behind): left unhandled, that closes the
 * whole app. The pages cannot be used again, so the screen is rebuilt; the session cookies and the payment journal stay.
 */
internal fun rendererGone(view: WebView): Boolean {
    val now = android.os.SystemClock.elapsedRealtime()
    if (now - rendererRestartAt > 5000) {
        rendererRestartAt = now
        LabLog.add("app", "las páginas se cerraron: se reabre la app")
        val activity = view.context as? android.app.Activity
        view.post { if (activity != null && !activity.isFinishing && !activity.isDestroyed) activity.recreate() }
    }
    return true
}

/** Browser automation is local, serial and restricted to the official origins. */
@SuppressLint("SetJavaScriptEnabled")
class StmEngine(private val context: Context) {
    var state by mutableStateOf(UiState())
        private set
    private val handler = Handler(Looper.getMainLooper())
    private val adapter = listOf("stm-session.js", "stm-adapter.js").joinToString("\n") { name -> context.assets.open(name).bufferedReader().use { it.readText() } }
    private var document: String? = null
    private var password: String? = null
    private val sessionAccess = SessionAccess()
    private var silentRelogin = false
    private var silentReloginAttempted = false
    private var lastAction = ""
    private var actionStarted = 0L
    private var active = false
    private var destroyed = false
    private var polling = false
    private var pageStage = ""
    private var passwordStepSeen = false
    private var navigationGeneration = 0
    private var secretsExpireAt = 0L
    private var sessionRequest = 0
    private val stageTrail = ArrayDeque<String>()
    private var interruptedAccess = false
    private var accountVerified = false
    /** STM already accepted this session: retrying or going back re-reads with it instead of signing in again. */
    val sessionVerified: Boolean get() = accountVerified
    private var accessRequestId = 0
    private var recoveringSession = false
    private val recoverySteps = mutableSetOf<String>()
    private var paymentNeedsReview = false
    private var pendingHttpError: String? = null
    private var debugDestination = ""
    private val choices = JourneyPreferences(context)
    private val paymentJournal = PaymentJournal(context)
    private var paymentRecord: PaymentRecord? = null
    private var priorAuthorizationRecord: PaymentRecord? = null
    private var journalUnavailable = false
    private var savedSessionAttempted = false
    private var foreground = true
    private var balanceRefreshTask: Runnable? = null
    private var balanceRefreshRecord = 0L
    private var balanceRefreshCount = 0
    private val payerStore = PayerProfiles(context)
    var payerProfiles by mutableStateOf<List<PayerProfile>>(emptyList())
        private set
    private var payerStoreAvailable = true
    private var choosingCard = false
    /** The card list is shown only when the person asked for it or must choose; passing through it to reach the usual card is invisible. */
    val cardPickerWanted: Boolean get() = choosingCard || state.selectedCard == null || state.cards.none { it.id == choices.card && it.active }
    private var activityOpened = false
    private var awaitingNavigation = false
    private val paymentBrowser = PaymentBrowser(context)
    val prexPayment = EmbeddedPrexPayment(context)
    private var paymentInFlight = false
    private var providerSubmitted = false
    private var handoffSent = false
    private var expressRequest: ExpressChoice? = null
    private var expressPayment = false
    private val expressTiming = ExpressTiming(android.os.SystemClock::elapsedRealtime)
    /** Shape of the last STM page the activity journey could not read: digits masked, no values. */
    private var activityShape = ""
    internal val helpDiagnostic: String get() = listOf(state.diagnostic,activityShape,expressTiming.report(),expressNotStarted).filter {it.isNotBlank()}.joinToString("\n\n")
    private val activityShapeScript = """(()=>{const t=e=>(e.innerText||e.value||'').replace(/\s+/g,' ').trim().replace(/\d/g,'#').slice(0,60);
const pick=(s,n)=>[...document.querySelectorAll(s)].map(t).filter(Boolean).slice(0,n);
return JSON.stringify({sitio:location.host,ruta:location.pathname.replace(/\d/g,'#').slice(0,80),titulo:document.title.replace(/\d/g,'#'),titulos:pick('h1,h2,h3,legend,.ui-panel-title',8),
columnas:pick('th',12),botones:pick('button,a.ui-commandlink,input[type=submit]',12),filas:document.querySelectorAll('tbody tr').length,
avisos:pick('.ui-messages,.ui-message,.ui-growl-message',4)});})()"""
    /** Records what STM showed, then fails; the owner can share it from Configuración → Información de ayuda. */
    private fun failActivityWithShape(message: String) = failWithShape("Actividad", message)
    /** Same as [fail], keeping the masked shape of the page so an unknown STM step can be diagnosed later. */
    private fun failWithShape(where: String, message: String) {
        active=false
        val request=sessionRequest
        web.evaluateJavascript(activityShapeScript) { raw ->
            activityShape = "$where · $message\n" + (try { org.json.JSONTokener(raw).nextValue().toString() } catch (_: Exception) { "" }).take(1500)
            // A late answer after Back or a new login must not paint an error over the next screen.
            if (request == sessionRequest && !destroyed) fail(message)
        }
    }
    val expressPreparing: Boolean get() = expressPayment && state.busy
    val web = WebView(context)
    val host = CaptchaHost(context, web)
    private val poll = object : Runnable {
        override fun run() { if (active && !destroyed) { inspect(); handler.postDelayed(this, if(state.busy)200 else 1100) } }
    }

    init {
        prexPayment.identifyCard=choices::cardIdentity
        prexPayment.beforeAuthorize=::trackAuthorization
        prexPayment.beforeOriginalReview=::trackOriginalReview
        prexPayment.authorizationNotSent={
            val account=choices.accountKey
            if(account!=null && paymentRecord?.phase=="authorizing") {
                val prior=priorAuthorizationRecord
                if(if(prior==null)paymentJournal.clear(account) else paymentJournal.write(account,prior)) {
                    paymentRecord=prior;paymentNeedsReview=prior!=null
                } else {journalUnavailable=true;paymentNeedsReview=true}
            }
        }
        prexPayment.outcomeListener=::observePaymentOutcome
        prexPayment.timingObserver=expressTiming::move
        prexPayment.manualContinueObserver=expressTiming::interaction
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
                fail("Este paso necesita otra pantalla o proveedor. Boletera se detuvo sin abrir la web ni iniciar el pago.")
                return true
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                pendingHttpError = null
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
                    // After Back or sign-out a late redirect is just stopped, never shown as an error.
                    view.stopLoading(); if (active) fail("Navegación no reconocida. No se enviaron credenciales desde la app.")
                }
            }
            override fun onPageFinished(view: WebView, url: String?) {
                if (url != null && allowedPage(url) && active) inspect()
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean = rendererGone(view)
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel(); fail("No se pudo verificar la conexión segura. No continuamos.")
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (!request.isForMainFrame || !active) return
                // With working internet, an unreachable STM is STM's outage, not the person's connection.
                if (stmHost(request.url.host) && online()) fail(stmDown("sin respuesta del servidor"))
                else fail("No se pudo conectar. Revisá tu conexión y el estado del pago antes de repetirlo.")
            }
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame && active) {
                    // The completed body may explicitly say session expired, even on a protected URL.
                    pendingHttpError = if (errorResponse.statusCode >= 500 && stmHost(request.url.host)) stmDown("error ${errorResponse.statusCode}")
                        else "El servicio no respondió correctamente. La consulta se detuvo."
                }
            }
        }
    }

    fun savedAccess(value: Boolean) { state = state.copy(hasSavedAccess = value) }
    fun trySavedSession(): Boolean {
        if(savedSessionAttempted || state.stage!="welcome" || state.busy || !state.hasSavedAccess)return false
        // Lab 21: without internet the entry says «Sin internet» (through the fingerprint path) instead of an error screen.
        if(IsLab && noInternet())return false
        savedSessionAttempted=true
        if(!choices.restoreSessionAccount() || CookieManager.getInstance().getCookie(START).isNullOrBlank())return false
        restorePaymentRecord()
        recoverSession()
        return true
    }
    fun dismissCredited() { state = state.copy(credited = null); lastReceipt = emptyList() }
    fun dismissNotice(message: String) { if (state.message == message) state = state.copy(message = "") }
    fun notice(message: String) { LabLog.add("aviso", message); state = state.copy(message = message, busy = false) }

    private fun restorePaymentRecord() {
        journalUnavailable=false
        paymentRecord=try {choices.accountKey?.let(paymentJournal::read)} catch(e:Exception) {
            // Lab: a record that can never be read again (lost key, damaged data) is cleared instead of blocking every payment;
            // a passing keystore error still keeps the lock.
            val unreadable=e is javax.crypto.BadPaddingException || e is org.json.JSONException || e is IllegalArgumentException ||
                e is java.security.UnrecoverableKeyException || e is android.security.keystore.KeyPermanentlyInvalidatedException
            if(!(IsLab && unreadable && choices.accountKey?.let { a -> runCatching { paymentJournal.clear(a) }.getOrDefault(false) }==true))journalUnavailable=true
            null
        }
        paymentNeedsReview=journalUnavailable || paymentRecord?.phase in setOf("reviewing","authorizing","pending","confirmed")
    }

    private fun trackAuthorization(): Boolean {
        val pending=state.activePayment ?: return false
        val account=choices.accountKey ?: return false
        // A finished payment (credited or rejected) on another card must not block new recharges forever.
        val open=paymentRecord?.takeUnless {it.phase in setOf("credited","rejected") || superseded(it,pending)}
        if(journalUnavailable || open?.let {it.phase!="reviewing" || it.startedAt!=pending.createdAt || it.card!=pending.card || it.provider!=pending.provider || it.amount!=pending.amount}==true)return false
        val transaction=prexPayment.summaryRows.firstOrNull {paymentLabel(it.first)=="transaccion"}?.second.orEmpty()
        val record=PaymentRecord(pending.card,pending.provider,pending.amount,pending.createdAt,state.balance,transaction=transaction)
        if(!paymentJournal.write(account,record))return false
        priorAuthorizationRecord=paymentRecord
        paymentRecord=record;paymentNeedsReview=true
        return true
    }
    /**
     * Lab (A3): a record left open by an EARLIER payment (pending, a receipt STM never confirmed, a look at Prex's own
     * page, an interrupted app) is superseded by the new one instead of blocking every later «Pagar». The balance
     * still tells how the earlier one ended.
     */
    private fun superseded(record: PaymentRecord, pending: ActivePayment) = IsLab && record.startedAt!=pending.createdAt
    private fun trackOriginalReview(): Boolean {
        val pending=state.activePayment ?: return false
        if(journalUnavailable)return false
        paymentRecord?.takeUnless {it.phase in setOf("credited","rejected") || superseded(it,pending)}?.let {return it.startedAt==pending.createdAt && it.card==pending.card && it.provider==pending.provider && it.amount==pending.amount}
        val account=choices.accountKey ?: return false
        val record=PaymentRecord(pending.card,pending.provider,pending.amount,pending.createdAt,state.balance,"reviewing")
        if(!paymentJournal.write(account,record))return false
        paymentRecord=record;paymentNeedsReview=true;return true
    }

    /** Prex's receipt rows of the last approved payment, kept until «Recarga hecha» is closed (its «Ver detalle del pago»). */
    var lastReceipt by mutableStateOf<List<Pair<String,String>>>(emptyList())
        private set

    private fun observePaymentOutcome(stage: String, rows: List<Pair<String,String>>) {
        if(stage=="receipt" && rows.isNotEmpty()) lastReceipt=rows
        val account=choices.accountKey ?: return
        val pending=state.activePayment ?: return
        val previous=paymentRecord ?: PaymentRecord(pending.card,pending.provider,pending.amount,pending.createdAt,state.balance)
        // Lab 21: STM's «Recarga no confirmada» over a payment still waiting for Prex's answer closes it as refused.
        val phase=when(stage) {"receipt"->"confirmed";"paymentRejected"->"rejected";"paymentPending"->"pending";"stmSuccess"->"credited"
            "stmNotConfirmed"->if(IsLab && previous.phase=="authorizing")"rejected" else return
            else->return}
        val transaction=rows.firstOrNull {paymentLabel(it.first)=="transaccion"}?.second.orEmpty()
        // Lab writes the outcome anyway (a stale record must not ask «sin respuesta» later); other apps stop to review.
        if(!IsLab && previous.transaction.isNotBlank() && transaction.isNotBlank() && previous.transaction!=transaction) {
            journalUnavailable=true
            paymentNeedsReview=true;return
        }
        val record=previous.copy(phase=phase,transaction=transaction.ifBlank {previous.transaction})
        if(paymentJournal.write(account,record)) {
            paymentRecord=record
            paymentNeedsReview=phase in setOf("authorizing","pending","confirmed")
            if(stage=="receipt") {
                choices.successfulProvider=pending.provider;choices.successfulPayer=pending.payerProfileId
                val suffix=rows.firstOrNull {it.first.trim().trimEnd(':').equals("Medio de pago",true)}?.second?.takeLast(4) ?: prexPayment.verifiedCardSuffix
                choices.successfulCardSuffix=suffix?.takeIf {it.matches(Regex("[0-9]{4}"))}
                choices.successfulCardIdentity=prexPayment.enteredCardIdentity
            }
        } else {journalUnavailable=true;paymentNeedsReview=true}
    }
    private fun paymentLabel(label: String)=java.text.Normalizer.normalize(label,java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").trim().trimEnd(':').lowercase()

    private fun showPaymentStatus() {
        val record=paymentRecord
        val status=when {
            journalUnavailable->"No pudimos recuperar el seguimiento del último pago. No inicies otra recarga hasta revisarlo."
            record?.phase=="confirmed"->"Prex confirmó el pago. Todavía falta comprobar su acreditación en STM."
            record?.phase=="credited"->if(balanceRefreshCount<3)"STM confirmó la recarga. Actualizando el saldo…" else "STM confirmó la recarga, pero el saldo sigue igual. Podés actualizarlo o revisar el pago."
            record?.phase in setOf("reviewing","pending","authorizing")->"Hay una recarga de ${Amounts.format(record?.amount)} por revisar. Consultar el saldo no confirma su resultado."
            else->""
        }
        state=state.copy(paymentNeedsReview=paymentNeedsReview,paymentStatus=status)
    }

    fun acknowledgePaymentReviewed(): Boolean {
        val account=choices.accountKey ?: return false
        if(!paymentJournal.clear(account))return false
        paymentRecord=null;paymentNeedsReview=false;journalUnavailable=false
        state=state.copy(paymentNeedsReview=false,paymentStatus="",message="Revisión registrada. No se envió otra recarga.")
        return true
    }

    fun connect(doc: String, pass: String, rememberForSession: Boolean = false) {
        if (state.busy) return
        // Cédulas under one million have 7 digits (check digit included); newer ones have 8.
        if (!Regex("\\d{7,8}").matches(doc) || pass.isBlank()) { notice("Ingresá tu cédula de 7 u 8 dígitos y la contraseña."); return }
        if (noInternet()) { notice(OFFLINE); return }
        expressTiming.clear()
        clearSecrets()
        switchingTo = null
        sessionAccess.clear()
        if (rememberForSession) try { sessionAccess.remember(doc, pass) } catch (_: Exception) { sessionAccess.clear() }
        silentRelogin = false; silentReloginAttempted = false
        recoveringSession = false; recoverySteps.clear(); pendingHttpError = null
        expressRequest = null
        prexPayment.reset()
        document = doc; password = pass
        accountVerified = false
        choices.useAccount(doc)
        restorePaymentRecord()
        choosingCard = false
        paymentInFlight = false; providerSubmitted = false; handoffSent = false
        interruptedAccess = false; stageTrail.clear()
        secretsExpireAt = System.currentTimeMillis() + 180000
        lastAction = ""; pageStage = ""; passwordStepSeen = false; actionStarted = System.currentTimeMillis()
        state = UiState(stage = "connecting", busy = true, hasSavedAccess = state.hasSavedAccess, accessRequestId = accessRequestId, paymentNeedsReview = paymentNeedsReview)
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
        // Re-reading the same card keeps its last balance on screen while STM answers; another card starts blank.
        val same = id == state.selectedCard
        choices.card = id
        choosingCard = false
        activityOpened=false
        state = state.copy(selectedCard = id, minimum = null, balance = if (same) state.balance else null, amount = null,activity=ActivityState())
        act("card", id)
    }

    /** The card Lab is switching to from the «Tus boleteras» sheet, while STM answers for it. */
    var switchingTo by mutableStateOf<String?>(null)
        private set

    /**
     * Lab: another card chosen in the sheet over the home, from the list already read. STM is asked straight for that
     * card (the usual card becomes this one, and the regular re-read reaches it); no full-screen picker in between.
     */
    fun switchCard(id: String) {
        if (IsLab && noInternet()) { notice("No hay internet en el celular. Seguís viendo la boletera de antes."); return }
        if (state.busy || id == state.selectedCard || state.cards.none { it.id == id && it.active }) return
        choices.card = id
        switchingTo = id
        activityOpened = false
        state = state.copy(selectedCard = id, balance = null, minimum = null, consultedAt = null, amount = null, activity = ActivityState())
        refresh()
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
        if (!payerStore.forget()) { notice(if (IsLab) "No pudimos borrar los datos para Prex guardados. Probá de nuevo." else "No pudimos borrar los datos de titular guardados. No se confirmó su eliminación."); return false }
        payerProfiles = emptyList()
        payerStoreAvailable = true
        choices.forgetAll()
        sessionAccess.clear()
        expressRequest = null
        return true
    }

    fun choosePayer(id: String) {
        // Lab chooses the payer in the amount panel, on the balance screen, before the payment starts.
        if (!accountVerified || state.busy || state.activePayment != null || state.stage != "paymentBoundary" && !(IsLab && state.stage == "balance")) return
        if (payerProfiles.none { it.id == id }) return
        choices.payerProfileId = id
        state = state.copy(payerProfileId = id, message = "")
    }

    fun savePayer(profile: PayerProfile): Boolean {
        if (IsLab && !payerStoreAvailable) { notice("Los datos para Prex guardados en este celular no se pueden leer. Borralos con «Olvidar este celular» y cargalos de nuevo."); return false }
        if (IsLab && (!accountVerified || state.busy)) { notice("STM todavía está leyendo tu saldo. Esperá un momento y volvé a intentar."); return false }
        if (!accountVerified || state.busy || state.activePayment != null || !payerStoreAvailable || !profile.valid()) return false
        // Editing keeps the person's place in the list; only a new one goes last.
        val unmarked = payerProfiles.map { if (it.id != profile.id && profile.personal && it.personal) it.copy(personal = false) else it }
        val updated = if (IsLab && unmarked.any { it.id == profile.id }) unmarked.map { if (it.id == profile.id) profile else it }
            else unmarked.filterNot { it.id == profile.id } + profile
        return try {
            if (!payerStore.write(updated)) false else {
                payerProfiles = updated
                choices.payerProfileId = profile.id
                state = state.copy(payerProfileId = profile.id, message = "")
                true
            }
        } catch (_: Exception) { notice(if (IsLab) "No se pudieron guardar los datos en este celular." else "No se pudieron guardar los datos del titular en este celular."); false }
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
        // Lab (A3): a previous payment never blocks the next one; the journal still resolves it from the balance.
        if (!accountVerified || (paymentNeedsReview && !IsLab) || journalUnavailable || paymentInFlight || state.stage != "paymentBoundary" || state.busy || provider !in PaymentPolicy.supported || !Amounts.valid(amount, state.minimum)) return
        if (provider == "1033" && payerProfiles.none { it.id == state.payerProfileId }) {
            notice("Elegí o guardá los datos para esta Prex antes de seguir.")
            return
        }
        if (provider == "1002" && !paymentBrowser.available()) { notice("Para pagar con eBROU necesitás Chrome instalado y habilitado."); return }
        val pending = ActivePayment(card, provider, amount, System.currentTimeMillis(), state.payerProfileId.takeIf { provider == "1033" })
        paymentInFlight = true; providerSubmitted = false; handoffSent = false
        active = true; lastAction = ""; actionStarted = System.currentTimeMillis()
        state = state.copy(stage = "openingPayment", busy = true, activePayment = pending, message = "")
        if(provider=="1002" && !trackAuthorization()) {fail("No pudimos guardar el seguimiento de esta recarga. No se abrió el banco.");return}
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
                expressAmount = state.activePayment?.amount?.takeIf { expressPayment }, expectedAmount = state.activePayment?.amount,
                expectedCardIdentity=choices.successfulCardIdentity,expectedCardSuffix=choices.successfulCardSuffix)) {
            paymentOpened()
            state = state.copy(stage = "embeddedPrex")
        } else fail("No se pudo abrir esta solicitud de Prex. No se volvió a enviar la recarga.")
    }

    fun leavePrexPayment() {
        if (state.stage != "embeddedPrex") return
        // Without internet the balance can't be read again: back to the last one, the payment session closed.
        if (IsLab && noInternet()) { leaveOffline(); return }
        refresh()
    }
    /** Lab: «Cancelar» / back while the recharge is being prepared (STM busy opening the payment). */
    fun cancelPreparing() {
        if (state.stage !in setOf("openingPayment", "paymentBoundary", "connecting")) return
        if (noInternet()) { leaveOffline(); return }
        state = state.copy(busy = false); refresh()
    }
    private fun leaveOffline() {
        // Lab: a payment Prex confirmed keeps saying «en camino» on the home even though the balance shown is the old one,
        // and the minimum is read again (HomePayment, tested on the JVM).
        val way = HomePayment.offlineOnTheWay(paymentRecord, state.selectedCard, state.balance, IsLab, state.onTheWay)
        clearPaymentSession(); active = false; handler.removeCallbacks(poll)
        state = state.copy(stage = if (state.balance != null) "balance" else "welcome", busy = false, amount = null, captcha = null,
            onTheWay = way, minimum = HomePayment.offlineMinimum(IsLab, state.minimum),
            message = "No hay internet en el celular. Te mostramos el último saldo consultado.")
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

    fun openActivity() {
        val card=state.selectedCard ?: return
        if(!accountVerified || state.busy || paymentInFlight || state.amount!=null || state.stage !in listOf("balance","activity"))return
        balanceRefreshTask?.let(handler::removeCallbacks);balanceRefreshTask=null
        activityOpened=true;active=true;lastAction="";pageStage=""
        state=state.copy(stage="activity",busy=true,message="",activity=ActivityState(card=card))
        actionStarted=System.currentTimeMillis();awaitingNavigation=true;navigationGeneration++;polling=false
        web.loadUrl("https://stm.gub.uy/app/mistm/cuenta/pages/principal.xhtml")
        handler.removeCallbacks(poll);handler.post(poll)
    }

    fun closeActivity() {
        if(!activityOpened)return
        activityOpened=false;state=state.copy(busy=false)
        refresh()
    }

    fun refresh() {
        if (state.busy) return
        if (noInternet()) { notice(if (state.balance != null) "No hay internet en el celular. Te mostramos el último saldo consultado." else OFFLINE); return }
        activityOpened=false
        expressTiming.move(ExpressTiming.Phase.RETURN)
        balanceRefreshTask?.let(handler::removeCallbacks);balanceRefreshTask=null
        clearPaymentSession()
        clearSecrets(); lastAction = ""; pageStage = ""; active = true; paymentInFlight = false; handoffSent = false
        state = state.copy(stage = "connecting", busy = true, minimum = null, amount = null, captcha = null, message = "")
        actionStarted = System.currentTimeMillis()
        awaitingNavigation = true; navigationGeneration++; polling = false
        web.loadUrl(CARDS)
        handler.removeCallbacks(poll); handler.post(poll)
    }

    fun prepare(amount: Long) {
        if (state.busy || pageStage != "amount") return
        if (!Amounts.valid(amount, state.minimum)) { notice("El monto debe alcanzar el mínimo informado por STM."); return }
        expressRequest = null
        expressPayment = false
        state = state.copy(amount = amount)
        // JS re-reads the minimum immediately before submission. Never selects/submits a payment provider.
        act("amount", amount.toString())
    }

    /**
     * Lab's «Continuar con Prex» from the amount panel: the amount and the payment go together, with the payer already
     * chosen there, so there is no «Tu recarga» step. It rides the same one-shot request as Express, but as a normal
     * journey: Prex still asks for the card and nothing is confirmed without the person's «Pagar».
     */
    fun prepareWithPrex(amount: Long, payerId: String? = null) {
        if (IsLab && noInternet()) { notice(OFFLINE); return }
        val card = state.selectedCard ?: return
        if (state.busy || pageStage != "amount") { if (IsLab) notice("STM todavía está leyendo tu saldo. Esperá un momento y volvé a intentar."); return }
        if (!Amounts.valid(amount, state.minimum)) { notice("El monto debe alcanzar el mínimo informado por STM."); return }
        // The person just chosen (Lab 18) wins over the one remembered.
        val payer = payerId?.let { id -> payerProfiles.find { it.id == id && it.valid() } }
            ?: payerProfiles.find { it.id == state.payerProfileId && it.valid() } ?: payerProfiles.singleOrNull()?.takeIf { it.valid() }
        if (payer == null) { notice(if (IsLab) "Agregá tus datos para Prex para seguir." else "Agregá el titular de la Prex para seguir."); return }
        if (IsLab) choices.payerProfileId = payer.id
        expressRequest = ExpressChoice(card, "1033", payer.id)
        expressPayment = false
        state = state.copy(amount = amount, payerProfileId = payer.id)
        act("amount", amount.toString())
    }

    private fun expressPayer(): PayerProfile? {
        if(IsLab) payerProfiles.filter { it.valid() }.let { valid -> return valid.find { it.personal } ?: valid.singleOrNull() }
        if(choices.successfulProvider=="1033")return payerProfiles.find {it.id==choices.successfulPayer && it.valid()}
        return payerProfiles.find {it.id==state.payerProfileId && it.valid()} ?: payerProfiles.singleOrNull()?.takeIf {it.valid()}
    }

    private val expressProvider: String? get()=choices.successfulProvider ?: choices.provider
    val expressProviderName: String get() = if(expressProvider=="1033")"Prex" else "Medio guardado"
    // Lab: the card is chosen in Google's list each time, so the hint names no last four (Lab 21).
    val expressPaymentLabel: String get()=expressProviderName + (if(IsLab) "" else choices.successfulCardSuffix?.let {" · •••• $it"}.orEmpty())
    private val expressProviderReady: Boolean get() = expressProvider=="1033" && payerStoreAvailable &&
        (expressPayer()!=null || IsLab && payerProfiles.any { it.valid() })
    /** Lab: several people saved and none marked as the person's own; Carga Express asks once which data are theirs. */
    val expressNeedsOwnData: Boolean get() = IsLab && expressPayer()==null && payerProfiles.count { it.valid() } > 1
    val expressAvailable: Boolean get() = accountVerified && (!paymentNeedsReview || IsLab) && !journalUnavailable && expressProviderReady &&
        choices.card==state.selectedCard &&
        state.selectedCard!=null && state.cards.any { it.id==state.selectedCard && it.active } &&
        Amounts.valid(state.minimum,state.minimum)

    /**
     * Lab 21 (owner, 17/10): a payment of the last 30 minutes for this card that never got Prex's answer (a cut right after
     * «Pagar», or a final page the app did not recognise). Only asked about when paying again; the home never shows it.
     */
    /** This payment already reached Prex's «Pagar» (its record is past «reviewing»): an error must not say nothing was charged. */
    internal fun paymentStarted(payment: ActivePayment?): Boolean = payment != null && paymentRecord?.let { it.startedAt == payment.createdAt && it.phase != "reviewing" } == true
    internal val unansweredPayment: PaymentRecord? get() = paymentRecord?.takeIf { IsLab && it.phase in setOf("authorizing","pending") &&
        it.card == state.selectedCard && System.currentTimeMillis() - it.startedAt in 0 until 30 * 60 * 1000L }

    fun startExpress() { startExpressOrWhy() }
    /** Starts Carga Express, or says (in plain words, for the report) why it cannot right now; null when it started. */
    fun startExpressOrWhy(): String? {
        if (IsLab && noInternet()) { notice(OFFLINE); return "sin internet" }
        val card=state.selectedCard ?: return "sin boletera elegida"
        val amount=state.minimum ?: return "sin mínimo de recarga"
        if(!accountVerified) return "sesión sin verificar"
        if(state.busy) return "STM ocupado"
        if(state.stage!="balance") return "pantalla ${state.stage}"
        if(pageStage!="amount") return "STM en ${pageStage.ifBlank { "carga" }}"
        if(state.activePayment!=null) return "hay un pago en curso"
        if(expressRequest!=null) return "ya hay una Carga Express en curso"
        if(!expressAvailable) return "Carga Express no disponible"
        val provider=expressProvider ?: return "sin medio de pago elegido"
        if(provider!="1033") return "el medio de pago no es Prex"
        val payer=expressPayer() ?: return if(expressNeedsOwnData) "elegir tus datos" else "sin titular"
        if(state.cards.none { it.id==card && it.active }) return "boletera no operativa"
        if(!Amounts.valid(amount,state.minimum)) return "importe inválido"
        expressRequest=ExpressChoice(card,provider,payer?.id)
        expressPayment=true
        expressTiming.start()
        state=state.copy(amount=amount)
        // The existing adapter checks the live minimum before submitting; authorization stays with the provider.
        act("amount",amount.toString())
        return null
    }
    /** Why the last Express with the saved card did not start, for «Enviar reporte». */
    var expressNotStarted = ""

    fun continueCaptcha() {
        if (!active || state.captcha == null) return
        lastAction = ""
        // Leaving the app wipes the typed access; the fingerprint-saved session copy can bring it back.
        if (pageStage in setOf("document", "password") && (document == null || password == null)) sessionAccess.open()?.let {
            document = it.first; password = it.second; secretsExpireAt = System.currentTimeMillis() + 180000
        }
        when (pageStage) {
            "document" -> document?.let { act("document", it) } ?: lostAccess()
            "password" -> password?.let { act("password", it) } ?: lostAccess()
            else -> notice("Terminá la verificación. Si aparece otro paso, Boletera se va a detener.")
        }
    }
    private fun lostAccess() {
        cancel()
        state = state.copy(message = "Por seguridad, Boletera olvida lo que escribiste cuando salís de la app. Ingresá de nuevo para terminar la verificación.")
    }

    fun cancel() {
        switchingTo = null
        activityOpened=false
        expressTiming.finish(ExpressTiming.End.CANCELLED)
        sessionRequest++
        passwordStepSeen = false
        recoveringSession = false; recoverySteps.clear(); paymentNeedsReview = false; pendingHttpError = null
        clearPaymentSession()
        clearSecrets(); sessionAccess.clear(); active = false; accountVerified = false; paymentInFlight = false; handler.removeCallbacks(poll)
        web.stopLoading(); web.loadUrl("about:blank"); host.crop = null
        prexPayment.reset()
        state = UiState(hasSavedAccess = state.hasSavedAccess, accessRequestId = accessRequestId)
    }

    fun logout() {
        cancel()
        expressTiming.clear()
        choices.clearSessionAccount()
        state = state.copy(busy = true)
        // Remove this app's local session, not sessions in another browser or device.
        val request = sessionRequest
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            if (!destroyed && request == sessionRequest) state = state.copy(busy = false)
        }
        WebStorage.getInstance().deleteAllData()
        web.clearHistory(); web.clearCache(true)
        // Lab's entry would read any message as a failed sign-in («No pudimos entrar»): there, closing the session just returns to it.
        state = state.copy(message = if (IsLab) "" else "Sesión local eliminada. El acceso cifrado se conserva hasta que elijas olvidarlo.")
    }

    private var pausedAt = 0L
    fun pause() {
        foreground=false; if (IsLab) pausedAt=System.currentTimeMillis()
        expressTiming.pause(true)
        // Suspend only: the existing authorization remains bound to this one in-memory journey.
        prexPayment.pause()
        handler.removeCallbacks(poll)
        if (document != null || password != null) interruptedAccess = true
        clearSecrets()
    }
    fun resume() {
        foreground=true
        if (pausedAt > 0) { actionStarted += System.currentTimeMillis() - pausedAt; pausedAt = 0 }
        expressTiming.pause(false)
        prexPayment.resume()
        // The decision lives in HomePayment (tested on the JVM). The refresh must not hang from the poll's condition: on the
        // home the poll keeps running, and Lab 21 never refreshed on coming back because of that.
        val plan = HomePayment.resumePlan(state.stage, active, IsLab, accountVerified, state.busy, state.credited,
            noInternet = state.stage == "balance" && noInternet(), onTheWay = state.onTheWay, consultedAt = state.consultedAt,
            now = System.currentTimeMillis(), pageStage = pageStage)
        if (plan.restartPoll) { handler.removeCallbacks(poll); handler.post(poll) }
        if (plan.refresh) refresh()
    }
    fun destroy() {
        destroyed = true; active = false; handler.removeCallbacksAndMessages(null); clearSecrets(); sessionAccess.clear()
        (web.parent as? ViewGroup)?.removeView(web); web.destroy()
        paymentBrowser.close()
        prexPayment.destroy()
    }

    private fun clearSecrets() { document = null; password = null; secretsExpireAt = 0L }
    private fun stmHost(host: String?) = host != null && (host == "stm.gub.uy" || host.endsWith(".stm.gub.uy") || host == "iduruguay.gub.uy" || host.endsWith(".iduruguay.gub.uy"))
    /** No network at all (airplane mode, Wi-Fi and data off): say so at once instead of waiting for a timeout. */
    internal fun noInternet(): Boolean = try {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        cm?.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) != true
    } catch (_: Exception) { false }
    /** The phone has validated internet access, so a failing STM is STM's problem. */
    private fun online(): Boolean = try {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        cm?.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    } catch (_: Exception) { false }
    /** Marker text recognized by errorPresentation as an STM outage. */
    private fun stmDown(detail: String) = "STM no está respondiendo ($detail). La página oficial parece caída o con problemas."

    private fun fail(message: String) {
        LabLog.add("STM", "falla: $message")
        if(activityOpened) {
            active=false;handler.removeCallbacks(poll)
            // Say which step failed; an unread page is not an empty history.
            val reason=when(message) {
                "Boletera distinta" -> "STM mostró otra boletera en la página de saldo. No seguimos para no mezclar datos de tarjetas."
                "Movimientos sin respuesta" -> "El botón Movimientos de STM no respondió. Puede ser una demora del sitio; probá de nuevo en un rato."
                "Actividad no reconocida" -> "STM abrió una pantalla de movimientos que Boletera todavía no sabe leer. Si tu cuenta tiene nivel 2, puede ser tu historial real: copiá la información de ayuda en Configuración y mandala."
                else -> "La consulta a STM se cortó: $message"
            }
            state=state.copy(stage="activity",busy=false,message="",activity=ActivityState(card=state.selectedCard.orEmpty(),access=ActivityAccess.UNAVAILABLE,reason=reason))
            return
        }
        expressTiming.finish(ExpressTiming.End.ERROR)
        expressRequest = null
        if (BuildConfig.DEBUG) captureDebugShape()
        // The masked shape of the page where it stopped (site, titles, buttons; digits hidden, never field values) goes
        // into «Enviar reporte», so an unknown step (a Google or gub.uy screen) can be recognised later.
        web.evaluateJavascript(activityShapeScript) { raw ->
            activityShape = "Error · $message\n" + (try { org.json.JSONTokener(raw).nextValue().toString() } catch (_: Exception) { "" }).take(1500)
        }
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
        if (!active || !foreground || destroyed || !NavigationPolicy.allowed(web.url ?: "")) return
        if (lastAction == action && state.busy) return
        lastAction = action; actionStarted = System.currentTimeMillis()
        LabLog.add("STM", "acción $action")
        state = state.copy(busy = true, message = "")
        val generation = navigationGeneration
        val request = sessionRequest
        val attemptedStage = pageStage
        // JSONObject.quote escapes user input as a literal; it can never become executable code.
        val script = "window.BoleteraAdapter && window.BoleteraAdapter.command(${JSONObject.quote(action)}, ${JSONObject.quote(value)})"
        web.evaluateJavascript(script) { result ->
            if (active && !destroyed && request == sessionRequest && generation == navigationGeneration && result != "true") {
                handler.postDelayed({
                if (!active || destroyed || request != sessionRequest || generation != navigationGeneration) return@postDelayed
                web.evaluateJavascript(adapter + "\nJSON.stringify({session:window.BoleteraSession.snapshot(),stage:window.BoleteraAdapter.snapshot().stage})") { raw ->
                    if (!active || destroyed || request != sessionRequest || generation != navigationGeneration) return@evaluateJavascript
                    val observed = try { JSONObject(JSONTokener(raw).nextValue() as String) } catch (_: Exception) { null }
                    val signal = observed?.optString("session")
                    if (signal in listOf("expired", "login") && accountVerified) recoverSession()
                    else if(observed?.optString("stage") in setOf("cards","balance","amount","paymentBoundary") && observed?.optString("stage")!=attemptedStage) return@evaluateJavascript
                    else if (paymentInFlight) fail("El proveedor no aceptó el siguiente paso. Revisá el estado del pago antes de repetirlo.")
                    else {
                        clearSecrets(); expressRequest = null; expressPayment=false
                        expressTiming.move(ExpressTiming.Phase.REVIEW)
                        // Lab's home has no «Consultar»: it says what to do there, and «Recargar» reads the page again.
                        state = if (IsLab && state.stage == "balance") state.copy(busy = false, amount = null, message = "STM no respondió como esperábamos. No se hizo nada; deslizá hacia abajo para probar de nuevo.")
                            else state.copy(busy = false, message = "La página cambió o no aceptó el paso. Volvé a consultar; no se repitió la operación.")
                    }
                }
                },1200)
            }
        }
    }

    private fun inspect() {
        if (!active || !foreground || destroyed || polling || !allowedPage(web.url ?: "")) return
        if (paymentInFlight && !NavigationPolicy.allowed(web.url ?: "")) {
            inspectPaymentPage()
            if (System.currentTimeMillis() - actionStarted > 45000) fail("El proveedor demoró demasiado. Revisá el pago antes de iniciar otro.")
            return
        }
        if (awaitingNavigation) {
            if (System.currentTimeMillis() - actionStarted > 35000) fail("No comenzó la nueva consulta. Volvé a ingresar.")
            return
        }
        // A slow verification is not an expired session: the person is still solving it.
        if (secretsExpireAt != 0L && System.currentTimeMillis() > secretsExpireAt && state.captcha == null) {
            expired(); return
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
        if(!foreground)return
        val stage = data.optString("stage", "unknown")
        if(stage=="password")passwordStepSeen=true
        val loginStage = stage in setOf("start", "identity", "document", "password", "signedOut")
        if (stage == "sessionExpired" || accountVerified && loginStage) {
            if (recoveringSession) expired() else recoverSession()
            return
        }
        if (recoveringSession && stage in setOf("document", "password", "signedOut")) {
            if (!restoreSessionAccess()) { expired(); return }
            if (stage == "signedOut") {
                actionStarted = System.currentTimeMillis(); awaitingNavigation = true; navigationGeneration++
                web.loadUrl(START)
                return
            }
        }
        if (data.optString("authError") == "credentials" && !accountVerified && !paymentInFlight) {
            if (silentRelogin) sessionAccess.clear()
            silentRelogin = false
            pendingHttpError = null
            fail("Documento o contraseña incorrectos. Revisalos e intentá de nuevo.")
            return
        }
        if (pendingHttpError != null && stage != "loading") {
            val message = pendingHttpError!!; pendingHttpError = null
            if (loginStage && recoveringSession) expired() else fail(message)
            return
        }
        val changed = stage != pageStage
        if(activityOpened && stage !in setOf("start","identity","document","password","signedOut","sessionExpired")) {
            if(changed) {lastAction="";actionStarted=System.currentTimeMillis(); LabLog.add("STM", "actividad: página $stage")}
            pageStage=stage
            when(stage) {
                "loading","handoff" -> state=state.copy(stage="activity",busy=true)
                "balance" -> {
                    if(data.optString("cardId")!=state.selectedCard) {fail("Boletera distinta");return}
                    if(lastAction!="movements")act("movements",state.selectedCard.orEmpty())
                    else if(System.currentTimeMillis()-actionStarted>20000)failActivityWithShape("Movimientos sin respuesta")
                }
                "activityIdentityRequired" -> {
                    active=false;handler.removeCallbacks(poll)
                    state=state.copy(stage="activity",busy=false,message="",activity=ActivityState(card=state.selectedCard.orEmpty(),access=ActivityAccess.IDENTITY_REQUIRED))
                }
                else -> {
                    // The authorized table has not been inspected yet. Never turn an unknown
                    // document into an empty history or derive a frequent-user count from it.
                    if(System.currentTimeMillis()-actionStarted>8000)failActivityWithShape("Actividad no reconocida")
                }
            }
            return
        }
        val knownStages = setOf("loading", "handoff", "start", "identity", "document", "password", "cards", "cardsLoading", "balance", "amount", "paymentBoundary", "signedOut", "sessionExpired", "unknown", "verification", "blocked")
        if (changed) {
            if (stageTrail.size >= 6) stageTrail.removeFirst()
            stageTrail.addLast(if (stage in knownStages) stage else "unknown")
        }
        val shape = if (stage == "cards" || stage == "cardsLoading")
            " · tabla=${if (data.optBoolean("tablePresent")) 1 else 0}, filas=${data.optInt("rowCount").coerceIn(0, 999)}, visibles=${data.optInt("visibleRowCount").coerceIn(0, 999)}, leídas=${(data.optJSONArray("cards")?.length() ?: 0).coerceIn(0, 999)}" else ""
        state = state.copy(diagnostic = stageTrail.joinToString(" → ") + shape)
        if (changed) { lastAction = ""; state = state.copy(busy = false); actionStarted = System.currentTimeMillis(); LabLog.add("STM", "página $stage") }
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
            expressTiming.finish(ExpressTiming.End.ERROR)
            expressRequest = null
            clearSecrets(); active = false
            state = state.copy(stage = "blocked", busy = false, message = "El sitio no aceptó los datos o la verificación. Podés ingresar otra vez manualmente.")
            return
        }
        if (state.busy && System.currentTimeMillis() - actionStarted > 35000 && rect == null) {
            if (recoveringSession) expired() else fail(if (online() && stmHost(android.net.Uri.parse(web.url ?: "").host)) stmDown("demasiado lenta") else "El sitio demoró demasiado. La app no repetirá el paso automáticamente.")
            return
        }
        if (rect != null) {
            state = state.copy(stage = "captcha", busy = false)
            return
        }
        fun cents(key: String): Long? = if (data.has(key) && !data.isNull(key)) data.getLong(key) else null
        when (stage) {
            "loading", "handoff" -> state = state.copy(stage = if (paymentInFlight) "openingPayment" else "connecting", busy = true)
            "start", "identity" -> if (recoveringSession) {
                if (lastAction != stage && recoverySteps.add(stage)) act(stage)
                else if (!state.busy) expired()
            } else if (password != null && lastAction != stage) act(stage) else if (password == null) expired()
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
                    try {
                        payerProfiles = payerStore.read(); payerStoreAvailable = true
                        // Lab 18: a single profile saved before «Son mis datos» existed is the person's own; with several, the app asks once.
                        val only = payerProfiles.singleOrNull()
                        if (IsLab && !choices.personalMigrated) {
                            val mine = only?.takeIf { !it.personal }?.copy(personal = true)
                            // Marked only once it is written; with nothing to mark (none, several, or already marked) it is done.
                            if (mine == null) choices.personalMigrated = true
                            else if (runCatching { payerStore.write(listOf(mine)) }.getOrDefault(false)) { payerProfiles = listOf(mine); choices.personalMigrated = true }
                        }
                    }
                    catch (_: Exception) { payerProfiles = emptyList(); payerStoreAvailable = false }
                }
                accountVerified = true
                silentRelogin = false; silentReloginAttempted = false
                choices.rememberVerifiedSession()
                CookieManager.getInstance().flush()
                recoveringSession = false; recoverySteps.clear()
                val preferred = choices.card
                // Passing through STM's card list to reach the usual card is not something to show:
                // publishing "cards" here made the picker flash after leaving it or refreshing.
                val passThrough = !choosingCard && !state.busy && lastAction != "card" && list.any { it.id == preferred && it.active }
                state = state.copy(stage = if (passThrough) "connecting" else "cards", recoveringSession = false, sessionExpired = false, cards = list, activePayment = null,
                    payerProfileId = choices.payerProfileId?.takeIf { id -> payerProfiles.any { it.id == id } } ?: payerProfiles.singleOrNull()?.id,
                    message = if (payerStoreAvailable) "" else "No pudimos recuperar los perfiles de Prex guardados. Conservamos sus archivos sin sobrescribirlos.")
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
                    fail("No pudimos leer la lista de boleteras. No quiere decir que no tengas ninguna: probá de nuevo en un rato.")
                }
            }
            "balance" -> {
                clearSecrets()
                switchingTo = null
                // Never show one card's balance under another card's name.
                // An all-digit match may be the person's document number, so only a clear card code counts.
                val pageCard = data.optString("cardId").takeIf { it.isNotBlank() && it != "null" && it.any(Char::isLetter) }
                if (pageCard != null && state.selectedCard != null && !pageCard.equals(state.selectedCard, ignoreCase = true)) {
                    fail("STM mostró una boletera distinta a la que elegiste. No mostramos ese saldo para no confundirte; volvé a consultar.")
                    return
                }
                state = state.copy(stage = "balance", balance = cents("balance"), consultedAt = System.currentTimeMillis())
                if (lastAction != "minimum") act("minimum")
            }
            "amount" -> {
                val pageBalance = cents("balance")
                val resolvedBalance = pageBalance ?: state.balance
                val minimum = cents("minimum")
                if (minimum == null || resolvedBalance == null) {
                    state = state.copy(
                        stage = "connecting",
                        busy = true,
                        balance = resolvedBalance,
                        minimum = minimum,
                        message = "Esperando que STM termine de mostrar el saldo y el mínimo."
                    )
                    // STM fills these by AJAX and can be slow; give it time before calling it a failure.
                    if (System.currentTimeMillis() - actionStarted > 20000) {
                        val missing = listOfNotNull(
                            if (resolvedBalance == null) "saldo" else null,
                            if (minimum == null) "mínimo" else null
                        ).joinToString(" y ")
                        state = state.copy(
                            diagnostic = state.diagnostic +
                                " · datos: saldo=${if (resolvedBalance != null) 1 else 0}, mínimo=${if (minimum != null) 1 else 0}"
                        )
                        fail("STM mostró la pantalla de recarga, pero faltó el $missing. No se habilitó la recarga.")
                    }
                    return
                }
                state = state.copy(
                    stage = "balance",
                    // The old form can remain visible during an asynchronous submission.
                    // Reading its amounts again does not acknowledge or cancel that request.
                    busy = lastAction == "amount" && state.busy,
                    balance = resolvedBalance,
                    minimum = minimum,
                    consultedAt = if(changed || state.minimum!=minimum || state.balance!=resolvedBalance || state.consultedAt==null) System.currentTimeMillis() else state.consultedAt,
                    message = if (lastAction == "amount") state.message else ""
                )
                if(expressTiming.phase in setOf(ExpressTiming.Phase.RETURN,ExpressTiming.Phase.RECOVERY))expressTiming.finish(ExpressTiming.End.BALANCE)
                run {
                    // What to show is decided in HomePayment (tested on the JVM); this only applies it.
                    val settled=HomePayment.settle(paymentRecord,state.selectedCard,state.balance,System.currentTimeMillis(),IsLab)
                    if(settled.forgetStale) {
                        choices.accountKey?.let {paymentJournal.clear(it)}
                        paymentRecord=null;paymentNeedsReview=false
                    }
                    if(settled.record!=null) paymentRecord=settled.record
                    val record=settled.record
                    if(settled.verdict!=HomePayment.Verdict.NONE) {
                        val account=choices.accountKey
                        if(settled.verdict==HomePayment.Verdict.AWAIT_BALANCE) {
                            paymentNeedsReview=true
                            if(balanceRefreshRecord!=record!!.startedAt) {balanceRefreshRecord=record.startedAt;balanceRefreshCount=0}
                            if(balanceRefreshTask==null && balanceRefreshCount<3) {
                                balanceRefreshTask=Runnable {
                                    balanceRefreshTask=null
                                    if(foreground && state.stage=="balance" && !state.busy && paymentRecord?.startedAt==record.startedAt) {
                                        balanceRefreshCount++;refresh()
                                    }
                                }.also {handler.postDelayed(it,1000L*(balanceRefreshCount+1))}
                            }
                        } else if(account!=null && paymentJournal.clear(account)) {
                            paymentRecord=null;paymentNeedsReview=false
                            val credited=settled.verdict==HomePayment.Verdict.CLOSE_CREDITED
                            // Lab shows its own "Recarga hecha" screen instead of a passing message.
                            state=state.copy(credited=if(credited && IsLab)record?.amount else null,
                                message=if(IsLab)"" else if(credited)"Recarga confirmada por STM. Saldo actualizado." else "El pago fue rechazado. Saldo consultado.")
                        }
                    }
                    // Lab 18: a payment Prex (or STM's receipt) confirmed, not yet in the balance, is said on the home so
                    // nobody pays twice; never a doubtful one («pending» stays silent, as the owner asked).
                    if(IsLab) {
                        val way=HomePayment.onTheWay(paymentRecord,state.selectedCard,state.balance,IsLab)
                        if(state.onTheWay!=way) state=state.copy(onTheWay=way)
                    }
                    showPaymentStatus()
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
                    val valid=accountVerified && express.provider=="1033" && express.card==state.selectedCard &&
                        list.any { it.id==express.provider } &&
                        payerProfiles.any { it.id==express.payerId && it.valid() }
                    if(valid) {
                        state=state.copy(selectedProvider=express.provider,payerProfileId=express.payerId)
                        beginPayment()
                    } else {
                        expressPayment=false
                        expressTiming.move(ExpressTiming.Phase.REVIEW)
                        if(!IsLab) notice("STM cambió las opciones disponibles. Revisá esta recarga para continuar.")
                    }
                }
                // Lab never shows the old «Tu recarga» screen (it still says «titular»): if the payment did not start
                // from here, back to the home with a short notice (Lab 21).
                if(IsLab && state.stage=="paymentBoundary") {
                    expressPayment=false
                    refresh()
                    // Without internet refresh() stays put: the home is shown with the last balance.
                    if(state.stage=="paymentBoundary") { clearPaymentSession(); active=false; handler.removeCallbacks(poll); state=state.copy(stage="balance",busy=false,amount=null,minimum=null) }
                    state=state.copy(message=StmEngine.LAB_PAYMENT_NOT_STARTED)
                }
            }
            "signedOut" -> expired()
            "unknown", "verification" -> if (data.optString("reason") == "passwordChange") {
                clearSecrets()
                failWithShape("Cambio de contraseña", "gub.uy te pide cambiar o confirmar tu contraseña. Ese paso se hace en el sitio; después volvé a entrar.")
            } else if (System.currentTimeMillis() - actionStarted > 25000) {
                if(recoveringSession)expired()
                else if(!accountVerified && !paymentInFlight && passwordStepSeen) {
                    clearSecrets();active=false;pendingHttpError=null;handler.removeCallbacks(poll)
                    host.crop=null;web.stopLoading()
                    state=state.copy(stage="accessHelp",busy=false,captcha=null,message="")
                } else failWithShape("Paso desconocido", "Este acceso requiere un paso que Boletera todavía no reconoce. No vamos a mostrarte la página completa.")
            }
            "blocked" -> fail("Pantalla fuera del recorrido permitido.")
        }
    }
    /** Retry only navigation to the account. Amounts, providers and financial confirmations are discarded. */
    private fun recoverSession() {
        activityOpened=false; switchingTo=null
        expressTiming.move(ExpressTiming.Phase.RECOVERY)
        if(paymentRecord==null && state.activePayment!=null && providerSubmitted && !trackOriginalReview())journalUnavailable=true
        paymentNeedsReview = journalUnavailable || paymentRecord?.phase in setOf("reviewing","authorizing","pending","confirmed")
        sessionRequest++
        clearSecrets(); clearPaymentSession()
        recoveringSession = true; recoverySteps.clear(); pendingHttpError = null
        silentRelogin = false; silentReloginAttempted = false
        accountVerified = false; choosingCard = false; providerSubmitted = false
        lastAction = ""; pageStage = ""; active = true; polling = false
        actionStarted = System.currentTimeMillis(); awaitingNavigation = true; navigationGeneration++
        host.crop = null
        state = state.copy(stage = "connecting", busy = true, amount = null, captcha = null, message = "",
            activePayment = null, accessRequestId = accessRequestId, recoveringSession = true,
            sessionExpired = false, paymentNeedsReview = paymentNeedsReview)
        web.stopLoading(); web.loadUrl(CARDS)
        handler.removeCallbacks(poll); handler.post(poll)
    }

    private fun expired() {
        activityOpened=false; switchingTo=null
        expressTiming.finish(ExpressTiming.End.CANCELLED)
        if(paymentRecord==null && state.activePayment!=null && providerSubmitted && !trackOriginalReview())journalUnavailable=true
        paymentNeedsReview = journalUnavailable || paymentRecord?.phase in setOf("reviewing","authorizing","pending","confirmed")
        sessionRequest++; accessRequestId++
        if (silentRelogin) sessionAccess.clear()
        silentRelogin = false; silentReloginAttempted = false
        clearSecrets(); clearPaymentSession()
        active = false; accountVerified = false; recoveringSession = false; recoverySteps.clear()
        pendingHttpError = null; providerSubmitted = false; polling = false; awaitingNavigation = false
        lastAction = ""; pageStage = ""; navigationGeneration++
        handler.removeCallbacks(poll); host.crop = null; web.stopLoading(); web.loadUrl("about:blank")
        state = UiState(hasSavedAccess = state.hasSavedAccess, accessRequestId = accessRequestId,
            sessionExpired = true, paymentNeedsReview = paymentNeedsReview)
    }

    private fun restoreSessionAccess(): Boolean {
        if (document != null && password != null) return true
        silentReloginAttempted = true
        val access = sessionAccess.open() ?: return false
        document = access.first
        password = access.second
        secretsExpireAt = System.currentTimeMillis() + 180000
        silentRelogin = true
        state = state.copy(recoveringSession = true, busy = true, sessionExpired = false)
        return true
    }
    companion object {
        const val OFFLINE = "No hay internet en el celular. Conectate a Wi-Fi o datos y volvé a intentar."
        // Lab 21, worded with the owner (17/10): says what happened, calmly, without blaming anyone.
        const val LAB_PAYMENT_NOT_STARTED = "El pago no llegó a hacerse, probá nuevamente."
        const val START ="https://stm.gub.uy/app/mistm/cuenta/"
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
