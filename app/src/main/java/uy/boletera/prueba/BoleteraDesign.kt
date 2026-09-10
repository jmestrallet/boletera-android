package uy.boletera.prueba

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

internal val Ink: Color @Composable get() = MaterialTheme.colorScheme.onBackground
internal val Lime: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
internal val Paper: Color @Composable get() = MaterialTheme.colorScheme.background
internal val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
internal val Panel: Color @Composable get() = MaterialTheme.colorScheme.surface

private val DisplayFamily = FontFamily(Font(R.font.google_sans_regular), Font(R.font.google_sans_medium, FontWeight.Medium), Font(R.font.google_sans_bold, FontWeight.Bold))
private val BodyFamily = FontFamily(Font(R.font.google_sans_text), Font(R.font.google_sans_medium, FontWeight.Medium), Font(R.font.google_sans_bold, FontWeight.Bold))
private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily=DisplayFamily, fontWeight=FontWeight.Medium, fontSize=64.sp, letterSpacing = (-2).sp, lineHeight = 68.sp),
    displayMedium = TextStyle(fontFamily=DisplayFamily, fontWeight=FontWeight.Medium, fontSize=48.sp, letterSpacing = (-1).sp, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontFamily=DisplayFamily, fontWeight=FontWeight.Medium, fontSize=36.sp, letterSpacing = (-0.8).sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily=DisplayFamily, fontWeight=FontWeight.Medium, fontSize=28.sp, letterSpacing = (-0.5).sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily=DisplayFamily, fontWeight=FontWeight.Medium, fontSize=22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily=DisplayFamily, fontWeight=FontWeight.Medium, fontSize=18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily=BodyFamily, fontWeight=FontWeight.Normal, fontSize=16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily=BodyFamily, fontWeight=FontWeight.Normal, fontSize=14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily=BodyFamily, fontWeight=FontWeight.Normal, fontSize=12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily=DisplayFamily, fontWeight=FontWeight.Medium, fontSize=16.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily=BodyFamily, fontWeight=FontWeight.Medium, fontSize=12.sp, lineHeight = 16.sp)
)

@Composable internal fun BoleteraTheme(appearance: String = "system", content: @Composable () -> Unit) {
    val dark = when (appearance) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val targetColors = if (dark) darkColorScheme(
        primary = Color(0xFFB9E991), onPrimary = Color(0xFF193616), primaryContainer = Color(0xFF2D4B2B), onPrimaryContainer = Color(0xFFD6F7AE),
        secondary = Color(0xFFB8CCAE), onSecondary = Color(0xFF243622), secondaryContainer = Color(0xFF334530), onSecondaryContainer = Color(0xFFD7E9CD),
        background = Color(0xFF101610), onBackground = Color(0xFFE3E9DF), surface = Color(0xFF1A221A), onSurface = Color(0xFFE3E9DF),
        surfaceVariant = Color(0xFF2A342A), onSurfaceVariant = Color(0xFFB9C5B5), outline = Color(0xFF85937F), outlineVariant = Color(0xFF3F4A3C),
        error = Color(0xFFFFB4A6), onError = Color(0xFF5D160D), errorContainer = Color(0xFF48221D), onErrorContainer = Color(0xFFFFDAD2)
    ) else lightColorScheme(
        primary = Color(0xFF285B35), onPrimary = Color.White, primaryContainer = Color(0xFFD6F7AE), onPrimaryContainer = Color(0xFF1D3516),
        secondary = Color(0xFF4E6545), onSecondary = Color.White, secondaryContainer = Color(0xFFE3EDDA), onSecondaryContainer = Color(0xFF293B24),
        background = Color(0xFFF5F7F1), onBackground = Color(0xFF19231A), surface = Color.White, onSurface = Color(0xFF19231A),
        surfaceVariant = Color(0xFFE8EDE2), onSurfaceVariant = Color(0xFF56634F), outline = Color(0xFF73806B), outlineVariant = Color(0xFFD9E0D1),
        error = Color(0xFFAC3024), onError = Color.White, errorContainer = Color(0xFFFFE5DE), onErrorContainer = Color(0xFF722316)
    )
    val colors=targetColors.animatedThemeColors()
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = colors.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp)), content = content)
}

/** Draw-phase entrance: no duplicate compositions or recreated browser during transitions. */
internal fun Modifier.pageEntrance(key: String, enabled: Boolean = true): Modifier = composed {
    val enter = remember { Animatable(1f) }
    LaunchedEffect(key, enabled) {
        if (enabled) { enter.snapTo(0f); enter.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        else enter.snapTo(1f)
    }
    graphicsLayer { alpha = enter.value; translationY = (1f - enter.value) * 40.dp.toPx(); scaleX=0.97f+0.03f*enter.value;scaleY=scaleX }
}

@Composable private fun themeColor(target:Color):Color = androidx.compose.animation.animateColorAsState(target,tween(360),label="theme color").value
@Composable private fun ColorScheme.animatedThemeColors():ColorScheme = copy(
    primary=themeColor(primary),onPrimary=themeColor(onPrimary),primaryContainer=themeColor(primaryContainer),onPrimaryContainer=themeColor(onPrimaryContainer),
    secondary=themeColor(secondary),onSecondary=themeColor(onSecondary),secondaryContainer=themeColor(secondaryContainer),onSecondaryContainer=themeColor(onSecondaryContainer),
    background=themeColor(background),onBackground=themeColor(onBackground),surface=themeColor(surface),onSurface=themeColor(onSurface),
    surfaceVariant=themeColor(surfaceVariant),onSurfaceVariant=themeColor(onSurfaceVariant),outline=themeColor(outline),outlineVariant=themeColor(outlineVariant),
    error=themeColor(error),onError=themeColor(onError),errorContainer=themeColor(errorContainer),onErrorContainer=themeColor(onErrorContainer)
)

internal enum class Glyph { Ticket, Arrow, Back, Close, Settings, Check, Lock, Fingerprint, Card, Refresh, Info, Moon, Sun, Phone, Chevron, Eye, EyeOff, Download, Exit }

@Composable internal fun AppGlyph(glyph: Glyph, modifier: Modifier = Modifier.size(24.dp), tint: Color = Ink, label: String? = null) {
    val resource = when(glyph) {
        Glyph.Arrow->R.drawable.ic_arrow_forward; Glyph.Back->R.drawable.ic_arrow_back; Glyph.Close->R.drawable.ic_close
        Glyph.Settings->R.drawable.ic_settings; Glyph.Check->R.drawable.ic_check; Glyph.Lock->R.drawable.ic_lock
        Glyph.Fingerprint->R.drawable.ic_fingerprint; Glyph.Card->R.drawable.ic_credit_card; Glyph.Refresh->R.drawable.ic_refresh
        Glyph.Info->R.drawable.ic_info; Glyph.Moon->R.drawable.ic_dark_mode; Glyph.Sun->R.drawable.ic_light_mode
        Glyph.Phone->R.drawable.ic_smartphone; Glyph.Chevron->R.drawable.ic_chevron_right; Glyph.Eye->R.drawable.ic_visibility
        Glyph.EyeOff->R.drawable.ic_visibility_off; Glyph.Download->R.drawable.ic_download; Glyph.Exit->R.drawable.ic_logout
        Glyph.Ticket->null
    }
    if(resource!=null) { Icon(painterResource(resource),contentDescription=label,modifier=modifier,tint=tint);return }
    Canvas(if(label==null)modifier else modifier.semantics { contentDescription=label }) {
        scale(size.width/24f,size.height/24f,pivot=Offset.Zero) {
            drawRoundRect(tint,Offset(3f,5f),Size(18f,14f),CornerRadius(3f),style=Stroke(1.8f,cap=StrokeCap.Round))
            drawLine(tint,Offset(7f,10f),Offset(17f,10f),1.8f,StrokeCap.Round)
            drawLine(tint,Offset(7f,14f),Offset(13f,14f),1.8f,StrokeCap.Round)
        }
    }
}

@Composable internal fun AppTopBar(onSettings: (() -> Unit)? = null, title: String? = null, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min=72.dp).padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically) {
        if(onBack != null) IconButton(onClick=onBack) { AppGlyph(Glyph.Back,label="Volver") }
        else Surface(color=Lime,shape=RoundedCornerShape(14.dp),modifier=Modifier.size(40.dp)) { Box(contentAlignment=Alignment.Center) { AppGlyph(Glyph.Ticket,tint=MaterialTheme.colorScheme.onPrimaryContainer) } }
        Text(title ?: "boletera",style=if(title==null)MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            modifier=Modifier.weight(1f).padding(start=12.dp),color=Ink)
        if(onSettings!=null) FilledTonalIconButton(onClick=onSettings, colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) { AppGlyph(Glyph.Settings,label="Configuración") }
    }
}

@Composable internal fun Title(text: String) { Text(text,style=MaterialTheme.typography.headlineLarge,color=Ink,modifier=Modifier.semantics { heading() }) }
@Composable internal fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color=Panel,shape=RoundedCornerShape(28.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content) }
}
@Composable internal fun Notice(text: String) {
    Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            AppGlyph(Glyph.Info,tint=MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSecondaryContainer,modifier=Modifier.weight(1f))
        }
    }
}
@Composable internal fun Primary(label: String, enabled: Boolean = true, action: () -> Unit) {
    val interaction=remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val radius by animateDpAsState(if(pressed)16.dp else 32.dp,spring(dampingRatio=0.72f,stiffness=700f),label="button shape")
    val compression by animateFloatAsState(if(pressed)0.94f else 1f,spring(dampingRatio=0.65f,stiffness=500f),label="button press")
    val arrowOffset by animateDpAsState(if(pressed)10.dp else 0.dp,spring(dampingRatio=0.7f,stiffness=550f),label="button arrow")
    Button(onClick={ haptic.performHapticFeedback(HapticFeedbackType.ContextClick); action() },enabled=enabled,interactionSource=interaction,shape=RoundedCornerShape(radius),
        modifier=Modifier.fillMaxWidth().heightIn(min=60.dp).graphicsLayer { scaleX=compression; scaleY=compression },contentPadding=PaddingValues(horizontal=24.dp,vertical=16.dp)) {
        Text(label,style=MaterialTheme.typography.labelLarge,modifier=Modifier.weight(1f))
        Spacer(Modifier.width(12.dp)); AppGlyph(Glyph.Arrow,modifier=Modifier.offset(x=arrowOffset),tint=LocalContentColor.current)
    }
}

@Composable internal fun ProgressSteps(current: Int, labels: List<String> = listOf("Importe", "Medio de pago", "Confirmación")) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i,label ->
            val active=i<=current
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                val color by androidx.compose.animation.animateColorAsState(if(active)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,label="step color")
                Box(Modifier.fillMaxWidth().height(4.dp).background(color,RoundedCornerShape(4.dp)))
                Text(label,style=MaterialTheme.typography.labelMedium,color=if(active)Ink else Muted)
            }
        }
    }
}
