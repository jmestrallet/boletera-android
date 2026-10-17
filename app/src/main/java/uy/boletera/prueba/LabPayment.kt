package uy.boletera.prueba

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The header every payment step shares (pago.html): the small card, the amount with its dim «$», and where it goes. It sits
 * in the same place on every step; only what is under it changes.
 */
@Composable internal fun LabPaymentHeader(amount: Long?, card: String?, phase: HeroPhase) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LabCardSlot(phase, CardSpot.HEADER, Modifier.requiredSize(LabSizes.HeroHeader).onGloballyPositioned { LabExpressLight.target = it.boundsInRoot().center })
        Box(Modifier.padding(top = 4.dp)) {
            Text(Amounts.format(amount).removePrefix("$ "), style = type.displaySmall.copy(fontSize = 54.sp, lineHeight = 58.sp, letterSpacing = (-1.5).sp), color = LabColors.On)
            Text("$", style = type.titleLarge.copy(fontSize = 27.sp, lineHeight = 27.sp), color = LabColors.Muted.copy(alpha = .5f),
                modifier = Modifier.align(Alignment.TopStart).offset(x = (-19).dp, y = 10.dp))
        }
        if (card != null) Text("a tu STM ···· ${card.takeLast(4)}", style = type.bodyLarge.copy(fontSize = 15.sp), color = LabColors.Muted, modifier = Modifier.padding(top = 8.dp))
    }
}

/** The header folded to one line where the keyboard needs the room: «$ 500 · STM ···· 1234 · Prex». */
@Composable internal fun LabPaymentLine(amount: Long?, card: String?) {
    val type = MaterialTheme.typography
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Text(Amounts.format(amount), style = type.titleSmall.copy(fontSize = 15.sp), color = LabColors.On)
        if (card != null) { Dot(); Text("STM ···· ${card.takeLast(4)}", style = type.bodyMedium.copy(fontSize = 14.5.sp), color = LabColors.Muted) }
        Dot(); Text("Prex", style = type.bodyMedium.copy(fontSize = 14.5.sp), color = LabColors.Muted)
    }
}
@Composable private fun Dot() = Box(Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(LabColors.Faint))

/**
 * The one wait of a payment (B9: «Preparando tu recarga» and «Abriendo Prex» are the same moment): the header with the card
 * loading, what is happening, and only «Cancelar». Express says it is the quick recharge.
 */
@Composable internal fun LabPaymentWait(amount: Long?, card: String?, express: Boolean, onCancel: (() -> Unit)?) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        LabPaymentHeader(amount, card, HeroPhase.LOADING)
        Column(Modifier.padding(top = 30.dp, start = 8.dp, end = 8.dp).labEnter(0, base = 90).semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (express) "Carga Express" else "Preparando tu pago", style = type.labStep, color = LabColors.On, textAlign = TextAlign.Center)
            Text(if (express) "Cargando el mínimo con Prex y tus datos." else "Completamos tus datos en Prex.",
                style = type.bodyLarge, color = LabColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        if (express) Row(Modifier.padding(top = 14.dp).labEnter(1, base = 90).clip(RoundedCornerShape(18.dp)).background(LabColors.Teal.copy(alpha = .12f)).padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_bolt), contentDescription = null, tint = LabColors.Teal, modifier = Modifier.size(17.dp))
            Text("Carga rápida", style = type.labelLarge.copy(fontSize = 13.5.sp), color = LabColors.Teal)
        } else Row(Modifier.padding(top = 14.dp).labEnter(1, base = 90), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppGlyph(Glyph.Lock, Modifier.size(16.dp), tint = LabColors.Faint)
            Text("Conexión segura con Prex", style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Faint)
        }
        Spacer(Modifier.weight(1.3f))
        Box(Modifier.height(52.dp), contentAlignment = Alignment.Center) { if (onCancel != null) LabTextLink("Cancelar", onCancel, quiet = true) }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * «Una verificación y seguimos» (faltantes.html): the provider's check, cut out of its page and shown inside a clean frame.
 * After «Pagar» there is no way out ([onCancel] null): solving it sends the confirmation already asked for.
 */
@Composable internal fun LabVerification(amount: Long?, card: String?, afterPay: Boolean, onCancel: (() -> Unit)?, onContinue: LabAction? = null,
    expanded: Boolean = false, check: @Composable () -> Unit) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize()) {
        LabPaymentLine(amount, card)
        Text(if (expanded) "Elegí las imágenes" else "Una verificación y seguimos", style = type.labStep, color = LabColors.On,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp).semantics { heading() })
        Text(when {
            expanded && afterPay -> "Tocá las que te pide y «Verificar». Después el pago se confirma solo."
            expanded -> "Tocá las que te pide y «Verificar». Después seguimos solos."
            afterPay -> "Marcá la casilla. Después el pago se confirma solo."
            else -> "Marcá la casilla y seguimos."
        }, style = type.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        if (expanded) {
            // Google's picture grid is taller than wide: it gets all the free height, centred, with no frame of its own around it.
            Box(Modifier.padding(top = 16.dp, bottom = 8.dp).fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { check() }
        } else {
            // The «I'm not a robot» box is wide and low: it sits in a light frame, as in faltantes.html.
            Box(Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFFF3F5F8)).padding(vertical = 22.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center) { check() }
            Spacer(Modifier.weight(1f))
        }
        if (onContinue != null) LabPrimary(onContinue.label, onContinue.enabled, onClick = onContinue.onClick, glyph = onContinue.glyph)
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { if (onCancel != null) LabTextLink("Cancelar", onCancel, quiet = true) }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * «Confirmá tu pago» (pago.html): the same header, three lines (with what, who, how it shows in Prex) and «Pagar $ 500»
 * with a lock. It is the last tap before paying, so it says the amount.
 */
@Composable internal fun LabConfirmPayment(amount: Long?, card: String?, rows: List<Pair<String, String>>, payerName: String?, canPay: Boolean, onPay: () -> Unit, onDetail: (() -> Unit)?,
    countdown: Int = 0, onCancel: (() -> Unit)? = null, expectedSuffix: String? = null) {
    val type = MaterialTheme.typography
    val method = rows.firstOrNull { it.first.trim().trimEnd(':').equals("Medio de pago", true) }?.second
    // «PREX **** 0849» reads as «Prex ···· 0849».
    val methodLabel = method?.let { m -> m.takeLast(4).takeIf { it.all(Char::isDigit) }?.let { "Prex ···· $it" } ?: m }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        LabPaymentHeader(amount, card, HeroPhase.IDLE)
        Text("Confirmá tu pago", style = type.labStep, color = LabColors.On, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp).labEnter(0, dy = 0.dp, dx = 28.dp, base = 40).semantics { heading() })
        Column(Modifier.fillMaxWidth().labEnter(1, dy = 0.dp, dx = 28.dp, base = 40).clip(RoundedCornerShape(24.dp)).background(LabColors.GlassTint).border(1.dp, LabColors.GlassLine, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 6.dp)) {
            listOfNotNull(methodLabel?.let { "Tarjeta" to it }, payerName?.let { "A nombre de" to it }, "En tu Prex" to "Pago STM").forEachIndexed { i, (label, value) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(LabColors.GlassLine))
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(label, style = type.bodyMedium.copy(fontSize = 15.sp), color = LabColors.Muted, modifier = Modifier.weight(1f))
                    Text(value, style = type.titleSmall.copy(fontSize = 15.sp), color = LabColors.On)
                }
            }
        }
        // Prex shows a card other than the one chosen: said plainly before anything, and it never pays by itself.
        val shownSuffix = methodLabel?.takeLast(4)?.takeIf { it.all(Char::isDigit) }
        val otherCard = expectedSuffix != null && shownSuffix != null && shownSuffix != expectedSuffix
        if (otherCard) Row(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LabColors.Rose.copy(alpha = .12f))
            .padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppGlyph(Glyph.Info, Modifier.size(18.dp), tint = LabColors.Rose)
            Text("Prex muestra otra tarjeta (···· $shownSuffix), no la que elegiste (···· $expectedSuffix). Revisá antes de pagar.",
                style = type.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp), color = LabColors.On)
        }
        // «Pagar» shrinks into the green circle that flies up to become the card's own (movimiento.html v2).
        var payRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        var paid by remember { mutableStateOf(false) }
        fun pay() { if (paid || !canPay) return; paid = true; payRect?.let(LabCardFlight::badgeFrom); onPay() }
        // If Prex did not take the tap (still ready a few seconds later), the button comes back; a second «Pagar» is refused
        // by the payment itself once one was sent.
        LaunchedEffect(paid) { if (paid) { kotlinx.coroutines.delay(4000); if (canPay) paid = false } }
        // Carga Express (owner, 17/10): a ring that fills while it says «Cargando en 4… 3… 2… 1»; then it pays.
        // «Pagar ahora» does not wait, «Cancelar» stops without paying, and leaving the app stops the count.
        // It pays by itself only for the card that was sent: Prex must show the same last four. Otherwise it is a plain «Pagar».
        val automatic = countdown > 0 && shownSuffix != null && !otherCard
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        var counting by remember(automatic) { mutableStateOf(automatic && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) }
        var left by remember { mutableIntStateOf(countdown) }
        var fill by remember { mutableFloatStateOf(0f) }
        // Google's «¿Guardar la tarjeta?» or any system sheet over the app: the count waits and starts again after it.
        val focused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
        DisposableEffect(lifecycle) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && counting) { counting = false; LabLog.add("Express", "cuenta frenada: la app salió de pantalla") }
            }
            lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(counting, canPay, focused, countdown) {
            if (!counting || !canPay || !focused || countdown <= 0) return@LaunchedEffect
            // Real time from the frame clock, not an animation: «quitar animaciones» must not make it pay at once.
            val total = countdown * 1000L
            val start = withFrameMillis { it }
            while (true) {
                val elapsed = withFrameMillis { it } - start
                fill = (elapsed.toFloat() / total).coerceAtMost(1f)
                left = (countdown - (elapsed / 1000).toInt()).coerceAtLeast(1)
                if (elapsed >= total) break
            }
            counting = false; LabLog.add("Express", "cuenta terminada: paga"); pay()
        }
        Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
            if (counting && countdown > 0) LabCountdownRing(left, fill)
        }
        Box(Modifier.labEnter(2, dy = 0.dp, dx = 28.dp, base = 40).onGloballyPositioned { payRect = it.boundsInWindow() }) {
            LabPrimary(when { counting && countdown > 0 -> "Pagar ahora"; otherCard -> "Pagar con ···· $shownSuffix"; else -> "Pagar ${Amounts.format(amount)} ahora" }, canPay && !paid,
                onClick = { if (counting) LabLog.add("Express", "pagó sin esperar la cuenta"); counting = false; pay() }, glyph = Glyph.Lock)
        }
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            if (onCancel != null && !paid) LabTextLink("Cancelar", { counting = false; onCancel() }, quiet = true)
            else if (onDetail != null) LabTextLink("Ver detalle del pago", onDetail, quiet = true)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Carga Express' count before paying: a ring that fills with the number in the middle, and what it is doing under it. */
@Composable private fun LabCountdownRing(left: Int, progress: Float) {
    val type = MaterialTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {
        liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            val track = LabColors.Primary.copy(alpha = .16f)
            val arc = LabColors.Primary
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2
                val size = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(track, 0f, 360f, false, topLeft, size, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawArc(arc, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            Text("$left", style = type.displaySmall.copy(fontSize = 40.sp, lineHeight = 44.sp), color = LabColors.On)
        }
        Text("Cargando en $left…", style = type.bodyLarge.copy(fontSize = 15.sp), color = LabColors.Muted, modifier = Modifier.padding(top = 12.dp))
    }
}

/** While the provider's check is being redrawn (coming back to the app): a loader where the check goes. */
@Composable internal fun LabCheckLoading(onLight: Boolean) {
    // The card tells the state, as everywhere in Lab: loading, with one line under it.
    Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LabCardHero(HeroPhase.LOADING, Modifier.size(110.dp, 88.dp))
        Text("Cargando la verificación…", style = MaterialTheme.typography.bodyLarge, color = if (onLight) Color(0xFF3C4350) else LabColors.Muted,
            modifier = Modifier.padding(top = 6.dp))
    }
}

/** No screenshots, screen recording or recent-apps thumbnail while the card's data is on screen. */
@Composable internal fun SecureScreen() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** Keeps the phone awake while a payment is in course: locking it mid-way stopped the reading of the result. */
@Composable internal fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
}

/**
 * Lab's Prex payment (pago.html, prex.html, faltantes.html), over the same engine as the shared screen. The provider's
 * summary and payer steps run by themselves behind «Abriendo Prex» (they carry what was already chosen); a check shows
 * inside «Una verificación y seguimos»; the card is typed natively; «Pagar» is the only tap that charges, and from there
 * on there is no way back until the result.
 */
@Composable internal fun LabEmbeddedPrexScreen(payment: EmbeddedPrexPayment, pending: ActivePayment?, card: String?, onClose: () -> Unit, showPayer: Boolean = false) {
    KeepScreenOn()
    var showOriginal by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    val stage = payment.nativeStage
    LaunchedEffect(stage) { showOriginal = false }
    LaunchedEffect(showOriginal || stage == "original") { payment.setPageAutofill(showOriginal || stage == "original") }
    // Plan B: if the payer could not be filled (and the check, if any, is already accepted), Prex's own page is shown so
    // the person completes it and goes on; the app never leaves them in front of a check that leads nowhere.
    val payerStuck = stage == "payer" && !payment.payerConflict && payment.payerValues.size == 5 && payment.payerValues.any { it.isBlank() } &&
        (payment.challenge == null && !payment.verificationRequired || payment.verificationApproved)
    LaunchedEffect(payerStuck) {
        if (!payerStuck) return@LaunchedEffect
        // Long enough for the payer module's own retries of the document-type list (4 × 1.2 s).
        kotlinx.coroutines.delay(7000)
        payment.pageShape { LabLog.add("forma", "titular sin completar · $it") }
        LabLog.add("Prex", "titular sin completar (${payment.payerWhy}): se muestra la página de Prex")
        if (payment.showPageBeforePaying()) showOriginal = true
    }
    // Watchdog for every step before the card: one that stays still with nothing for the person to do leaves the
    // page's shape in the record at 6 s and, at 10 s, Prex's own page is shown so the person can go on.
    val waitingForPerson = payment.challenge != null && !payment.verificationApproved || stage in setOf("card", "original") ||
        payment.message.isNotBlank() || payment.slowStep || payment.payerConflict || showOriginal
    LaunchedEffect(stage, waitingForPerson, payment.canContinue, payment.completionSubmitted, payment.payerValues, payment.expressPhase) {
        // A «Continuar» already sent is Prex's to answer: its own 30 s limit (slowStep) covers it, never this one.
        if (waitingForPerson || payment.completionSubmitted || stage !in setOf("summary", "payer", "loading") || payment.returningToWallet) return@LaunchedEffect
        kotlinx.coroutines.delay(6000)
        payment.pageShape { LabLog.add("forma", "$stage quieto 6 s · $it") }
        if (stage == "loading") return@LaunchedEffect   // a stalled load already offers «Seguir esperando» (slowStep)
        kotlinx.coroutines.delay(4000)
        LabLog.add("Prex", "$stage quieto 10 s: se muestra la página de Prex")
        if (payment.showPageBeforePaying()) showOriginal = true
    }
    LaunchedEffect(stage) { if (stage in listOf("returnBalance", "sessionExpired")) onClose() }
    val amount = pending?.amount
    val express = payment.expressJourney
    // «A nombre de» only says something when more than one person is saved (Lab 18).
    val payerName = payment.chosenPayer?.takeIf { showPayer }?.let { if (it.personal) "${it.givenName} ${it.familyName}".trim() else it.label }
    // From «Pagar» on, nothing leaves the payment until the result (A4).
    val paid = payment.authorizationSent || stage == "finalConfirmation" && (payment.completionSubmitted || payment.captchaRetry) ||
        stage in listOf("receipt", "paymentRejected", "paymentPending", "stmSuccess") || payment.returningToWallet
    BackHandler { if (!paid) onClose() }
    val completion = stage in listOf("finalConfirmation", "receipt", "paymentRejected", "paymentPending", "stmSuccess", "returnBalance", "sessionExpired") ||
        payment.returningToWallet && stage == "loading"
    var stuckAfterPay by remember { mutableStateOf(false) }
    LaunchedEffect(stage, payment.challenge != null, paid) {
        stuckAfterPay = false
        if (paid && stage == "finalConfirmation" && payment.challenge != null) { kotlinx.coroutines.delay(25_000); stuckAfterPay = true }
    }
    val waitStage = stage in listOf("loading", "summary", "payer") && !payment.returningToWallet
    // The provider's summary and payer steps carry what was already chosen: they go on by themselves (B7, B9). A check stops it.
    LaunchedEffect(stage, payment.canContinue, payment.challenge, payment.payerValues, payment.verificationApproved, payment.payerConflict) {
        if (stage == "summary" && payment.canContinue) payment.advance(stage)
        // Once Prex accepts the check (the ticked box stays on screen, so «no check» is not the signal) it goes on by itself.
        // The app never reads Google's answer; Prex's own flag says it, and its page is watched daily (.tools/watch-sites.mjs).
        val checked = payment.challenge == null && !payment.verificationRequired || payment.verificationApproved
        if (stage == "payer" && payment.canContinue && checked && !payment.payerConflict &&
            payment.payerValues.size == 5 && payment.payerValues.all { it.isNotBlank() }) payment.advance(stage)
    }
    LaunchedEffect(stage, payment.challenge != null) {
        if (stage in listOf("payer", "card", "finalConfirmation") && payment.challenge != null) payment.positionVerification() else payment.restoreVerification()
    }
    Box(Modifier.fillMaxSize()) {
        LabAmbient()
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            // A wait or the time after «Pagar» has no arrow (reglas.html); the steps before paying do.
            LabTopBar(onBack = if (paid || waitStage || payment.challenge != null && stage == "payer") null else onClose,
                step = if (paid || waitStage) null else when (stage) { "card" -> 0; "finalConfirmation" -> 1; else -> null })
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
                val browserWidth = constraints.maxWidth
                val browserHeight = constraints.maxHeight
                val density = LocalDensity.current.density
                val cssToDp = if (payment.cssViewportWidth > 0f) browserWidth / payment.cssViewportWidth / density else 1f
                @Composable fun check() {
                    val cap = payment.challenge
                    // Coming back to the app: until the page answers, a loader instead of the old (maybe empty) crop.
                    if (payment.staleAfterResume) LabCheckLoading(onLight = !payment.expandedChallenge)
                    else if (payment.expandedChallenge) ExpandedPaymentChallenge(payment, browserWidth, browserHeight, Modifier.fillMaxSize(), corner = 14.dp)
                    else {
                        val scale = if (cap == null) 1f else minOf(cssToDp, (maxWidth.value - 48f) / cap.width, (browserHeight / density - 200f).coerceAtLeast(120f) / cap.height)
                        PaymentBrowserView(payment, cap == null, cap, browserWidth, browserHeight,
                            if (cap == null) Modifier.size(1.dp) else Modifier.width((cap.width * scale).dp).height((cap.height * scale).dp))
                    }
                }
                when {
                    payment.message.isNotBlank() -> AppErrorScreen(payment.message, payment = true, paymentAmount = amount, onExit = onClose, charged = paid)
                    showOriginal || stage == "original" -> Column(Modifier.fillMaxSize()) {
                        if (stage != "original") LabTextLink("Volver", { showOriginal = false }, Modifier.align(Alignment.End), quiet = true)
                        PaymentBrowserView(payment, false, null, browserWidth, browserHeight, Modifier.weight(1f).fillMaxWidth())
                    }
                    // Before paying, a page that stalls offers to keep waiting or go back (after «Pagar» it just keeps processing).
                    // After «Pagar» a stall still has a way out: the balance on the home tells how it ended.
                    payment.slowStep && paid -> {
                        Box(Modifier.size(1.dp)) { PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp)) }
                        LabStatusScreen(HeroPhase.LOADING, "Prex está tardando", "Podés esperar o volver al inicio: ahí ves tu saldo.",
                            primary = LabAction("Seguir esperando") { payment.keepWaiting() },
                            secondary = LabAction("Volver al inicio", quiet = true, onClick = onClose))
                    }
                    payment.slowStep && !paid -> {
                        Box(Modifier.size(1.dp)) { PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp)) }
                        LabStatusScreen(HeroPhase.LOADING, "Prex está tardando", "La página de Prex responde más lento que de costumbre.",
                            primary = LabAction("Seguir esperando") { payment.keepWaiting() },
                            secondary = LabAction("Volver al inicio", quiet = true, onClick = onClose))
                    }
                    // Prex already has other holder data: nothing is overwritten without asking (the shared screen did this too).
                    stage == "payer" && payment.payerConflict -> {
                        Box(Modifier.size(1.dp)) { PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp)) }
                        val chosen = payment.chosenPayer?.let { if (it.personal) "tus datos" else "los datos de ${it.label}" } ?: "tus datos"
                        LabStatusScreen(HeroPhase.WARN, "Prex tiene otros datos", "Los datos que tiene Prex no coinciden con $chosen.",
                            primary = LabAction("Usar ${chosen.replaceFirstChar { it.uppercase() }}", logLabel = "Usar los datos elegidos") { payment.applyChosenPayer() },
                            secondary = LabAction("Cancelar", quiet = true, onClick = onClose))
                    }
                    // Solving the check is enough: the app notices it and goes on (the provider's pages are watched daily for changes).
                    stage == "payer" && payment.challenge != null -> LabVerification(amount, card, afterPay = false, onCancel = onClose, expanded = payment.expandedChallenge) { check() }
                    // A check before the first «Pagar» is solved and then paid here; after «Pagar» the app confirms by itself.
                    stage == "finalConfirmation" && payment.challenge != null && !paid -> LabVerification(amount, card, afterPay = false, onCancel = onClose, expanded = payment.expandedChallenge,
                        onContinue = LabAction("Pagar ${Amounts.format(amount)}", glyph = Glyph.Lock, enabled = payment.canContinue) { payment.advance(stage) }) { check() }
                    stage == "finalConfirmation" && payment.challenge != null -> LabVerification(amount, card, afterPay = true,
                        onCancel = if (stuckAfterPay) onClose else null, expanded = payment.expandedChallenge) { check() }
                    stage == "card" -> Column(Modifier.fillMaxSize()) {
                        // Only where the card is on screen: no screenshots or recording (the rest of the payment can be captured to report a problem).
                        SecureScreen()
                        LabPaymentLine(amount, card)
                        // Google's list opens by itself on every card step; Express goes on once Google filled it all.
                        NativeCardForm(payment.cardBusy, payment.canContinue, payment.cardError, payment.expandedChallenge, payment::submitCard,
                            { if (payment.prepareOriginalReview()) showOriginal = true }, focusCard = true, autoContinue = express, express = express,
                            verificationRequired = payment.verificationRequired && !payment.verificationApproved, onManualContinue = payment.manualContinueObserver,
                            onVerificationShown = payment::cardVerificationShown, onAutomaticSubmit = payment::submitAutofilledCard,
                            onOtherCard = { LabRetry.reopenAmount = true; onClose() }) { check() }
                    }
                    stage == "finalConfirmation" && !payment.completionSubmitted && !payment.authorizationSent -> {
                        Box(Modifier.size(1.dp)) { PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp)) }
                        LabConfirmPayment(amount, card, payment.summaryRows, payerName, payment.canContinue, onPay = { payment.advance(stage) },
                            onDetail = if (payment.summaryRows.isNotEmpty()) ({ details = true }) else null,
                            countdown = if (express) 4 else 0, onCancel = if (express && !payment.authorizationSent) onClose else null,
                            expectedSuffix = payment.cardSuffix)
                    }
                    completion -> LabPaymentResult(payment, pending, onOriginal = { if (payment.prepareOriginalReview()) showOriginal = true }, onLeave = onClose) {
                        PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp))
                    }
                    else -> {
                        Box(Modifier.size(1.dp)) { PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp)) }
                        LabPaymentWait(amount, card, express, onCancel = onClose)
                    }
                }
            }
        }
    }
    if (details) LabSheet({ details = false }) {
        Text("Detalle del pago", style = MaterialTheme.typography.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
        payment.summaryRows.forEach { (label, value) ->
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(label.trimEnd(':'), style = MaterialTheme.typography.labelMedium, color = LabColors.Muted)
                Text(value, style = MaterialTheme.typography.bodyLarge, color = LabColors.On)
            }
        }
        LabTextLink("Ver página original", { details = false; if (payment.prepareOriginalReview()) showOriginal = true }, Modifier.align(Alignment.CenterHorizontally), quiet = true)
    }
}

/**
 * «Tu tarjeta» (pago.html 3, medios-de-pago v4): the header folded to one line, Google's list opened by the form itself,
 * expiry and code on one row, one line under them (what Google filled, or «Boletera no guarda tu tarjeta»), and
 * «Continuar» right above the keyboard. Express says only the card is left and goes on by itself once it is filled.
 */
@Composable internal fun LabCardFields(busy: Boolean, canContinue: Boolean, providerError: Boolean, express: Boolean, waitingCheck: Boolean,
    checkRequired: Boolean, number: @Composable (Modifier) -> Unit, expiry: @Composable (Modifier) -> Unit, code: @Composable (Modifier) -> Unit,
    verification: @Composable () -> Unit, verificationView: androidx.compose.foundation.relocation.BringIntoViewRequester,
    onContinue: () -> Unit, onOriginal: () -> Unit, onOtherCard: (() -> Unit)? = null, filledNote: String? = null) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 18.dp)) {
            Text(if (express) "Solo falta la tarjeta" else "Tu tarjeta", style = type.labStep, color = LabColors.On,
                modifier = Modifier.padding(horizontal = 4.dp).labEnter(0, dy = 0.dp, dx = 28.dp, base = 50).semantics { heading() })
            if (express) Row(Modifier.padding(start = 4.dp, top = 12.dp).labEnter(1, dy = 0.dp, dx = 28.dp, base = 50).clip(RoundedCornerShape(18.dp)).background(LabColors.Teal.copy(alpha = .12f)).padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_bolt), contentDescription = null, tint = LabColors.Teal, modifier = Modifier.size(17.dp))
                Text("Elegí tu tarjeta y el resto se hace solo", style = type.labelLarge.copy(fontSize = 13.5.sp), color = LabColors.Teal)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.labEnter(2, dy = 0.dp, dx = 28.dp, base = 50)) { number(Modifier.fillMaxWidth().heightIn(min = 60.dp)) }
            Row(Modifier.padding(top = 12.dp).labEnter(3, dy = 0.dp, dx = 28.dp, base = 50), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                expiry(Modifier.weight(1f).heightIn(min = 60.dp))
                code(Modifier.weight(1f).heightIn(min = 60.dp))
            }
            when {
                providerError -> Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppGlyph(Glyph.Info, Modifier.size(17.dp), tint = LabColors.Rose)
                    Text("Prex no aceptó la tarjeta. Revisá los datos o probá con otra.", style = type.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp), color = LabColors.Rose)
                }
                waitingCheck || checkRequired -> Text(if (waitingCheck) "Completá la verificación y seguimos solos." else "Completá la tarjeta y la verificación para seguir.",
                    style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 4.dp, top = 10.dp))
            }
            Row(Modifier.padding(start = 4.dp, top = 14.dp).labEnter(4, dy = 0.dp, dx = 28.dp, base = 50), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (filledNote != null) {
                    AppGlyph(Glyph.Check, Modifier.size(16.dp), tint = LabColors.Primary)
                    Text(filledNote, style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Muted)
                } else {
                    AppGlyph(Glyph.Lock, Modifier.size(16.dp), tint = LabColors.Faint)
                    Text("Boletera no guarda tu tarjeta.", style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Faint)
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp).bringIntoViewRequester(verificationView)) { verification() }
        }
        Box(Modifier.padding(top = 8.dp, bottom = 10.dp).labEnter(5, dy = 0.dp, dx = 28.dp, base = 50)) {
            // Prex's own answer to a refused card stays one tap away; it is the only way to see why.
            if (providerError) Column {
                LabPrimary("Probar con otra tarjeta", onClick = onOtherCard ?: onOriginal)
                if (onOtherCard != null) LabTextLink("Ver qué respondió Prex", onOriginal, Modifier.align(Alignment.CenterHorizontally), quiet = true)
            }
            else LabPrimary(if (busy) "Enviando…" else "Continuar", enabled = canContinue && !busy, onClick = onContinue)
        }
    }
}
