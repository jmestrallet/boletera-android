package uy.boletera.prueba

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One piece of news: an icon, a short phrase and one line (avisos.html). */
internal data class LabNews(@DrawableRes val icon: Int, val title: String, val line: String)

/** What Lab 7 brings, shown once after updating. Up to four; the full detail stays on GitHub. */
internal val LabWhatsNewItems = listOf(
    LabNews(R.drawable.ic_download, "Primero la actualización", "Si hay versión nueva, te avisa antes de pedir la huella."),
    LabNews(R.drawable.ic_credit_card, "Google completa y sigue", "Elegís tu Prex en la lista y pasa solo a «Confirmá tu pago»."),
    LabNews(R.drawable.ic_payments, "Con tu tarjeta Prex", "La app dice con qué se paga antes de empezar."),
    LabNews(R.drawable.ic_check, "Vuelve al inicio", "Si el pago no sale, ya no te deja en la página de STM."),
)

/** The release notes as news: «Título — línea» (or «Título: línea») becomes a phrase and its line, with an icon by topic. */
internal fun labNewsFrom(notes: List<String>): List<LabNews> = notes.take(4).map { note ->
    val parts = note.split(" — ", ": ", limit = 2)
    val (title, line) = if (parts.size == 2) parts[0].trim() to parts[1].trim() else note.trim() to ""
    val topic = note.lowercase()
    val icon = when {
        "pago" in topic || "prex" in topic || "recarga" in topic -> R.drawable.ic_payments
        "express" in topic -> R.drawable.ic_bolt
        "entrada" in topic || "entrar" in topic || "huella" in topic -> R.drawable.ic_login
        "error" in topic -> R.drawable.ic_error
        "boleto" in topic || "tarifa" in topic -> R.drawable.ic_directions_bus
        "boletera" in topic || "tarjeta" in topic -> R.drawable.ic_transit_card
        else -> R.drawable.ic_check
    }
    LabNews(icon, title.trimEnd('.'), line.replaceFirstChar { it.uppercase() })
}

@Composable private fun LabNewsList(items: List<LabNews>, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(LabColors.Primary.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(item.icon), contentDescription = null, tint = LabColors.Primary, modifier = Modifier.size(21.dp))
                }
                Column(Modifier.weight(1f).padding(top = 1.dp)) {
                    Text(item.title, style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.On)
                    if (item.line.isNotBlank()) Text(item.line, style = type.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp), color = LabColors.Muted)
                }
            }
        }
    }
}

@Composable private fun LabTag(text: String, color: androidx.compose.ui.graphics.Color = LabColors.Primary) =
    Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp), color = color,
        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = .13f)).padding(horizontal = 10.dp, vertical = 4.dp))

/**
 * A new version found on opening Lab. A normal one is recommended and can wait («Ahora no» skips that version); a
 * critical one (STM or Prex changed how their pages talk to the app) says plainly that without it the app will not
 * work, and asks again on every opening.
 */
@Composable internal fun LabUpdatePrompt(release: UpdateRelease, progress: Int?, busy: Boolean, onUpdate: () -> Unit, onLater: () -> Unit,
    problem: String? = null) {
    val version = UpdatePolicy.display(release.version).substringAfter(' ')
    val type = MaterialTheme.typography
    LabSheet(onLater) {
        if (release.critical) LabTag("Actualización necesaria", LabColors.Warn) else LabTag("Actualización")
        Text(if (release.critical) "Actualizá para que Boletera siga andando" else "$version está disponible", style = type.labSheet, color = LabColors.On,
            modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() })
        Text(if (release.critical) "STM o Prex cambiaron cómo funciona su página. Sin $version, la app no va a poder entrar ni recargar."
            else "Recomendamos actualizar. Si preferís, podés hacerlo después desde Configuración.",
            style = type.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
        val items = labNewsFrom(release.notes)
        if (items.isNotEmpty()) LabNewsList(items, Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 22.dp))
        else Spacer(Modifier.height(22.dp))
        if (problem != null && progress == null) Text(problem, style = type.bodyMedium.copy(fontSize = 14.sp), color = LabColors.Warn,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 14.dp))
        LabPrimary(when { progress != null -> "Descargando… $progress %"; problem != null -> "Probar de nuevo"; release.critical -> "Actualizar ahora"; else -> "Actualizar" },
            enabled = !busy, onClick = onUpdate, sheet = true)
        LabTextLink(if (release.critical) "Seguir sin actualizar" else "Ahora no", onLater, Modifier.align(Alignment.CenterHorizontally), quiet = true)
    }
}

/**
 * The same sheet before and after updating: before, «Lab 7 está lista» with «Instalar» and «Ahora no»;
 * after, «Novedades» with «Listo». [version] is the short name («Lab 7»).
 */
@Composable internal fun LabNewsSheet(version: String, items: List<LabNews>, installed: Boolean, onPrimary: () -> Unit, onLater: () -> Unit) {
    LabSheet(onLater) {
        LabTag(if (installed) "$version instalada" else "Actualización")
        Text(if (installed) "Novedades" else "$version está lista", style = MaterialTheme.typography.labSheet, color = LabColors.On,
            modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() })
        if (items.isEmpty()) Text("Esta versión no trae un resumen de novedades.", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = LabColors.Muted,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 22.dp))
        else LabNewsList(items, Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 22.dp))
        LabPrimary(if (installed) "Listo" else "Instalar", onClick = onPrimary, sheet = true)
        if (!installed) LabTextLink("Ahora no", onLater, Modifier.align(Alignment.CenterHorizontally), quiet = true)
    }
}

/**
 * «Antes de entrar» (avisos.html 1): one sheet for the first open and for «¿Es tu primer ingreso a STM?». The main button
 * is the common case and goes straight to the document; the link opens STM's site.
 */
@Composable internal fun LabAccessGuideSheet(onReady: () -> Unit, onFirstTime: () -> Unit, onClose: () -> Unit) {
    LabSheet(onClose) {
        Text("Antes de entrar", style = MaterialTheme.typography.labSheet, color = LabColors.On, modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() })
        Text("La primera vez, STM te pide autorizar el acceso en su web. Se hace una sola vez.", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
            color = LabColors.Muted, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
        LabAccessSteps(Modifier.padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 22.dp), accent = LabColors.Primary)
        LabPrimary("Ya entré a STM con mi usuario gub.uy", onClick = onReady, sheet = true)
        LabTextLink("Es mi primera vez: abrir STM ↗", onFirstTime, Modifier.align(Alignment.CenterHorizontally))
    }
}
