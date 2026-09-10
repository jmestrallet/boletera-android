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
        Surface(color=Lime,shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp,bottomEnd=64.dp,bottomStart=32.dp)) {
            Box(Modifier.fillMaxWidth().height(144.dp).padding(24.dp)) {
                val routeColor=MaterialTheme.colorScheme.onPrimaryContainer
                Canvas(Modifier.fillMaxSize()) {
                    val p=Path().apply { moveTo(size.width*0.03f,size.height*0.8f);lineTo(size.width*0.25f,size.height*0.8f);cubicTo(size.width*0.48f,size.height*0.8f,size.width*0.42f,size.height*0.15f,size.width*0.64f,size.height*0.15f);lineTo(size.width*0.95f,size.height*0.15f) }
                    drawPath(p,routeColor.copy(alpha=0.18f),style=Stroke(12.dp.toPx(),cap=StrokeCap.Round))
                    drawCircle(routeColor,7.dp.toPx(),Offset(size.width*0.03f,size.height*0.8f))
                    drawCircle(routeColor,7.dp.toPx(),Offset(size.width*0.95f,size.height*0.15f))
                }
                Surface(color=Panel,shape=RoundedCornerShape(20.dp),modifier=Modifier.align(Alignment.Center).size(80.dp).graphicsLayer { rotationZ=-10f }) {
                    Box(contentAlignment=Alignment.Center) { AppGlyph(Glyph.Ticket,Modifier.size(42.dp),tint=MaterialTheme.colorScheme.primary) }
                }
            }
        }
        Title("Tu próximo viaje\nempieza acá.")
        Text("Consultá tu saldo y recargá tu STM,\nsin las vueltas.",color=Muted,style=MaterialTheme.typography.bodyLarge)
    }
}

@Composable internal fun WalletHome(state: UiState, onChangeCard: () -> Unit, onCharge: () -> Unit, onRefresh: () -> Unit,
    onExpressCharge: (() -> Unit)? = null, onExpressHelp: (() -> Unit)? = null, onTicketGuide: (() -> Unit)? = null, expressPreparing: Boolean = false, expressProvider: String = "Medio guardado") {
    val colors=MaterialTheme.colorScheme
    val haptic=LocalHapticFeedback.current
    fun chooseCard() { haptic.performHapticFeedback(HapticFeedbackType.ContextClick);onChangeCard() }
    Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Text("STM · ${state.selectedCard?.takeLast(4).orEmpty()}",style=MaterialTheme.typography.bodyMedium,color=Muted)
        Surface(color=Lime,shape=RoundedCornerShape(32.dp),modifier=Modifier.semantics {
            customActions=listOf(CustomAccessibilityAction("Actualizar saldo") { if (!state.busy) { onRefresh(); true } else false })
        }) {
            Column(Modifier.fillMaxWidth().padding(28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Saldo disponible",style=MaterialTheme.typography.titleMedium,color=colors.onPrimaryContainer,modifier=Modifier.weight(1f))
                    IconButton(onClick=::chooseCard,enabled=!state.busy) { AppGlyph(Glyph.Swap,label="Cambiar boletera",tint=colors.onPrimaryContainer) }
                }
                Text(Amounts.format(state.balance),style=if(Amounts.format(state.balance).length>10)MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                    color=colors.onPrimaryContainer,modifier=Modifier.fillMaxWidth())
                Text(if((state.balance?:0)<0) "Saldo pendiente de cubrir" else "Informado por STM",style=MaterialTheme.typography.bodyMedium,color=colors.onPrimaryContainer)
                HorizontalDivider(color=colors.onPrimaryContainer.copy(alpha=0.15f))
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    AppGlyph(Glyph.Refresh,Modifier.size(16.dp),tint=colors.onPrimaryContainer)
                    Text(if(state.busy) "Actualizando…" else state.consultedAt?.let { "Actualizado a las ${SimpleDateFormat("HH:mm",Locale.forLanguageTag("es-UY")).format(Date(it))}" } ?: "Deslizá hacia abajo para actualizar",
                        style=MaterialTheme.typography.bodySmall,color=colors.onPrimaryContainer)
                }
            }
        }
            Primary("Recargar boletera",enabled=!state.busy && state.minimum!=null,action=onCharge)
            if(onExpressCharge!=null) ExpressShortcut(state.minimum,enabled=!state.busy,onExplain=onExpressHelp,preparing=expressPreparing,providerName=expressProvider,onStart=onExpressCharge)
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
                    val fill by animateColorAsState(if(active)Lime else Panel,spring(stiffness=650f),label="amount surface")
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
        Surface(color=Lime,shape=RoundedCornerShape(28.dp),modifier=Modifier.size(88.dp)) {
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
    onTicketGuide: (() -> Unit)? = null) {
    var details by remember { mutableStateOf(false) }
    val haptic=LocalHapticFeedback.current
    val view=LocalView.current
    val vibration=remember(view.context) {AppHaptics(view.context)}
    var pulseResult by remember { mutableStateOf<PulseResult?>(null) }
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
                        Surface(color=if(appearance==value)Lime else Panel,shape=RoundedCornerShape(selectionRadius),modifier=Modifier.weight(1f).selectable(appearance==value,role=Role.RadioButton){if(appearance!=value){haptic.performHapticFeedback(HapticFeedbackType.ContextClick);onAppearance(value)}}) {
                            Column(Modifier.padding(vertical=16.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                AppGlyph(glyph,tint=if(appearance==value)MaterialTheme.colorScheme.onPrimaryContainer else Ink)
                                Text(label,style=MaterialTheme.typography.bodyMedium,color=if(appearance==value)MaterialTheme.colorScheme.onPrimaryContainer else Ink)
                            }
                        }
                    }
                }
            }
            WhiteCard {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    AppGlyph(Glyph.Download,tint=MaterialTheme.colorScheme.primary)
                    Column { Text("Siempre al día",style=MaterialTheme.typography.titleMedium); Text("Versión ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.bodySmall,color=Muted) }
                }
                Text(updates.message,style=MaterialTheme.typography.bodyMedium,color=Muted)
                if(updates.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
                if(updates.ready)Primary("Instalar actualización",!updates.busy,onInstall)
                else if(updates.release!=null)Primary("Actualizar ahora",!updates.busy,updates::download)
                else OutlinedButton(onClick=updates::check,enabled=!updates.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Buscar actualizaciones") }
                if(updates.release!=null)TextButton(onClick=updates::check,enabled=!updates.busy) { Text("Buscar actualizaciones") }
            }
            WhiteCard {
                Text("Respuesta táctil",style=MaterialTheme.typography.titleMedium)
                Text("Probá la vibración en este teléfono.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                OutlinedButton(onClick={pulseResult=vibration.testPulse()},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Probar vibración")}
                if(pulseResult!=null)Text(when(pulseResult) {
                    PulseResult.Requested->"Se pidieron dos pulsos. Si no los sentís, revisá la intensidad de respuesta táctil del teléfono."
                    PulseResult.Disabled->"La respuesta táctil está apagada en Android. Podés activarla en los ajustes del teléfono."
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
                Text("Una forma más simple de moverte.",style=MaterialTheme.typography.bodyMedium,color=Ink)
                Text("App independiente de STM. Versión de prueba.",style=MaterialTheme.typography.bodySmall,color=Muted)
                if(diagnostic.isNotBlank()) {
                    TextButton(onClick={details=!details}) { Text("Información de ayuda") }
                    AnimatedVisibility(details) { Text(diagnostic,style=MaterialTheme.typography.bodySmall,color=Muted) }
                }
            }
        }
    }
}
