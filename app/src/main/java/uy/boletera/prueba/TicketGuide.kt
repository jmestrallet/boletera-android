package uy.boletera.prueba

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable internal fun ExpressIntroDialog(amount: Long?, onClose: () -> Unit, onAccept: (Boolean) -> Unit, onSkipChanged: (Boolean) -> Unit = {}) {
    var skip by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onClose,containerColor=Paper,
        icon={AppGlyph(Glyph.Arrow,tint=MaterialTheme.colorScheme.primary)},
        title={Text("Así funciona Carga Express")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text("Recarga el mínimo que STM indique para esta boletera. Puede cambiar entre cuentas y consultas. Ahora es ${Amounts.format(amount)}.")
            Text("Usa el medio de tu última recarga exitosa. Google completa tu tarjeta; si falta el CVV, lo ingresás y tocás Listo.")
            Text("Mantené Carga Express apretado durante 1,2 segundos para autorizar esa recarga. Si soltás antes, se cancela el atajo.")
            Text("Si reconoce la misma tarjeta de pago, confirma y vuelve al saldo automáticamente. La primera vez o con una tarjeta distinta, pide confirmación. Las verificaciones del proveedor siguen siendo tuyas.")
            Row(Modifier.fillMaxWidth().toggleable(skip,role=Role.Checkbox,onValueChange={skip=it;onSkipChanged(it)}),verticalAlignment=Alignment.CenterVertically) {
                Checkbox(skip,onCheckedChange=null)
                Text("No volver a mostrar",modifier=Modifier.weight(1f))
            }
        }},
        confirmButton={TextButton(onClick={onAccept(skip)}) {Text("Entendido")}},
        dismissButton={TextButton(onClick=onClose) {Text("Ahora no")}})
}

private const val FARES="https://montevideo.gub.uy/tipo/area-tematica/sistema-de-transporte-metropolitano/tarifas-del-transporte-colectivo-urbano"
private const val TYPES="https://montevideo.gub.uy/areas-tematicas/sistema-de-transporte-metropolitano/tipos-de-viaje"
private const val EXCHANGES="https://montevideo.gub.uy/tipo/area-tematica/sistema-de-transporte-metropolitano/puntos-de-intercambio"
private const val SUBURBAN="https://cutcsa.com.uy/informacion/tarifas"

private data class TicketHelp(val title: String,val price: String,val text: String,val source: String=TYPES,val sourceLabel: String="Ver explicación oficial")
private val tickets=listOf(
    TicketHelp("1 hora","$52","Hasta dos ómnibus urbanos, incluso para ir y volver. Subí al segundo dentro de una hora; la IM publica cinco minutos de tolerancia. Necesitás tu STM. Los diferenciales no están incluidos."),
    TicketHelp("2 horas","$78","Para encadenar varios viajes: sin límite de ómnibus urbanos, en cualquier sentido, entre el primer y el último ascenso dentro de dos horas. Necesitás tu STM."),
    TicketHelp("Paradas de intercambio","Sin extra en 1 h","Son paradas habilitadas para ampliar el boleto de una hora: permiten un tercer tramo y tiempo adicional. No cualquier parada sirve.\n\nEjemplo: primer ómnibus a las 10:00; segundo en parada común a las 10:30; tercero desde una parada de intercambio antes de las 12:00. El cuarto se paga.\n\nOtro caso: si el segundo ascenso es en una parada de intercambio antes de las 12:00, se habilita un tercero durante los siguientes 60 minutos. Mirá las paradas y ejemplos oficiales. El boleto de dos horas no se extiende por esta modalidad.",EXCHANGES,"Ver paradas y ejemplos"),
    TicketHelp("Combinación metropolitana","Según recorrido","Combina un urbano con un suburbano, con descuento. Es lo que necesitás cuando hacés parte del viaje en Montevideo y después seguís hacia Canelones o San José, o al revés.\n\nPedí «combinación metropolitana» y decí tu destino antes de pagar; presentá tu STM y conservá el boleto. Consultá dónde hacer el cambio.\n\nLos $80 publicados por la IM no significan que cualquier recorrido suburbano completo cueste eso. Puede haber un complemento según el tramo. Por ejemplo, Cutcsa publica para anillo 1: $80 al salir en el urbano + $55 en el suburbano = $135; entrante, $135. Ejemplo de esa empresa, no tarifa universal.",SUBURBAN,"Ver tarifas por recorrido · Cutcsa"),
    TicketHelp("Céntrico","$38","Para recorridos dentro de la zona céntrica delimitada. Pedí céntrico y consultá si tu origen y destino están incluidos. El mapa oficial muestra los límites."),
    TicketHelp("Zonal","$27","Más económico en las zonas habilitadas. Generalmente es un tramo; con STM, el trasbordo en Terminal Colón habilita un segundo tramo, excepto diferenciales."),
    TicketHelp("Diferencial","$78","Servicio semidirecto con menos paradas. Cubre un tramo en una línea diferencial. No admite boletos bonificados de estudiante o jubilado."),
    TicketHelp("Común en efectivo","$64","El boleto común es para un solo viaje. Para combinar con un boleto de una o dos horas necesitás presentar la STM, aunque pagues en efectivo.\n\nEn efectivo: una hora $64; dos horas $97; céntrico $48; zonal $34; diferencial $97.",FARES,"Ver tarifas oficiales"),
    TicketHelp("Tarifas especiales","Según categoría","Estudiante A: $28,50 · B: $39,90.\nJubilado A: $14 · B: $23 con dinero electrónico.\nPrepago institucional: $46,80.\n\nRequieren la tarjeta o condición correspondiente. Los beneficios TUS y de usuario frecuente se devuelven posteriormente; no se descuentan del precio a bordo mostrado arriba.",FARES,"Ver categorías y devoluciones")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun TicketGuideSheet(onClose: () -> Unit) {
    val uri=LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Paper) {
        Column(Modifier.widthIn(max=600.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(horizontal=24.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Boletos y tarifas",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f),color=Ink)
                TextButton(onClick=onClose){Text("Cerrar")}
            }
            Column(Modifier.weight(1f,false).verticalScroll(rememberScrollState()).padding(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Montevideo · Precios con dinero electrónico, salvo indicación. Tocá un boleto para saber cómo usarlo.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                Text("Tarifas desde el 5/1/2026 · Revisado el 10/9/2026",style=MaterialTheme.typography.labelSmall,color=Muted)
                tickets.forEach { ticket ->
                    val open=expanded==ticket.title
                    Surface(color=Panel,shape=MaterialTheme.shapes.large) {
                        Column {
                            Column(Modifier.fillMaxWidth().clickable(role=Role.Button,onClickLabel=if(open)"Ocultar explicación" else "Ver explicación") {expanded=if(open)null else ticket.title}
                                .semantics {stateDescription=if(open)"Expandido" else "Contraído"}.padding(18.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Text(ticket.title,style=MaterialTheme.typography.titleMedium,color=Ink)
                                Text(ticket.price+if(open)"  −" else "  +",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)
                            }
                            AnimatedVisibility(open) {
                                Column(Modifier.padding(horizontal=18.dp).padding(bottom=14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(ticket.text,style=MaterialTheme.typography.bodyMedium,color=Ink)
                                    TextButton(onClick={uri.openUri(ticket.source)}){Text(ticket.sourceLabel)}
                                }
                            }
                        }
                    }
                }
                Text("Guía de consulta: los precios pueden cambiar. Para un trayecto suburbano, confirmá el recorrido con la empresa.",style=MaterialTheme.typography.bodySmall,color=Muted)
                TextButton(onClick={uri.openUri(FARES)}){Text("Consultar tarifas vigentes · IM")}
            }
        }
    }
}
