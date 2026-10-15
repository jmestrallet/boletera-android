package uy.boletera.prueba

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One-hour STM fare with the card (public IM fare, also shown in the ticket guide). */
internal const val LAB_ONE_HOUR_FARE = 5200L

/** Scrolling part of the Lab home. The bottom bar and Express live in [LabHomeChrome], over this content. */
@Composable internal fun LabHomeContent(state: UiState, onPaymentReviewed: (() -> Unit)?, onActivity: (() -> Unit)?, onTicketGuide: (() -> Unit)?) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Spacer(Modifier.height(40.dp))
        Text("Saldo", style = type.titleMedium.copy(fontSize = 15.sp), color = LabColors.Muted)
        val number = Amounts.format(state.balance).removePrefix("$ ")
        val big = when { number.length > 7 -> 64.sp; number.length > 5 -> 84.sp; else -> 104.sp }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            Text("$", style = type.displayLarge.copy(fontSize = 44.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp), color = LabColors.Muted, modifier = Modifier.padding(top = 10.dp, end = 10.dp))
            Text(number, style = type.displayLarge.copy(fontSize = big, lineHeight = big, letterSpacing = (-4).sp), color = LabColors.On)
        }
        val balance = state.balance
        if (balance != null) {
            val trips = (balance / LAB_ONE_HOUR_FARE).coerceAtLeast(0)
            Text(if (balance < 0) buildAnnotatedString { append("Saldo pendiente de cubrir") }
            else if (trips == 0L) buildAnnotatedString { append("No alcanza para un viaje de 1 hora") }
            else buildAnnotatedString {
                append("Te alcanza para ")
                withStyle(SpanStyle(color = LabColors.Primary, fontWeight = FontWeight.Medium)) { append(if (trips == 1L) "1 viaje" else "$trips viajes") }
                append(" de 1 hora")
            }, style = type.titleMedium.copy(fontWeight = FontWeight.Normal), color = LabColors.On, modifier = Modifier.padding(top = 20.dp))
        }
        Text(if (state.busy && state.consultedAt == null) "Actualizando…" else state.consultedAt?.let { "Informado por STM · ${consultedLabel(it)}" }
            ?: "Deslizá hacia abajo para actualizar", style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Faint, modifier = Modifier.padding(top = 6.dp))
        if (state.paymentStatus.isNotBlank()) Surface(color = LabColors.Warn.copy(alpha = 0.12f), shape = RoundedCornerShape(28.dp), modifier = Modifier.padding(top = 28.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Antes de otra recarga", style = type.titleMedium, color = LabColors.Warn)
                Text(state.paymentStatus, style = type.bodyMedium, color = LabColors.Muted)
                if (onPaymentReviewed != null) Button(onClick = onPaymentReviewed, enabled = !state.busy,
                    colors = ButtonDefaults.buttonColors(containerColor = LabColors.Warn, contentColor = LabColors.OnWarn)) { Text("Ya revisé el pago") }
            }
        }
        val tiles = listOfNotNull(
            onActivity?.let { Triple("Actividad", "Movimientos", it) to (R.drawable.ic_history to 0) },
            onTicketGuide?.let { Triple("Tarifas", "1 hora · ${Amounts.format(LAB_ONE_HOUR_FARE)}", it) to (R.drawable.ic_directions_bus to 1) })
        if (tiles.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 36.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.forEach { (info, icon) -> LabTile(info.first, info.second, icon.first, icon.second == 1, !state.busy, info.third, Modifier.weight(1f)) }
            if (tiles.size == 1) Spacer(Modifier.weight(1f))
        }
        Text("Los viajes de las últimas 72 horas pueden figurar sin descontar.", style = type.bodySmall.copy(fontSize = 12.5.sp), color = LabColors.Faint,
            modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp))
        Spacer(Modifier.height(180.dp))
    }
}


@Composable private fun LabTile(title: String, subtitle: String, icon: Int, teal: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val type = MaterialTheme.typography
    val shape = RoundedCornerShape(28.dp)
    Column(modifier.height(128.dp).clip(shape).background(LabColors.GlassTint).clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Box(Modifier.size(44.dp).background(if (teal) LabColors.Teal else LabColors.Primary, if (teal) PolarShape(7, 0.07f) else PolarShape(4, 0.14f)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = if (teal) LabColors.OnTeal else LabColors.OnPrimary, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(title, style = type.titleMedium.copy(fontSize = 17.sp), color = LabColors.On)
            Text(subtitle, style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Muted)
        }
    }
}

/** Top bar, bottom bar and Express, drawn over the blurred home. */
@Suppress("DEPRECATION")
@Composable internal fun LabHomeChrome(state: UiState, haze: HazeState, blur: Boolean, onChangeCard: () -> Unit, onSettings: () -> Unit, onCharge: () -> Unit,
    onExpressCharge: (() -> Unit)?, expressPreparing: Boolean, expressProvider: String) {
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
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            val pill = RoundedCornerShape(22.dp)
            Row(Modifier.height(44.dp).labGlass(haze, pill, blur).clickable(enabled = !state.busy) { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onChangeCard() }
                .padding(start = 16.dp, end = 12.dp).semantics(mergeDescendants = true) { contentDescription = "Cambiar boletera" },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("STM ···· ${state.selectedCard?.takeLast(4).orEmpty()}", style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.On)
                Icon(painterResource(R.drawable.ic_expand_more), contentDescription = null, tint = LabColors.Muted, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(44.dp).labGlass(haze, RoundedCornerShape(22.dp), blur).clickable(onClick = onSettings), contentAlignment = Alignment.Center) {
                AppGlyph(Glyph.Settings, Modifier.size(22.dp), tint = LabColors.On, label = "Configuración")
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (onExpressCharge != null) Text(when {
                expressPreparing -> "Preparando tu recarga · abriendo $expressProvider"
                pressing -> "Soltá antes de que se llene para cancelar"
                else -> "Mantené el rayo para cargar ${Amounts.format(state.minimum)} con $expressProvider"
            }, style = type.bodyMedium.copy(fontSize = 13.5.sp), color = LabColors.Muted, modifier = Modifier.padding(bottom = 14.dp, start = 24.dp, end = 24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val enabled = !state.busy && !state.paymentNeedsReview && state.minimum != null
                Box(Modifier.weight(1f).height(80.dp).labGlass(haze, RoundedCornerShape(40.dp), blur).padding(8.dp)) {
                    Row(Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(if (enabled) LabColors.Primary else LabColors.Primary.copy(alpha = 0.38f))
                        .clickable(enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onCharge() },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = LabColors.OnPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Recargar", style = type.titleMedium, color = LabColors.OnPrimary)
                    }
                }
                if (onExpressCharge != null) LabExpressButton(state.minimum, enabled = !state.busy, preparing = expressPreparing, providerName = expressProvider,
                    onPressing = { pressing = it }, onStart = onExpressCharge)
            }
        }
    }
}

/** Express as an expressive cookie: hold 1.2 s (the ring fills), releasing early cancels; accepted turns it into a circle. */
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun LabExpressButton(amount: Long?, enabled: Boolean, preparing: Boolean, providerName: String, onPressing: (Boolean) -> Unit, onStart: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var fired by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressed) { onPressing(pressed && enabled && !preparing) }
    LaunchedEffect(pressed, enabled, preparing) {
        if (!pressed) fired = false
        if (preparing) progress = 1f
        else if (pressed && enabled) {
            val started = android.os.SystemClock.uptimeMillis()
            var pulses = 0
            while (!fired) {
                val elapsed = android.os.SystemClock.uptimeMillis() - started
                progress = (elapsed / EXPRESS_HOLD_MILLIS.toFloat()).coerceIn(0f, 0.98f)
                val due = (elapsed / 400).toInt().coerceAtMost(2)
                if (due > pulses) { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); pulses = due }
                delay(16)
            }
        } else progress = 0f
    }
    val spin = rememberInfiniteTransition(label = "lab express")
    val rotation by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(if (preparing) 1400 else 60_000, easing = LinearEasing)), label = "cookie rotation")
    val depth by animateFloatAsState(if (preparing || fired) 0f else 0.085f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "cookie to circle")
    val scale by animateFloatAsState(if (pressed && enabled && !preparing) 1.08f else 1f, spring(dampingRatio = 0.55f, stiffness = 400f), label = "express scale")
    val configuration = LocalViewConfiguration.current
    val hold = remember(configuration) { object : ViewConfiguration by configuration { override val longPressTimeoutMillis = EXPRESS_HOLD_MILLIS } }
    fun start() { if (enabled && !preparing && !fired) { fired = true; progress = 1f; onStart() } }
    CompositionLocalProvider(LocalViewConfiguration provides hold) {
        Box(Modifier.size(88.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .combinedClickable(interactionSource = interaction, indication = null, enabled = enabled && !preparing, onClick = {},
                onLongClickLabel = "Recargar ${Amounts.format(amount)} con $providerName", onLongClick = ::start)
            .testTag("expressShortcut")
            .semantics {
                contentDescription = "Carga Express"
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                stateDescription = if (preparing) "Preparando la recarga. Ya podés soltar." else if (fired) "Pulsación aceptada" else "Mantené apretado para recargar"
                if (enabled && !preparing) customActions = listOf(CustomAccessibilityAction("Recargar ${Amounts.format(amount)} con $providerName") { start(); true })
            }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 6.dp.toPx()
                rotate(if (preparing) 0f else rotation) {
                    drawPath(polarPath(Size(size.width - inset * 2, size.height - inset * 2), 12, depth).apply { translate(Offset(inset, inset)) },
                        color = if (enabled) LabColors.Primary else LabColors.Primary.copy(alpha = 0.38f))
                }
                val ring = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                val r = size.minDimension / 2 + 2.dp.toPx()
                val topLeft = Offset(center.x - r, center.y - r)
                if (preparing) drawArc(LabColors.On, rotation - 90f, 90f, false, topLeft, Size(r * 2, r * 2), style = ring)
                else if (progress > 0f) drawArc(LabColors.On, -90f, 360f * progress, false, topLeft, Size(r * 2, r * 2), style = ring)
            }
            Icon(painterResource(R.drawable.ic_bolt), contentDescription = null, tint = LabColors.OnPrimary, modifier = Modifier.size(34.dp))
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
