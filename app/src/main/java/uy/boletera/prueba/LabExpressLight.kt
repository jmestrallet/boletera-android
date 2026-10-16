package uy.boletera.prueba

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import kotlin.math.pow

/**
 * Express's moment (express.html): when the bolt fires, a light leaves from under the button, runs the screen's edge
 * both ways, meets at the top in a thin flare while the whole edge breathes once, and drops into the card of the
 * Carga Express wait. Under one second; with animations off it does not run.
 */
internal object LabExpressLight {
    /** Uptime of the shot, or null. */
    var firedAt by mutableStateOf<Long?>(null)
    /** Where the bolt was (window px) and where the waiting card sits, measured by those screens. */
    var from by mutableStateOf(Offset.Unspecified)
    var target by mutableStateOf(Offset.Unspecified)
    fun fire() { firedAt = android.os.SystemClock.uptimeMillis() }
}

private const val RUN_START = 80f; private const val MEET = 500f; private const val HIT = 610f; private const val END = 1100f
private val Teal = Color(0xFF9EE3E0); private val Core = Color(0xFFEAFFFE)
private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun easeInOut(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

@Composable internal fun LabExpressLightOverlay() {
    val fired = LabExpressLight.firedAt ?: return
    val context = LocalContext.current
    val still = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    if (still) { LabExpressLight.firedAt = null; return }
    var t by remember(fired) { mutableFloatStateOf(0f) }
    LaunchedEffect(fired) {
        while (true) {
            withFrameMillis { }
            t = (android.os.SystemClock.uptimeMillis() - fired).toFloat()
            if (t > END) { LabExpressLight.firedAt = null; break }
        }
    }
    LabExpressLightFrame(t)
}

/** One frame of the light, [t] milliseconds after the shot. */
@Composable internal fun LabExpressLightFrame(t: Float) {
    val context = LocalContext.current
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val inset = 1.5f * density
        // The screen's own corner: Android reports it from version 12; otherwise a straight edge without corners.
        val corner = if (android.os.Build.VERSION.SDK_INT >= 31) context.display?.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_LEFT)?.radius else null
        val r = (corner ?: 0).toFloat().coerceAtLeast(0f)
        val bolt = LabExpressLight.from.takeIf { it.isSpecified } ?: Offset(w * .86f, h * .91f)
        val land = LabExpressLight.target.takeIf { it.isSpecified } ?: Offset(w / 2, h * .34f)
        fun half(dir: Int) = Path().apply {
            moveTo(bolt.x, bolt.y + 38f * density)
            lineTo(bolt.x, h - inset)
            if (dir > 0) { lineTo(w - r, h - inset); quadraticTo(w - inset, h - inset, w - inset, h - r); lineTo(w - inset, r); quadraticTo(w - inset, inset, w - r, inset) }
            else { lineTo(r, h - inset); quadraticTo(inset, h - inset, inset, h - r); lineTo(inset, r); quadraticTo(inset, inset, r, inset) }
            lineTo(w / 2, inset)
        }
        val run = easeInOut(seg(t, RUN_START, MEET)); val fade = 1 - seg(t, MEET, MEET + 360f)
        if (t >= RUN_START && fade > 0f) for (dir in listOf(1, -1)) {
            val measure = PathMeasure().apply { setPath(half(dir), false) }
            val head = measure.length * run; val tail = 170f * density
            val piece = Path(); measure.getSegment((head - tail).coerceAtLeast(0f), head, piece, true)
            drawPath(piece, Teal.copy(alpha = .24f * fade), style = Stroke(15f * density, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
            drawPath(piece, Core.copy(alpha = .95f * fade), style = Stroke(3f * density, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
            if (t < MEET) glow(measure.getPosition(head), 28f * density, Color(0xFFCDFFFC), .8f)
        }
        // They meet at the top: a thin flare, and the whole edge breathes once.
        if (t > MEET - 20 && t < MEET + 400) {
            val k = seg(t, MEET - 20, MEET + 380)
            translate(w / 2, inset) { scale(1f, .1f, Offset.Zero) { glow(Offset.Zero, (80f + 220f * (1 - (1 - k).pow(3))) * density, Color(0xFFE1FFFE), 1 - k) } }
            glow(Offset(w / 2, inset), (24f + 36f * k) * density, Color(0xFFE8FFFE), 1 - k)
            drawRoundRect(Teal.copy(alpha = .32f * (1 - k)), Offset(inset, inset), androidx.compose.ui.geometry.Size(w - 2 * inset, h - 2 * inset), CornerRadius(r), style = Stroke(5f * density))
        }
        // The energy drops into the card of the wait.
        if (t > MEET && t < HIT + 120) {
            val k = seg(t, MEET, HIT).pow(3); val x = w / 2 + (land.x - w / 2) * k; val y = inset + (land.y - inset) * k; val f = 1 - seg(t, HIT, HIT + 120)
            drawLine(Brush.verticalGradient(listOf(Teal.copy(alpha = 0f), Core.copy(alpha = f)), (y - 140f * density).coerceAtLeast(inset), y),
                Offset(x, (y - 140f * density).coerceAtLeast(inset)), Offset(x, y), 3f * density, StrokeCap.Round)
            glow(Offset(x, y), 26f * density, Color(0xFFD7FFFC), .9f * f)
        }
        if (t > HIT - 10) glow(land, (20f + 34f * seg(t, HIT, HIT + 260)) * density, Color(0xFFE1FFFE), .85f * (1 - seg(t, HIT, HIT + 300)))
    }
}

private fun DrawScope.glow(at: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0f || radius <= 0f) return
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), at, radius), radius, at, blendMode = BlendMode.Plus)
}
