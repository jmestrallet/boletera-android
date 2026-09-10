package uy.boletera.prueba

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ExpressSheet(state: UiState, profiles: List<PayerProfile>, onActivate: (String,String?) -> Boolean,
    onDisable: () -> Unit, onAddPayer: () -> Unit, onClose: () -> Unit) {
    var provider by remember { mutableStateOf(state.express?.provider ?: state.selectedProvider ?: "1033") }
    var payer by remember { mutableStateOf(state.express?.payerId ?: state.payerProfileId ?: profiles.singleOrNull()?.id) }
    var activated by remember { mutableStateOf(false) }
    var activationFailed by remember { mutableStateOf(false) }
    val valid=!state.busy && (provider=="1002" || profiles.any { it.id==payer })
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Paper) {
        Column(Modifier.fillMaxWidth().widthIn(max=560.dp).align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState())
            .padding(horizontal=24.dp).padding(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text(if(activated)"Express activado" else "Menos vueltas.\nMás viaje.",style=MaterialTheme.typography.headlineLarge,color=Ink)
            if(activated) {
                ExpressActivation(enabled=false,activated=true,onActivate={})
                Text("Tu próxima recarga, directo a ${if(provider=="1033")"Prex" else "eBROU"}.",style=MaterialTheme.typography.titleLarge,color=Ink)
                Text("Todavía no se inició ninguna recarga.",color=Muted)
                Primary("Listo, vamos",action=onClose)
            } else {
                Text("MODO EXPRESS",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                Surface(color=Panel,shape=RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("STM · ${state.selectedCard?.takeLast(4).orEmpty()}",style=MaterialTheme.typography.titleMedium,color=Ink)
                        Text("${Amounts.format(state.minimum)} por recarga",style=MaterialTheme.typography.headlineMedium,color=Ink)
                        Text("Siempre el mínimo vigente de STM. Si cambia, vas a ver el nuevo importe antes de recargar.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf("1033" to "Prex","1002" to "eBROU").forEach { (id,label) ->
                        FilterChip(selected=provider==id,onClick={provider=id},label={Text(label)},modifier=Modifier.weight(1f).heightIn(min=48.dp))
                    }
                }
                if(provider=="1033") {
                    Text("Titular de la Prex",style=MaterialTheme.typography.titleMedium,color=Ink)
                    profiles.forEach { profile ->
                        FilterChip(selected=payer==profile.id,onClick={payer=profile.id},label={Text(profile.label)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp))
                    }
                    TextButton(onClick=onAddPayer){Text(if(profiles.isEmpty())"Agregar datos del titular" else "Agregar otro titular")}
                }
                Text("Al recargar, salteás la elección de importe y medio de pago. Completás la autorización en ${if(provider=="1033")"Prex" else "eBROU"}.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                ExpressActivation(enabled=valid,activated=false) { activated=onActivate(provider,payer);activationFailed=!activated }
                if(activationFailed)Text("No se pudo activar Express. Revisá los datos y volvé a intentar.",color=MaterialTheme.colorScheme.error)
                if(!valid && !state.busy)Text("Elegí o agregá un titular para activar Express con Prex.",style=MaterialTheme.typography.bodySmall,color=Muted)
                if(state.express!=null)TextButton(onClick={onDisable();onClose()},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Desactivar Express")}
            }
        }
    }
}

/** A hold enables a preference only. It never initiates a financial operation. */
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun ExpressActivation(enabled: Boolean, activated: Boolean, onActivate: () -> Unit) {
    val interaction=remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic=LocalHapticFeedback.current
    LaunchedEffect(pressed,enabled,activated) {
        if(pressed && enabled && !activated) {
            delay(400); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(400); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val progress by animateFloatAsState(if(activated || pressed && enabled)1f else 0f,
        if(pressed)tween(1200,easing=LinearEasing) else spring(stiffness=550f),label="express energy")
    val scale by animateFloatAsState(if(activated)1f else if(pressed)0.96f else 1f,spring(dampingRatio=0.6f,stiffness=450f),label="express compression")
    val viewConfiguration=LocalViewConfiguration.current
    val holdConfiguration=remember(viewConfiguration) { object:ViewConfiguration by viewConfiguration { override val longPressTimeoutMillis=1200L } }
    val ink=MaterialTheme.colorScheme.onPrimaryContainer
    CompositionLocalProvider(LocalViewConfiguration provides holdConfiguration) {
        Surface(color=Lime,shape=RoundedCornerShape(32.dp),modifier=Modifier.fillMaxWidth().graphicsLayer { scaleX=scale;scaleY=scale;shape=RoundedCornerShape(32.dp);clip=true }
            .combinedClickable(interactionSource=interaction,indication=ripple(),enabled=enabled&&!activated,
                onClick={},onLongClickLabel="Activar modo Express",onLongClick=onActivate)
            .semantics { if(enabled&&!activated)customActions=listOf(CustomAccessibilityAction("Activar modo Express") { onActivate();true }) }) {
            Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(88.dp),contentAlignment=Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val fraction=progress.coerceIn(0f,1f)
                        val radius=size.minDimension*0.33f
                        for(i in 0 until 12) {
                            val angle=i*Math.PI/6
                            val start=radius+9.dp.toPx()
                            val end=start+fraction*8.dp.toPx()
                            drawLine(ink.copy(alpha=0.12f+fraction*0.45f),Offset(center.x+cos(angle).toFloat()*start,center.y+sin(angle).toFloat()*start),Offset(center.x+cos(angle).toFloat()*end,center.y+sin(angle).toFloat()*end),2.dp.toPx(),StrokeCap.Round)
                        }
                        drawCircle(ink.copy(alpha=0.12f),radius,style=Stroke(3.dp.toPx()))
                        drawArc(ink,-90f,360f*fraction,false,Offset(center.x-radius,center.y-radius),Size(radius*2,radius*2),style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
                    }
                    AppGlyph(if(activated)Glyph.Check else Glyph.Arrow,Modifier.size(30.dp).graphicsLayer { rotationZ=if(activated)0f else -35f*progress },tint=ink)
                }
                Text(if(activated)"EXPRESS · ON" else if(pressed)"Cargando Express…" else "Mantené para activar",style=MaterialTheme.typography.titleMedium,color=ink)
                Text(if(activated)"Todo listo." else "Soltá antes para cancelar",style=MaterialTheme.typography.bodySmall,color=ink)
            }
        }
    }
}
