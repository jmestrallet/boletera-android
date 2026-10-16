package uy.boletera.prueba

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Err = Color(0xFFFFB4AB)
private val RowLine = Color.White.copy(alpha = .09f)

/**
 * Configuración (configuracion.html): like Android's settings, ordered by what matters. Tu cuenta (only with a session),
 * Preferencias, Ayuda, Versión, and «Olvidar este celular» alone at the end, asking first in a sheet.
 */
@Composable internal fun LabSettingsScreen(updates: AppUpdates, hasSavedAccess: Boolean, canLogout: Boolean, diagnostic: String,
    effectsMode: EffectsMode, onEffectsMode: (EffectsMode) -> Unit, onLogout: () -> Unit, onForget: () -> Unit,
    onTicketGuide: () -> Unit, onInstall: () -> Unit, onClose: () -> Unit, onDemo: (() -> Unit)? = null) {
    val context = LocalContext.current
    val type = MaterialTheme.typography
    val vibration = remember(context) { AppHaptics(context) }
    var vibrationOn by remember { mutableStateOf(vibration.enabled) }
    var copied by remember { mutableStateOf(false) }
    var forgetting by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1800); copied = false } }
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0D13))) {
        LabAmbient()
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LabCircle(onClose) { AppGlyph(Glyph.Back, Modifier.size(22.dp), tint = LabColors.On, label = "Volver") }
                Text("Configuración", style = type.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 14.dp).semantics { heading() })
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                if (canLogout) {
                    GroupTitle("Tu cuenta")
                    Group {
                        SettingRow(R.drawable.ic_logout, "Cerrar sesión", if (hasSavedAccess) "Tu huella sigue guardada" else "Volvés a entrar con tu contraseña", onClick = onLogout)
                    }
                }
                GroupTitle("Preferencias")
                Group {
                    SettingRow(R.drawable.ic_vibration, "Vibración", "Al tocar los controles",
                        modifier = Modifier.toggleable(vibrationOn, role = Role.Switch) {
                            vibrationOn = it; vibration.enabled = it; if (it) vibration.testPulse()
                        }) {
                        Switch(vibrationOn, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = LabColors.Primary, checkedThumbColor = LabColors.OnPrimary))
                    }
                    Divider()
                    val auto = VisualEffects.capable(context)
                    SettingRow(R.drawable.ic_blur, "Efectos visuales", when {
                        !VisualEffects.supported -> "Tu Android no admite el vidrio: paneles lisos"
                        effectsMode == EffectsMode.AUTO -> if (auto) "Automático · en este celular, con vidrio" else "Automático · en este celular, paneles lisos"
                        effectsMode == EffectsMode.ON -> "Vidrio siempre, aunque el celular sea lento"
                        else -> "Paneles lisos, sin desenfoque"
                    }, bottom = 6.dp)
                    Row(Modifier.fillMaxWidth().padding(start = 66.dp, end = 16.dp, bottom = 18.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(EffectsMode.AUTO to "Automático", EffectsMode.ON to "Siempre", EffectsMode.OFF to "Nunca").forEach { (mode, label) ->
                            val on = mode == effectsMode
                            Box(Modifier.weight(1f).height(48.dp).selectable(on, role = Role.RadioButton) { if (!on) onEffectsMode(mode) }, contentAlignment = Alignment.Center) {
                                Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(18.dp))
                                    .background(if (on) LabColors.Primary else Color.Transparent)
                                    .then(if (on) Modifier else Modifier.border(1.dp, RowLine, RoundedCornerShape(18.dp))), contentAlignment = Alignment.Center) {
                                    Text(label, style = type.labelLarge.copy(fontSize = 13.sp), color = if (on) LabColors.OnPrimary else LabColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                // The card saved for Carga Express: which one, and how to forget it.
                val savedCard = LabExpressCard.vault?.label
                if (savedCard != null) {
                    GroupTitle("Carga Express")
                    Group {
                        SettingRow(R.drawable.ic_credit_card, "Tarjeta guardada", "$savedCard · con tu huella") {
                            SmallAction("Olvidar") { LabExpressCard.vault?.forget() }
                        }
                    }
                }
                GroupTitle("Ayuda")
                Group {
                    SettingRow(R.drawable.ic_directions_bus, "Boletos y tarifas", "Qué cubre cada boleto y cuánto sale", onClick = onTicketGuide) {
                        AppGlyph(Glyph.Chevron, Modifier.size(22.dp), tint = LabColors.Faint)
                    }
                    Divider()
                    val copy = {
                        context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                            android.content.ClipData.newPlainText("Boletera · ayuda", LabReport.text("Reporte desde Configuración")))
                        copied = true
                    }
                    if (onDemo != null) {
                        SettingRow(R.drawable.ic_bolt, "Modo demo", "Probá Carga Express y el pago sin cargar plata", onClick = onDemo) {
                            AppGlyph(Glyph.Chevron, Modifier.size(22.dp), tint = LabColors.Faint)
                        }
                        Divider()
                    }
                    // Always here, also when nothing failed on screen: it sends the last 24 hours of what happened in the app.
                    SettingRow(R.drawable.ic_content_copy, "Enviar reporte", "Lo que hiciste y pasó en las últimas 24 h, sin tus datos", onClick = { LabReport.send(context, "Reporte desde Configuración") }) {
                        SmallAction(if (copied) "Copiado ✓" else "Copiar", done = copied, onClick = copy)
                    }
                    Divider()
                    var erased by remember { mutableStateOf(false) }
                    SettingRow(R.drawable.ic_history, "Borrar registro", "Lo anotado en este celular para los reportes") {
                        SmallAction(if (erased) "Borrado ✓" else "Borrar", done = erased) { LabLog.clear(); erased = true }
                    }
                }
                GroupTitle("Versión")
                val release = updates.release
                val next = release?.let { UpdatePolicy.display(it.version).let { v -> if (' ' in v) v.substringAfter(' ') else v } }
                val progress = updates.progress
                val painted = release != null && progress == null
                Group(painted) {
                    if (progress != null) Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("Descargando ${next ?: "la versión nueva"}", style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.On)
                        Text("$progress % · tenés ${BuildConfig.DISPLAY_VERSION} · podés seguir usando la app", style = type.bodySmall.copy(fontSize = 13.sp), color = LabColors.Muted, modifier = Modifier.padding(top = 1.dp))
                        Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = .1f))) {
                            Box(Modifier.fillMaxWidth(progress / 100f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(LabColors.Primary))
                        }
                    } else SettingRow(R.drawable.ic_science, "Versión ${BuildConfig.DISPLAY_VERSION}", when {
                        updates.ready -> "$next lista para instalar"
                        release != null -> "$next disponible"
                        updates.busy -> "Buscando…"
                        searched -> updates.message
                        else -> "Boletera Lab · experimental"
                    }, painted = painted) {
                        when {
                            updates.ready -> SmallAction("Instalar", painted = true, onClick = onInstall)
                            release != null -> SmallAction("Descargar", painted = true, enabled = !updates.busy, onClick = updates::download)
                            else -> SmallAction("Buscar", enabled = !updates.busy) { searched = true; updates.check() }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Group {
                    SettingRow(R.drawable.ic_phonelink_erase, "Olvidar este celular", "Borra la huella, los titulares y tus preferencias", danger = true, onClick = { forgetting = true })
                }
                Spacer(Modifier.weight(1f).heightIn(min = 20.dp))
                Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 30.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    AppGlyph(Glyph.Lock, Modifier.size(15.dp), tint = LabColors.Faint)
                    Text("Proyecto independiente y no oficial de STM", style = type.bodySmall.copy(fontSize = 12.5.sp), color = LabColors.Faint, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
    if (forgetting) LabSheet({ forgetting = false }) {
        Text("¿Olvidar este celular?", style = type.labSheet, color = LabColors.On, modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() })
        Text("Se borran el acceso con huella, los titulares guardados y tus preferencias, y se cierra la sesión. La próxima vez entrás con tu contraseña de gub.uy.",
            style = type.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp))
        Box(Modifier.padding(top = 22.dp).fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(Err.copy(alpha = .16f))
            .clickable(role = Role.Button) { forgetting = false; onForget() }, contentAlignment = Alignment.Center) {
            Text("Olvidar este celular", style = type.titleMedium.copy(fontSize = 18.sp), color = Err)
        }
        LabTextLink("Cancelar", { forgetting = false }, Modifier.align(Alignment.CenterHorizontally), quiet = true)
    }
}

@Composable private fun GroupTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp), color = LabColors.Primary, modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 8.dp))

@Composable private fun Group(painted: Boolean = false, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(if (painted) LabColors.Primary else Color.White.copy(alpha = .075f)), content = content)

@Composable private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(RowLine))

@Composable private fun SettingRow(@DrawableRes icon: Int, title: String, line: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    danger: Boolean = false, painted: Boolean = false, bottom: androidx.compose.ui.unit.Dp = 10.dp, trailing: (@Composable () -> Unit)? = null) {
    val type = MaterialTheme.typography
    val on = if (painted) LabColors.OnPrimary else if (danger) Err else LabColors.On
    Row(modifier.fillMaxWidth().heightIn(min = 64.dp).then(if (onClick != null) Modifier.clickable(role = Role.Button) { LabLog.tap("ajustes: $title"); onClick() } else Modifier)
        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = bottom),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(when { painted -> LabColors.OnPrimary.copy(alpha = .12f); danger -> Err.copy(alpha = .12f); else -> Color.White.copy(alpha = .07f) }),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = when { painted -> LabColors.OnPrimary; danger -> Err; else -> LabColors.Muted }, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = type.titleMedium.copy(fontSize = 16.sp), color = on)
            Text(line, style = type.bodySmall.copy(fontSize = 13.sp), color = if (painted) LabColors.OnPrimary.copy(alpha = .7f) else LabColors.Muted, modifier = Modifier.padding(top = 1.dp))
        }
        trailing?.invoke()
    }
}

/** A small button with a background (Copiar, Buscar, Instalar) that still answers in 48 dp. */
@Composable private fun SmallAction(label: String, done: Boolean = false, painted: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val color = when { done -> Color(0xFF9FE3B8); painted -> LabColors.OnPrimary; else -> LabColors.Primary }
    Box(Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).clickable(enabled = enabled, role = Role.Button) { LabLog.tap("ajustes: $label"); onClick() }, contentAlignment = Alignment.Center) {
        Box(Modifier.height(40.dp).widthIn(min = 76.dp).clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = if (painted) .14f else .12f)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp), color = color.copy(alpha = if (enabled) 1f else .5f), maxLines = 1)
        }
    }
}
