package uy.boletera.prueba

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.flow.first
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.withResumed
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity

// Entry messages that are the person's own choice or the phone's state, not a failure: no «Enviar reporte» for them.
private const val UNLOCK_NOT_CONFIRMED = "No se desbloqueó el acceso. Podés volver a intentar o ingresar manualmente."
private const val FINGERPRINTS_CHANGED = "Cambiaron las huellas del celular y Android borró el acceso guardado. Ingresá con tu contraseña para guardarlo de nuevo."
private val NOT_A_FAILURE = setOf(StmEngine.OFFLINE, UNLOCK_NOT_CONFIRMED, FINGERPRINTS_CHANGED, "Documento o contraseña incorrectos. Revisalos e intentá de nuevo.")
/** The crash line is written by one handler, however many times the activity is created in the process. */
private var crashLogInstalled = false

/** The engine's reason for not starting Carga Express, said to a person (the raw reason goes to the report). */
internal fun expressWhyCopy(why: String) = when {
    why.startsWith("STM") || why.startsWith("pantalla") || why == "STM ocupado" -> "STM todavía está leyendo tu saldo. Esperá un momento y volvé a intentar."
    why == "sin titular" -> "Para usar Carga Express, cargá tus datos para Prex."
    why == "hay un pago en curso" || why == "ya hay una Carga Express en curso" -> "Ya hay una recarga en curso."
    else -> "No pudimos empezar la Carga Express. Probá de nuevo en un momento."
}

class MainActivity : FragmentActivity() {
    private lateinit var engine: StmEngine
    private lateinit var vault: AccessVault
    /** Set by the launcher's "Recargar" shortcut; the amount panel opens once the balance and minimum are in. */
    private var rechargeShortcut by mutableStateOf(false)
    private fun handleShortcut(intent: android.content.Intent?) { if (intent?.action == ACTION_RECHARGE) rechargeShortcut = true }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); handleShortcut(intent) }
    /** Long-press on the Lab icon: "Ver saldo" and "Recargar", like system apps. */
    private fun publishShortcuts() {
        if (!IsLab) return
        try {
            val manager = getSystemService(android.content.pm.ShortcutManager::class.java) ?: return
            fun shortcut(id: String, label: String, icon: Int, action: String) = android.content.pm.ShortcutInfo.Builder(this, id)
                .setShortLabel(label).setIcon(android.graphics.drawable.Icon.createWithResource(this, icon))
                .setIntent(android.content.Intent(this, MainActivity::class.java).setAction(action)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)).build()
            manager.dynamicShortcuts = listOf(
                shortcut("balance", "Ver saldo", R.drawable.ic_refresh, android.content.Intent.ACTION_MAIN),
                shortcut("recharge", "Recargar", R.drawable.ic_add, ACTION_RECHARGE))
        } catch (_: Exception) {}
    }
    companion object { const val ACTION_RECHARGE = "uy.boletera.action.RECHARGE" }
    private fun openStmAccess() {
        try {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://stm.gub.uy/app/mistm/cuenta/")).addCategory(android.content.Intent.CATEGORY_BROWSABLE))
        } catch (_:android.content.ActivityNotFoundException) {
            engine.notice("No se pudo abrir un navegador. Entrá a stm.gub.uy y buscá Mi STM.")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Screenshots remain available for the owner's prototype feedback.
        engine = StmEngine(this)
        vault = AccessVault(this)
        if (IsLab) OldCardVault.erase(this)
        engine.savedAccess(vault.exists)
        handleShortcut(intent); publishShortcuts()
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        if (IsLab) LabDemo.load(this)
        if (IsLab) {
            LabLog.init(this)
            // A crash leaves its first lines in the record before Android closes the app.
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            if (!crashLogInstalled) Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                LabLog.add("CIERRE", "${error.javaClass.simpleName}: ${error.message.orEmpty().take(200)} @ " +
                    error.stackTrace.take(6).joinToString(" < ") { "${it.fileName}:${it.lineNumber}" })
                Thread.sleep(300)
                previous?.uncaughtException(thread, error)
            }
            crashLogInstalled = true
        }
        LabReport.diagnostic = { if (::engine.isInitialized) engine.helpDiagnostic else "" }
        if (IsLab) lifecycleScope.launch {
            // What STM and Prex are doing, as it changes (screen names and flags only, never page text or data).
            androidx.compose.runtime.snapshotFlow {
                val p = engine.prexPayment
                "app=${engine.state.stage}${if (engine.state.busy) " (ocupado)" else ""} · prex=${p.nativeStage}" +
                    (if (p.challenge != null) " · captcha" else "") + (if (p.verificationApproved) " · captcha aprobado" else "") +
                    (if (p.cardBusy) " · tarjeta enviándose" else "") + (if (p.cardError) " · tarjeta con error" else "") +
                    (if (p.slowStep) " · Prex lento" else "") + (if (p.staleAfterResume) " · recargando al volver" else "") +
                    " · express=${p.expressPhase}" + (if (p.canContinue) " · puede seguir" else " · no puede seguir") +
                    (if (p.completionSubmitted) " · paso enviado" else "") + " · titular ${p.payerValues.count { it.isNotBlank() }}/5" + (if (p.nativeStage == "payer" && p.payerWhy.isNotBlank()) " (${p.payerWhy})" else "") +
                    (if (p.payerConflict) " · titular distinto" else "") + (if (p.verificationRequired) " · pide verificación" else "") +
                    (if (p.message.isNotBlank()) " · error: ${p.message}" else "") +
                    (if (engine.expressPreparing) " · Express preparando" else "")
            }.collect { LabLog.add("estado", it) }
        }
        setContent {
            var appearance by remember { mutableStateOf(preferences.getString("theme", "system") ?: "system") }
            val haptics=remember {AppHaptics(this@MainActivity)}
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
            BoleteraTheme(if(IsLab)"dark" else appearance) {
                // Modo demo replaces the whole app with its made-up journey; the real session waits untouched behind it.
                if(IsLab && LabDemo.on) LabDemoApp() else App(appearance) { appearance = it; preferences.edit().putString("theme", it).apply() }
                if(IsLab) { LabCardFlightOverlay(); LabExpressLightOverlay() }
            }
            }
        }
    }
    override fun onStop() { super.onStop(); LabLog.add("app", "a segundo plano"); if (::engine.isInitialized) engine.pause() }
    override fun onStart() { super.onStart(); LabLog.add("app", "en pantalla"); if (::engine.isInitialized) engine.resume() }
    // Lab: where the finger went (as % of the screen), how long and, if it moved, to where. The keyboard is another
    // window: its keys never reach the app, so typed digits can never be here.
    private var downAt = 0L; private var downX = 0f; private var downY = 0f
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (IsLab) {
            val w = window.decorView.width.coerceAtLeast(1); val h = window.decorView.height.coerceAtLeast(1)
            fun at(x: Float, y: Float) = "${(x * 100 / w).toInt()}%,${(y * 100 / h).toInt()}%"
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> { downAt = event.eventTime; downX = event.x; downY = event.y }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val ms = event.eventTime - downAt
                    val moved = kotlin.math.hypot(event.x - downX, event.y - downY) > 24 * resources.displayMetrics.density
                    val cancel = if (event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) " (cancelado)" else ""
                    LabLog.add("dedo", if (moved) "arrastró ${at(downX, downY)} → ${at(event.x, event.y)} en $ms ms$cancel" else "tocó ${at(event.x, event.y)} · $ms ms$cancel")
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
    override fun onDestroy() { if (::vault.isInitialized) vault.cancel(); if (::engine.isInitialized) engine.destroy(); super.onDestroy() }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun App(appearance: String, onAppearance: (String) -> Unit) {
        val state = engine.state
        val updates: AppUpdates = viewModel()
        LaunchedEffect(updates) { updates.checkAutomatic() }
        DisposableEffect(updates) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) updates.onForeground(this@MainActivity)
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(updates.installRequested, updates.busy) {
            if (updates.installRequested && !updates.busy) lifecycle.withResumed { updates.install(this@MainActivity) }
        }
        var automaticUnlockHandled by rememberSaveable { mutableIntStateOf(-1) }
        var authenticating by remember { mutableStateOf(false) }
        // Lab: this sign-in uses the access saved with the fingerprint; if STM says the password is wrong it is forgotten,
        // so the fingerprint never keeps retrying a wrong password (and gub.uy never locks the account for it).
        var vaultAttempt by remember { mutableStateOf(false) }
        fun unlockSavedAccess() {
            if (authenticating || engine.state.busy || engine.state.stage != "welcome") return
            if (engine.noInternet()) { engine.notice(StmEngine.OFFLINE); return }
            val accessId=engine.state.accessRequestId
            authenticating = true
            vault.unlock { doc, pass ->
                authenticating = false
                if (!isDestroyed && !isFinishing && engine.state.stage=="welcome" && engine.state.accessRequestId==accessId) {
                    if (doc != null && pass != null) { vaultAttempt = true; engine.connect(doc, pass, rememberForSession = true) }
                    else if (vault.invalidated) { engine.savedAccess(false); engine.notice(FINGERPRINTS_CHANGED) }
                    else engine.notice(UNLOCK_NOT_CONFIRMED)
                }
            }
        }
        // Lab (owner, 17/10): a new version found on opening comes before the fingerprint; the fingerprint is asked once the
        // notice is answered «Ahora no» (after «Actualizar» it is not asked: the app is about to be replaced).
        var unlockAfterUpdate by remember { mutableStateOf(false) }
        LaunchedEffect(state.accessRequestId) {
            // Let the Lab opening play smoothly before the STM page loads or the fingerprint prompt covers it.
            kotlinx.coroutines.delay((LabLaunch.openingUntil-android.os.SystemClock.uptimeMillis()).coerceAtLeast(0))
            // The look for a new version gets at most 2 s; without internet or with GitHub slow, the fingerprint is not held back.
            if (IsLab) kotlinx.coroutines.withTimeoutOrNull(2000) { androidx.compose.runtime.snapshotFlow { updates.busy }.first { !it } }
            lifecycle.withResumed {
                val current=engine.state
                if (automaticUnlockHandled != current.accessRequestId) {
                    automaticUnlockHandled = current.accessRequestId
                    val savedSession = vault.exists && current.stage == "welcome" && engine.trySavedSession()
                    if (savedSession) vaultAttempt = true
                    if (vault.exists && current.stage == "welcome" && !savedSession) {
                        if (IsLab && updates.prompt) unlockAfterUpdate = true else unlockSavedAccess()
                    }
                }
            }
        }
        LaunchedEffect(unlockAfterUpdate, updates.prompt, updates.busy, updates.reviewRequested, updates.installRequested) {
            // «Actualizar» and then «Más tarde» (or Android's installer cancelled) also ends here: the fingerprint is asked.
            if (unlockAfterUpdate && !updates.prompt && !updates.busy && !updates.reviewRequested && !updates.installRequested) {
                // Android's installer takes a moment to cover the app after «Actualizar»: the fingerprint waits for it
                // (and comes once the person is back, if the install was cancelled). The flag is cleared only after the
                // wait: clearing it first restarted this effect and the fingerprint was never asked.
                kotlinx.coroutines.delay(1500)
                lifecycle.withResumed { if (unlockAfterUpdate) { unlockAfterUpdate = false; if (engine.state.stage == "welcome") unlockSavedAccess() } }
            }
        }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        val context = LocalContext.current
        var effectsMode by remember { mutableStateOf(VisualEffects.mode(context)) }
        var showTicketGuide by rememberSaveable { mutableStateOf(false) }
        var showAccessGuide by rememberSaveable { mutableStateOf(false) }
        var showPaymentReview by remember {mutableStateOf(false)}
        val helpPreferences=remember { getSharedPreferences("feature_help",MODE_PRIVATE) }
        // First launch explains STM's one-time authorization before the first login attempt, not after it fails.
        var firstAccessIntro by remember { mutableStateOf(!helpPreferences.getBoolean("stm_first_access_v1",false)) }
        fun closeFirstAccessIntro(openStm: Boolean) {
            helpPreferences.edit().putBoolean("stm_first_access_v1",true).apply()
            firstAccessIntro=false
            if(openStm)openStmAccess()
        }
        var showBetaIntro by rememberSaveable {
            mutableStateOf(!BuildConfig.DEBUG && updates.installedChannel==UpdateChannel.STABLE && !helpPreferences.getBoolean("beta_channel_intro_v1",false))
        }
        fun closeBetaIntro() {
            helpPreferences.edit().putBoolean("beta_channel_intro_v1",true).apply()
            showBetaIntro=false
        }
        // Lab: several people saved and none marked as the person's own: Carga Express asks once which data are theirs.
        var askOwnData by remember { mutableStateOf(false) }
        // Lab 18: «¿De quién es la tarjeta?» for the amount chosen, the «Datos para Prex» form, and one person's detail.
        var whoPaysFor by remember { mutableStateOf<Long?>(null) }
        var labPayerForm by remember { mutableStateOf<LabPayerForm?>(null) }
        var labPayerDetail by remember { mutableStateOf<String?>(null) }
        fun ownMissing() = engine.payerProfiles.none { it.personal }
        // Lab 21: a payment left without Prex's answer is asked about once, only when paying again.
        var unansweredAsk by remember { mutableStateOf<Pair<PaymentRecord, () -> Unit>?>(null) }
        var unansweredOk by remember { mutableLongStateOf(0L) }
        fun afterUnanswered(go: () -> Unit) {
            val r = engine.unansweredPayment
            if (r == null || unansweredOk == r.startedAt) go() else unansweredAsk = r to go
        }
        /** Lab: after the amount, the person whose card it is (asked only with two or more), then Prex. */
        fun labPay(amount: Long): Unit = afterUnanswered {
            val people = engine.payerProfiles.filter { it.valid() }
            when {
                people.isEmpty() -> labPayerForm = LabPayerForm(null, askMine = true, startMine = true, action = "Continuar") { engine.prepareWithPrex(amount, engine.state.payerProfileId) }
                people.size == 1 -> engine.prepareWithPrex(amount, people[0].id)
                else -> whoPaysFor = amount
            }
        }
        fun requestExpress() {
            if(!engine.expressAvailable || engine.state.busy)return
            if(IsLab && engine.unansweredPayment.let { it!=null && unansweredOk!=it.startedAt }) { afterUnanswered(::requestExpress); return }
            if(IsLab) {
                val why=engine.startExpressOrWhy()
                if(why=="elegir tus datos") { askOwnData=true; return }
                if(why!=null) { engine.expressNotStarted="Carga Express no empezó: $why"; if(why!="sin internet") engine.notice(expressWhyCopy(why)) }
                return
            }
            engine.startExpress()
        }
        if (state.stage == "embeddedPrex") {
            if(IsLab) BoleteraTheme("dark") { LabEmbeddedPrexScreen(engine.prexPayment, state.activePayment, state.selectedCard, engine::leavePrexPayment,
                showPayer = engine.payerProfiles.size > 1) }
            else EmbeddedPrexScreen(engine.prexPayment, state.activePayment, engine::leavePrexPayment)
            return
        }
        if(state.stage=="activity") {
            BackHandler {engine.closeActivity()}
            ActivityScreen(state.activity,engine::closeActivity,engine::openActivity)
            return
        }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(updates.automaticNotice) {
            if(updates.automaticNotice) {
                if(snackbar.showSnackbar("Hay una nueva versión de Boletera.",actionLabel="Ver",withDismissAction=true,duration=SnackbarDuration.Long)==SnackbarResult.ActionPerformed)showSettings=true
                updates.dismissAutomaticNotice()
            }
        }
        LaunchedEffect(state.message) {
            if(IsLab && vaultAttempt && state.message.startsWith("Documento o contraseña incorrectos")) {
                vaultAttempt=false; vault.forget(); engine.savedAccess(false)
                LabLog.add("entrada", "contraseña guardada rechazada por STM: se olvida el acceso con huella")
            }
        }
        // Lab: sign-in problems stay written on the entry (with the card in red) until the next attempt, instead of a passing snackbar.
        var labEntryError by remember { mutableStateOf("") }
        LaunchedEffect(state.message, state.stage) {
            if (IsLab && state.stage == "welcome" && state.message.isNotBlank()) {
                labEntryError = state.message; engine.dismissNotice(state.message)
            } else if (state.message.isNotBlank() && state.stage != "blocked") {
                snackbar.showSnackbar(state.message, withDismissAction = true, duration = SnackbarDuration.Short)
                engine.dismissNotice(state.message)
            }
        }
        var manual by remember { mutableStateOf(false) }
        var document by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var revealPassword by remember { mutableStateOf(false) }
        // Saving access with the fingerprint is the practical default whenever the phone supports it.
        var save by remember { mutableStateOf(vault.available) }
        var showForget by remember { mutableStateOf(false) }
        var showAmount by remember { mutableStateOf(false) }
        var showPayerPicker by remember { mutableStateOf(false) }
        var showCardsSheet by remember { mutableStateOf(false) }
        var namingCard by remember { mutableStateOf<String?>(null) }
        // «Probar de nuevo» after a rejection: the amount panel opens again once the balance is back.
        LaunchedEffect(LabRetry.reopenAmount, state.stage, state.busy, state.minimum) {
            if(IsLab && LabRetry.reopenAmount && state.stage=="balance" && !state.busy && state.minimum!=null) { LabRetry.reopenAmount=false; showAmount=true }
        }
        val lastBalances = remember { LastBalanceStore(context) }
        fun forgetPhone() {
            if (engine.forgetChoices()) {
                // Forgetting the phone is not a new install: the news already read stay read.
                val seen=helpPreferences.getString("whats_new_seen",null)
                helpPreferences.edit().clear().putString("whats_new_seen",seen).apply();vault.forget(); lastBalances.clear()
                // Like a first open: the notice «Antes de entrar» comes back right away (Lab 21).
                if(IsLab) firstAccessIntro=true
                for (name in listOf("lab_card_names","lab_hints")) context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE).edit().clear().apply(); engine.savedAccess(false); engine.logout()
                // Lab says it with the clean entry itself; a notice there would paint the entry red as if signing in failed.
                if(!IsLab) engine.notice("Acceso guardado, perfiles, preferencias y sesión local eliminados.")
            }
        }
        // Every balance STM reports becomes the «último saldo» shown while connecting next time.
        LaunchedEffect(state.balance, state.consultedAt, state.selectedCard) {
            val card=state.selectedCard; val balance=state.balance; val at=state.consultedAt
            if(IsLab && card!=null && balance!=null && at!=null) lastBalances.save(card, LastBalance(balance, at))
        }
        var showPayerEditor by remember { mutableStateOf(false) }
        var editingPayer by remember { mutableStateOf<PayerProfile?>(null) }
        LaunchedEffect(rechargeShortcut, state.stage, state.minimum, state.busy) {
            if(rechargeShortcut && state.stage=="balance" && state.minimum!=null && !state.busy) { showAmount=true; rechargeShortcut=false }
        }
        // Lab: once per version, after the first balance, a short list of what changed so the owner knows what to try.
        var showWhatsNew by remember { mutableStateOf(false) }
        LaunchedEffect(state.stage) {
            if(IsLab && state.stage=="balance" && helpPreferences.getString("whats_new_seen",null)!=BuildConfig.VERSION_NAME) {
                // B12: a fresh install has nothing «new»; only an update shows what changed.
                val info=packageManager.getPackageInfo(packageName,0)
                helpPreferences.edit().putString("whats_new_seen",BuildConfig.VERSION_NAME).apply(); showWhatsNew=info.firstInstallTime!=info.lastUpdateTime
            }
        }
        LaunchedEffect(state.sessionExpired, state.recoveringSession) {
            if(state.sessionExpired || state.recoveringSession) {
                showAmount=false; showPayerPicker=false; showPayerEditor=false
                whoPaysFor=null; labPayerForm=null; labPayerDetail=null; askOwnData=false; unansweredAsk=null
                if(IsLab) {
                    showCardsSheet=false; namingCard=null; showTicketGuide=false
                    if(state.sessionExpired) { showWhatsNew=false; LabRetry.reopenAmount=false }
                }
                showSettings=false; document=""; password=""; revealPassword=false; manual=false
            }
        }
        // Lab: the sign-in card stays on screen while STM connects, celebrates the entry, or flags a failed attempt.
        var labLoggingIn by remember { mutableStateOf(false) }
        var labConnected by remember { mutableStateOf(false) }
        var labHeroResult by remember { mutableStateOf(HeroPhase.IDLE) }
        LaunchedEffect(authenticating) { if(IsLab && authenticating) labLoggingIn=true }
        LaunchedEffect(state.stage, state.busy, authenticating) {
            if(!IsLab || !labLoggingIn) return@LaunchedEffect
            if(state.stage=="connecting") labConnected=true
            when {
                state.stage=="balance" || state.stage=="cards" -> { labLoggingIn=false; labConnected=false; labHeroResult=HeroPhase.SUCCESS }
                state.stage=="blocked" || state.stage=="accessHelp" -> { labLoggingIn=false; labConnected=false }
                state.stage=="welcome" && !authenticating && !state.busy -> {
                    if(labConnected) { labHeroResult=HeroPhase.ERROR; if(labEntryError.isBlank() || labEntryError.startsWith("No se guardó"))labEntryError="No pudimos entrar. Probá de nuevo." }
                    labLoggingIn=false; labConnected=false }
            }
        }
        LaunchedEffect(labHeroResult) {
            if(labHeroResult==HeroPhase.SUCCESS) { kotlinx.coroutines.delay(800); labHeroResult=HeroPhase.IDLE }
        }
        // A new attempt clears the previous problem.
        LaunchedEffect(authenticating, labLoggingIn) {
            if(authenticating || labLoggingIn) { labEntryError=""; if(labHeroResult==HeroPhase.ERROR)labHeroResult=HeroPhase.IDLE }
        }
        val labHeroPhase = when { labHeroResult!=HeroPhase.IDLE -> labHeroResult; authenticating || labLoggingIn -> HeroPhase.LOADING; else -> HeroPhase.IDLE }
        val labHold = IsLab && ((labLoggingIn && state.stage=="connecting") || labHeroResult==HeroPhase.SUCCESS)
        val labOpening = remember { (IsLab && !LabLaunch.openingPlayed).also { if(it) LabLaunch.openingUntil = android.os.SystemClock.uptimeMillis()+650 } }
        SideEffect { if(IsLab) LabLaunch.openingPlayed = true }
        val labVeil = remember { androidx.compose.animation.core.Animatable(if(labOpening) 1f else 0f) }
        LaunchedEffect(Unit) { if(labOpening) { labVeil.animateTo(0f, androidx.compose.animation.core.tween(350)) } }
        val stage = if(labHold) "welcome" else when {
            state.stage=="connecting" && state.selectedCard!=null && state.balance!=null && state.amount==null -> "balance"
            // Never flash the card list while the engine passes through it on the way back to the balance.
            state.stage=="cards" && !engine.cardPickerWanted -> if(state.balance!=null && state.amount==null) "balance" else "connecting"
            else -> state.stage
        }
        val scroll = rememberScrollState()
        val pullState = rememberPullToRefreshState()
        // The spinner belongs to a pull the person made; reading the minimum after the balance is already on screen stays quiet.
        var pulling by remember { mutableStateOf(false) }
        LaunchedEffect(state.busy) { if(!state.busy) pulling=false }
        LaunchedEffect(stage) { scroll.scrollTo(0) }
        val activity: android.app.Activity? = this@MainActivity
        fun back() {
            document=""; password=""
            if(engine.returnFromCardPicker())return
            // A switch of boletera has nothing to go back to: Back waits for it instead of signing out.
            if(IsLab && engine.switchingTo!=null && state.stage=="connecting" && !state.recoveringSession) return
            // Leaving the home is leaving the app, like any app: the session stays, the next open is straight in.
            if(IsLab && stage=="balance") { activity?.moveTaskToBack(true); return }
            // After an error with a live session, back is the way to the balance, not a sign-out.
            if(IsLab && state.stage=="blocked" && state.activePayment==null && engine.sessionVerified) { engine.refresh(); return }
            // «Cancelar» while Prex opens goes back to the balance; it never signs out (A2).
            if(IsLab && engine.sessionVerified && (state.stage=="openingPayment" || state.stage=="connecting" && state.amount!=null)) { engine.cancelPreparing(); return }
            // After a payment problem, back re-reads the balance like the on-screen button instead of signing out.
            if(state.stage in listOf("paymentBoundary", "externalPayment") || (state.stage=="blocked" && state.activePayment!=null)) engine.refresh() else engine.cancel()
        }
        BackHandler(enabled = state.stage != "welcome") { back() }
        BackHandler(enabled = IsLab && showAmount) { showAmount=false }
        BackHandler(enabled = IsLab && manual && state.stage=="welcome") { manual=false }
        // Boletera Lab draws its own home over a blurred, wallpaper-like light; every other stage keeps the shared layout.
        val blockedScreen: @Composable () -> Unit = {
            AppErrorScreen(state.message,state.diagnostic,payment=state.activePayment!=null,onOpenSite=::openStmAccess,
                // Retry really retries: with a live session it reads again with it (no fingerprint), otherwise the saved
                // fingerprint access signs in again, or back to the sign-in.
                onRetry=if(state.activePayment!=null)null else ({ if(IsLab && engine.sessionVerified)engine.refresh() else { engine.cancel(); if(vault.exists)unlockSavedAccess() } }),
                paymentAmount=state.activePayment?.amount,
                // Lab: this screen comes before Prex opened (STM's own steps), so nothing was charged.
                charged=!IsLab || engine.paymentStarted(state.activePayment),
                onExit={
                    if(state.activePayment!=null || (IsLab && engine.sessionVerified))engine.refresh() else {
                        val accessError=errorPresentation(state.message).code=="DATOS DE ACCESO"
                        engine.cancel()
                        if(accessError)manual=true
                    }
                })
        }
        val labHome=IsLab && stage=="balance"
        // Lab screens share the home's dark light; the payment and error screens keep the shared layout for now.
        val labScreen=IsLab && stage in setOf("balance","welcome","cards","connecting","openingPayment","accessHelp","blocked","paymentBoundary","externalPayment","captcha")
        val haze=remember { HazeState() }
        val onExpressCharge=if(updates.installedChannel!=UpdateChannel.STABLE && engine.expressAvailable)::requestExpress else null
        Surface(color=if(labScreen)LabColors.Base else Paper,modifier=Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize()) {
           Box(Modifier.fillMaxSize().then(if(labHome)Modifier.hazeSource(haze) else Modifier)) {
            if(labScreen)LabAmbient()
            // App opening: dark like the system splash, dissolving while the card leaves the launcher icon.
            if(labScreen && labVeil.value>0f)Box(Modifier.fillMaxSize().background(LabColors.Base.copy(alpha=labVeil.value)))
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding(),horizontalAlignment=Alignment.CenterHorizontally) {
                if(labHome)Spacer(Modifier.height(64.dp))
                else if(labScreen)LabTopBar(title=when(stage){ "paymentBoundary"->"Tu recarga"; "captcha"->"Verificación"; "externalPayment"->"Pago en eBROU"; else->null },
                    // An error is a moment, not a place for settings: only the way back.
                    onBack=if(stage in setOf("blocked","paymentBoundary","captcha","externalPayment","accessHelp"))::back else null,
                    // A moment or a wait is not a place for settings (reglas.html).
                    onSettings=if(stage in setOf("blocked","connecting","accessHelp","openingPayment"))null else ({showSettings=true}))
                else AppTopBar(onSettings={showSettings=true},onBack=if(stage in listOf("paymentBoundary","cards","captcha","blocked","externalPayment"))::back else null,
                    title=when(stage){ "paymentBoundary"->"Tu recarga"; "cards"->"Boleteras"; "captcha"->"Verificación"; "externalPayment"->"Pago en eBROU"; else->null })
                Box(Modifier.weight(1f).widthIn(max=600.dp).fillMaxWidth().pullToRefresh(
                    isRefreshing=stage=="balance"&&state.busy&&(pulling||!IsLab),state=pullState,enabled=stage=="balance"&&!state.busy,
                    onRefresh={if(engine.state.stage=="balance"&&!engine.state.busy){pulling=true;engine.refresh()}}
                )) {
                    if(IsLab && stage=="blocked") Box(Modifier.fillMaxSize().padding(horizontal=24.dp)) { blockedScreen() }
                    else if(IsLab && stage=="connecting" && state.amount==null) Box(Modifier.fillMaxSize().padding(horizontal=24.dp)) {
                        val switching=engine.switchingTo
                        when {
                            state.recoveringSession -> LabConnecting("Volviendo a entrar","Entramos de nuevo con tu acceso guardado.",::back)
                            // A switch takes a moment and the previous card is already left: nothing to cancel back to.
                            switching!=null -> LabConnecting("Cambiando de boletera","Consultando el saldo de STM ···· ${switching.takeLast(4)}.",null)
                            else -> LabConnecting("Conectando con STM","Tu saldo aparece en un momento.",::back,last=lastBalances.read(state.selectedCard))
                        }
                    }
                    // B9: preparing the recharge and opening Prex are one wait, with the amount and only «Cancelar».
                    else if(IsLab && (stage=="openingPayment" || stage=="connecting" && state.amount!=null)) Box(Modifier.fillMaxSize().padding(horizontal=24.dp)) {
                        LabPaymentWait(state.amount, state.selectedCard, engine.expressPreparing, onCancel=::back)
                    }
                    else if(IsLab && stage=="accessHelp") Box(Modifier.fillMaxSize().padding(horizontal=24.dp)) {
                        LabAccessHelp(onOpenSite=::openStmAccess,onDone={ engine.cancel(); if(vault.exists)unlockSavedAccess() })
                    }
                    else if(IsLab && stage=="welcome") Box(Modifier.fillMaxSize().padding(horizontal=24.dp)) {
                        LabWelcome(state,savedAccess=state.hasSavedAccess,authenticating=authenticating,onUnlock={unlockSavedAccess()},onManual={manual=true},
                            onAccessGuide={showAccessGuide=true},heroPhase=labHeroPhase,opening=labOpening,errorText=labEntryError,
                            onOpenSite=if(labHeroResult==HeroPhase.ERROR && labEntryError!=StmEngine.OFFLINE)::openStmAccess else null,
                            onReport=if(labEntryError.isNotBlank() && labEntryError !in NOT_A_FAILURE)({LabReport.send(context,"No pude entrar · $labEntryError")}) else null)
                    } else Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal=24.dp).padding(top=if(stage=="balance")4.dp else 12.dp,bottom=if(stage in listOf("balance","blocked"))4.dp else 32.dp)
                        .pageEntrance(stage, state.captcha==null),verticalArrangement=Arrangement.spacedBy(if(stage in listOf("balance","blocked"))0.dp else 24.dp)) {
                        when(stage) {
                            "welcome" -> if(IsLab) Unit
                            else {
                                if(state.sessionExpired) {
                                    Title("Tu sesión venció")
                                    Text(if(state.hasSavedAccess)"STM cerró el acceso después de un tiempo. Confirmá tu huella para volver a entrar." else "STM cerró el acceso después de un tiempo. Ingresá de nuevo para continuar.",color=Muted)
                                    if(state.paymentNeedsReview)Text("Si ya autorizaste un pago, revisaremos el saldo al entrar. La app no repetirá la recarga.",color=Muted,style=MaterialTheme.typography.bodyMedium)
                                } else WelcomeHero()
                                WhiteCard {
                                    Text(if(state.sessionExpired)"Recuperá el acceso" else if(state.hasSavedAccess&&!manual)"Qué bueno verte de nuevo" else "Entrá a tu STM",style=MaterialTheme.typography.titleLarge)
                                    if(state.hasSavedAccess&&!manual) {
                                        Text("Tu acceso está protegido en este teléfono.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                                        Box(Modifier.fillMaxWidth().padding(vertical=8.dp),contentAlignment=Alignment.Center) { AppGlyph(Glyph.Fingerprint,Modifier.size(56.dp),tint=MaterialTheme.colorScheme.primary) }
                                        Primary(if(authenticating)"Esperando tu huella…" else "Entrar con huella",enabled=!authenticating&&!state.busy) {
                                            unlockSavedAccess()
                                        }
                                        TextButton(onClick={manual=true},modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("Ingresar manualmente") }
                                    } else {
                                        Text("Usá tu Usuario gub.uy.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                                        OutlinedTextField(document,{document=it.filter(Char::isDigit).take(8)},label={Text("Documento uruguayo")},singleLine=true,
                                            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp))
                                        OutlinedTextField(password,{password=it},label={Text("Contraseña de gub.uy")},singleLine=true,
                                            visualTransformation=if(revealPassword)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
                                            modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),trailingIcon={IconButton(onClick={revealPassword=!revealPassword}) { AppGlyph(if(revealPassword)Glyph.EyeOff else Glyph.Eye,label=if(revealPassword)"Ocultar contraseña" else "Mostrar contraseña",tint=Muted) }})
                                        if(vault.available) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                            Text("Guardar acceso con huella",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
                                            Switch(checked=save,onCheckedChange={save=it})
                                        }
                                        Primary(if(authenticating)"Esperando tu huella…" else if(save)"Guardar e ingresar" else "Ingresar",enabled=document.length==8&&password.isNotBlank()&&!authenticating&&!state.busy) {
                                            val doc=document; val pass=password; password="";revealPassword=false
                                            if(save) {
                                                authenticating=true
                                                vault.save(doc,pass) { ok -> authenticating=false;engine.savedAccess(vault.exists)
                                                    if(!ok)engine.notice("No se guardó el acceso con huella. Entrás igual; podés guardarlo la próxima vez.")
                                                    engine.connect(doc,pass,rememberForSession=ok) }
                                            } else engine.connect(doc,pass)
                                        }
                                    }
                                }
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                    AppGlyph(Glyph.Lock,Modifier.size(16.dp),tint=Muted)
                                    Text("Proyecto independiente y no oficial de STM",style=MaterialTheme.typography.bodySmall,color=Muted)
                                }
                                TextButton(onClick={showAccessGuide=true}) {Text("¿Es tu primer ingreso a STM?")}
                            }
                            "balance" -> if(IsLab) LabHomeContent(state,offline=remember(state.busy,state.message,state.consultedAt){engine.noInternet()},
                                onTicketGuide={showTicketGuide=true})
                            else WalletHome(state,engine::changeCard,{showAmount=true},engine::refresh,
                                onExpressCharge=onExpressCharge,
                                onTicketGuide={showTicketGuide=true},expressPreparing=engine.expressPreparing,expressProvider=engine.expressPaymentLabel,
                                onPaymentReviewed={showPaymentReview=true},onActivity=if(BuildConfig.DEBUG || updates.installedChannel in setOf(UpdateChannel.ALPHA,UpdateChannel.LAB))engine::openActivity else null)
                            "connecting" -> {
                                LoadingState(if(state.recoveringSession)"Volviendo a entrar" else if(state.amount!=null)"Preparando tu recarga" else "Conectando con STM", if(state.recoveringSession)"Tu sesión de STM venció. Estamos renovándola sin pedirte los datos otra vez." else if(engine.expressPreparing)"Estamos abriendo Prex y completando los datos conocidos." else "Estamos consultando STM. Tu información va a aparecer acá.",express=engine.expressPreparing)
                                TextButton(onClick=::back) { Text("Cancelar") }
                            }
                            "accessHelp" -> {
                                Title("Completá el acceso\nen la web de STM")
                                Text("Después de la contraseña apareció un paso que Boletera todavía no reconoce.",color=Muted)
                                StmAccessInstructions()
                                Primary("Abrir STM en el navegador",action=::openStmAccess)
                                TextButton(onClick=engine::cancel,modifier=Modifier.align(Alignment.CenterHorizontally)) {Text("Volver a ingresar")}
                            }
                            "openingPayment" -> LoadingState("Un momento…","Abriendo ${if(state.selectedProvider=="1033")"Prex" else "eBROU"} para tu recarga de ${Amounts.format(state.amount)}.",express=engine.expressPreparing)
                            "cards" -> if(IsLab) LabCards(state,engine::chooseCard) else {
                                Title("Elegí tu boletera")
                                Text("Recordamos tu elección para la próxima.",color=Muted)
                                state.cards.forEach { card -> WhiteCard {
                                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                        AppGlyph(Glyph.Ticket,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                                        Column { Text("STM · ${card.id.takeLast(4)}",style=MaterialTheme.typography.titleLarge);Text(card.status,style=MaterialTheme.typography.bodyMedium,color=Muted) }
                                    }
                                    if(card.active)Primary("Usar esta boletera",!state.busy){engine.chooseCard(card.id)}
                                    else Text("Esta boletera no está habilitada.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                                } }
                                if(state.cards.isEmpty())Notice("Todavía no pudimos leer las boleteras.")
                            }
                            "captcha" -> { Title("Una verificación\ny seguimos.");Text("Completá el desafío de abajo para continuar.",color=Muted) }
                            "paymentBoundary" -> PaymentSetupContent(state,engine.payerProfiles,engine::chooseProvider,
                                onAddPayer={editingPayer=null;showPayerEditor=true},
                                onChoosePayer={showPayerPicker=true},
                                onEditPayer={editingPayer=it;showPayerEditor=true})
                            "externalPayment" -> {
                                Title("Pago abierto en eBROU")
                                Text("Al volver del banco se actualizará el saldo.",color=Muted)
                                Primary("Volver al saldo",action=engine::refresh)
                            }
                            "blocked" -> if(!IsLab) blockedScreen()
                        }
                        // One stable host: transitions never duplicate or recreate the authenticated browser.
                        val cap=state.captcha
                        val screenHeight=LocalConfiguration.current.screenHeightDp
                        BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                            val captchaScale=if(cap==null)1f else minOf(1f,maxWidth.value/cap.width,(screenHeight-260).coerceAtLeast(180).toFloat()/cap.height)
                            AndroidView(factory={engine.host},modifier=if(cap==null)Modifier.size(1.dp) else Modifier.width((cap.width*captchaScale).dp).height((cap.height*captchaScale).dp).clip(RoundedCornerShape(8.dp)),update={it.crop=cap})
                        }
                        if(cap!=null)Primary("Ya completé la verificación"){engine.continueCaptcha()}
                    }
                    SnackbarHost(hostState=snackbar,modifier=Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom=if(labHome)140.dp else 0.dp))
                    if(stage=="balance")PullToRefreshDefaults.Indicator(state=pullState,isRefreshing=state.busy,modifier=Modifier.align(Alignment.TopCenter),containerColor=Highlight,color=MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if(stage=="paymentBoundary")Box(Modifier.widthIn(max=600.dp).fillMaxWidth()) {
                    PaymentSetupFooter(state,engine.payerProfiles,
                        onAddPayer={editingPayer=null;showPayerEditor=true},
                        onChoosePayer={showPayerPicker=true},onContinue=engine::beginPayment)
                }
            }
           }
           // Lab: the cards open as a sheet over the balance, from the list already read (no STM round trip to show them).
           if(labHome)LabHomeChrome(state,haze,blur=VisualEffects.enabled(context,effectsMode),onChangeCard={showCardsSheet=true},onSettings={showSettings=true},
               onCharge={ if(IsLab && engine.noInternet()) engine.notice(StmEngine.OFFLINE) else showAmount=true },onExpressCharge=onExpressCharge,expressPreparing=engine.expressPreparing,expressProvider=engine.expressPaymentLabel)
           // Lab sign-in: document and password rise in a panel over the entry; sending closes it so the card shows the progress.
           if(IsLab && stage=="welcome")LabSignInSheet(open=manual,document=document,
               onDocument={document=it.filter(Char::isDigit).take(8);labEntryError="";if(labHeroResult==HeroPhase.ERROR)labHeroResult=HeroPhase.IDLE},
               password=password,onPassword={password=it;labEntryError="";if(labHeroResult==HeroPhase.ERROR)labHeroResult=HeroPhase.IDLE},
               reveal=revealPassword,onReveal={revealPassword=!revealPassword},save=save,onSave={save=it},canSave=vault.available,
               enabled=!authenticating&&!state.busy,onClose={manual=false},onSubmit={
                   manual=false
                   // Without internet nothing starts: the entry says it instead of «Entrando…» forever (Lab 21).
                   if(engine.noInternet()) { password="";revealPassword=false;engine.notice(StmEngine.OFFLINE) } else {
                   labLoggingIn=true
                   // The document stays so a mistyped password only needs the password again.
                   val doc=document; val pass=password; password="";revealPassword=false
                   fun start(remember: Boolean) { vaultAttempt=remember; engine.connect(doc,pass,rememberForSession=remember); if(engine.state.stage!="connecting")labLoggingIn=false }
                   if(save) {
                       authenticating=true
                       vault.save(doc,pass) { ok -> authenticating=false;engine.savedAccess(vault.exists)
                           if(!ok)engine.notice("No se guardó el acceso con huella. Entrás igual; podés guardarlo la próxima vez.")
                           start(ok) }
                   } else start(false)
                   }
               })
           // Lab: the moment a recharge is credited, over the home, until "Listo".
           val credited=state.credited
           if(labHome && credited!=null) BackHandler { engine.dismissCredited() }
           if(labHome && credited!=null) Box(Modifier.fillMaxSize().background(LabColors.Base)
               .clickable(interactionSource=remember{androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null){}) {
               LabAmbient()
               Box(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal=24.dp).padding(top=64.dp)) { LabRechargeDone(state,credited,engine.lastReceipt,engine::dismissCredited) }
           }
           // B7: the panel carries the payer; «Continuar con Prex» goes straight to Prex (no «Tu recarga» step).
           if(labHome)LabAmountPanel(state,open=showAmount,onClose={showAmount=false},expressShown=onExpressCharge!=null,
               onChoose={amount->showAmount=false;labPay(amount)})
          }
        }
        if(showAmount && !IsLab)AmountSheet(state,{showAmount=false}){amount->showAmount=false;engine.prepare(amount)}
        if(showCardsSheet && IsLab)LabCardsSheet(state,onChoose={id->showCardsSheet=false;engine.switchCard(id)},onEdit={showCardsSheet=false;namingCard=it},onClose={showCardsSheet=false})
        namingCard?.let { id -> LabCardNameSheet(id) { namingCard=null } }
        if(showTicketGuide){ if(IsLab)LabTicketGuideSheet{showTicketGuide=false} else TicketGuideSheet{showTicketGuide=false} }
        if(showWhatsNew && IsLab)LabNewsSheet(BuildConfig.DISPLAY_VERSION.substringAfter(' '),LabWhatsNewItems,installed=true,onPrimary={showWhatsNew=false},onLater={showWhatsNew=false})
        if(showWhatsNew && !IsLab)AlertDialog(onDismissRequest={showWhatsNew=false},title={Text("Qué hay de nuevo en ${BuildConfig.DISPLAY_VERSION}")},
            text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) { LabWhatsNew.forEach { Text("· $it",style=MaterialTheme.typography.bodyMedium) } }},
            confirmButton={TextButton(onClick={showWhatsNew=false}){Text("Listo")}})
        // Lab: «Antes de entrar» is one sheet for the first open and for the link; the button goes straight to the document.
        if(IsLab && state.stage=="welcome" && (showAccessGuide || firstAccessIntro && !state.hasSavedAccess))
            LabAccessGuideSheet(onReady={showAccessGuide=false;closeFirstAccessIntro(false);manual=true},onFirstTime=::openStmAccess,
                onClose={showAccessGuide=false;closeFirstAccessIntro(false)})
        if(showAccessGuide && !IsLab)AlertDialog(onDismissRequest={showAccessGuide=false},title={Text("Primer ingreso a STM")},
            text={Column(Modifier.heightIn(max=350.dp).verticalScroll(rememberScrollState())) {StmAccessInstructions()}},
            confirmButton={TextButton(onClick=::openStmAccess){Text("Abrir STM")}},
            dismissButton={TextButton(onClick={showAccessGuide=false}){Text("Cerrar")}})
        if(!IsLab && firstAccessIntro && state.stage=="welcome" && !state.hasSavedAccess && !showBetaIntro)AlertDialog(onDismissRequest={},
            title={Text("Antes de entrar")},
            text={Text("Boletera entra a STM con tu Usuario gub.uy, igual que la web Mi STM.\n\nSi nunca usaste Mi STM, la primera vez STM te pide autorizar el acceso en su web. Se hace una sola vez: abrís STM, entrás con tu usuario, aceptás y volvés acá.")},
            confirmButton={TextButton(onClick={closeFirstAccessIntro(false)}){Text("Ya usé Mi STM")}},
            dismissButton={TextButton(onClick={closeFirstAccessIntro(true)}){Text("Abrir STM primero")}})
        if(showPaymentReview)AlertDialog(onDismissRequest={showPaymentReview=false},title={Text("¿Ya comprobaste el resultado?")},
            text={Text("Confirmá solo después de revisar el pago en Prex o en tu banco. Esto habilita otra recarga; no cancela ni devuelve el pago anterior.")},
            confirmButton={TextButton(onClick={if(engine.acknowledgePaymentReviewed())showPaymentReview=false}) {Text("Sí, ya lo revisé")}},
            dismissButton={TextButton(onClick={showPaymentReview=false}) {Text("Volver")}})
        if(showBetaIntro)AlertDialog(onDismissRequest=::closeBetaIntro,
            title={Text("¿Querés probar Boletera Beta?")},
            text={Text("Incluye funciones experimentales, como Carga Express, antes de llegar a la versión estable. Podés probarla desde Configuración y volver a la versión estable cuando quieras.")},
            confirmButton={TextButton(onClick={closeBetaIntro();updates.selectChannel(UpdateChannel.BETA);showSettings=true}){Text("Ver Boletera Beta")}},
            dismissButton={TextButton(onClick=::closeBetaIntro){Text("Ahora no")}})
        if(showSettings && IsLab)LabSettingsScreen(updates,state.hasSavedAccess,canLogout=state.stage!="welcome",engine.helpDiagnostic,effectsMode,
            onEffectsMode={effectsMode=it;VisualEffects.setMode(context,it)},onLogout={showSettings=false;lastBalances.clear();engine.logout()},onForget={showSettings=false;forgetPhone()},
            onTicketGuide={showSettings=false;showTicketGuide=true},onInstall=updates::requestReview,onClose={showSettings=false},
            onDemo={showSettings=false;LabDemo.set(context,true)},
            payers=if(state.stage!="welcome")engine.payerProfiles else null,onPayer={labPayerDetail=it.id},
            onAddPayer={labPayerForm=LabPayerForm(null,askMine=ownMissing(),startMine=ownMissing(),action="Guardar")})
        if(IsLab && showSettings) labPayerDetail?.let { id -> engine.payerProfiles.find { it.id==id }?.let { p ->
            LabPayerDetail(p,engine.payerProfiles,onEdit={labPayerForm=LabPayerForm(p,askMine=false,startMine=p.personal,action="Guardar")},
                onMine={mine->if(!engine.savePayer(p.copy(personal=mine)))engine.notice("No pudimos guardar el cambio. Probá de nuevo.")},onDelete={engine.deletePayer(p.id)},onClose={labPayerDetail=null})
        } ?: run { labPayerDetail=null } }
        if(IsLab && whoPaysFor!=null) whoPaysFor?.let { amount ->
            LabWhoPaysSheet(engine.payerProfiles.filter{it.valid()},onChoose={p->whoPaysFor=null;engine.prepareWithPrex(amount,p.id)},
                onAdd={whoPaysFor=null;labPayerForm=LabPayerForm(null,askMine=ownMissing(),startMine=ownMissing(),action="Continuar"){engine.prepareWithPrex(amount,engine.state.payerProfileId)}},
                onClose={whoPaysFor=null})
        }
        if(IsLab && askOwnData) LabOwnDataSheet(engine.payerProfiles.filter{it.valid()},
            onMine={p->askOwnData=false;if(engine.savePayer(p.copy(personal=true)))requestExpress()},
            onNone={askOwnData=false;labPayerForm=LabPayerForm(null,askMine=false,startMine=true,action="Continuar"){requestExpress()}},
            onClose={askOwnData=false})
        unansweredAsk?.let { (record, go) ->
            LabUnansweredSheet(record.amount, record.startedAt, onAgain={ unansweredAsk=null; unansweredOk=record.startedAt; go() }, onWait={ unansweredAsk=null })
        }
        labPayerForm?.let { form ->
            LabPayerScreen(form.existing,form.askMine,form.action,startMine=form.startMine,
                onSave={p->engine.savePayer(p).also{ok->if(ok){labPayerForm=null;form.then?.invoke()}}},onClose={labPayerForm=null})
        }
        if(showSettings && !IsLab)SettingsSheet(updates,appearance,onAppearance,state.hasSavedAccess,state.stage!="welcome",engine.helpDiagnostic,
            onForget={showSettings=false;showForget=true},onLogout={showSettings=false;engine.logout()},onInstall=updates::requestReview,onClose={showSettings=false},
            onTicketGuide={showSettings=false;showTicketGuide=true},
            effectsMode=if(IsLab)effectsMode else null,onEffectsMode={effectsMode=it;VisualEffects.setMode(context,it)})

        // Lab: a version found on opening, offered on the entry or the home (never in the middle of a payment).
        val promptRelease=updates.release
        if(IsLab && updates.prompt && promptRelease!=null && !updates.reviewRequested && !showSettings && state.stage in setOf("welcome","balance") &&
            !showAmount && state.credited==null && !manual && !showAccessGuide && labPayerForm==null && whoPaysFor==null && !askOwnData && unansweredAsk==null &&
            // Never over the fingerprint or a sign-in in course, nor over the first-time notice (Lab 21).
            !authenticating && !labLoggingIn && !(state.stage=="welcome" && firstAccessIntro && !state.hasSavedAccess))
            LabUpdatePrompt(promptRelease,updates.progress,updates.busy,onUpdate=updates::acceptPrompt,onLater=updates::skipPrompt,
                problem=if(updates.downloadFailed && !updates.busy)updates.message else null)
        if(updates.reviewRequested && updates.ready && !updates.busy && IsLab) updates.release.let { r ->
            LabNewsSheet(r?.let { UpdatePolicy.display(it.version).substringAfter(' ') } ?: "La versión nueva",labNewsFrom(r?.notes.orEmpty()),installed=false,
                onPrimary=updates::acceptReview,onLater=updates::dismissReview) }
        else if(updates.reviewRequested && updates.ready && !updates.busy) UpdateReviewDialog(updates.release,updates::acceptReview,updates::dismissReview)

        if (showPayerPicker) PayerPicker(engine.payerProfiles,state.payerProfileId,
            onChoose={engine.choosePayer(it);showPayerPicker=false},
            onAdd={editingPayer=null;showPayerPicker=false;showPayerEditor=true},
            onClose={showPayerPicker=false})
        if (showPayerEditor) PayerProfileEditor(editingPayer, onSave = engine::savePayer, onDelete = engine::deletePayer, onClose = {
            showPayerEditor = false
        })
        if (showForget) AlertDialog(onDismissRequest = { showForget = false }, title = { Text("¿Olvidar este acceso?") },
            text = { Text("Se eliminan las credenciales cifradas, las preferencias y la sesión local.") },
            confirmButton = { TextButton(onClick = {
                forgetPhone()
                showForget = false
            }) { Text("Olvidar") } },
            dismissButton = { TextButton(onClick = { showForget = false }) { Text("Cancelar") } })
    }
}

@Composable internal fun StmAccessInstructions() {
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("La primera vez, STM puede pedirte autorización para vincular tu Usuario gub.uy. Esa autorización la decidís vos en su sitio.",color=Muted)
        Text("1. Abrí STM en el navegador e ingresá con el mismo Usuario gub.uy.",color=Ink)
        Text("2. Revisá y completá allí la autorización u otro paso que te pida el sitio.",color=Ink)
        Text("3. Volvé a Boletera e ingresá nuevamente.",color=Ink)
    }
}

@Composable internal fun UpdateReviewDialog(release: UpdateRelease?, onInstall: () -> Unit, onLater: () -> Unit) {
    AlertDialog(onDismissRequest=onLater,
        title={Text("Actualización lista")},
        text={Column(Modifier.heightIn(max=320.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            val history=release?.history.orEmpty().ifEmpty {listOf(UpdateNews(release?.version ?: "nueva",release?.notes.orEmpty()))}
            history.forEach { entry ->
                Text("Novedades de la versión ${entry.version}",fontWeight=FontWeight.SemiBold)
                if(entry.notes.isEmpty())Text("Esta versión no incluye un resumen de novedades.")
                else entry.notes.forEach { note -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {Text("•");Text(note)} }
            }
            Text("La descarga está lista. ¿Querés instalarla ahora?",color=Muted)
        }},
        confirmButton={TextButton(onClick=onInstall){Text("Instalar")}},
        dismissButton={TextButton(onClick=onLater){Text("Ahora no")}})
}

@Composable internal fun EmbeddedPrexScreen(payment: EmbeddedPrexPayment, pending: ActivePayment?, onClose: () -> Unit) {
    var showOriginal by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val expressAdvancing=payment.expressPhase=="advancing"
    val expressVerification=payment.expressPhase=="verification"
    LaunchedEffect(payment.nativeStage) { showOriginal = false }
    val completion = payment.nativeStage in listOf("finalConfirmation", "receipt", "paymentRejected", "paymentPending", "stmSuccess", "returnBalance", "sessionExpired") || payment.returningToWallet && payment.nativeStage=="loading"
    LaunchedEffect(payment.nativeStage) { if(payment.nativeStage in listOf("returnBalance", "sessionExpired")) onClose() }
    // Outside Lab, a provider check on the confirmation is still done on the original page.
    val native = (completion || payment.nativeStage in listOf("summary", "payer", "card", "loading")) && !showOriginal &&
        !(payment.nativeStage=="finalConfirmation" && payment.challenge!=null)
    LaunchedEffect(native, payment.nativeStage, payment.expandedChallenge, payment.challenge != null) {
        if (native && payment.nativeStage in listOf("payer","card")) payment.positionVerification() else payment.restoreVerification()
    }
    BackHandler(onBack = onClose)
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            AppTopBar(title="Pago con Prex",onBack=onClose)
            Row(Modifier.padding(horizontal=24.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                AppGlyph(Glyph.Lock,Modifier.size(16.dp),tint=Muted)
                Text("${if(payment.currentHost=="stm.gub.uy")"STM" else "Sistarbanc"} · ${Amounts.format(pending?.amount)}",color=Muted,style=MaterialTheme.typography.bodySmall)
            }
            if (payment.payerNotice.isNotBlank()) {
                Text(payment.payerNotice, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                if (payment.payerConflict) TextButton(onClick = payment::applyChosenPayer, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Usar el titular seleccionado") }
            }
            if (payment.busy && !payment.slowStep) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if(payment.slowStep && payment.message.isBlank()) {
                Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp)) {
                    Text("Este paso está demorando más de lo habitual. Podés revisar la misma solicitud en la página original. No se volvió a enviar.",color=Muted,style=MaterialTheme.typography.bodyMedium)
                    TextButton(onClick={if(payment.prepareOriginalReview())showOriginal=true}) {Text("Revisar esta solicitud")}
                    TextButton(onClick=onClose) {Text("Volver al saldo")}
                }
            }
            if (payment.message.isNotBlank()) {
                Column(Modifier.weight(1f).then(if(IsLab)Modifier else Modifier.verticalScroll(rememberScrollState())).padding(24.dp)) {
                    AppErrorScreen(payment.message,payment=true,paymentAmount=pending?.amount,onExit=onClose)
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val browserWidth = constraints.maxWidth
                    val browserHeight = constraints.maxHeight
                    val pixelsPerDp = LocalDensity.current.density
                    val cssPixelsToDp = if (payment.cssViewportWidth > 0f) browserWidth / payment.cssViewportWidth / pixelsPerDp else 1f
                    if (!native) Column(Modifier.fillMaxSize()) {
                        if(completion || payment.nativeStage in listOf("summary","payer","card"))TextButton(onClick={showOriginal=false},modifier=Modifier.align(Alignment.End)) {
                            Text(when(payment.nativeStage){"summary"->"Ver resumen";"card"->"Ver formulario";else->"Ver datos"})
                        }
                        PaymentBrowserView(payment,false,null,browserWidth,browserHeight,Modifier.weight(1f).fillMaxWidth())
                    }
                    if (native && completion) {
                        PaymentCompletionScreen(payment, pending, onOriginal={if(payment.prepareOriginalReview())showOriginal=true}) {
                            PaymentBrowserView(payment,true,null,browserWidth,browserHeight,Modifier.size(1.dp))
                        }
                    }
                    if (native && payment.nativeStage=="card") NativeCardForm(payment.cardBusy,payment.canContinue,payment.cardError,payment.expandedChallenge,payment::submitCard,{if(payment.prepareOriginalReview())showOriginal=true},focusCard=payment.expressPhase=="done",verificationRequired=payment.verificationRequired&&!payment.verificationApproved,onManualContinue=payment.manualContinueObserver,onVerificationShown=payment::cardVerificationShown,onAutomaticSubmit=payment::submitAutofilledCard) {
                        if(payment.expandedChallenge) ExpandedPaymentChallenge(payment,browserWidth,browserHeight,Modifier.fillMaxSize())
                        else BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                            val cap=payment.challenge
                            val maxPanelHeight=(browserHeight/pixelsPerDp-160f).coerceAtLeast(140f)
                            val scale=if(cap==null)1f else minOf(cssPixelsToDp,maxWidth.value/cap.width,maxPanelHeight/cap.height)
                            PaymentBrowserView(payment,cap==null,cap,browserWidth,browserHeight,if(cap==null)Modifier.size(1.dp) else Modifier.width((cap.width*scale).dp).height((cap.height*scale).dp))
                        }
                    }
                    if(native && !completion && payment.nativeStage!="card" && payment.expandedChallenge)Column(Modifier.fillMaxSize()) {
                        ExpandedPaymentChallenge(payment,browserWidth,browserHeight,Modifier.weight(1f).fillMaxWidth().padding(12.dp))
                        TextButton(onClick={if(payment.prepareOriginalReview())showOriginal=true},modifier=Modifier.fillMaxWidth()){Text("Ver página original")}
                    }
                    if (native && !completion && payment.nativeStage!="card" && !payment.expandedChallenge) Column(Modifier.fillMaxSize().background(Paper)) {
                      Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (payment.nativeStage != "payer" || expressAdvancing) PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.fillMaxWidth().height(1.dp))
                        if(expressAdvancing) LoadingState("Preparando Prex", "Usando el mínimo y tus datos guardados.",express=true) else {
                        ProgressSteps(if(payment.nativeStage=="payer")1 else 0,listOf("Recarga","Titular","Tarjeta"))
                        Text(if(expressVerification)"Una verificación\ny seguimos" else if (payment.nativeStage == "payer") "Datos del titular" else "Revisá tu recarga",style=MaterialTheme.typography.headlineMedium,color=Ink)
                        if (payment.nativeStage == "loading") {
                            if(!payment.slowStep)LoadingState("Un momento…","Estamos recuperando tu solicitud.")
                        } else if (payment.nativeStage == "summary") {
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(payment.summaryRows.singleOrNull { it.first.trim().equals("Total:", ignoreCase = true) }?.second ?: Amounts.format(pending?.amount),style=MaterialTheme.typography.displayMedium,color=Ink)
                                    payment.summaryRows.filterNot { it.first.trim().equals("Total:", ignoreCase = true) }.forEach { (label, value) ->
                                        Column { Text(label, color = Muted, fontSize = 12.sp); Text(value, color = Ink, fontSize = 16.sp) }
                                    }
                                }
                            }
                        } else {
                            if (!payment.expandedChallenge && !expressVerification) Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val values = payment.payerValues
                                    val rows = if (values.size == 5) listOf("Titular" to "${values[0]} ${values[1]}", "Documento" to values[2], "Correo" to values[3], "Celular" to values[4]) else emptyList()
                                    rows.forEach { (label, value) ->
                                        Column { Text(label, color = Muted, fontSize = 12.sp); Text(value.ifBlank { "Sin completar" }, color = Ink, fontSize = 16.sp) }
                                    }
                                }
                            }
                            Text(if(payment.challenge!=null)"Completá la verificación para continuar." else "Revisá los datos antes de seguir.", color = Muted)
                            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                val cap = payment.challenge
                                val maxPanelHeight = (browserHeight / pixelsPerDp - 140f).coerceAtLeast(140f)
                                val scale = if (cap == null) 1f else minOf(cssPixelsToDp, maxWidth.value / cap.width, maxPanelHeight / cap.height)
                                PaymentBrowserView(payment, cap == null, cap, browserWidth, browserHeight, if (cap == null) Modifier.size(1.dp) else Modifier.width((cap.width * scale).dp).height((cap.height * scale).dp))
                            }
                        }
                        }
                      }
                      if(payment.nativeStage in listOf("summary","payer") && !expressAdvancing) Surface(color=Paper,shadowElevation=if(IsLab)0.dp else 6.dp) {
                        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Primary(if(payment.nativeStage=="payer")"Continuar a la tarjeta" else "Continuar a los datos",payment.canContinue,payment::advance)
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                if(payment.nativeStage=="payer" && !expressVerification)TextButton(onClick={payment.stopExpress();editing=true},enabled=payment.chosenPayer!=null){Text("Editar datos")}
                                TextButton(onClick={if(payment.prepareOriginalReview())showOriginal=true}){Text("Ver página original")}
                            }
                        }
                      }
                    }
                }
            }
        }
    }
    if (editing) payment.chosenPayer?.let { profile ->
        val values = payment.payerValues
        val current = if (values.size == 5) profile.copy(givenName = values[0], familyName = values[1], document = values[2], email = values[3], phone = values[4]) else profile
        PayerProfileEditor(current, onSave = payment::editPayer, onDelete = { false }, onClose = { editing = false }, paymentOnly = true)
    }
}

/** Keep the real page laid out while native content owns display and accessibility. */
@Composable internal fun ExpandedPaymentChallenge(payment: EmbeddedPrexPayment, browserWidth: Int, browserHeight: Int, modifier: Modifier, corner: androidx.compose.ui.unit.Dp = 4.dp) {
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {keyboard?.hide()}
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val cap=payment.challenge
        val cssScale=if(payment.cssViewportWidth>0f)browserWidth/payment.cssViewportWidth/LocalDensity.current.density else 1f
        val scale=if(cap==null)1f else minOf(cssScale,maxWidth.value/cap.width,maxHeight.value/cap.height)
        PaymentBrowserView(payment,cap==null,cap,browserWidth,browserHeight,
            if(cap==null)Modifier.size(1.dp) else Modifier.width((cap.width*scale).dp).height((cap.height*scale).dp),corner)
    }
}

@Composable internal fun PaymentBrowserView(payment: EmbeddedPrexPayment, hidden: Boolean, crop: CaptchaRect?, browserWidth: Int, browserHeight: Int, modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp = 4.dp) {
    AndroidView(factory = {
        (payment.web.parent as? android.view.ViewGroup)?.removeView(payment.web)
        PaymentPageHost(it, payment.web)
    }, update = {
        payment.maskProviderBackground(hidden || crop!=null)
        it.viewportWidth = browserWidth
        // A translated crop cannot reveal pixels clipped by the WebView's own viewport.
        // Keep tall provider challenges fully laid out, then fit their complete rectangle.
        val cssScale=if(payment.cssViewportWidth>0f)browserWidth/payment.cssViewportWidth else it.resources.displayMetrics.density
        it.viewportHeight = maxOf(browserHeight,if(crop!=null)kotlin.math.ceil((crop.height+32f)*cssScale).toInt() else browserHeight)
        it.cssViewportWidth = payment.cssViewportWidth
        it.nativeHidden = hidden; it.crop = crop
    }, modifier = modifier.clip(RoundedCornerShape(corner)))
}

internal class PaymentPageHost(context: android.content.Context, private val browser: android.webkit.WebView) : android.view.ViewGroup(context) {
    var viewportWidth = 1
        set(value) { if (field != value) { field = value.coerceAtLeast(1); requestLayout() } }
    var viewportHeight = 1
        set(value) { if (field != value) { field = value.coerceAtLeast(1); requestLayout() } }
    var cssViewportWidth = 0f
        set(value) { if (field != value) { field = value; requestLayout() } }
    var crop: CaptchaRect? = null
        set(value) { if (field != value) { field = value; requestLayout() } }
    var nativeHidden: Boolean = false
        set(value) {
            field = value
            browser.alpha = if (value) 0f else 1f
            browser.importantForAccessibility = if (value) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            browser.isEnabled = !value
            requestLayout()
        }
    init { clipChildren = true; clipToPadding = true; addView(browser) }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        browser.measure(MeasureSpec.makeMeasureSpec(viewportWidth, MeasureSpec.EXACTLY),
            if (crop != null || nativeHidden) MeasureSpec.makeMeasureSpec(viewportHeight, MeasureSpec.EXACTLY) else heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val rect = crop
        val density = if (cssViewportWidth > 0f) browser.measuredWidth / cssViewportWidth else resources.displayMetrics.density
        val scale = if (rect == null) 1f else minOf(1f, width / (rect.width * density), height / (rect.height * density))
        browser.pivotX = 0f; browser.pivotY = 0f; browser.scaleX = scale; browser.scaleY = scale
        val x = ((rect?.x ?: 0f) * density * scale).toInt()
        val y = ((rect?.y ?: 0f) * density * scale).toInt()
        browser.layout(-x, -y, browser.measuredWidth-x, browser.measuredHeight-y)
    }
}

@Composable private fun AutofillProbe(onClose: () -> Unit) {
    // Native fields ONLY. No readback to Kotlin/JS, no submit, no disk storage, no network.
    var fields by remember { mutableStateOf<LinearLayout?>(null) }
    fun clear() {
        fields?.let { group ->
            group.context.getSystemService(android.view.autofill.AutofillManager::class.java)?.cancel()
            (0 until group.childCount).forEach { (group.getChildAt(it) as? EditText)?.text?.clear() }
        }
    }
    DisposableEffect(Unit) { onDispose { clear() } }
    AlertDialog(onDismissRequest = { clear(); onClose() }, title = { Text("Prueba local de autocompletado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tocá el campo para ver si Android ofrece tu tarjeta y pide la huella. Esto no paga, no se envía y no se guarda. No demuestra compatibilidad con Sistarbanc.")
                AndroidView(factory = { context ->
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        fun field(label: String, hint: String, type: Int) {
                            addView(EditText(context).apply {
                                this.hint = label; inputType = type
                                setAutofillHints(hint); importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                                isSaveEnabled = false
                                layoutParams = LinearLayout.LayoutParams(-1, -2)
                            })
                        }
                        field("Número de tarjeta", View.AUTOFILL_HINT_CREDIT_CARD_NUMBER, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                        field("Vencimiento", View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE, InputType.TYPE_CLASS_DATETIME)
                        fields = this
                    }
                })
            }
        }, confirmButton = { TextButton(onClick = { clear(); onClose() }) { Text("Borrar y cerrar") } })
}

/** Shown once after updating Lab. Keep it short and in the owner's words. */
internal val LabWhatsNew = listOf(
    "La app abre con un fundido, sin destello claro.",
    "Si STM está caída te lo dice, y casi todos los errores te ofrecen probar en el sitio de STM.",
    "Los errores del ingreso quedan escritos en la tarjeta roja hasta que volvés a intentar.",
    "El saldo nunca muestra un número dudoso: si STM lo escribe raro, avisa que no lo pudo leer.",
    "El saldo dice de qué día es, y si no tenés internet te avisa al toque.",
    "Al cargar otro importe, el panel sube con el teclado.",
    "El pago, la verificación y Configuración tienen el diseño de Lab.",
    "Mantené apretado el ícono: «Ver saldo» y «Recargar».",
)

/** The «Datos para Prex» form open in Lab: who it edits, whether it asks «¿Son tus datos?», and what comes after saving. */
internal data class LabPayerForm(val existing: PayerProfile?, val askMine: Boolean, val startMine: Boolean, val action: String, val then: (() -> Unit)? = null)
