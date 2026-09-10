package uy.boletera.prueba

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** A one-shot shortcut. No activation preference, configuration screen, or change to ordinary recharge. */
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun ExpressShortcut(amount: Long?, enabled: Boolean, onExplain: (() -> Unit)? = null, onStart: () -> Unit) {
    val interaction=remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var fired by remember { mutableStateOf(false) }
    val haptic=LocalHapticFeedback.current
    LaunchedEffect(pressed,enabled) {
        if(!pressed) fired=false
        if(pressed && enabled) {
            delay(400);haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            delay(400);haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
    val progress by animateFloatAsState(if(pressed && enabled)1f else 0f,
        if(pressed)tween(1200,easing=LinearEasing) else spring(stiffness=550f),label="express energy")
    val scale by animateFloatAsState(if(pressed)0.97f else 1f,spring(dampingRatio=0.65f,stiffness=450f),label="express press")
    val configuration=LocalViewConfiguration.current
    val hold=remember(configuration) { object:ViewConfiguration by configuration { override val longPressTimeoutMillis=1200L } }
    val ink=MaterialTheme.colorScheme.primary
    fun start() { if(enabled && !fired) { fired=true;onStart() } }
    CompositionLocalProvider(LocalViewConfiguration provides hold) {
        Surface(color=Panel,shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()
            .graphicsLayer { scaleX=scale;scaleY=scale;shape=RoundedCornerShape(24.dp);clip=true }
            .combinedClickable(interactionSource=interaction,indication=ripple(),enabled=enabled,
                onClick={haptic.performHapticFeedback(HapticFeedbackType.ContextClick);onExplain?.invoke()},
                onLongClickLabel="Recargar el mínimo con Prex",onLongClick=::start)
            .semantics { if(enabled) customActions=listOf(CustomAccessibilityAction("Recargar ${Amounts.format(amount)} con Prex") { start();true }) }) {
            Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(42.dp),contentAlignment=Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val radius=size.minDimension*0.43f
                        drawCircle(ink.copy(alpha=0.16f),radius,style=Stroke(2.dp.toPx()))
                        drawArc(ink,-90f,360f*progress.coerceIn(0f,1f),false,Offset(center.x-radius,center.y-radius),Size(radius*2,radius*2),style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
                    }
                    AppGlyph(Glyph.Arrow,Modifier.size(22.dp).graphicsLayer {rotationZ=-35f*progress},tint=ink)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text("Modo Express",style=MaterialTheme.typography.titleMedium,color=ink)
                    Text(if(pressed)"Soltá para cancelar" else "Prex · ${Amounts.format(amount)} · mantené apretado",style=MaterialTheme.typography.bodySmall,color=Muted)
                }
            }
        }
    }
}
