package uy.boletera.prueba

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize
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
 * one screen hands over to the next, a copy flies from the old place to the new one while the new place waits empty.
 *
 * Everything here is read while drawing, not while composing: the trip starts in the same frame the new screen is laid
 * out (no blank frame between the two cards), and each frame only redraws the copy (no stutter from rebuilding it).
 */
internal object LabCardFlight {
    class Slot(val id: Any, var rect: Rect, var spot: CardSpot, var phase: HeroPhase)
    class Flight(val from: Rect, val fromSpot: CardSpot, val to: Slot, var start: Float, val badgeFrom: Rect? = null)
    private val live = mutableListOf<Slot>()
    private var gone: Pair<Slot, Long>? = null
    private var badgeSource: Pair<Rect, Long>? = null
    /**
     * The trip in course, or null. Observed while drawing: the slots redraw the moment it starts and the moment it ends
     * (a plain field left the landed card hidden until something else redrew its screen).
     */
    var flight by mutableStateOf<Flight?>(null)
    /** Animation clock in seconds, advanced by the overlay every frame. */
    var now = 0f
    /** Only the window that draws the flying copy turns flights on (screens shown alone, as in tests, keep their card). */
    var enabled = false
    /** A full screen (Configuración) is on top: no trip starts, and one in course lands at once, out of sight. */
    var covered = false
        set(value) { field = value; if (value) flight = null }
    const val LENGTH = .62f

    fun progress(f: Flight) = if (f.start < 0f) 0f else (now - f.start).coerceAtLeast(0f)
    /** The copy flying now draws this slot's card, so the slot itself stays empty until it lands. */
    fun hides(id: Any): Boolean {
        val f = flight ?: return false
        return f.to.id == id && progress(f) < LENGTH
    }
    /** «Pagar» was just pressed here: the next trip of the card brings its circle from this button. */
    fun badgeFrom(rect: Rect) { badgeSource = rect to SystemClock.uptimeMillis() }

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
        // The same place (a change of state where the card stays): no trip at all.
        if (!enabled || covered || hypot(from.center.x - to.rect.center.x, from.center.y - to.rect.center.y) < 2f && fromSpot == to.spot) return
        val badge = badgeSource?.takeIf { SystemClock.uptimeMillis() - it.second < 900 }?.first
        badgeSource = null
        // The clock starts at the first frame that actually draws it: building the next screen can take a few frames on
        // a slow phone, and the trip must not have advanced in the meantime.
        flight = Flight(from, fromSpot, to, -1f, badge)
    }
}

/** The place of the card on a screen; the card itself is drawn here, except while a copy is flying in. */
@Composable internal fun LabCardSlot(phase: HeroPhase, spot: CardSpot, modifier: Modifier = Modifier, opening: Boolean = false) {
    val id = remember { Any() }
    DisposableEffect(id) { onDispose { LabCardFlight.leave(id) } }
    SideEffect { LabCardFlight.update(id, phase) }
    // The whole box, not boundsInWindow(): that one is clipped by the parent, and a card bigger than its room (the
    // 288 dp moment in a shorter space) landed off-centre and jumped at the end of the trip.
    Box(modifier.onGloballyPositioned { val p = it.positionInWindow(); LabCardFlight.arrive(id, Rect(p, it.size.toSize()), spot, phase) }
        // Decided while drawing, in the same frame the trip starts or ends: never a frame with no card or with two.
        .drawWithContent { if (!LabCardFlight.hides(id)) drawContent() }) {
        if (spot == CardSpot.CHIP) LabMiniCard(Modifier.fillMaxSize()) else LabCardHero(phase, Modifier.fillMaxSize(), opening = opening)
    }
}

/** px per card unit for a slot: the hero shows the card 44 of 80 units wide, the pill 44 of 52. */
private fun unitOf(spot: CardSpot, r: Rect) =
    if (spot == CardSpot.CHIP) minOf(r.width / 52f, r.height / 36f) else minOf(r.width / 80f, r.height / 64f)

/** Damped spring step (the prototype's): firm, settles in about a third of a second with a small overshoot. */
private fun springAt(t: Float, z: Float = .74f, w: Float = 19f): Float {
    if (t <= 0f) return 0f
    val wd = w * kotlin.math.sqrt(1 - z * z)
    return 1 - kotlin.math.exp(-z * w * t) * (kotlin.math.cos(wd * t) + z / kotlin.math.sqrt(1 - z * z) * kotlin.math.sin(wd * t))
}

/** Draws the flying copy over everything, in window coordinates; put it at the root of the window. */
@Composable internal fun LabCardFlightOverlay() {
    val still = rememberLabStill()
    DisposableEffect(still) { LabCardFlight.enabled = !still; onDispose { LabCardFlight.enabled = false; LabCardFlight.flight = null } }
    // The clock: one tick per frame redraws this layer only (the screens underneath are not rebuilt).
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(still) {
        if (!still) while (true) withInfiniteAnimationFrameMillis {
            LabCardFlight.now = LabCardClock.seconds(it); tick++
            // The trip ends here, once per frame and outside drawing; the landed slot is redrawn with its card.
            LabCardFlight.flight?.let { f ->
                if (f.start < 0f) f.start = LabCardFlight.now
                else if (LabCardFlight.progress(f) >= LabCardFlight.LENGTH) LabCardFlight.flight = null
            }
        }
    }
    val density = LocalDensity.current
    // Where this layer starts in the window (below the status bar on some phones): the slots report window positions.
    val origin = remember { arrayOf(Offset.Zero) }
    Canvas(Modifier.fillMaxSize().onGloballyPositioned { origin[0] = it.positionInWindow() }) {
        tick
        val f = LabCardFlight.flight ?: return@Canvas
        val t = LabCardFlight.progress(f)
        if (t >= LabCardFlight.LENGTH) return@Canvas
        val to = f.to
        val x = springAt(t); val before = springAt((t - 1 / 60f).coerceAtLeast(0f))
        val shift = origin[0]
        val c0 = f.from.center - shift; val c1 = to.rect.center - shift
        val dist = hypot(c1.x - c0.x, c1.y - c0.y)
        // An arc only on a real trip; a short move (two screens that put the card almost in the same place) just slides.
        val arc = minOf(28.dp.toPx(), dist * .18f) * sin(PI.toFloat() * x.coerceIn(0f, 1f))
        val c = Offset(c0.x + (c1.x - c0.x) * x, c0.y + (c1.y - c0.y) * x - arc)
        val k0 = unitOf(f.fromSpot, f.from); val k1 = unitOf(to.spot, to.rect); val k = k0 + (k1 - k0) * x
        // It leans toward where it is going and straightens as it lands.
        val lean = ((x - before) * (c1.x - c0.x) / 18.dp.toPx()).coerceIn(-9f, 9f)
        val fromChip = f.fromSpot == CardSpot.CHIP; val toChip = to.spot == CardSpot.CHIP
        val badge = when {
            f.badgeFrom != null -> if (x > .97f) 1f else 0f
            fromChip && toChip -> 0f
            toChip -> 1 - (x / .25f).coerceIn(0f, 1f)
            fromChip -> ((x - .6f) / .4f).coerceIn(0f, 1f)
            else -> 1f
        }
        val w = 80f * k; val h = 64f * k
        val e = if (LabCardClock.phase == to.phase) (LabCardFlight.now - LabCardClock.since).coerceAtLeast(0f) else 10f
        translate(c.x - w / 2, c.y - h / 2) {
            rotate(lean, Offset(w / 2, h / 2)) {
                withSize(w, h) { drawLabCard(to.phase, LabCardFlight.now, e, 10f, badge) }
            }
        }
        // «Pagar»: the pill closes into a circle where it is, then flies up in an arc and lands as the card's circle.
        val b = f.badgeFrom?.translate(-shift)
        if (b != null && x <= .97f) {
            val target = Offset(c1.x + 24f * k1, c1.y + 6f * k1)
            val close = (t / .18f).coerceIn(0f, 1f); val go = ((x - .1f) / .9f).coerceIn(0f, 1.1f)
            val d0 = b.height; val d1 = 18f * k1
            val wNow = if (go > 0f) d0 + (d1 - d0) * go else b.width + (d0 - b.width) * close
            val hNow = if (go > 0f) wNow else d0
            val lift = 40.dp.toPx() * sin(PI.toFloat() * go.coerceIn(0f, 1f))
            val center = Offset(b.center.x + (target.x - b.center.x) * go, b.center.y + (target.y - b.center.y) * go - lift)
            val ink = androidx.compose.ui.graphics.lerp(LabColors.Primary, Color(0xFF2FA35A), (t / .15f).coerceIn(0f, 1f))
            drawRoundRect(ink, Offset(center.x - wNow / 2, center.y - hNow / 2), Size(wNow, hNow), CornerRadius(hNow / 2))
        }
    }
}

/** Draws [block] as if the canvas were [w] × [h] (the card's drawing sizes itself from the canvas). */
private inline fun DrawScope.withSize(w: Float, h: Float, block: DrawScope.() -> Unit) {
    val previous = drawContext.size
    drawContext.size = Size(w, h)
    try { block() } finally { drawContext.size = previous }
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

/** A button that answers the finger: it sinks a little while held and springs back when let go. */
internal fun Modifier.labPress(interaction: androidx.compose.foundation.interaction.InteractionSource, round: Boolean = true): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val still = rememberLabStill()
    // How deep the finger has pushed it: quick at first, then slower and slower while held (it never quite stops, so
    // it keeps feeling pressed); on release it springs back past its size and settles.
    val depth = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (still) { depth.snapTo(if (pressed) .4f else 0f); return@LaunchedEffect }
        if (pressed) depth.animateTo(1f, tween(1400, easing = CubicBezierEasing(.1f, .8f, .2f, 1f)))
        else depth.animateTo(0f, spring(dampingRatio = .42f, stiffness = 380f))
    }
    graphicsLayer { val s = 1f - .08f * depth.value; scaleX = s; scaleY = s }
        .drawWithContent {
            drawContent()
            // It also darkens a little, as if it went in.
            val a = (.16f * depth.value).coerceIn(0f, .16f)
            if (a > 0f) drawRoundRect(Color.Black.copy(alpha = a), cornerRadius = CornerRadius(if (round) size.height / 2 else 0f))
        }
}

/**
 * A sheet pulled up past its top stretches with more and more resistance and springs back on release, like Android's
 * own stretch at the end of a list. Pulling down is left to the sheet (it closes as always).
 */
internal fun Modifier.labStretchUp(fill: Color = Color.Transparent): Modifier = composed {
    val pull = remember { Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var travel = 0f; var stretching = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                travel += change.position.y - change.previousPosition.y
                if (!stretching) {
                    if (travel < -viewConfiguration.touchSlop) stretching = true
                    else if (travel > viewConfiguration.touchSlop) break
                }
                if (stretching) {
                    // More pull, less stretch: it approaches a limit and never reaches it.
                    val reach = (-travel).coerceAtLeast(0f); val limit = 48.dp.toPx()
                    val stretched = limit * (1f - 1f / (reach / limit * .6f + 1f))
                    scope.launch { pull.snapTo(stretched) }
                    change.consume()
                }
            }
            if (stretching) scope.launch { pull.animateTo(0f, spring(dampingRatio = .55f, stiffness = 420f)) }
        }
    }.graphicsLayer {
        // It rises without deforming the text; the strip it uncovers below is painted in its own colour (no gap).
        translationY = -pull.value
    }.drawBehind {
        if (pull.value > 0f) drawRect(fill, Offset(0f, size.height - 1f), Size(size.width, pull.value + 1f))
    }
}

/**
 * Pull to close for the panels the app draws itself (the amount panel, which is the Recargar button grown, and the
 * sign-in sheet), through Compose's own nested scroll, the way Google's sheets do it: pulled down it follows the
 * finger and closes past a third of its height or with a quick flick, otherwise it springs back; pulled up it
 * stretches with more and more resistance and springs back. Scrolling content inside still scrolls first.
 */
@Stable internal class LabPull(private val scope: kotlinx.coroutines.CoroutineScope, private val close: () -> Unit, private val limit: Float,
    /** Whether it goes back up when closing (the amount panel shrinks into the button), or keeps falling (a sheet sliding away). */
    private val settleOnClose: Boolean) {
    /** How far it is pulled: positive down (px), negative stretched up. */
    val offset = Animatable(0f)
    /** The panel's height, measured by it. */
    var height = 1f
    private var up = 0f
    private fun stretch(reach: Float) = limit * (1f - 1f / (reach / limit * .6f + 1f))
    private fun snap(value: Float) { scope.launch { offset.snapTo(value) } }
    val connection = object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
            if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) return Offset.Zero
            val y = available.y; val now = offset.value
            // Going back the other way first undoes the pull, before the content scrolls.
            if (now > 0f && y < 0f) { val used = maxOf(y, -now); snap(now + used); return Offset(0f, used) }
            if (now < 0f && y > 0f) { up = (up - y).coerceAtLeast(0f); snap(-stretch(up)); return Offset(0f, y) }
            return Offset.Zero
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
            if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) return Offset.Zero
            val y = available.y
            if (y > 0f) { snap(offset.value + y); return Offset(0f, y) }
            if (y < 0f) { up += -y; snap(-stretch(up)); return Offset(0f, y) }
            return Offset.Zero
        }
        override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
            up = 0f
            val now = offset.value
            if (now == 0f) return androidx.compose.ui.unit.Velocity.Zero
            if (now > 0f && (now > height / 3f || available.y > 1400f)) { close(); if (!settleOnClose) return available }
            scope.launch { offset.animateTo(0f, spring(dampingRatio = if (now < 0f) .55f else .8f, stiffness = if (now < 0f) 420f else 380f), initialVelocity = available.y) }
            return available
        }
    }
}

@Composable internal fun rememberLabPull(onClose: () -> Unit, settleOnClose: Boolean = true): LabPull {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val close by rememberUpdatedState(onClose)
    val limit = with(androidx.compose.ui.platform.LocalDensity.current) { 48.dp.toPx() }
    return remember(scope, limit) { LabPull(scope, { close() }, limit, settleOnClose) }
}
