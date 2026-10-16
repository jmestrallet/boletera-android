package uy.boletera.prueba

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Material's emphasized curves: one for things that move, one for things that arrive, one for things that leave. */
internal val LabEmphasized = CubicBezierEasing(.2f, 0f, 0f, 1f)
internal val LabDecelerate = CubicBezierEasing(.05f, .7f, .1f, 1f)
internal val LabAccelerate = CubicBezierEasing(.3f, 0f, .8f, .15f)

@Composable internal fun rememberLabStill(): Boolean {
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** Where a screen keeps the card: the big one of a moment, the payment's header, or the home's pill. */
internal enum class CardSpot { MOMENT, HEADER, CHIP }

/**
 * The card is one piece that travels (movimiento.html). Every screen marks where its card goes with [LabCardSlot]; when
 * one screen hands over to the next, a copy flies in an arc from the old place to the new one while the new place waits
 * empty, and the card's state keeps its own clock ([LabCardClock]) so a change in the middle of the trip goes on.
 */
internal object LabCardFlight {
    class Slot(val id: Any, var rect: Rect, var spot: CardSpot, var phase: HeroPhase)
    class Flight(val from: Rect, val fromSpot: CardSpot, val to: Slot, val badgeFrom: Rect? = null)
    private val live = mutableListOf<Slot>()
    private var gone: Pair<Slot, Long>? = null
    private var badgeSource: Pair<Rect, Long>? = null
    /** «Pagar» was just pressed here: the next trip of the card brings its circle from this button. */
    fun badgeFrom(rect: Rect) { badgeSource = rect to SystemClock.uptimeMillis() }
    var flight by mutableStateOf<Flight?>(null)
    /** Only the window that draws the flying copy turns flights on (screens shown alone, as in tests, keep their card). */
    var enabled = false
    /** The copy flying now draws this slot's card, so the slot itself stays empty until it lands. */
    fun hides(id: Any) = flight?.to?.id == id

    fun arrive(id: Any, rect: Rect, spot: CardSpot, phase: HeroPhase) {
        live.firstOrNull { it.id == id }?.let { it.rect = rect; it.spot = spot; it.phase = phase; return }
        val from = live.lastOrNull() ?: gone?.takeIf { SystemClock.uptimeMillis() - it.second < 450 }?.first
        val slot = Slot(id, rect, spot, phase); live += slot; gone = null
        if (from != null) fly(from.rect, from.spot, slot)
    }
    fun update(id: Any, phase: HeroPhase) { live.firstOrNull { it.id == id }?.phase = phase }
    fun leave(id: Any) {
        val slot = live.firstOrNull { it.id == id } ?: return
        val wasTop = live.lastOrNull() === slot
        live.remove(slot)
        if (flight?.to === slot) flight = null
        // The top card leaves while another is still on screen (the moment over the home closes): it flies back there.
        val back = live.lastOrNull()
        if (wasTop && back != null) fly(slot.rect, slot.spot, back) else if (wasTop) gone = slot to SystemClock.uptimeMillis()
    }
    private fun fly(from: Rect, fromSpot: CardSpot, to: Slot) {
        // A trip shorter than a few pixels is the same place: the card just stays (an in-place change of state).
        if (!enabled || hypot(from.center.x - to.rect.center.x, from.center.y - to.rect.center.y) < 6f && fromSpot == to.spot) return
        val badge = badgeSource?.takeIf { SystemClock.uptimeMillis() - it.second < 900 }?.first
        badgeSource = null
        flight = Flight(from, fromSpot, to, badge)
    }
}

/** The place of the card on a screen; the card itself is drawn here, except while a copy is flying in. */
@Composable internal fun LabCardSlot(phase: HeroPhase, spot: CardSpot, modifier: Modifier = Modifier, opening: Boolean = false) {
    val id = remember { Any() }
    DisposableEffect(id) { onDispose { LabCardFlight.leave(id) } }
    SideEffect { LabCardFlight.update(id, phase) }
    Box(modifier.onGloballyPositioned { LabCardFlight.arrive(id, it.boundsInWindow(), spot, phase) }) {
        if (!LabCardFlight.hides(id)) {
            if (spot == CardSpot.CHIP) LabMiniCard(Modifier.fillMaxSize()) else LabCardHero(phase, Modifier.fillMaxSize(), opening = opening)
        }
    }
}

/** px per card unit for a slot: the hero shows the card 44 of 80 units wide, the pill 44 of 52. */
private fun unitOf(spot: CardSpot, r: Rect) =
    if (spot == CardSpot.CHIP) minOf(r.width / 52f, r.height / 36f) else minOf(r.width / 80f, r.height / 64f)

/** Draws the flying copy over everything, in window coordinates; put it at the root of the window. */
@Composable internal fun LabCardFlightOverlay() {
    val still = rememberLabStill()
    DisposableEffect(still) { LabCardFlight.enabled = !still; onDispose { LabCardFlight.enabled = false; LabCardFlight.flight = null } }
    val f = LabCardFlight.flight ?: return
    val p = remember(f) { Animatable(0f) }
    LaunchedEffect(f) {
        // A firm spring (movimiento.html v2): it lands in about a third of a second with a small overshoot.
        p.animateTo(1f, spring(dampingRatio = .74f, stiffness = 360f, visibilityThreshold = .002f))
        if (LabCardFlight.flight === f) LabCardFlight.flight = null
    }
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        val t = p.value
        val to = f.to
        val c0 = f.from.center; val c1 = to.rect.center
        // An arc, not a straight line: the card lifts a little on the way.
        val lift = with(density) { 28.dp.toPx() } * sin(PI.toFloat() * t.coerceIn(0f, 1f))
        val c = Offset(c0.x + (c1.x - c0.x) * t, c0.y + (c1.y - c0.y) * t - lift)
        val k = unitOf(f.fromSpot, f.from) + (unitOf(to.spot, to.rect) - unitOf(f.fromSpot, f.from)) * t
        val fromChip = f.fromSpot == CardSpot.CHIP; val toChip = to.spot == CardSpot.CHIP
        val badge = when {
            fromChip && toChip -> 0f
            toChip -> 1 - (t / .25f).coerceIn(0f, 1f)
            fromChip -> ((t - .6f) / .4f).coerceIn(0f, 1f)
            else -> 1f
        }
        val w = 80f * k; val h = 64f * k
        // The card leans toward where it is going and straightens as it lands.
        val lean = (p.velocity * (c1.x - c0.x) / with(density) { 1000.dp.toPx() }).coerceIn(-9f, 9f)
        val fromButton = f.badgeFrom
        LabCardHero(to.phase, Modifier.offset { IntOffset((c.x - w / 2).roundToInt(), (c.y - h / 2).roundToInt()) }
            .size(with(density) { w.toDp() }, with(density) { h.toDp() }).graphicsLayer { rotationZ = lean }, badgeAlpha = if (fromButton != null) (if (t > .97f) 1f else 0f) else badge)
        if (fromButton != null && t <= .97f) Canvas(Modifier.fillMaxSize()) {
            // The pill closes into a circle where it is, then flies up in an arc and lands as the card's corner circle.
            val k1 = unitOf(to.spot, to.rect)
            val target = Offset(c1.x + 24f * k1, c1.y + 6f * k1)
            val close = (t / .3f).coerceIn(0f, 1f); val go = ((t - .15f) / .85f).coerceIn(0f, 1.1f)
            val d0 = fromButton.height; val d1 = 18f * k1
            val width = fromButton.width + (d0 - fromButton.width) * close
            val diameter = d0 + (d1 - d0) * go
            val wNow = if (go > 0f) diameter else width; val hNow = if (go > 0f) diameter else d0
            val arc = 40.dp.toPx() * sin(PI.toFloat() * go.coerceIn(0f, 1f))
            val center = Offset(fromButton.center.x + (target.x - fromButton.center.x) * go, fromButton.center.y + (target.y - fromButton.center.y) * go - arc)
            val ink = androidx.compose.ui.graphics.lerp(LabColors.Primary, Color(0xFF2FA35A), (t / .25f).coerceIn(0f, 1f))
            drawRoundRect(ink, Offset(center.x - wNow / 2, center.y - hNow / 2), androidx.compose.ui.geometry.Size(wNow, hNow), androidx.compose.ui.geometry.CornerRadius(hNow / 2))
        }
    }
}

/**
 * Arrival of a piece of a screen: it fades in while it rises [dy] (or comes from the side, [dx]), one after another by
 * [order]. With animations off it is simply there.
 */
internal fun Modifier.labEnter(order: Int = 0, dy: Dp = 10.dp, dx: Dp = 0.dp, base: Int = 0, millis: Int = 240, blur: Boolean = false): Modifier = composed {
    val still = rememberLabStill()
    val a = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { if (!still) { delay((base + order * 22).toLong()); a.animateTo(1f, tween(millis, easing = LabDecelerate)) } }
    graphicsLayer {
        alpha = a.value; translationY = (1 - a.value) * dy.toPx(); translationX = (1 - a.value) * dx.toPx()
        // A text that changes blinks in out of focus instead of sliding (Android 12+; older phones just fade).
        if (blur && a.value < 1f && android.os.Build.VERSION.SDK_INT >= 31) {
            val r = (1 - a.value) * 2.dp.toPx() + .01f
            renderEffect = androidx.compose.ui.graphics.BlurEffect(r, r)
        } else renderEffect = null
    }
}

/** Something that rises from below the edge with a soft spring (the home's Recargar bar). */
internal fun Modifier.labRise(delayMillis: Int = 160, from: Dp = 140.dp): Modifier = composed {
    val still = rememberLabStill()
    val a = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { if (!still) { delay(delayMillis.toLong()); a.animateTo(1f, spring(dampingRatio = .78f, stiffness = 360f)) } }
    graphicsLayer { translationY = (1 - a.value) * from.toPx() }
}

/** A money amount that counts from the value it had to the new one (the balance after a recharge). */
@Composable internal fun rememberCountedAmount(target: Long?, millis: Int = 900): Long? {
    val still = rememberLabStill()
    var shown by remember { mutableStateOf(target) }
    val anim = remember { Animatable(0f) }
    var from by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        val start = shown
        if (target == null || start == null || still || start == target) { shown = target; return@LaunchedEffect }
        from = start; anim.snapTo(0f)
        anim.animateTo(1f, tween(millis, easing = LabDecelerate)) { shown = (from!! + (target - from!!) * value).roundToLong() }
        shown = target
    }
    return shown
}

private fun Float.roundToLong() = kotlin.math.round(this.toDouble()).toLong()

/**
 * An odometer: every digit that changes rolls through the numbers in between, right to left, with a firm spring
 * («Recarga hecha»: you see the money go in). [from] is the text before; digits that did not change stay still.
 */
@Composable internal fun LabRollingNumber(text: String, from: String?, style: androidx.compose.ui.text.TextStyle, color: Color) {
    val still = rememberLabStill()
    val before = from?.padStart(text.length)?.takeLast(text.length)
    androidx.compose.foundation.layout.Row {
        text.forEachIndexed { i, ch ->
            if (!ch.isDigit() || before == null || still) { androidx.compose.material3.Text(ch.toString(), style = style, color = color); return@forEachIndexed }
            val f = before[i].takeIf { it.isDigit() }?.digitToInt() ?: ch.digitToInt()
            val steps = (ch.digitToInt() - f + 10) % 10
            if (steps == 0) { androidx.compose.material3.Text(ch.toString(), style = style, color = color); return@forEachIndexed }
            val roll = remember(text) { Animatable(0f) }
            LaunchedEffect(text) { delay(200L + (text.length - 1 - i) * 40L); roll.animateTo(steps.toFloat(), spring(dampingRatio = .7f, stiffness = 225f)) }
            androidx.compose.ui.layout.Layout({
                androidx.compose.foundation.layout.Column { for (s in 0..steps) androidx.compose.material3.Text(((f + s) % 10).toString(), style = style, color = color) }
            }, Modifier.androidxClip()) { measurables, constraints ->
                val column = measurables.first().measure(constraints.copy(maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
                val digit = column.height / (steps + 1)
                layout(column.width, digit) { column.place(0, -(roll.value * digit).roundToInt()) }
            }
        }
    }
}

private fun Modifier.androidxClip() = this.then(Modifier.clipToBounds())
