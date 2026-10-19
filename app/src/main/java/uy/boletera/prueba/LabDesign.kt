package uy.boletera.prueba

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The new Boletera experience (design, Express, «Ver la página», the use record): born in Boletera Lab and, since
 * 0.3.0 Alpha 1 (owner 19/10), also Boletera Alpha. Which app it is (name, icon, updates) is [IsLabApp] and the channel.
 */
internal val IsLab: Boolean get() = BuildConfig.DISTRIBUTION_CHANNEL == "lab" || BuildConfig.DISTRIBUTION_CHANNEL == "alpha"
/** Boletera Lab itself: the owner's experimental app, installed next to Boletera. */
internal val IsLabApp: Boolean get() = BuildConfig.DISTRIBUTION_CHANNEL == "lab"

/** The Lab home is dark by design, independent of the app theme. */
internal object LabColors {
    val Base = Color(0xFF0A0D13)
    val On = Color(0xFFECEEF6)
    val Muted = Color(0xFFB9C2D3)
    val Faint = Color(0xFF8B95A8)
    val Primary = Color(0xFFB3D0FF)
    val OnPrimary = Color(0xFF08284F)
    val Teal = Color(0xFF9EE3E0)
    val OnTeal = Color(0xFF06312F)
    /** A negative balance: Android's dark error tone, soft enough not to shout. */
    val Rose = Color(0xFFF2B8B5)
    val Warn = Color(0xFFF3C77F)
    val OnWarn = Color(0xFF3B2500)
    val GlassTint = Color.White.copy(alpha = 0.075f)
    val GlassLine = Color.White.copy(alpha = 0.09f)
    /** What a glass panel looks like without blur: a see-through tint, not an opaque surface. */
    val FallbackTint = Color.White.copy(alpha = 0.10f)
}

/**
 * The shared rules (outputs/home-2026/reglas.html), defined once: the card has two sizes, titles three, buttons two.
 * The card's drawing scales with its box (80 × 64 card units), so a fixed box means the very same card on every screen.
 */
internal object LabSizes {
    /** Screens that tell a state: sign-in, connecting, errors, results. */
    val HeroMoment = DpSize(360.dp, 288.dp)
    /** Above the payment steps, with the amount. */
    val HeroHeader = DpSize(132.dp, 106.dp)
}
/** A moment's title (with the big card). */
internal val Typography.labMoment: TextStyle get() = headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp)
/** A sheet's title, or a list screen's. */
internal val Typography.labSheet: TextStyle get() = headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp)
/** A form step's title (card, confirmation, verification). */
internal val Typography.labStep: TextStyle get() = headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp)

/** One radius per angle: [lobes] bumps of relative [depth] (0 = circle). Shared shapes morph by animating [depth]. */
internal fun polarPath(size: Size, lobes: Int, depth: Float, rotation: Float = 0f): Path {
    val path = Path()
    val cx = size.width / 2; val cy = size.height / 2
    val base = min(cx, cy) / (1 + depth)
    val steps = 180
    for (i in 0..steps) {
        val t = i.toFloat() / steps * 2f * PI.toFloat()
        val r = base * (1 + depth * cos(lobes * (t + rotation)))
        val x = cx + r * cos(t - PI.toFloat() / 2); val y = cy + r * sin(t - PI.toFloat() / 2)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Material 3 Expressive–style shape: cookie (12, .085), clover (4, .14), soft burst (7, .07). */
internal class PolarShape(private val lobes: Int, private val depth: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(polarPath(size, lobes, depth))
}

/** The wallpaper-like light behind the Lab home; the glass panels blur it. */
@Composable internal fun LabAmbient(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().drawBehind {
        drawRect(LabColors.Base)
        fun glow(color: Color, x: Float, y: Float, radius: Float) =
            drawRect(Brush.radialGradient(listOf(color, Color.Transparent), Offset(size.width * x, size.height * y), size.width * radius))
        glow(Color(0x8C346ED6), 0.18f, 0.06f, 0.95f)
        glow(Color(0x4D1696A0), 1.0f, 0.42f, 0.70f)
        glow(Color(0x733C5AC8), 0.40f, 1.02f, 1.05f)
    })
}

/** A glass panel: real blur on capable devices, a solid tinted surface otherwise. */
internal fun Modifier.labGlass(state: HazeState, shape: Shape, blur: Boolean): Modifier =
    clip(shape).hazeEffect(state) {
        blurEnabled = blur
        backgroundColor = LabColors.Base
        tints = listOf(HazeTint(LabColors.GlassTint))
        blurRadius = 28.dp
        noiseFactor = 0f
        // Without blur the panel stays see-through: over the smooth ambient light it reads almost like glass.
        fallbackTint = HazeTint(LabColors.FallbackTint)
    }.border(1.dp, LabColors.GlassLine, shape)

internal enum class EffectsMode(val key: String, val label: String) {
    AUTO("auto", "Automático"), ON("on", "Activados"), OFF("off", "Desactivados");
    companion object { fun from(value: String?) = entries.firstOrNull { it.key == value } ?: AUTO }
}

/** Glass costs GPU time, so Automático only turns it on where the phone can afford it. */
internal object VisualEffects {
    private fun preferences(context: Context) = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun mode(context: Context) = EffectsMode.from(preferences(context).getString("visual_effects", null))
    fun setMode(context: Context, mode: EffectsMode) = preferences(context).edit().putString("visual_effects", mode.key).apply()
    /** Backdrop blur needs Android 12's RenderEffect. */
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    fun capable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val manager = context.getSystemService(ActivityManager::class.java) ?: return false
        if (manager.isLowRamDevice) return false
        // People who turn animations off also expect a lighter interface.
        if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return false
        val performanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS
        val memory = ActivityManager.MemoryInfo().also(manager::getMemoryInfo).totalMem
        return performanceClass >= Build.VERSION_CODES.S || memory >= 5_500_000_000L
    }
    fun enabled(context: Context, mode: EffectsMode = mode(context)) = when (mode) {
        EffectsMode.OFF -> false
        EffectsMode.ON -> supported
        EffectsMode.AUTO -> capable(context)
    }
}
