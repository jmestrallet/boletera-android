package uy.boletera.prueba

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot

import android.app.Activity
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** One-hour STM fare with the card (public IM fare, also shown in the ticket guide). */
internal const val LAB_ONE_HOUR_FARE = 5200L

/**
 * Scrolling part of the Lab home (inicio.html): the balance is the only protagonist. Under it, how far it goes, when STM
 * reported it, the 72-hour note and the fares guide as a quiet link. The bottom bar and Express live in [LabHomeChrome].
 * Activity is not shown until an account with identity level 2 can test it.
 */
@Composable internal fun LabHomeContent(state: UiState, offline: Boolean, onTicketGuide: (() -> Unit)?) {
    val type = MaterialTheme.typography
    val balance = state.balance
    val negative = balance != null && balance < 0
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Spacer(Modifier.height(40.dp))
        // The home arrives in a quick cascade while the card lands in its pill (movimiento.html v2).
        Text(if (negative) "Saldo negativo" else "Saldo", style = type.titleMedium.copy(fontSize = 15.sp), color = if (negative) LabColors.Rose else LabColors.Muted,
            modifier = Modifier.labEnter(0, base = 40))
        val number = Amounts.format(balance?.let { kotlin.math.abs(it) }).removePrefix("$ ")
        // Already huge: with large text it keeps its size instead of growing out of the screen.
        val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
        val big = (when { number.length > 7 -> 64f; number.length > 5 -> 88f; else -> 112f } / scale).sp
        Row(Modifier.padding(top = 8.dp).labEnter(1, base = 40).semantics(mergeDescendants = true) {
            contentDescription = if (balance == null) "Saldo no disponible" else (if (negative) "Saldo negativo, " else "Saldo, ") + Amounts.format(kotlin.math.abs(balance))
        }, verticalAlignment = Alignment.Top) {
            // The «$» hangs small and dim; when negative, «−$» hang together as one prefix (the usual «-$ 120»).
            Text(if (negative) "−$" else "$", style = type.displayLarge.copy(fontSize = 36.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
                color = if (negative) LabColors.Rose.copy(alpha = .85f) else LabColors.Muted.copy(alpha = .5f), modifier = Modifier.padding(top = 16.dp, end = if (negative) 8.dp else 6.dp))
            Text(number, style = type.displayLarge.copy(fontSize = big, lineHeight = big, letterSpacing = (-5).sp), color = if (negative) LabColors.Rose else LabColors.On)
        }
        if (balance != null) {
            val trips = (balance / LAB_ONE_HOUR_FARE).coerceAtLeast(0)
            Text(when {
                negative && state.minimum != null -> buildAnnotatedString {
                    append("Recarga mínima: ")
                    withStyle(SpanStyle(color = LabColors.Primary, fontWeight = FontWeight.Medium)) { append(Amounts.format(state.minimum)) }
                }
                negative -> buildAnnotatedString { append("Cargá para volver a viajar") }
                trips == 0L -> buildAnnotatedString { append("No alcanza para un viaje de 1 hora") }
                else -> buildAnnotatedString {
                    append("Te alcanza para ")
                    withStyle(SpanStyle(color = LabColors.Primary, fontWeight = FontWeight.Medium)) { append(if (trips == 1L) "1 viaje" else "$trips viajes") }
                    append(" de 1 hora")
                }
            }, style = type.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal), color = LabColors.On, modifier = Modifier.padding(top = 20.dp).labEnter(2, base = 40))
        }
        // A recharge Prex confirmed that STM has not added yet: said under the balance until the balance shows it.
        state.onTheWay?.let { amount ->
            Row(Modifier.padding(top = 14.dp).clip(RoundedCornerShape(16.dp)).background(LabColors.Primary.copy(alpha = .12f)).padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), color = LabColors.Primary, strokeWidth = 2.dp)
                Column {
                    Text("Recarga de ${Amounts.format(amount)} en camino", style = type.titleSmall.copy(fontSize = 15.sp), color = LabColors.On)
                    Text("STM la suma en unos minutos", style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Muted)
                }
            }
        }
        // On the home the day is always said («hoy 08:53»), so an old balance never passes for a fresh one.
        val consulted = state.consultedAt?.let { consultedLabel(it).let { label -> if (label.length == 5) "hoy $label" else label } }
        Row(Modifier.padding(top = 8.dp).heightIn(min = 32.dp).labEnter(3, base = 40), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (offline) Icon(painterResource(R.drawable.ic_cloud_off), contentDescription = null, tint = LabColors.Faint, modifier = Modifier.size(17.dp))
            Text(when {
                offline && consulted != null -> "Sin internet · saldo de $consulted"
                offline -> "Sin internet"
                state.busy && consulted == null -> "Actualizando…"
                consulted != null -> "Informado por STM · $consulted"
                else -> "Deslizá hacia abajo para actualizar"
            }, style = type.bodySmall.copy(fontSize = 13.5.sp), color = LabColors.Faint)
        }
        Text("Puede no incluir viajes de las últimas 72 horas.", style = type.bodySmall.copy(fontSize = 12.5.sp), color = LabColors.Faint.copy(alpha = .8f),
            modifier = Modifier.labEnter(4, base = 40))
        if (onTicketGuide != null) Row(Modifier.padding(top = 14.dp).labEnter(5, base = 40).minimumInteractiveComponentSize().clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onTicketGuide).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Boletos y tarifas", style = type.titleSmall.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium), color = LabColors.Muted)
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = LabColors.Muted, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(200.dp))
    }
}

/** Top bar, bottom bar and Express, drawn over the blurred home. */
@Suppress("DEPRECATION")
@Composable internal fun LabHomeChrome(state: UiState, haze: HazeState, blur: Boolean, onChangeCard: () -> Unit, onSettings: () -> Unit, onCharge: () -> Unit,
    onExpressCharge: (() -> Unit)?, expressPreparing: Boolean, expressProvider: String, ring: Boolean = true) {
    val view = LocalView.current
    // The Lab home is always dark: keep the system bars dark while it is on screen, then restore the theme's.
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = window?.let { Triple(it.statusBarColor, it.navigationBarColor, controller!!.isAppearanceLightStatusBars) }
        window?.statusBarColor = LabColors.Base.toArgb()
        window?.navigationBarColor = LabColors.Base.toArgb()
        controller?.isAppearanceLightStatusBars = false; controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (window != null && previous != null) {
                window.statusBarColor = previous.first
                window.navigationBarColor = previous.second
                controller!!.isAppearanceLightStatusBars = previous.third; controller.isAppearanceLightNavigationBars = previous.third
            }
        }
    }
    val type = MaterialTheme.typography
    val haptic = LocalHapticFeedback.current
    var pressing by remember { mutableStateOf(false) }
    // The bolt's help shows the first times; once Express has been used a few times it is known and the line goes.
    val hints = LocalContext.current.getSharedPreferences("lab_hints", android.content.Context.MODE_PRIVATE)
    var expressUses by remember { mutableIntStateOf(hints.getInt("express_uses", 0)) }
    // With one card in the account there is nothing to choose: no arrow and no tap. Two or more: the picker, even if only
    // one of them can be used, so people learn it exists.
    val canChoose = state.cards.size > 1
    val cardNames = rememberLabCardNames()
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val pill = RoundedCornerShape(24.dp)
            val chipPress = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Row(Modifier.height(48.dp).then(if (canChoose) Modifier.labPress(chipPress) else Modifier).labGlass(haze, pill, blur)
                .then(if (canChoose) Modifier.clickable(chipPress, indication = androidx.compose.material3.ripple(), enabled = !state.busy, role = Role.Button) { LabLog.tap("elegir boletera"); haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onChangeCard() } else Modifier)
                .padding(start = 10.dp, end = if (canChoose) 12.dp else 16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "STM terminada en ${state.selectedCard?.takeLast(4).orEmpty()}" + if (canChoose) ", cambiar boletera" else ""
                },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabCardSlot(HeroPhase.IDLE, CardSpot.CHIP, Modifier.size(34.dp))
                Text(cardNames.get(state.selectedCard) ?: "STM ···· ${state.selectedCard?.takeLast(4).orEmpty()}", style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.On)
                if (canChoose) Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null, tint = LabColors.Muted, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            val gearPress = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(Modifier.size(48.dp).labPress(gearPress).labGlass(haze, RoundedCornerShape(24.dp), blur).clickable(gearPress, indication = androidx.compose.material3.ripple()) { LabLog.tap("Configuración"); onSettings() }, contentAlignment = Alignment.Center) {
                AppGlyph(Glyph.Settings, Modifier.size(22.dp), tint = LabColors.On, label = "Configuración")
            }
        }
        // inicio.html: the bar's ring keeps its visible edge 40 dp from the screen bottom (gesture inset + 16).
        // The bar comes up from below the edge with a firm spring.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp).labRise(60), horizontalAlignment = Alignment.CenterHorizontally) {
            if (onExpressCharge != null && (expressUses < 3 || pressing || expressPreparing)) Text(when {
                expressPreparing -> "Preparando tu pago · $expressProvider"
                pressing -> "Si soltás antes, no se carga"
                else -> "Mantené el rayo para cargar ${Amounts.format(state.minimum)} con $expressProvider"
            }, style = type.bodyMedium.copy(fontSize = 13.5.sp), color = LabColors.Muted, modifier = Modifier.padding(bottom = 10.dp, start = 24.dp, end = 24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Lab (A3): a previous payment never leaves Recargar grey without a reason.
                val enabled = !state.busy && state.minimum != null
                val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                // [ring]: the glass ring around the pill (inicio.html), or the plain 64 dp pill like every other main button.
                Box(Modifier.weight(1f).height(76.dp).then(if (ring) Modifier.labGlass(haze, RoundedCornerShape(38.dp), blur).padding(6.dp) else Modifier),
                    contentAlignment = Alignment.Center) {
                    // It answers the finger: sinks a little and darkens while held, with Android's ripple from where it was touched.
                    Row(Modifier.fillMaxWidth().height(if (ring) 64.dp else 64.dp).labPress(press).clip(RoundedCornerShape(32.dp)).background(if (enabled) LabColors.Primary else LabColors.Primary.copy(alpha = 0.38f))
                        .clickable(press, indication = androidx.compose.material3.ripple(color = LabColors.OnPrimary), enabled = enabled, role = Role.Button) { LabLog.tap("Recargar"); haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onCharge() },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = LabColors.OnPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Recargar", style = type.titleMedium.copy(fontSize = 19.sp), color = LabColors.OnPrimary)
                    }
                }
                if (onExpressCharge != null) LabExpressButton(state.minimum, enabled = !state.busy, preparing = expressPreparing, providerName = expressProvider,
                    onPressing = { pressing = it }, onStart = { expressUses++; hints.edit().putInt("express_uses", expressUses).apply(); onExpressCharge() })
            }
        }
    }
}

/** When the hold tics: closer and closer together, like something charging (express.html). */
private val ExpressTicks = longArrayOf(0, 380, 640, 820, 950, 1040, 1105, 1150)

/**
 * Express: hold 1.2 s and the button itself charges, without a ring around it. It sinks, spins faster and faster,
 * turns lighter and trembles with tics that come closer together; letting go early springs it back and nothing happens.
 * At 1.2 s it jumps, flashes white and fires. With animations off it only turns lighter.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun LabExpressButton(amount: Long?, enabled: Boolean, preparing: Boolean, providerName: String, onPressing: (Boolean) -> Unit, onStart: () -> Unit) {
    val context = LocalContext.current
    val still = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    var fired by remember { mutableStateOf(false) }
    var charge by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var tremorX by remember { mutableFloatStateOf(0f) }
    var tremorY by remember { mutableFloatStateOf(0f) }
    var shot by remember { mutableFloatStateOf(-1f) } // seconds since firing, -1 when not fired
    val enabledNow by rememberUpdatedState(enabled)
    val preparingNow by rememberUpdatedState(preparing)
    LaunchedEffect(pressed) { onPressing(pressed && enabled && !preparing); if (pressed) LabLog.add("rayo", if (enabled && !preparing) "apretado" else "apretado sin efecto (enabled=$enabled, preparando=$preparing)") }
    // One clock drives the charge, the spin, the tremor and the shot, so they stay in step with the tics.
    LaunchedEffect(Unit) {
        var last = -1L; var pressStart = -1L; var released = -1L; var from = 0f; var kickAt = -1L; var nextTick = 0
        var firedAt = -1L
        while (true) withInfiniteAnimationFrameMillis { now ->
            // Milliseconds since the app's first frame: small enough to stay exact as a float (see LabCardClock.seconds).
            val ms = LabCardClock.seconds(now) * 1000f
            val dt = if (last < 0) 16f else (now - last).toFloat().coerceAtMost(64f); last = now
            val holding = pressed && enabledNow && !preparingNow && !fired
            if (holding) {
                if (pressStart < 0) { pressStart = now; nextTick = 0; released = -1 }
                val e = now - pressStart
                charge = (e / EXPRESS_HOLD_MILLIS.toFloat()).coerceIn(0f, 1f)
                while (nextTick < ExpressTicks.size && e >= ExpressTicks[nextTick]) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick); kickAt = now; nextTick++
                }
            } else {
                if (pressStart >= 0) { pressStart = -1; released = now; from = charge }
                if (!fired) charge = if (released < 0) 0f else (from - (now - released) / 260f).coerceAtLeast(0f)
            }
            if (fired && firedAt < 0) firedAt = now
            if (!fired) firedAt = -1
            shot = if (firedAt < 0) -1f else (now - firedAt) / 1000f
            if (!still) {
                val speed = when { preparingNow -> .26f; shot >= 0f -> .85f * (1 - (shot / .6f).coerceIn(0f, 1f)).pow(3) + .006f; else -> .006f + (.85f - .006f) * charge * charge }
                rotation = (rotation + dt * speed) % 360f
                // A smooth tremor that grows with the charge (two frequencies, not frame noise), plus a kick on every tic.
                val amp = 1.7f * charge.pow(1.6f)
                val j = (now - kickAt).toFloat()
                val kick = if (kickAt >= 0 && j < 70f) sin(j / 70f * 2 * PI.toFloat()) * 1.3f * (1 - j / 70f) else 0f
                tremorX = if (shot >= 0f && shot < .14f) sin(shot / .14f * 4 * PI.toFloat()) * 2.2f * (1 - shot / .14f)
                    else amp * (sin(ms * .138f) + .6f * sin(ms * .211f + 1.3f)) / 1.6f + kick
                tremorY = if (shot >= 0f) 0f else amp * .5f * sin(ms * .173f + .7f)
            }
        }
    }
    LaunchedEffect(pressed) { if (!pressed && !preparing) fired = false }
    val press = when {
        shot >= 0f && shot < .09f -> .9f + (1.12f - .9f) * (1 - (1 - shot / .09f).pow(3))
        shot >= .09f -> 1.12f + (1f - 1.12f) * (1 - (1 - ((shot - .09f) / .33f).coerceIn(0f, 1f)).pow(3))
        still -> 1f
        else -> 1f - .1f * (1 - (1 - charge).pow(3))
    }
    val white = if (shot >= 0f) (if (shot < .05f) .75f + .25f * shot / .05f else 1f - ((shot - .16f) / .32f).coerceIn(0f, 1f)) else 0f
    val fill = lerp(lerp(LabColors.Primary, Color(0xFFE2F3FF), charge), Color(0xFFEFFFFE), white)
    val halo = if (shot >= 0f) .75f * white * (1 - ((shot - .28f) / .28f).coerceIn(0f, 1f)) else if (still) 0f else .42f * charge * charge
    val configuration = LocalViewConfiguration.current
    val hold = remember(configuration) { object : ViewConfiguration by configuration { override val longPressTimeoutMillis = maxOf(EXPRESS_HOLD_MILLIS, configuration.longPressTimeoutMillis) } }
    fun start() { if (enabled && !preparing && !fired) { LabLog.tap("rayo: Carga Express disparada"); fired = true; haptic.performHapticFeedback(HapticFeedbackType.Confirm); LabExpressLight.fire(); onStart() } }
    CompositionLocalProvider(LocalViewConfiguration provides hold) {
        Box(Modifier.size(76.dp).onGloballyPositioned { LabExpressLight.from = it.boundsInRoot().center }.graphicsLayer { scaleX = press; scaleY = press; translationX = tremorX.dp.toPx(); translationY = tremorY.dp.toPx() }
            .combinedClickable(interactionSource = interaction, indication = null, enabled = enabled && !preparing, onClick = {},
                onLongClickLabel = "Recargar ${Amounts.format(amount)} con $providerName", onLongClick = ::start)
            .testTag("expressShortcut")
            .semantics {
                contentDescription = "Carga Express"
                progressBarRangeInfo = ProgressBarRangeInfo(charge, 0f..1f)
                stateDescription = if (preparing) "Preparando la recarga" else if (fired) "Pulsación aceptada" else "Mantené apretado para recargar"
                if (enabled && !preparing) customActions = listOf(CustomAccessibilityAction("Recargar ${Amounts.format(amount)} con $providerName") { start(); true })
            }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                if (halo > 0f) drawCircle(Brush.radialGradient(listOf(Color(0xFFE9FFFE).copy(alpha = .9f * halo), Color(0xFF9EE3E0).copy(alpha = 0f)), center, size.minDimension * .82f),
                    size.minDimension * .82f)
                rotate(rotation) {
                    drawPath(polarPath(size, 9, .07f), color = if (enabled) fill else LabColors.Primary.copy(alpha = 0.38f))
                }
            }
            Icon(painterResource(R.drawable.ic_bolt), contentDescription = null, tint = LabColors.OnPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

/** "14:32" today, "ayer 14:32", or "12 oct 14:32": an old balance must never look like today's. */
internal fun consultedLabel(at: Long, now: Long = System.currentTimeMillis()): String {
    val locale = Locale.forLanguageTag("es-UY")
    val day = SimpleDateFormat("yyyyMMdd", locale)
    val time = SimpleDateFormat("HH:mm", locale).format(Date(at))
    return when (day.format(Date(at))) {
        day.format(Date(now)) -> time
        day.format(Date(now - 86_400_000L)) -> "ayer $time"
        else -> SimpleDateFormat("d MMM", locale).format(Date(at)).trimEnd('.') + " $time"
    }
}
