package uy.boletera.prueba

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val activityLocale=Locale.forLanguageTag("es-UY")
private val monthLabel=DateTimeFormatter.ofPattern("MMMM yyyy",activityLocale)
private const val identityHelp="https://mi.iduruguay.gub.uy/ayuda/que-niveles-de-garantia-de-identidad-maneja-usuario-gubuy"
private const val frequentHelp="https://montevideo.gub.uy/tipo/area-tematica/sistema-de-transporte-metropolitano/programa-de-beneficios-stm"

@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable internal fun ActivityScreen(state:ActivityState,onBack:()->Unit,onRefresh:()->Unit) {
    val uri=LocalUriHandler.current
    val current=YearMonth.now()
    val months=(state.availableMonths+current+current.minusMonths(1)).distinct().sortedDescending()
    var month by remember(state.card) {mutableStateOf(current)}
    var filter by remember(state.card) {mutableStateOf(MovementKind.ALL)}
    var selected by remember(state.card) {mutableStateOf<StmMovement?>(null)}
    Surface(color=Paper,modifier=Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            AppTopBar(title="Actividad",onBack=onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("STM · ${state.card.takeLast(4)}",style=MaterialTheme.typography.bodyMedium,color=Muted,modifier=Modifier.weight(1f))
                    IconButton(onClick=onRefresh,enabled=state.access!=ActivityAccess.LOADING) {AppGlyph(Glyph.Refresh,label="Actualizar actividad")}
                }
                when(state.access) {
                    ActivityAccess.LOADING -> WhiteCard {
                        CircularProgressIndicator(Modifier.size(28.dp))
                        Text("Consultando movimientos",style=MaterialTheme.typography.titleLarge,color=Ink)
                        Text("Estamos consultando la actividad de esta boletera en STM.",color=Muted)
                    }
                    ActivityAccess.IDENTITY_REQUIRED -> {
                        WhiteCard {
                            AppGlyph(Glyph.Lock,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.primary)
                            Text("Habilitá tus movimientos",style=MaterialTheme.typography.headlineSmall,color=Ink)
                            Text("STM permite consultar tu saldo, pero para ver los movimientos pide una identidad de nivel intermedio o avanzado.",color=Ink)
                            Text("Tu acceso actual no alcanza ese nivel. La habilitación se hace en ID Uruguay.",color=Muted)
                            Primary("Cómo habilitar el acceso") {uri.openUri(identityHelp)}
                        }
                        FrequentUnavailable("Cuando STM habilite tus movimientos podremos comprobar qué viajes cuentan para el beneficio.")
                    }
                    ActivityAccess.UNAVAILABLE -> {
                        WhiteCard {
                            Text("No pudimos leer la actividad",style=MaterialTheme.typography.headlineSmall,color=Ink)
                            Text("Todavía no tenemos movimientos verificados de esta boletera. Esto no significa que no hayas viajado.",color=Muted)
                            Primary("Volver a consultar",action=onRefresh)
                        }
                        FrequentUnavailable("Necesitamos los datos de STM para mostrar tu progreso.")
                    }
                    ActivityAccess.READY -> {
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            months.forEach {value->FilterChip(selected=value==month,onClick={month=value;selected=null},label={Text(value.format(monthLabel).replaceFirstChar {it.uppercase()})})}
                        }
                        FrequentCard(state.progress(month))
                        WhiteCard {
                            Text("Resumen del mes",style=MaterialTheme.typography.titleLarge,color=Ink)
                            listOf(MovementKind.TRIP to "Gasto en viajes",MovementKind.RECHARGE to "Recargas",MovementKind.REFUND to "Devoluciones").forEach {(kind,label)->
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                    Text(label,color=Muted,modifier=Modifier.weight(1f))
                                    Text(state.total(month,kind)?.let {Amounts.format(kotlin.math.abs(it))}?:"—",color=Ink)
                                }
                            }
                            if(month !in state.completeMonths)Text("El mes todavía no está completo. Mostramos los movimientos disponibles, sin calcular un total parcial como si fuera el total del mes.",style=MaterialTheme.typography.bodySmall,color=Muted)
                        }
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            listOf(MovementKind.ALL,MovementKind.TRIP,MovementKind.RECHARGE,MovementKind.REFUND).forEach {kind->
                                FilterChip(selected=filter==kind,onClick={filter=kind},label={Text(kind.label)})
                            }
                        }
                        val items=state.movements(month,filter)
                        if(items.isEmpty())Text(if(month in state.completeMonths)"No hay movimientos de este tipo en el mes consultado." else "No recibimos movimientos para este período.",color=Muted)
                        items.groupBy {it.date.toLocalDate()}.forEach {(day,rows)->
                            Text(day.format(DateTimeFormatter.ofPattern("EEEE d",activityLocale)).replaceFirstChar {it.uppercase()},style=MaterialTheme.typography.titleSmall,color=Muted,modifier=Modifier.semantics {heading()})
                            Surface(color=Panel,shape=RoundedCornerShape(24.dp)) {
                                Column {
                                    rows.forEachIndexed {index,entry->
                                        Row(Modifier.fillMaxWidth().clickable(onClickLabel="Ver detalle de ${entry.title}"){selected=entry}.padding(18.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                                Text(entry.title,style=MaterialTheme.typography.titleMedium,color=Ink)
                                                Text(entry.date.format(DateTimeFormatter.ofPattern("HH:mm"))+if(entry.status!=MovementStatus.RECORDED)" · ${entry.status.label}" else "",style=MaterialTheme.typography.bodySmall,color=Muted)
                                            }
                                            Text((if(entry.amount>0)"+ " else "")+Amounts.format(entry.amount),color=if(entry.amount>0)MaterialTheme.colorScheme.primary else Ink)
                                        }
                                        if(index<rows.lastIndex)HorizontalDivider(Modifier.padding(horizontal=18.dp),color=Muted.copy(alpha=0.15f))
                                    }
                                }
                            }
                        }
                        Text("STM puede registrar viajes con demora. La actividad corresponde a los datos recibidos en la última consulta.",style=MaterialTheme.typography.bodySmall,color=Muted)
                    }
                }
                TextButton(onClick={uri.openUri(frequentHelp)}) {Text("Cómo funciona Usuario frecuente")}
            }
        }
    }
    selected?.let {entry->
        ModalBottomSheet(onDismissRequest={selected=null},containerColor=Paper) {
            Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(entry.title,style=MaterialTheme.typography.headlineSmall,color=Ink)
                Text(Amounts.format(entry.amount),style=MaterialTheme.typography.displaySmall,color=Ink)
                Text(entry.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy · HH:mm",activityLocale)),color=Muted)
                Text(entry.status.label,color=Muted)
                entry.detail.forEach {(label,value)->Text("$label: $value",color=Ink)}
                TextButton(onClick={selected=null}) {Text("Cerrar detalle")}
            }
        }
    }
}

@Composable private fun FrequentUnavailable(message:String) {
    WhiteCard {
        Text("Usuario frecuente",style=MaterialTheme.typography.titleLarge,color=Ink)
        Text(message,color=Muted)
    }
}

@Composable private fun FrequentCard(progress:FrequentProgress?) {
    if(progress==null) {FrequentUnavailable("STM todavía no nos permite comprobar cuántos de estos viajes cuentan para el beneficio.");return}
    Surface(color=Lime,shape=RoundedCornerShape(28.dp),modifier=Modifier.testTag("frequentProgress")) {
        Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            val ink=MaterialTheme.colorScheme.onPrimaryContainer
            Text("Usuario frecuente",style=MaterialTheme.typography.titleMedium,color=ink)
            Text("${progress.eligibleTrips} de ${progress.target} viajes",style=MaterialTheme.typography.headlineMedium,color=ink)
            LinearProgressIndicator(progress={ (progress.eligibleTrips.toFloat()/progress.target).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth(),color=ink,trackColor=ink.copy(alpha=0.16f))
            Text(if(progress.remaining>0)"Te faltan ${progress.remaining} para alcanzar el beneficio." else if(progress.estimated)"Alcanzás el mínimo según los datos disponibles." else "Alcanzaste el mínimo de viajes.",color=ink)
            progress.estimatedRefund?.let {Text("Devolución estimada: ${Amounts.format(it)}",style=MaterialTheme.typography.titleMedium,color=ink)}
            Text(progress.regime+if(progress.estimated)" · Progreso estimado" else " · Viajes informados por STM",style=MaterialTheme.typography.bodySmall,color=ink)
            if(progress.estimatedRefund!=null)Text("La devolución queda sujeta a la confirmación de STM; no significa que ya esté acreditada.",style=MaterialTheme.typography.bodySmall,color=ink)
        }
    }
}
