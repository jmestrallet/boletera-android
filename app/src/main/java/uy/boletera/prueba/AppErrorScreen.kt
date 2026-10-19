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
import androidx.compose.ui.unit.sp

// Almost every failure offers doing it by hand on the official site; not when the phone itself has no internet.
internal val siteFallbackCodes=setOf("DATOS DE ACCESO","CONEXIÓN SEGURA","SERVICIO","CARGA DE DATOS","VERIFICACIÓN","PASO INTERRUMPIDO")
internal data class ErrorPresentation(val title:String,val explanation:String,val code:String)
internal fun errorPresentation(message:String):ErrorPresentation {
    val text=message.lowercase()
    return when {
        "stm no está respondiendo" in text -> ErrorPresentation("STM no responde","La página de STM está caída o muy lenta en este momento. No es tu conexión ni Boletera. Podés probar directo en el sitio o volver a intentar en unos minutos.","STM CAÍDO")
        "documento o contraseña incorrectos" in text -> ErrorPresentation("Revisá tus datos","El documento o la contraseña de gub.uy no son correctos. Corregilos para volver a intentar.","DATOS DE ACCESO")
        "conexión segura" in text || "certificado" in text -> ErrorPresentation("No pudimos conectar\nde forma segura","No se pudo verificar la seguridad de la conexión. Cerrá este paso y probá más tarde.","CONEXIÓN SEGURA")
        "conectar" in text || "conexión" in text || "internet" in text -> ErrorPresentation("La conexión\nse interrumpió","Revisá tu conexión a internet. También puede haber una interrupción del servicio.","CONEXIÓN")
        "demor" in text || "demasiado tiempo" in text || "no respondió" in text -> ErrorPresentation("El servicio\nno respondió","Este paso demoró o devolvió un error. Podés volver al inicio e intentarlo más tarde.","SERVICIO")
        "leer" in text || "interpretar" in text || "cargar" in text || "consulta" in text -> ErrorPresentation("No pudimos\ncargar los datos","No recibimos la información necesaria para seguir. No mostramos un saldo ni un mínimo estimados.","CARGA DE DATOS")
        "verificación" in text || "desafío" in text || "datos o" in text -> ErrorPresentation("No pudimos completar\nla verificación","Este paso necesita una nueva comprobación. Volvé al inicio para intentarlo otra vez.","VERIFICACIÓN")
        else -> ErrorPresentation("No pudimos\nseguir esta vez","Algo interrumpió este paso. Podés salir de esta pantalla y volver a intentarlo más tarde.","PASO INTERRUMPIDO")
    }
}

@Composable internal fun AppErrorScreen(message:String,reference:String="",payment:Boolean=false,onOpenSite:(()->Unit)?=null,onRetry:(()->Unit)?=null,paymentAmount:Long?=null,onExit:()->Unit,charged:Boolean=true,
    onViewPage:(()->Unit)?=null) {
    if(IsLab){LabErrorScreen(message,payment,paymentAmount,onOpenSite,onRetry,onExit,charged,onViewPage);return}
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
            if(info.code=="STM CAÍDO" && onOpenSite!=null && !payment) {
                Primary("Abrir STM en el navegador",action=onOpenSite)
                TextButton(onClick=onRetry?:onExit) {Text(if(onRetry!=null)"Volver a intentar" else "Volver al inicio")}
            } else {
                Primary(if(info.code=="DATOS DE ACCESO")"Corregir datos" else if(payment)"Volver a Boletera" else "Volver al inicio",action=onExit)
                // Quick way out when in a hurry; never with a payment in flight, so nobody pays twice.
                if(onOpenSite!=null && !payment && info.code in siteFallbackCodes)
                    TextButton(onClick=onOpenSite) {Text("Probar en el sitio de STM ↗")}
            }
        }
    }
}

/**
 * Lab error, the same shape as the sign-in error (reglas.html): the card turns red, the title says what happened, one grey
 * line explains, the official site is a link in the text, and the one button below repeats what failed. The arrow at the
 * top is the way back to the balance, so there is no second exit. A cut during a payment says only what happened: the
 * balance is read again on the way back, and nothing here talks about charges.
 */
@Composable private fun LabErrorScreen(message:String,payment:Boolean,paymentAmount:Long?,onOpenSite:(()->Unit)?,onRetry:(()->Unit)?,onExit:()->Unit,charged:Boolean=true,
    onViewPage:(()->Unit)?=null) {
    val info=remember(message){errorPresentation(message)}
    val (title,line)=labErrorCopy(info.code)
    val context=androidx.compose.ui.platform.LocalContext.current
    val report=LabAction("Enviar reporte",quiet=true){LabReport.send(context,"$title · $message")}
    // Lab: the page the app was using, live, to see what it shows (owner 19/10).
    val page:(@Composable ()->Unit)?=onViewPage?.let { open -> { LabTextLink("Ver la página",open,Modifier.padding(top=6.dp),quiet=true,logLabel="Ver la página") } }
    if(payment) {
        // A payment cut is not an STM error: say what to do (look at the balance) and never invite to pay again blindly.
        if(!charged) LabStatusScreen(HeroPhase.ERROR,"No pudimos seguir con la recarga","No se cobró nada. Podés intentarlo desde el inicio.",primary=LabAction("Volver al inicio",onClick=onExit),secondary=report,extra=page)
        else LabStatusScreen(HeroPhase.ERROR,"El pago se interrumpió","En el inicio vas a ver tu saldo actualizado.",primary=LabAction("Volver al inicio",onClick=onExit),secondary=report,extra=page)
        return
    }
    val site=if(onOpenSite!=null && (info.code=="STM CAÍDO" || info.code in siteFallbackCodes))LabAction("Probar en el sitio de STM ↗",onClick=onOpenSite) else null
    val primary=when {
        info.code=="DATOS DE ACCESO" -> LabAction("Corregir datos",onClick=onExit)
        onRetry!=null -> LabAction("Volver a intentar",onClick=onRetry)
        else -> LabAction("Volver al inicio",onClick=onExit)
    }
    // A wrong password is not a failure of the app: nothing to report (Lab 21).
    LabStatusScreen(HeroPhase.ERROR,title,line,primary=primary,secondary=if(info.code=="DATOS DE ACCESO")null else report,link=site,extra=page)
}
