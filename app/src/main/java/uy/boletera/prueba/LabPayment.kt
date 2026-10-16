package uy.boletera.prueba

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
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
            Text(if (express) "Carga Express" else "Abriendo Prex", style = type.labStep, color = LabColors.On, textAlign = TextAlign.Center)
            Text(if (express) "Cargando el mínimo con Prex y tus datos guardados." else "Tus datos de titular ya van completos.",
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
@Composable internal fun LabConfirmPayment(amount: Long?, card: String?, rows: List<Pair<String, String>>, payerName: String?, canPay: Boolean, onPay: () -> Unit, onDetail: (() -> Unit)?) {
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
            listOfNotNull(methodLabel?.let { "Tarjeta" to it }, payerName?.let { "Titular" to it }, "En tu Prex" to "Pago STM").forEachIndexed { i, (label, value) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(LabColors.GlassLine))
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(label, style = type.bodyMedium.copy(fontSize = 15.sp), color = LabColors.Muted, modifier = Modifier.weight(1f))
                    Text(value, style = type.titleSmall.copy(fontSize = 15.sp), color = LabColors.On)
                }
            }
        }
        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
        // «Pagar» shrinks into the green circle that flies up to become the card's own (movimiento.html v2).
        var payRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        Box(Modifier.labEnter(2, dy = 0.dp, dx = 28.dp, base = 40).onGloballyPositioned { payRect = it.boundsInWindow() }) {
            LabPrimary("Pagar ${Amounts.format(amount)}", canPay, onClick = { payRect?.let(LabCardFlight::badgeFrom); onPay() }, glyph = Glyph.Lock)
        }
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { if (onDetail != null) LabTextLink("Ver detalle del pago", onDetail, quiet = true) }
        Spacer(Modifier.height(8.dp))
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
@Composable internal fun LabEmbeddedPrexScreen(payment: EmbeddedPrexPayment, pending: ActivePayment?, card: String?, onClose: () -> Unit) {
    KeepScreenOn()
    var showOriginal by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    val stage = payment.nativeStage
    LaunchedEffect(stage) { showOriginal = false }
    LaunchedEffect(stage) { if (stage in listOf("returnBalance", "sessionExpired")) onClose() }
    val amount = pending?.amount
    val express = payment.expressJourney
    // Decided once per payment: the saved card is used, or (if Prex refuses it) the normal card step takes over.
    val usingSaved = remember { LabExpressCard.pending != null }
    // The opened card never outlives this payment.
    DisposableEffect(Unit) { onDispose { LabExpressCard.pending = null } }
    val payerName = payment.chosenPayer?.let { "${it.givenName} ${it.familyName}".trim() }
    // From «Pagar» on, nothing leaves the payment until the result (A4).
    val paid = stage == "finalConfirmation" && (payment.completionSubmitted || payment.captchaRetry) ||
        stage in listOf("receipt", "paymentRejected", "paymentPending", "stmSuccess") || payment.returningToWallet
    BackHandler { if (!paid) onClose() }
    val completion = stage in listOf("finalConfirmation", "receipt", "paymentRejected", "paymentPending", "stmSuccess", "returnBalance", "sessionExpired") ||
        payment.returningToWallet && stage == "loading"
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
                    payment.message.isNotBlank() -> AppErrorScreen(payment.message, payment = true, paymentAmount = amount, onExit = onClose)
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
                        LabStatusScreen(HeroPhase.WARN, "Prex tiene otro titular", "Los datos que tiene Prex no coinciden con los de ${payerName ?: "tu titular"}.",
                            primary = LabAction("Usar ${payerName ?: "mi titular"}") { payment.applyChosenPayer() },
                            secondary = LabAction("Cancelar", quiet = true, onClick = onClose))
                    }
                    // Solving the check is enough: the app notices it and goes on (the provider's pages are watched daily for changes).
                    stage == "payer" && payment.challenge != null -> LabVerification(amount, card, afterPay = false, onCancel = onClose, expanded = payment.expandedChallenge) { check() }
                    // A check before the first «Pagar» is solved and then paid here; after «Pagar» the app confirms by itself.
                    stage == "finalConfirmation" && payment.challenge != null && !paid -> LabVerification(amount, card, afterPay = false, onCancel = onClose, expanded = payment.expandedChallenge,
                        onContinue = LabAction("Pagar ${Amounts.format(amount)}", glyph = Glyph.Lock, enabled = payment.canContinue) { payment.advance(stage) }) { check() }
                    stage == "finalConfirmation" && payment.challenge != null -> LabVerification(amount, card, afterPay = true, onCancel = null, expanded = payment.expandedChallenge) { check() }
                    // Express with the saved card: nothing to choose, it is sent as soon as Prex is ready.
                    stage == "card" && express && usingSaved && !payment.cardError -> {
                        Box(Modifier.size(1.dp)) { PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp)) }
                        LaunchedEffect(payment.canContinue, payment.cardBusy) {
                            val saved = LabExpressCard.pending
                            if (saved != null && payment.canContinue && !payment.cardBusy) { LabExpressCard.pending = null; payment.submitSavedCard(saved.pan, saved.expiry, saved.cvv) }
                        }
                        LabPaymentWait(amount, card, express = true, onCancel = null)
                    }
                    stage == "card" -> Column(Modifier.fillMaxSize()) {
                        // Only where the card is on screen: no screenshots or recording (the rest of the payment can be captured to report a problem).
                        SecureScreen()
                        LabPaymentLine(amount, card)
                        NativeCardForm(payment.cardBusy, payment.canContinue, payment.cardError, payment.expandedChallenge, payment::submitCard,
                            { if (payment.prepareOriginalReview()) showOriginal = true }, focusCard = express || payment.expressPhase == "done",
                            verificationRequired = payment.verificationRequired && !payment.verificationApproved, onManualContinue = payment.manualContinueObserver,
                            onVerificationShown = payment::cardVerificationShown, onAutomaticSubmit = payment::submitAutofilledCard,
                            onOtherCard = { LabRetry.reopenAmount = true; onClose() },
                            // Offered while no card is saved for Express yet.
                            offerSave = LabExpressCard.vault?.exists == false,
                            savedCard = LabExpressCard.vault?.label,
                            onUseSaved = { LabExpressCard.vault?.unlock { c -> if (c != null) payment.submitCard(c.pan, c.expiry, c.cvv) } }) { check() }
                    }
                    stage == "finalConfirmation" && !payment.completionSubmitted -> {
                        Box(Modifier.size(1.dp)) { PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.size(1.dp)) }
                        LabConfirmPayment(amount, card, payment.summaryRows, payerName, payment.canContinue, onPay = { payment.advance(stage) },
                            onDetail = if (payment.summaryRows.isNotEmpty()) ({ details = true }) else null)
                    }
                    completion -> LabPaymentResult(payment, pending, onOriginal = { if (payment.prepareOriginalReview()) showOriginal = true }) {
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
 * «Tu tarjeta» (pago.html 3, faltantes.html r3 and x1): the header folded to one line, «Usar una tarjeta guardada» as a
 * visible action, expiry and code on one row, privacy in one line with its lock, and «Continuar» right above the keyboard.
 * Express says «Elegí tu tarjeta» and goes on by itself. The fields and what they do come from [NativeCardForm].
 */
@Composable internal fun LabCardFields(busy: Boolean, canContinue: Boolean, providerError: Boolean, express: Boolean, waitingCheck: Boolean,
    checkRequired: Boolean, number: @Composable (Modifier) -> Unit, expiry: @Composable (Modifier) -> Unit, code: @Composable (Modifier) -> Unit,
    verification: @Composable () -> Unit, verificationView: androidx.compose.foundation.relocation.BringIntoViewRequester,
    onSaved: () -> Unit, onContinue: () -> Unit, onOriginal: () -> Unit, onOtherCard: (() -> Unit)? = null,
    saveForExpress: Boolean? = null, onSaveForExpress: (Boolean) -> Unit = {}, savedCard: String? = null, onUseSaved: (() -> Unit)? = null) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 18.dp)) {
            Text(if (express) "Elegí tu tarjeta" else "Tu tarjeta", style = type.labStep, color = LabColors.On,
                modifier = Modifier.padding(horizontal = 4.dp).labEnter(0, dy = 0.dp, dx = 28.dp, base = 50).semantics { heading() })
            if (express) {
                Text("Tocá la que tenés guardada y seguimos solos.", style = type.bodyLarge.copy(fontSize = 15.sp), color = LabColors.Muted,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                Row(Modifier.padding(start = 4.dp, top = 12.dp).labEnter(1, dy = 0.dp, dx = 28.dp, base = 50).clip(RoundedCornerShape(18.dp)).background(LabColors.Teal.copy(alpha = .12f)).padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_bolt), contentDescription = null, tint = LabColors.Teal, modifier = Modifier.size(17.dp))
                    Text("Carga rápida", style = type.labelLarge.copy(fontSize = 13.5.sp), color = LabColors.Teal)
                }
            } else Row(Modifier.padding(start = 2.dp, top = 14.dp, bottom = 4.dp).labEnter(1, dy = 0.dp, dx = 28.dp, base = 50).height(48.dp).clip(RoundedCornerShape(24.dp))
                .clickable(enabled = !busy, role = androidx.compose.ui.semantics.Role.Button, onClick = if (savedCard != null && onUseSaved != null) onUseSaved else onSaved).testTag("requestCardAutofill"),
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(LabColors.GlassTint).border(1.dp, LabColors.GlassLine, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppGlyph(Glyph.Card, Modifier.size(19.dp), tint = LabColors.Primary)
                    Text(if (savedCard != null && onUseSaved != null) "Usar tu tarjeta $savedCard" else "Usar una tarjeta guardada", style = type.titleSmall.copy(fontSize = 14.5.sp), color = LabColors.On)
                }
            }
            Spacer(Modifier.height(12.dp))
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
                AppGlyph(Glyph.Lock, Modifier.size(16.dp), tint = LabColors.Faint)
                Text("Boletera no guarda tu tarjeta.", style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Faint)
            }
            // Carga Express can use this card next time with only a fingerprint (CardVault).
            if (saveForExpress != null) Row(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp))
                .toggleable(saveForExpress, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onSaveForExpress).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Guardar para Carga Express", style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.On)
                    Text("Cifrada en este celular; se abre solo con tu huella.", style = type.bodyMedium.copy(fontSize = 13.5.sp), color = LabColors.Muted)
                }
                androidx.compose.material3.Switch(saveForExpress, onCheckedChange = null,
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = LabColors.Primary, checkedThumbColor = LabColors.OnPrimary))
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp).bringIntoViewRequester(verificationView)) { verification() }
        }
        Box(Modifier.padding(top = 8.dp, bottom = 10.dp).labEnter(5, dy = 0.dp, dx = 28.dp, base = 50)) {
            // Prex's own answer to a refused card stays one tap away; it is the only way to see why.
            // A refused card cannot be sent again from here yet (Lab 8): try another one from the amounts, or read Prex's answer.
            if (providerError) Column {
                LabPrimary("Probar con otra tarjeta", onClick = onOtherCard ?: onOriginal)
                if (onOtherCard != null) LabTextLink("Ver qué respondió Prex", onOriginal, Modifier.align(Alignment.CenterHorizontally), quiet = true)
            }
            else LabPrimary(if (busy) "Enviando…" else "Continuar", enabled = canContinue && !busy, onClick = onContinue)
        }
    }
}
