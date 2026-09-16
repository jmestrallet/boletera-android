package uy.boletera.prueba

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** A one-shot shortcut. No activation preference, configuration screen, or change to ordinary recharge. */
@Composable internal fun ExpressShortcut(amount: Long?, enabled: Boolean, preparing: Boolean = false, providerName: String = "Prex", onStart: () -> Unit) {
    val interaction=remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic=LocalHapticFeedback.current
    val scale by animateFloatAsState(if(pressed)0.97f else 1f,spring(dampingRatio=0.65f,stiffness=450f),label="express press")
    val ink=MaterialTheme.colorScheme.primary
    val progress=if(preparing)1f else 0f
    Surface(color=Panel,shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()
        .graphicsLayer { scaleX=scale;scaleY=scale;shape=RoundedCornerShape(24.dp);clip=true }
        .clickable(interactionSource=interaction,indication=ripple(),enabled=enabled&&!preparing,role=Role.Button,
            onClickLabel="Recargar ${Amounts.format(amount)} con $providerName") {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onStart()
            }
        .testTag("expressShortcut")
        .semantics {
            progressBarRangeInfo=ProgressBarRangeInfo(progress,0f..1f)
            stateDescription=if(preparing)"Preparando la recarga" else "Tocá una vez para recargar el mínimo"
        }) {
        Row(Modifier.padding(horizontal=18.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            ExpressEnergy(progress,preparing,ink,Modifier.size(56.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                Text(if(preparing)"Preparando tu recarga" else "Carga Express",style=MaterialTheme.typography.titleMedium,color=ink)
                Text(if(preparing)"Abriendo Prex y completando tus datos" else "$providerName · ${Amounts.format(amount)} · tocá para empezar",style=MaterialTheme.typography.bodySmall,color=Muted)
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
