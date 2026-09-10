package uy.boletera.prueba

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal data class ErrorPresentation(val title:String,val explanation:String,val code:String)
internal fun errorPresentation(message:String):ErrorPresentation {
    val text=message.lowercase()
    return when {
        "conexión segura" in text || "certificado" in text -> ErrorPresentation("No pudimos conectar\nde forma segura","No se pudo verificar la seguridad de la conexión. Cerrá este paso y probá más tarde.","CONEXIÓN SEGURA")
        "conectar" in text || "conexión" in text || "internet" in text -> ErrorPresentation("La conexión\nse interrumpió","Revisá tu conexión a internet. También puede haber una interrupción del servicio.","CONEXIÓN")
        "demor" in text || "demasiado tiempo" in text || "no respondió" in text -> ErrorPresentation("El servicio\nno respondió","Este paso demoró o devolvió un error. Podés volver al inicio e intentarlo más tarde.","SERVICIO")
        "leer" in text || "interpretar" in text || "cargar" in text || "consulta" in text -> ErrorPresentation("No pudimos\ncargar los datos","No recibimos la información necesaria para seguir. No mostramos un saldo ni un mínimo estimados.","CARGA DE DATOS")
        "verificación" in text || "desafío" in text || "datos o" in text -> ErrorPresentation("No pudimos completar\nla verificación","Este paso necesita una nueva comprobación. Volvé al inicio para intentarlo otra vez.","VERIFICACIÓN")
        else -> ErrorPresentation("No pudimos\nseguir esta vez","Algo interrumpió este paso. Podés salir de esta pantalla y volver a intentarlo más tarde.","PASO INTERRUMPIDO")
    }
}

@Composable internal fun AppErrorScreen(message:String,reference:String="",payment:Boolean=false,onExit:()->Unit) {
    val info=remember(message){errorPresentation(message)}
    var details by remember(message){mutableStateOf(false)}
    val ink=MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth().height(96.dp)) {
            Box(contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                    val y=size.height/2
                    val p=Path().apply {moveTo(0f,y);lineTo(size.width*.30f,y);cubicTo(size.width*.38f,y,size.width*.38f,0f,size.width*.43f,0f)}
                    drawPath(p,ink.copy(alpha=.22f),style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
                    drawLine(ink.copy(alpha=.22f),Offset(size.width*.67f,y),Offset(size.width,y),3.dp.toPx(),StrokeCap.Round)
                    drawCircle(ink.copy(alpha=.35f),4.dp.toPx(),Offset(size.width,y))
                }
                Surface(color=Paper,shape=RoundedCornerShape(22.dp),modifier=Modifier.size(72.dp)) {
                    Box(contentAlignment=Alignment.Center){AppGlyph(Glyph.Info,Modifier.size(36.dp),tint=ink)}
                }
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(info.code,style=MaterialTheme.typography.labelMedium,color=ink)
            Text(info.title,style=MaterialTheme.typography.headlineMedium,color=Ink)
            Text(info.explanation,style=MaterialTheme.typography.bodyLarge,color=Muted)
            if(payment)Text("Si ya autorizaste el pago, revisá su estado en el proveedor antes de iniciar otra recarga.",style=MaterialTheme.typography.bodyMedium,color=Muted)
        }
        Column {
            TextButton(onClick={details=!details}) {Text(if(details)"Ocultar detalle" else "Ver qué pasó")}
            AnimatedVisibility(details) {
                Surface(color=Panel,shape=RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(message.ifBlank {"No hay más información disponible."},style=MaterialTheme.typography.bodySmall,color=Muted)
                        if(reference.isNotBlank())Text("Referencia: $reference",style=MaterialTheme.typography.labelSmall,color=Muted)
                    }
                }
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Primary(if(payment)"Volver a Boletera" else "Volver al inicio",action=onExit)
            OutlinedButton(onClick={},enabled=false,modifier=Modifier.fillMaxWidth()) {Text("Enviar error al desarrollador")}
            Text("Próximamente",style=MaterialTheme.typography.labelSmall,color=Muted)
        }
    }
}
