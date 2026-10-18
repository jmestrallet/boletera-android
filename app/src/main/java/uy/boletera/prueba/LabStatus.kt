package uy.boletera.prueba

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A label and what it does; a [quiet] secondary reads grey (leaving or technical), otherwise blue (another way forward). */
internal class LabAction(val label: String, val glyph: Glyph? = null, val enabled: Boolean = true, val quiet: Boolean = false,
    /** What Lab's record writes for this button: a fixed text when [label] carries a person's name. */
    val logLabel: String = label, val onClick: () -> Unit)

/**
 * The shared shape of every Lab "moment" (errors, payment results): the card tells the state with its badge, a short
 * title says what happened, one grey line explains, and the actions sit at the same place at the bottom. The text block
 * keeps one height, so moving from one state to the next only changes the badge and the words, never the card's place.
 */
@Composable internal fun LabStatusScreen(phase: HeroPhase, title: String, line: String, primary: LabAction?, secondary: LabAction?,
    link: LabAction? = null, extra: (@Composable () -> Unit)? = null) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // The card sits a little below the middle of the free space, so it reads together with the title.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = BiasAlignment(0f, .35f)) {
            LabCardSlot(phase, CardSpot.MOMENT, Modifier.requiredSize(LabSizes.HeroMoment))
        }
        Column(Modifier.fillMaxWidth().heightIn(min = 196.dp).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Title and line are announced together when the state changes; they arrive in a short, blurred blink.
            Column(Modifier.labEnter(0, dy = 8.dp, base = 40, blur = true).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = type.labMoment, color = LabColors.On, textAlign = TextAlign.Center)
                if (line.isNotEmpty()) Text(line, style = type.bodyLarge, color = LabColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
            }
            if (extra != null) Box(Modifier.labEnter(1, dy = 8.dp, base = 40)) { extra() }
            if (link != null) LabInlineLink(link.label, link.onClick, Modifier.padding(top = 4.dp).labEnter(2, dy = 8.dp, base = 40))
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.heightIn(min = 64.dp).fillMaxWidth().labEnter(2, base = 40)) { if (primary != null) LabPrimary(primary.label, primary.enabled, onClick = primary.onClick, glyph = primary.glyph, logLabel = primary.logLabel) }
        Box(Modifier.heightIn(min = 52.dp).labEnter(3, base = 40), contentAlignment = Alignment.Center) { if (secondary != null) LabTextLink(secondary.label, secondary.onClick, quiet = secondary.quiet, logLabel = secondary.logLabel) }
        Spacer(Modifier.height(16.dp))
    }
}

/** The balance STM reported, under a result: big number and one caption line. */
@Composable internal fun LabReportedBalance(balance: Long?, consultedAt: Long?, from: Long? = null) {
    val type = MaterialTheme.typography
    Column(Modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Top) {
            Text("$", style = type.titleLarge.copy(fontWeight = FontWeight.Normal), color = LabColors.Muted, modifier = Modifier.padding(top = 6.dp, end = 5.dp))
            // After a recharge the number rolls from the old balance to the new one, digit by digit.
            LabRollingNumber(Amounts.format(balance).removePrefix("$ "), from?.let { Amounts.format(it).removePrefix("$ ") },
                type.displayMedium.copy(fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1.2).sp), LabColors.On)
        }
        if (consultedAt != null) Text("Saldo informado por STM · ${consultedLabel(consultedAt)}", style = type.bodySmall, color = LabColors.Faint)
    }
}

/** Short title and one line for an error screen, from the engine's longer message. */
internal fun labErrorCopy(code: String): Pair<String, String> = when (code) {
    "STM CAÍDO" -> "STM no responde" to "La página de STM está caída o muy lenta. No es tu conexión."
    "DATOS DE ACCESO" -> "Revisá tus datos" to "El documento o la contraseña de gub.uy no coinciden."
    "CONEXIÓN SEGURA" -> "Conexión no segura" to "No pudimos verificar la seguridad de la conexión. Probá más tarde."
    "CONEXIÓN" -> "Se cortó la conexión" to "Fijate tu internet y probá de nuevo."
    "SERVICIO" -> "STM no respondió" to "Tardó demasiado o dio un error."
    "CARGA DE DATOS" -> "Saldo no disponible" to "STM no terminó de mostrar la información."
    "VERIFICACIÓN" -> "Falta una verificación" to "STM pidió otra comprobación. Volvé a intentar."
    else -> "No pudimos seguir" to "Algo interrumpió este paso. Volvé a intentar en un momento."
}

/**
 * How a Prex payment ends, in the Lab shape. The confirmation step itself stays in the payment flow; this covers what comes
 * after: processing, confirmed, crediting in STM, pending and rejected. [browser] keeps the hidden payment page alive.
 */
@Composable internal fun LabPaymentResult(payment: EmbeddedPrexPayment, pending: ActivePayment?, onOriginal: () -> Unit, onLeave: () -> Unit = {},
    browser: @Composable () -> Unit) {
    val stage = payment.nativeStage
    val amount = pending?.amount?.let(Amounts::format) ?: "la recarga"
    var details by remember(stage) { mutableStateOf(false) }
    val rows = payment.summaryRows
    val detail = if (rows.isNotEmpty()) LabAction("Ver detalle del pago", quiet = true) { details = true } else null
    val free = payment.canContinue && !payment.completionSubmitted
    // After a refusal the way home says so («Procesando tu pago» flashed while STM answered, Lab 21).
    var leaving by remember { mutableStateOf(false) }
    // STM's «Recarga no confirmada» without its button working still has a way out after a while (Lab 21). So do a receipt
    // or STM's «Recarga exitosa» whose own button is not found (the site changed: nothing would ever move on) and a
    // confirmation nobody answers: the balance on the home tells how it ended.
    var stuck by remember { mutableStateOf(false) }
    LaunchedEffect(stage, leaving) {
        stuck = false
        val wait = when {
            stage == "stmNotConfirmed" || leaving -> 8000L
            stage == "receipt" || stage == "stmSuccess" -> 15_000L
            stage == "finalConfirmation" -> 35_000L
            else -> 0L
        }
        if (wait > 0) { kotlinx.coroutines.delay(wait); stuck = true }
    }
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Box(Modifier.size(1.dp)) { browser() }
        val crediting = stage in listOf("stmSuccess", "returnBalance", "sessionExpired") || payment.returningToWallet && stage == "loading" ||
            payment.expressJourney && stage == "receipt"
        // B8: one screen from «Pagar» to the balance. Processing, the receipt and STM crediting are all «Procesando tu pago»;
        // «Recarga hecha» comes when STM shows the new balance. No «Pago confirmado / Ver mi saldo» stop.
        val processing = LabStatusScreenArgs("Procesando tu pago", "Prex está confirmando el pago de $amount.")
        when {
            // After a refusal STM shows «Recarga no confirmada»; the app answers it by itself and goes back to the home.
            stage == "stmNotConfirmed" || leaving && stage != "paymentRejected" ->
                LabStatusScreen(HeroPhase.LOADING, "Volviendo al inicio", "Un momento.", null, if (stuck) LabAction("Volver al inicio", quiet = true, onClick = onLeave) else null)
            stage == "finalConfirmation" || crediting || stage == "receipt" ->
                LabStatusScreen(HeroPhase.LOADING, processing.title, processing.line, null, if (stuck) LabAction("Volver al inicio", quiet = true, onClick = onLeave) else null)
            // The provider gives no reason («Existen datos incorrectos» also means no funds), so both causes are named.
            stage == "paymentRejected" -> LabStatusScreen(HeroPhase.ERROR, "Prex no aceptó el pago", "Puede faltar saldo en tu Prex o un dato de la tarjeta.",
                LabAction("Probar de nuevo") { LabRetry.reopenAmount = true; leaving = true; if (free) payment.advance(stage) else onLeave() },
                LabAction("Volver al inicio", quiet = true) { leaving = true; if (free) payment.advance(stage) else onLeave() })
            stage == "paymentPending" -> LabStatusScreen(HeroPhase.LOADING, processing.title, "Prex todavía está confirmando el pago de $amount.",
                LabAction("Volver al inicio") { if (free) payment.advance(stage) else onLeave() }, null)
            else -> LabStatusScreen(HeroPhase.LOADING, processing.title, processing.line, null, null)
        }
    }
    // The receipt from Prex, one tap away and out of the way.
    if (details) AlertDialog(onDismissRequest = { details = false }, title = { Text("Detalle del pago") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { (label, value) -> Column { Text(label.trimEnd(':'), style = MaterialTheme.typography.labelMedium, color = LabColors.Muted); Text(value, style = MaterialTheme.typography.bodyLarge) } }
        } },
        confirmButton = { TextButton(onClick = { details = false }) { Text("Cerrar") } },
        dismissButton = { TextButton(onClick = { details = false; onOriginal() }) { Text("Ver página original") } })
}

private class LabStatusScreenArgs(val title: String, val line: String)

/** After «Probar de nuevo» on a rejection, the amount panel opens again once the balance is back (it never resends). */
internal object LabRetry { var reopenAmount by mutableStateOf(false) }

/** "Recarga hecha": only once STM shows the new balance, which is the number shown (never a computed one). */
@Composable internal fun LabRechargeDone(state: UiState, credited: Long, receipt: List<Pair<String, String>> = emptyList(), onDone: () -> Unit) {
    var details by remember { mutableStateOf(false) }
    LabStatusScreen(HeroPhase.SUCCESS, "Recarga hecha", "Cargaste ${Amounts.format(credited)} en STM ···· ${state.selectedCard?.takeLast(4).orEmpty()}.",
        LabAction("Listo", onClick = onDone), if (receipt.isNotEmpty()) LabAction("Ver detalle del pago", quiet = true) { details = true } else null,
        extra = { LabReportedBalance(state.balance, state.consultedAt, from = state.balance?.let { it - credited }) })
    // The receipt from Prex, one tap away and out of the way.
    if (details) LabSheet({ details = false }) {
        Text("Detalle del pago", style = MaterialTheme.typography.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
        receipt.forEach { (label, value) ->
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(label.trimEnd(':'), style = MaterialTheme.typography.labelMedium, color = LabColors.Muted)
                Text(value, style = MaterialTheme.typography.bodyLarge, color = LabColors.On)
            }
        }
    }
}

/** Connecting (boleteras.html): the card loading, a short title, one line and, when it can be left, only «Cancelar». */
@Composable internal fun LabConnecting(title: String, line: String, onCancel: (() -> Unit)?, last: LastBalance? = null) =
    // With the last balance known, it stands in for the line: «Último saldo $ 1.240 · hoy 08:53», labelled as old.
    LabStatusScreen(HeroPhase.LOADING, title, if (last != null) "" else line, primary = null, secondary = onCancel?.let { LabAction("Cancelar", quiet = true, onClick = it) },
        extra = last?.let { { LabLastBalance(it) } })

@Composable private fun LabLastBalance(last: LastBalance) {
    val type = MaterialTheme.typography
    val label = consultedLabel(last.at).let { if (it.length == 5) "hoy $it" else it }
    Row(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(16.dp)).background(LabColors.GlassTint).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Último saldo", style = type.bodyMedium.copy(fontSize = 14.sp), color = LabColors.Muted)
        Text(Amounts.format(last.amount), style = type.titleSmall.copy(fontSize = 14.sp), color = LabColors.On)
        Text("· $label", style = type.bodyMedium.copy(fontSize = 14.sp), color = LabColors.Muted)
    }
}

/** «Falta un paso en STM»: amber (something for you to do, not a failure) with the same three steps as «Antes de entrar». */
@Composable internal fun LabAccessHelp(onOpenSite: () -> Unit, onDone: () -> Unit) =
    LabStatusScreen(HeroPhase.WARN, "Falta un paso en STM", "La primera vez, STM te pide autorizar el acceso en su web. Se hace una sola vez.",
        primary = LabAction("Abrir el sitio de STM ↗", onClick = onOpenSite), secondary = LabAction("Ya lo hice, entrar", onClick = onDone),
        extra = { LabAccessSteps(Modifier.padding(top = 20.dp)) })

/** The three steps of a first access to STM, numbered in amber; shared by «Antes de entrar» and «Falta un paso». */
@Composable internal fun LabAccessSteps(modifier: Modifier = Modifier, accent: androidx.compose.ui.graphics.Color = LabColors.Warn) {
    val type = MaterialTheme.typography
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("Abrí el sitio de STM y entrá con tu usuario gub.uy.", "Aceptá la autorización que te pide.", "Volvé acá y entrá.").forEachIndexed { i, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = type.labelLarge.copy(fontSize = 14.sp), color = accent)
                }
                Text(step, style = type.bodyLarge.copy(fontSize = 15.5.sp, lineHeight = 22.sp), color = androidx.compose.ui.graphics.Color(0xFFD7DDE8), modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}
