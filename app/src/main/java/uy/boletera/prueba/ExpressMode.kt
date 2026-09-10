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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** A one-shot shortcut. No activation preference, configuration screen, or change to ordinary recharge. */
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun ExpressShortcut(amount: Long?, enabled: Boolean, onExplain: (() -> Unit)? = null, preparing: Boolean = false, onStart: () -> Unit) {
    val interaction=remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var fired by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val haptic=LocalHapticFeedback.current
    LaunchedEffect(pressed,enabled,preparing) {
        if(!pressed) fired=false
        if(preparing)progress=1f
        else if(pressed && enabled) {
            // Elapsed hold time is functional feedback, independent of Android's animation speed.
            val started=android.os.SystemClock.uptimeMillis()
            var pulses=0
            while(!fired) {
                val elapsed=android.os.SystemClock.uptimeMillis()-started
                // Only the gesture's actual acceptance may finish the circle.
                progress=(elapsed/1200f).coerceIn(0f,0.98f)
                val due=(elapsed/400).toInt().coerceAtMost(2)
                if(due>pulses){haptic.performHapticFeedback(HapticFeedbackType.ContextClick);pulses=due}
                delay(16)
            }
        } else progress=0f
    }
    val scale by animateFloatAsState(if(pressed)0.97f else 1f,spring(dampingRatio=0.65f,stiffness=450f),label="express press")
    val configuration=LocalViewConfiguration.current
    val hold=remember(configuration) { object:ViewConfiguration by configuration { override val longPressTimeoutMillis=1200L } }
    val ink=MaterialTheme.colorScheme.primary
    fun start() { if(enabled && !preparing && !fired) { fired=true;progress=1f;onStart() } }
    CompositionLocalProvider(LocalViewConfiguration provides hold) {
        Surface(color=Panel,shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()
            .graphicsLayer { scaleX=scale;scaleY=scale;shape=RoundedCornerShape(24.dp);clip=true }
            .combinedClickable(interactionSource=interaction,indication=ripple(),enabled=enabled,
                onClick={if(!preparing){haptic.performHapticFeedback(HapticFeedbackType.ContextClick);onExplain?.invoke()}},
                onLongClickLabel="Recargar el mínimo con Prex",onLongClick=::start)
            .testTag("expressShortcut")
            .semantics {
                progressBarRangeInfo=ProgressBarRangeInfo(progress,0f..1f)
                stateDescription=if(preparing)"Preparando la recarga. Ya podés soltar." else if(fired)"Pulsación aceptada" else "Mantené apretado para recargar"
                if(enabled && !preparing) customActions=listOf(CustomAccessibilityAction("Recargar ${Amounts.format(amount)} con Prex") { start();true })
            }) {
            Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                ExpressEnergy(progress,preparing,ink,Modifier.size(56.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    Text(if(preparing)"Preparando tu recarga" else "Modo Express",style=MaterialTheme.typography.titleMedium,color=ink)
                    Text(when {preparing->"Ya podés soltar · conectando con STM";fired->"Listo · ya podés soltar";pressed->"Soltá antes de completar para cancelar";else->"Prex · ${Amounts.format(amount)} · mantené apretado"},style=MaterialTheme.typography.bodySmall,color=Muted)
                }
            }
        }
    }
}

@Composable internal fun ExpressEnergy(progress: Float, preparing: Boolean, ink: Color, modifier: Modifier) {
    val phase=if(preparing) {
        val transition=rememberInfiniteTransition(label="express rays")
        transition.animateFloat(0f,1f,infiniteRepeatable(tween(1000,easing=LinearEasing)),label="charging lines").value
    } else 0f
    Box(modifier,contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val fraction=progress.coerceIn(0f,1f)
            val radius=size.minDimension*0.27f
            for(i in 0 until 12) {
                val angle=i*Math.PI/6
                val wave=if(preparing)((cos((phase-i/12f)*Math.PI*2)+1)/2).toFloat() else fraction
                val start=radius+size.minDimension*0.08f
                val end=start+size.minDimension*0.12f*(if(preparing)0.45f+0.55f*wave else fraction)
                drawLine(ink.copy(alpha=0.12f+wave*0.75f),Offset(center.x+cos(angle).toFloat()*start,center.y+sin(angle).toFloat()*start),Offset(center.x+cos(angle).toFloat()*end,center.y+sin(angle).toFloat()*end),2.dp.toPx(),StrokeCap.Round)
            }
            drawCircle(ink.copy(alpha=0.16f),radius,style=Stroke(2.dp.toPx()))
            drawArc(ink,-90f,360f*fraction,false,Offset(center.x-radius,center.y-radius),Size(radius*2,radius*2),style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
        }
        AppGlyph(Glyph.Arrow,Modifier.size(22.dp).graphicsLayer {rotationZ=-35f*progress},tint=ink)
    }
}
