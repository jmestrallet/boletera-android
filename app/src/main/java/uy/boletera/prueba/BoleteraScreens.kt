package uy.boletera.prueba

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable internal fun WelcomeHero() {
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Surface(color=Highlight,shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp,bottomEnd=64.dp,bottomStart=32.dp)) {
            Box(Modifier.fillMaxWidth().height(144.dp).padding(24.dp)) {
                val (main,green,teal)=TransitLines
                val stopFill=Highlight
                val stopRing=MaterialTheme.colorScheme.onPrimaryContainer
                Canvas(Modifier.fillMaxSize()) {
                    val w=size.width; val h=size.height
                    val line=Stroke(8.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round)
                    drawPath(Path().apply { moveTo(w*0.10f,-24.dp.toPx());lineTo(w*0.10f,h*0.12f);lineTo(w*0.22f,h*0.38f);lineTo(w+24.dp.toPx(),h*0.38f) },green.copy(alpha=0.9f),style=line)
                    drawPath(Path().apply { moveTo(w+24.dp.toPx(),h*0.70f);lineTo(w*0.84f,h*0.70f);lineTo(w*0.76f,h*0.38f);lineTo(w*0.76f,-24.dp.toPx()) },teal.copy(alpha=0.9f),style=line)
                    drawPath(Path().apply { moveTo(-24.dp.toPx(),h*0.86f);lineTo(w*0.30f,h*0.86f);lineTo(w*0.42f,h*1.0f+24.dp.toPx()) },main.copy(alpha=0.9f),style=line)
                    listOf(Offset(w*0.10f,h*0.12f),Offset(w*0.92f,h*0.70f),Offset(w*0.18f,h*0.86f)).forEach {
                        drawCircle(stopFill,6.dp.toPx(),it); drawCircle(stopRing,6.dp.toPx(),it,style=Stroke(3.dp.toPx()))
                    }
                    val interchange=Offset(w*0.76f,h*0.38f)
                    drawCircle(stopFill,10.dp.toPx(),interchange); drawCircle(stopRing,10.dp.toPx(),interchange,style=Stroke(3.dp.toPx()))
                }
                BrandTile(Modifier.align(Alignment.Center).size(80.dp))
            }
        }
        Title("Tu próximo viaje\nempieza acá.")
        Text("Consultá tu saldo y recargá tu STM,\nsin las vueltas.",color=Muted,style=MaterialTheme.typography.bodyLarge)
    }
}

@Composable internal fun WalletHome(state: UiState, onChangeCard: () -> Unit, onCharge: () -> Unit, onRefresh: () -> Unit,
    onExpressCharge: (() -> Unit)? = null, onTicketGuide: (() -> Unit)? = null, expressPreparing: Boolean = false, expressProvider: String = "Prex",
    onPaymentReviewed: (() -> Unit)? = null, onActivity: (() -> Unit)? = null) {
    val colors=MaterialTheme.colorScheme
    val haptic=LocalHapticFeedback.current
    fun chooseCard() { haptic.performHapticFeedback(HapticFeedbackType.ContextClick);onChangeCard() }
    Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Text("STM · ${state.selectedCard?.takeLast(4).orEmpty()}",style=MaterialTheme.typography.bodyMedium,color=Muted)
        Surface(color=Highlight,shape=RoundedCornerShape(32.dp),modifier=Modifier.semantics {
            customActions=listOf(CustomAccessibilityAction("Actualizar saldo") { if (!state.busy) { onRefresh(); true } else false })
        }) {
            Column(Modifier.fillMaxWidth().padding(28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Saldo disponible",style=MaterialTheme.typography.titleMedium,color=colors.tertiary,modifier=Modifier.weight(1f))
                    IconButton(onClick=::chooseCard,enabled=!state.busy) { AppGlyph(Glyph.Swap,label="Cambiar boletera",tint=colors.tertiary) }
                }
                Text(Amounts.format(state.balance),style=if(Amounts.format(state.balance).length>10)MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                    color=colors.onPrimaryContainer,modifier=Modifier.fillMaxWidth())
                Text(if((state.balance?:0)<0) "Saldo pendiente de cubrir" else "Informado por STM",style=MaterialTheme.typography.bodyMedium,color=colors.tertiary)
                HorizontalDivider(color=colors.tertiary.copy(alpha=0.28f))
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    AppGlyph(Glyph.Refresh,Modifier.size(16.dp),tint=colors.tertiary)
                    Text(if(state.busy) "Actualizando…" else state.consultedAt?.let { "Actualizado a las ${SimpleDateFormat("HH:mm",Locale.forLanguageTag("es-UY")).format(Date(it))}" } ?: "Deslizá hacia abajo para actualizar",
                        style=MaterialTheme.typography.bodySmall,color=colors.tertiary)
                }
            }
        }
            if(state.paymentStatus.isNotBlank()) Surface(color=Panel,shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(state.paymentStatus,color=Ink,style=MaterialTheme.typography.bodyMedium)
                    if(onPaymentReviewed!=null)TextButton(onClick=onPaymentReviewed,enabled=!state.busy) {Text("Ya revisé el pago")}
                }
            }
            Primary("Recargar boletera",enabled=!state.busy && !state.paymentNeedsReview && state.minimum!=null,action=onCharge)
            if(onExpressCharge!=null) ExpressShortcut(state.minimum,enabled=!state.busy,preparing=expressPreparing,providerName=expressProvider,onStart=onExpressCharge)
        if(onActivity!=null) Surface(color=Panel,shape=RoundedCornerShape(24.dp)) {
            Row(Modifier.fillMaxWidth().clickable(enabled=!state.busy,onClick=onActivity).padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Actividad",style=MaterialTheme.typography.titleMedium,color=Ink)
                    Text("Movimientos y Usuario frecuente",style=MaterialTheme.typography.bodySmall,color=Muted)
                }
                AppGlyph(Glyph.Chevron,label="Ver actividad")
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Surface(color=Panel,shape=RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Surface(color=colors.secondaryContainer,shape=RoundedCornerShape(14.dp),modifier=Modifier.size(44.dp)) { Box(contentAlignment=Alignment.Center) { AppGlyph(Glyph.Ticket,tint=colors.onSecondaryContainer) } }
                    Column(Modifier.weight(1f)) {
                        Text("Recarga mínima",style=MaterialTheme.typography.bodyMedium,color=Muted)
                        Text(Amounts.format(state.minimum),style=MaterialTheme.typography.titleLarge,color=Ink)
                    }
                }
            }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.Top) {
                AppGlyph(Glyph.Info,Modifier.size(16.dp),tint=Muted)
                Text("Los viajes de las últimas 72 horas pueden estar pendientes de descuento.",style=MaterialTheme.typography.bodySmall.copy(fontSize=11.sp,lineHeight=14.sp),color=Muted,modifier=Modifier.weight(1f))
            }
            if(onTicketGuide!=null) Row(
                Modifier.width(90.dp).heightIn(min=48.dp).clickable(onClickLabel="Abrir guía",role=Role.Button,onClick=onTicketGuide)
                    .semantics(mergeDescendants=true) {contentDescription="Boletos y tarifas"},
                horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.Top) {
                AppGlyph(Glyph.Info,Modifier.size(16.dp),tint=Muted)
                Text("Boletos y\ntarifas",color=Muted,style=MaterialTheme.typography.bodySmall.copy(fontSize=11.sp,lineHeight=14.sp),modifier=Modifier.weight(1f))
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun AmountSheet(state: UiState, onClose: () -> Unit, onChoose: (Long) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var selected by remember(state.minimum) { mutableStateOf(state.minimum) }
    val amount=if(custom)Amounts.parse(text) else selected
    val haptic=LocalHapticFeedback.current
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Paper) {
        Column(Modifier.fillMaxWidth().widthIn(max=560.dp).align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).imePadding().padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Elegí el importe",style=MaterialTheme.typography.headlineMedium,color=Ink)
                Text("Mínimo ${Amounts.format(state.minimum)} · STM ${state.selectedCard?.takeLast(4).orEmpty()}",style=MaterialTheme.typography.bodyMedium,color=Muted)
            }
            val choices=(listOfNotNull(state.minimum)+listOf(50000L,100000L).filter { it>(state.minimum?:Long.MAX_VALUE) }).distinct().take(3)
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                choices.forEachIndexed { i,value ->
                    val active=!custom&&selected==value
                    val fill by animateColorAsState(if(active)Highlight else Panel,spring(stiffness=650f),label="amount surface")
                    val radius by animateDpAsState(if(active)32.dp else 12.dp,spring(dampingRatio=0.7f,stiffness=500f),label="amount shape")
                    val checkScale by animateFloatAsState(if(active)1f else 0f,spring(dampingRatio=0.6f,stiffness=500f),label="amount check")
                    Surface(color=fill,shape=RoundedCornerShape(radius),modifier=Modifier.fillMaxWidth().selectable(active,enabled=!state.busy,role=Role.RadioButton) {
                        if(!active) { custom=false; selected=value; haptic.performHapticFeedback(HapticFeedbackType.ContextClick) }
                    }) {
                        Row(Modifier.padding(horizontal=20.dp,vertical=18.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(Amounts.format(value),style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f),color=if(active)MaterialTheme.colorScheme.onPrimaryContainer else Ink)
                            if(i==0)Text("Mínimo",style=MaterialTheme.typography.bodyMedium,color=if(active)MaterialTheme.colorScheme.onPrimaryContainer else Muted,modifier=Modifier.padding(end=16.dp))
                            AppGlyph(Glyph.Check,modifier=Modifier.graphicsLayer { scaleX=checkScale; scaleY=checkScale; alpha=checkScale.coerceIn(0f,1f) },tint=MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                TextButton(onClick={custom=!custom},modifier=Modifier.fillMaxWidth()) { Text(if(custom)"Usar un importe sugerido" else "Elegir otro importe") }
                AnimatedVisibility(custom) {
                    OutlinedTextField(text,{text=it},label={Text("Importe en pesos")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth(),
                        isError=text.isNotEmpty()&&!Amounts.valid(amount,state.minimum),supportingText={ if(text.isNotEmpty()&&!Amounts.valid(amount,state.minimum))Text("Ingresá al menos ${Amounts.format(state.minimum)}.") })
                }
            }
            if(Amounts.valid(amount,state.minimum)&&state.balance!=null) Text("Saldo estimado después: ${Amounts.format(state.balance+amount!!)}",style=MaterialTheme.typography.bodyMedium,color=Muted)
            Primary("Continuar",enabled=Amounts.valid(amount,state.minimum)&&!state.busy) { amount?.let(onChoose) }
        }
    }
}

@Composable internal fun LoadingState(title: String, description: String, express: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(vertical=32.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Surface(color=Highlight,shape=RoundedCornerShape(28.dp),modifier=Modifier.size(88.dp)) {
            Box(contentAlignment=Alignment.Center) {
                if(express)ExpressEnergy(1f,true,MaterialTheme.colorScheme.onPrimaryContainer,Modifier.size(80.dp))
                else CircularProgressIndicator(Modifier.size(40.dp),strokeWidth=3.dp,color=MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Title(title)
        Text(description,style=MaterialTheme.typography.bodyLarge,color=Muted)
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth(0.82f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(8.dp)))
            Box(Modifier.fillMaxWidth(0.55f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(8.dp)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun SettingsSheet(updates: AppUpdates, appearance: String, onAppearance: (String) -> Unit, hasSavedAccess: Boolean,
    canLogout: Boolean, diagnostic: String, onForget: () -> Unit, onLogout: () -> Unit, onInstall: () -> Unit, onClose: () -> Unit,
    onTicketGuide: (() -> Unit)? = null, effectsMode: EffectsMode? = null, onEffectsMode: (EffectsMode) -> Unit = {}) {
    var details by remember { mutableStateOf(false) }
    var copied by remember(diagnostic) { mutableStateOf(false) }
    val haptic=LocalHapticFeedback.current
    val view=LocalView.current
    val vibration=remember(view.context) {AppHaptics(view.context)}
    var pulseResult by remember { mutableStateOf<PulseResult?>(null) }
    var vibrationEnabled by remember {mutableStateOf(vibration.enabled)}
    var labWarning by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Paper) {
        Column(Modifier.fillMaxWidth().widthIn(max=560.dp).align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            Text("Configuración",style=MaterialTheme.typography.headlineLarge,color=Ink)
            if(onTicketGuide!=null)TextButton(onClick=onTicketGuide) {
                AppGlyph(Glyph.Ticket,tint=LocalContentColor.current);Spacer(Modifier.width(8.dp));Text("Boletos y tarifas")
            }
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Apariencia",style=MaterialTheme.typography.titleMedium,color=Ink)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("system","Sistema",Glyph.Phone),Triple("light","Claro",Glyph.Sun),Triple("dark","Oscuro",Glyph.Moon)).forEach { (value,label,glyph) ->
                        val selectionRadius by animateDpAsState(if(appearance==value)28.dp else 20.dp,spring(dampingRatio=0.7f,stiffness=550f),label="appearance shape")
                        Surface(color=if(appearance==value)Highlight else Panel,shape=RoundedCornerShape(selectionRadius),modifier=Modifier.weight(1f).selectable(appearance==value,role=Role.RadioButton){if(appearance!=value){haptic.performHapticFeedback(HapticFeedbackType.ContextClick);onAppearance(value)}}) {
                            Column(Modifier.padding(vertical=16.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                AppGlyph(glyph,tint=if(appearance==value)MaterialTheme.colorScheme.onPrimaryContainer else Ink)
                                Text(label,style=MaterialTheme.typography.bodyMedium,color=if(appearance==value)MaterialTheme.colorScheme.onPrimaryContainer else Ink)
                            }
                        }
                    }
                }
            }
            if(effectsMode!=null) Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Efectos visuales",style=MaterialTheme.typography.titleMedium,color=Ink)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    EffectsMode.entries.forEach { option ->
                        Surface(color=if(effectsMode==option)Highlight else Panel,shape=RoundedCornerShape(20.dp),
                            modifier=Modifier.weight(1f).selectable(effectsMode==option,role=Role.RadioButton) {
                                if(effectsMode!=option){haptic.performHapticFeedback(HapticFeedbackType.ContextClick);onEffectsMode(option)}
                            }) {
                            Text(option.label,style=MaterialTheme.typography.bodyMedium,textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                                color=if(effectsMode==option)MaterialTheme.colorScheme.onPrimaryContainer else Ink,modifier=Modifier.padding(vertical=14.dp,horizontal=4.dp).fillMaxWidth())
                        }
                    }
                }
                val context=view.context
                Text(when {
                    !VisualEffects.supported -> "Tu Android no admite el vidrio esmerilado: los paneles se ven lisos. Hace falta Android 12 o más nuevo."
                    effectsMode==EffectsMode.AUTO && VisualEffects.capable(context) -> "Automático: este celular puede con el vidrio, así que está activado."
                    effectsMode==EffectsMode.AUTO -> "Automático: para que no se trabe, este celular usa paneles lisos. Podés activarlo igual."
                    effectsMode==EffectsMode.ON -> "Vidrio activado. Si notás que la app va lenta, probá Automático."
                    else -> "Paneles lisos, sin desenfoque."
                },style=MaterialTheme.typography.bodySmall,color=Muted)
            }
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Versión",style=MaterialTheme.typography.titleMedium,color=Ink)
                val separateLab=updates.installedChannel!=UpdateChannel.LAB
                updates.channels.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        pair.forEach { option ->
                            Surface(color=if(updates.channel==option)Highlight else Panel,shape=RoundedCornerShape(22.dp),
                                modifier=Modifier.weight(1f).selectable(updates.channel==option,enabled=!updates.busy,role=Role.RadioButton) {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    if(option==UpdateChannel.LAB && separateLab && updates.channel!=option)labWarning=true else updates.selectChannel(option)
                                }) {
                                Column(Modifier.padding(vertical=14.dp,horizontal=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text(option.label,style=MaterialTheme.typography.titleSmall,
                                        color=if(updates.channel==option)MaterialTheme.colorScheme.onPrimaryContainer else Ink)
                                    Text(when(option){UpdateChannel.STABLE->"Uso diario";UpdateChannel.BETA->"En pruebas";UpdateChannel.ALPHA->"Desarrollo";UpdateChannel.LAB->"Experimental"},style=MaterialTheme.typography.bodySmall,
                                        color=if(updates.channel==option)MaterialTheme.colorScheme.onPrimaryContainer else Muted)
                                }
                            }
                        }
                        if(pair.size==1)Spacer(Modifier.weight(1f))
                    }
                }
                if(updates.channel==UpdateChannel.ALPHA)Text("Alpha incluye funciones en construcción que pueden estar incompletas.",style=MaterialTheme.typography.bodySmall,color=Muted)
                if(updates.channel==UpdateChannel.LAB && separateLab) {
                    Text("Boletera Lab se instala como otra app, al lado de esta. Esta app no cambia.",style=MaterialTheme.typography.bodySmall,color=Muted)
                    if(updates.labVersion()!=null)TextButton(onClick={updates.openLab(view.context)}) {Text("Abrir Boletera Lab")}
                } else {
                    Text("Versión instalada: ${updates.installedChannel.label}.",style=MaterialTheme.typography.bodySmall,color=Muted)
                    Text(if(!separateLab)"Esta es Boletera Lab: cambia seguido y puede fallar. Tu Boletera de siempre sigue instalada aparte."
                        else if(updates.channel==updates.installedChannel && updates.channel==UpdateChannel.STABLE)"Recibirás actualizaciones estables."
                        else if(updates.channel==updates.installedChannel)"Recibirás actualizaciones ${updates.channel.label}."
                        else "Elegiste ${updates.channel.label}. Instalá la versión encontrada para completar el cambio.",style=MaterialTheme.typography.bodySmall,color=Muted)
                }
            }
            if(labWarning)AlertDialog(onDismissRequest={labWarning=false},
                title={Text("Boletera Lab")},
                text={Text("Es la versión de pruebas del creador: cambia de golpe, seguido, y puede dejar de funcionar. No se recomienda para usar.\n\nSe instala como otra app, al lado de Boletera, así que tu Boletera no cambia. Vas a tener que ingresar de nuevo en Lab.\n\nSi querés novedades con menos riesgo, usá Alpha o Beta.")},
                confirmButton={TextButton(onClick={labWarning=false;updates.selectChannel(UpdateChannel.LAB)}){Text("Buscar Boletera Lab")}},
                dismissButton={TextButton(onClick={labWarning=false}){Text("Cancelar")}})
            WhiteCard {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    AppGlyph(Glyph.Download,tint=MaterialTheme.colorScheme.primary)
                    Column { Text("Actualizaciones",style=MaterialTheme.typography.titleMedium); Text("Boletera ${BuildConfig.DISPLAY_VERSION}",style=MaterialTheme.typography.bodySmall,color=Muted) }
                }
                Text(updates.message,style=MaterialTheme.typography.bodyMedium,color=Muted)
                if(updates.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                val labInstall=updates.channel==UpdateChannel.LAB && updates.installedChannel!=UpdateChannel.LAB
                if(updates.ready)Primary(if(labInstall)"Instalar Boletera Lab" else "Instalar actualización",!updates.busy,onInstall)
                else if(updates.release!=null)Primary(if(labInstall)"Descargar Boletera Lab" else "Actualizar ahora",!updates.busy,updates::download)
                else OutlinedButton(onClick=updates::check,enabled=!updates.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Buscar actualizaciones") }
                if(updates.release!=null)TextButton(onClick=updates::check,enabled=!updates.busy) { Text("Buscar actualizaciones") }
            }
            WhiteCard {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Vibración",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    Switch(checked=vibrationEnabled,onCheckedChange={vibrationEnabled=it;vibration.enabled=it;pulseResult=null},modifier=Modifier.semantics {contentDescription="Vibración de Boletera"})
                }
                Text("Elegí si Boletera vibra al tocar los controles.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                OutlinedButton(onClick={pulseResult=vibration.testPulse()},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Probar vibración")}
                if(pulseResult!=null)Text(when(pulseResult) {
                    PulseResult.Requested->"Se pidieron dos pulsos. Si no los sentís, el teléfono puede estar limitando la vibración."
                    PulseResult.Disabled->"La vibración de Boletera está apagada. Activala con el interruptor de arriba."
                    PulseResult.Unavailable->"Android no informa un motor de vibración disponible."
                    else->"Android no pudo iniciar la vibración."
                },style=MaterialTheme.typography.bodySmall,color=Muted)
            }
            if(hasSavedAccess||canLogout) WhiteCard {
                Text("En este teléfono",style=MaterialTheme.typography.titleMedium)
                if(hasSavedAccess)TextButton(onClick=onForget) { AppGlyph(Glyph.Lock,tint=LocalContentColor.current); Spacer(Modifier.width(12.dp)); Text("Olvidar acceso guardado") }
                if(canLogout)TextButton(onClick=onLogout) { AppGlyph(Glyph.Exit,tint=LocalContentColor.current); Spacer(Modifier.width(12.dp)); Text("Cerrar sesión local") }
            }
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Proyecto independiente y no oficial de STM.",style=MaterialTheme.typography.bodySmall,color=Muted)
                if(diagnostic.isNotBlank()) {
                    TextButton(onClick={details=!details}) { Text("Información de ayuda") }
                    AnimatedVisibility(details) {
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(diagnostic,style=MaterialTheme.typography.bodySmall,color=Muted)
                            TextButton(onClick={
                                view.context.getSystemService(android.content.ClipboardManager::class.java)?.let { clipboard ->
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Boletera · ayuda","Boletera ${BuildConfig.DISPLAY_VERSION}\n$diagnostic"))
                                    copied=true
                                }
                            }) { Text(if(copied)"Información copiada" else "Copiar información") }
                        }
                    }
                }
            }
        }
    }
}
