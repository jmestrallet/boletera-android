package uy.boletera.prueba

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable internal fun PaymentCompletionScreen(payment: EmbeddedPrexPayment, pending: ActivePayment?, onOriginal: () -> Unit, browser: @Composable () -> Unit) {
    val stage=payment.nativeStage
    if(stage in listOf("stmSuccess","returnBalance","sessionExpired") || payment.returningToWallet && stage=="loading" ||
        payment.expressJourney && (stage=="receipt" || stage=="finalConfirmation" && payment.completionSubmitted)) {
        Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            browser()
            if(!payment.slowStep)LoadingState(if(stage=="finalConfirmation")"Procesando tu recarga" else "Volviendo a tu boletera",
                if(stage=="finalConfirmation")"Esperando la confirmación de Prex…" else "Estamos consultando el saldo actualizado en STM.",express=payment.expressJourney)
        }
        return
    }
    val confirming=stage=="finalConfirmation"
    val success=stage=="receipt"
    val scroll=rememberScrollState()
    LaunchedEffect(stage) { scroll.scrollTo(0) }
    var details by remember(stage) { mutableStateOf(false) }
    val rows=payment.summaryRows
    val title=when(stage) {
        "finalConfirmation" -> "Confirmá tu pago"
        "receipt" -> "Pago confirmado"
        "paymentRejected" -> "El pago fue rechazado"
        "paymentPending" -> "Pago pendiente"
        else -> "Volviendo a tu boletera"
    }
    val explanation=when(stage) {
        "finalConfirmation" -> "Revisá el importe y la tarjeta. Al confirmar, autorizás el pago en Prex."
        "receipt" -> "Prex confirmó el pago. Volvé a tu boletera para ver el saldo actualizado."
        "paymentRejected" -> "El proveedor informó que esta transacción fue rechazada."
        "paymentPending" -> "El proveedor todavía no confirmó el pago. Revisá su estado antes de iniciar otra recarga."
        else -> "Estamos recuperando tu saldo desde STM."
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            browser()
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(24.dp),modifier=Modifier.size(72.dp)) {
                Box(contentAlignment=Alignment.Center) {AppGlyph(if(success)Glyph.Check else if(confirming)Glyph.Lock else Glyph.Info,Modifier.size(34.dp),tint=MaterialTheme.colorScheme.primary)}
            }
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(title,style=MaterialTheme.typography.headlineMedium,color=Ink)
                Text(explanation,style=MaterialTheme.typography.bodyLarge,color=Muted)
            }
            Surface(color=Panel,shape=RoundedCornerShape(26.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text(if(confirming)"Importe a pagar" else "Importe de la solicitud",style=MaterialTheme.typography.labelMedium,color=Muted)
                    Text(Amounts.format(pending?.amount),style=MaterialTheme.typography.displayMedium,color=Ink)
                    if(confirming) rows.filter {it.first.trimEnd(':').lowercase() in listOf("comercio","medio de pago")}.forEach { (label,value) ->
                        Column {Text(label,style=MaterialTheme.typography.labelSmall,color=Muted);Text(value,style=MaterialTheme.typography.bodyLarge,color=Ink)}
                    }
                }
            }
            if(payment.completionNotice.isNotBlank())Text(payment.completionNotice,style=MaterialTheme.typography.bodyMedium,color=Muted)
            if(rows.isNotEmpty()) Column {
                TextButton(onClick={details=!details}) {Text(if(details)"Ocultar detalle" else if(confirming)"Ver detalle de la solicitud" else "Ver comprobante y detalle")}
                AnimatedVisibility(details) {
                    Column(Modifier.fillMaxWidth().padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        rows.forEach { (label,value) -> Column {Text(label,style=MaterialTheme.typography.labelSmall,color=Muted);Text(value,style=MaterialTheme.typography.bodyMedium,color=Ink)} }
                    }
                }
            }
            if(payment.completionSubmitted)Text(if(confirming)"Esperando la respuesta de Prex…" else "Volviendo a STM…",style=MaterialTheme.typography.bodyMedium,color=Muted)
        }
        Surface(color=Paper,shadowElevation=6.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Primary(if(confirming)"Confirmar pago" else "Volver a mi boletera",payment.canContinue) {payment.advance(stage)}
                TextButton(onClick=onOriginal) {Text("Ver página original")}
            }
        }
    }
}
