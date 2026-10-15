package uy.boletera.prueba

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import kotlin.math.*

/** What the sign-in card is showing; it is the sign-in's progress indicator, not decoration. */
internal enum class HeroPhase { IDLE, LOADING, SUCCESS, ERROR }

private val HeroBlue = Color(0xFF1A73D9)
private val HeroEmpty = Color(0xFFD5DEEA)
private val HeroGreen = Color(0xFF2FA35A)
private val HeroRed = Color(0xFFC5483D)
private val HeroRing = Color(0xFF0B111A)
private val HeroWave = Color(0xFF5ED08E)

/** The four slanted bars of the launcher mark (ic_boletera_foreground), in the card's own units. */
private val HeroBars = listOf(
    "M-11.932,4.887 L-6.632,-3.313 A1.45,1.45 0 0,0 -9.068,-4.887 L-14.368,3.313 A1.45,1.45 0 0,0 -11.932,4.887 Z",
    "M-4.932,4.887 L0.368,-3.313 A1.45,1.45 0 0,0 -2.068,-4.887 L-7.368,3.313 A1.45,1.45 0 0,0 -4.932,4.887 Z",
    "M2.068,4.887 L7.368,-3.313 A1.45,1.45 0 0,0 4.932,-4.887 L-0.368,3.313 A1.45,1.45 0 0,0 2.068,4.887 Z",
    "M9.068,4.887 L14.368,-3.313 A1.45,1.45 0 0,0 11.932,-4.887 L6.632,3.313 A1.45,1.45 0 0,0 9.068,4.887 Z",
).map { PathParser().parsePathString(it).toPath() }

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun mixF(a: Float, b: Float, t: Float) = a + (b - a) * t
/** Damped spring step response: firm settle with a small overshoot. */
private fun springStep(t: Float, z: Float = .55f, w: Float = 14f): Float {
    if (t <= 0f) return 0f
    val wd = w * sqrt(1 - z * z)
    return 1 - exp(-z * w * t) * (cos(wd * t) + z / sqrt(1 - z * z) * sin(wd * t))
}

/** Material-style polar shape blending a 9-lobe cookie into a 4-lobe clover. */
private fun loaderPath(radius: Float, blend: Float, rotation: Float): Path {
    val path = Path()
    for (i in 0..120) {
        val t = i / 120f * 2f * PI.toFloat()
        val a = 1 + .13f * cos(9 * (t + rotation)); val b = 1 + .2f * cos(4 * (t + rotation))
        val r = radius * mixF(a, b, blend)
        if (i == 0) path.moveTo(r * cos(t), r * sin(t)) else path.lineTo(r * cos(t), r * sin(t))
    }
    path.close(); return path
}

/**
 * The sign-in card. With [opening] it fades in on launch; [openingFromY] is kept for callers and no longer moves it.
 */
@Composable internal fun LabCardHero(phase: HeroPhase, modifier: Modifier = Modifier, opening: Boolean = false, openingFromY: Float = 0f) {
    val context = LocalContext.current
    val still = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    var now by remember { mutableFloatStateOf(0f) }
    // An infinite animation by design: tests freeze it, the app keeps it running.
    LaunchedEffect(still) { if (!still) while (true) androidx.compose.animation.core.withInfiniteAnimationFrameMillis { now = it / 1000f } }
    var origin by remember { mutableFloatStateOf(-1f) }
    // Keyed on phase so a new phase never draws one frame with the previous phase's clock.
    var phaseStart by remember(phase) { mutableFloatStateOf(-1f) }
    // The first frames after launch are slow; start the clock once they are out of the way so the card glides instead of jumping.
    LaunchedEffect(Unit) { repeat(3) { androidx.compose.animation.core.withInfiniteAnimationFrameMillis { origin = it / 1000f } } }
    LaunchedEffect(phase) { androidx.compose.animation.core.withInfiniteAnimationFrameMillis { phaseStart = it / 1000f } }
    Canvas(modifier) {
        val T = if (still) 0f else now
        val e = if (still) 10f else if (phaseStart < 0f) 0f else (now - phaseStart).coerceAtLeast(0f)
        val o = if (still || !opening) 10f else if (origin < 0f) 0f else (now - origin).coerceAtLeast(0f)
        val k = size.width / 80f // px per card unit; the card is 44 units wide
        var y = 0f; var rot = 0f; var scale = 1f
        var loaderAlpha = 0f; var plus = 1f; var blend = 0f; var wave = -1f; var badge = HeroGreen; var cross = false
        val barColors = MutableList(4) { HeroBlue }
        val drift = if (phase == HeroPhase.LOADING) .4f else 1f
        y += sin(T * 1.3f) * 2.2f * drift; rot += sin(T * .8f) * 1.2f * drift
        when (phase) {
            HeroPhase.IDLE -> Unit
            HeroPhase.LOADING -> {
                y -= sin(clamp01(e / .35f) * PI.toFloat()) * 5f
                for (i in 0..3) {
                    val ph = ((e * 1.6f - i * .22f) % 1f + 1f) % 1f
                    val lit = if (ph < .5f) sin(ph / .5f * PI.toFloat()) else 0f
                    barColors[i] = lerp(HeroEmpty, HeroBlue, if (e < .25f) 1 - e / .25f else lit)
                }
                plus = 1 - clamp01(e / .18f); loaderAlpha = clamp01((e - .1f) / .2f); blend = (sin(e * 4) + 1) / 2
            }
            HeroPhase.SUCCESS -> {
                for (i in 0..3) barColors[i] = lerp(HeroEmpty, HeroBlue, clamp01((e - i * .07f) / .18f))
                loaderAlpha = 1 - clamp01(e / .22f); blend = 1f; plus = springStep(clamp01(e - .12f), .45f, 16f)
                val hop = sin(clamp01((e - .2f) / .45f) * PI.toFloat())
                y -= hop * 9f * (1 - clamp01((e - .2f) / .9f)); scale = 1 + .04f * hop
                wave = clamp01((e - .25f) / .8f)
            }
            HeroPhase.ERROR -> {
                rot += exp(-e * 6) * sin(e * 40) * 4f; badge = HeroRed; cross = true
                plus = if (e > .15f) springStep(e - .15f, .5f, 16f) else 0f
            }
        }
        // App opening: the card simply fades in where it sits, settling from a hair smaller.
        var openAlpha = 1f
        if (o < 1f) {
            val t = clamp01(o / .5f); val eased = 1 - (1 - t) * (1 - t) * (1 - t)
            openAlpha = eased; scale *= mixF(.94f, 1f, eased)
        }
        val cx = size.width / 2; val cy = size.height / 2
        translate(cx, cy + y * k) {
            rotate(rot, Offset.Zero) {
                scale(scale * k, Offset.Zero) {
                    rotate(-10f, Offset.Zero) {
                        drawRoundRect(Color.White.copy(alpha = openAlpha), Offset(-22f, -14f), Size(44f, 28f), CornerRadius(5.5f))
                        HeroBars.forEachIndexed { i, p -> drawPath(p, barColors[i].copy(alpha = openAlpha)) }
                    }
                    drawBadge(Offset(20f, 12f), badge, loaderAlpha, blend, T, plus, cross, wave, openAlpha)
                }
            }
        }
    }
}

private fun DrawScope.drawBadge(at: Offset, fill: Color, loaderAlpha: Float, blend: Float, t: Float, plus: Float, cross: Boolean, wave: Float, alpha: Float) {
    translate(at.x, at.y) {
        if (wave in 0f..0.999f) drawCircle(HeroWave.copy(alpha = (1 - wave) * .9f * alpha), 9f + wave * 26f, Offset.Zero, style = Stroke(2.2f * (1 - wave) + .4f))
        drawCircle(HeroRing.copy(alpha = alpha), 10.2f, Offset.Zero)
        drawCircle(fill.copy(alpha = alpha), 9f, Offset.Zero)
        if (loaderAlpha > 0f) rotate(t * 2.4f * 30f, Offset.Zero) { drawPath(loaderPath(5.2f, blend, t * 2.4f), Color.White.copy(alpha = loaderAlpha * alpha)) }
        if (plus > 0f) scale(plus, Offset.Zero) {
            val s = Stroke(2.3f, cap = StrokeCap.Round)
            if (cross) {
                drawLine(Color.White.copy(alpha = alpha), Offset(-3.6f, -3.6f), Offset(3.6f, 3.6f), 2.3f, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = alpha), Offset(3.6f, -3.6f), Offset(-3.6f, 3.6f), 2.3f, StrokeCap.Round)
            } else {
                drawLine(Color.White.copy(alpha = alpha), Offset(0f, -4.2f), Offset(0f, 4.2f), s.width, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = alpha), Offset(-4.2f, 0f), Offset(4.2f, 0f), s.width, StrokeCap.Round)
            }
        }
    }
}
