package uy.boletera.prueba

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The people whose data Prex asks for before the card (Lab 18, medios-de-pago v4): «¿De quién es la tarjeta?» only with
 * two or more, «Son mis datos» marked once, and the form that asks «¿Son tus datos?» until some data are the person's own.
 */

internal val PayerProfile.fullName get() = "$givenName $familyName".trim()
/** How a person is called on screen: «Mía» for the person's own data, else the name it was saved with. */
internal val PayerProfile.shownName get() = if (personal) "Mía" else label
/** Enough to recognise the data without showing them whole. */
internal val PayerProfile.maskedDocument get() = (if (documentType == "CI") "Cédula" else "Pasaporte") + " ···· " + document.takeLast(minOf(3, document.length / 2))
internal val PayerProfile.maskedLine get() = email.substringBefore('@').let { local ->
    maskedDocument + " · " + local.take(minOf(3, local.length / 2)) + "···@" + email.substringAfter('@') + " · " + phone.take(2) + " ··· " + phone.takeLast(2) }
/** The person's own data first, then the others in the order they were saved. */
internal fun List<PayerProfile>.ordered() = sortedByDescending { it.personal }

/** One person: the initial in a circle, the name and a second line, and where it leads. */
@Composable internal fun LabPersonRow(title: String, line: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    Row(modifier.padding(bottom = 8.dp).fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(22.dp))
        .background(Color.White.copy(alpha = .06f)).clickable(role = Role.Button) { LabLog.tap("persona"); onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(21.dp)).background(LabColors.Primary.copy(alpha = .16f)).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            Text(title.take(1).uppercase(), style = type.titleMedium.copy(fontSize = 17.sp), color = LabColors.Primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = type.titleMedium.copy(fontSize = 17.sp), color = LabColors.On, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(line, style = type.bodyMedium.copy(fontSize = 13.5.sp), color = LabColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AppGlyph(Glyph.Chevron, Modifier.size(22.dp), tint = LabColors.Faint)
    }
}

@Composable private fun LabAddPersonRow(label: String, onClick: () -> Unit) {
    val type = MaterialTheme.typography
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(22.dp)).border(1.5.dp, LabColors.GlassLine, RoundedCornerShape(22.dp))
        .clickable(role = Role.Button) { LabLog.tap(label); onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(R.drawable.ic_person_add), contentDescription = null, tint = LabColors.Primary, modifier = Modifier.size(22.dp))
        Text(label, style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.Primary)
    }
}

/** «¿De quién es la tarjeta?» (B): only with two or more people. «Mía» first; a tap chooses and goes on to Prex. */
@Composable internal fun LabWhoPaysSheet(profiles: List<PayerProfile>, onChoose: (PayerProfile) -> Unit, onAdd: () -> Unit, onClose: () -> Unit) {
    val type = MaterialTheme.typography
    LabSheet(onClose) {
        Text("¿De quién es la tarjeta?", style = type.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp).semantics { heading() })
        Text("Prex pide los datos de quien paga.", style = type.bodyLarge.copy(fontSize = 15.sp), color = LabColors.Muted,
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 18.dp))
        profiles.ordered().forEach { p ->
            LabPersonRow(p.shownName, if (p.personal || p.label != p.fullName) p.fullName else p.maskedDocument, { onChoose(p) })
        }
        LabAddPersonRow("Agregar otra persona", onAdd)
    }
}

/** Carga Express with several people and none marked as the person's own (F2): asked once, a tap marks and goes on. */
@Composable internal fun LabOwnDataSheet(profiles: List<PayerProfile>, onMine: (PayerProfile) -> Unit, onNone: () -> Unit, onClose: () -> Unit) {
    val type = MaterialTheme.typography
    LabSheet(onClose) {
        Text("¿Cuáles son tus datos?", style = type.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp).semantics { heading() })
        Text("Elegí los tuyos. Te lo preguntamos una sola vez.", style = type.bodyLarge.copy(fontSize = 15.sp), color = LabColors.Muted,
            modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 18.dp))
        profiles.forEach { p -> LabPersonRow(p.fullName, p.maskedDocument, { onMine(p) }) }
        LabAddPersonRow("No están mis datos", onNone)
    }
}

/** Two options side by side, the chosen one painted (as «Efectos visuales» in Configuración). */
@Composable internal fun LabSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().selectableGroup().clip(RoundedCornerShape(26.dp)).border(1.dp, LabColors.GlassLine, RoundedCornerShape(26.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(22.dp)).background(if (on) LabColors.Primary else Color.Transparent)
                .selectable(on, role = Role.RadioButton) { if (!on) { LabLog.tap(label); onSelect(i) } }, contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp), color = if (on) LabColors.OnPrimary else LabColors.Muted, maxLines = 1)
            }
        }
    }
}

/**
 * «Datos para Prex» (C): the first time, or to add or edit someone. While no data are marked as the person's own the top
 * asks «¿Son tus datos?» (it starts on «Sí»); other people get a name to be saved with. [actionLabel] is «Continuar» in
 * the middle of a recharge and «Guardar» from Configuración. [onSave] answers whether it was saved; when it was not,
 * [failure] may say why (this screen covers the snackbar, so the engine's notice would never be seen).
 */
@Composable internal fun LabPayerScreen(existing: PayerProfile?, askMine: Boolean, actionLabel: String, onSave: (PayerProfile) -> Boolean, onClose: () -> Unit,
    startMine: Boolean = askMine, failure: () -> String = { "" }) {
    val type = MaterialTheme.typography
    val id = remember(existing?.id) { existing?.id ?: java.util.UUID.randomUUID().toString() }
    var mine by remember(id) { mutableStateOf(existing?.personal ?: startMine) }
    var given by remember(id) { mutableStateOf(existing?.givenName.orEmpty()) }
    var family by remember(id) { mutableStateOf(existing?.familyName.orEmpty()) }
    var documentType by remember(id) { mutableStateOf(existing?.documentType ?: "CI") }
    // Each document type keeps its own number while the form is open (owner, 17/10): switching back brings it back.
    var cedula by remember(id) { mutableStateOf(existing?.takeIf { it.documentType == "CI" }?.document.orEmpty()) }
    var passport by remember(id) { mutableStateOf(existing?.takeIf { it.documentType != "CI" }?.document.orEmpty()) }
    val document = if (documentType == "CI") cedula else passport
    var email by remember(id) { mutableStateOf(existing?.email.orEmpty()) }
    var phone by remember(id) { mutableStateOf(existing?.phone.orEmpty()) }
    // A label equal to the name or to the given name is the default, not a nickname: it follows the name when this is edited.
    var label by remember(id) { mutableStateOf(existing?.takeIf { !it.personal && it.label != it.fullName && it.label != it.givenName }?.label.orEmpty()) }
    var validate by remember(id) { mutableStateOf(false) }
    var error by remember(id) { mutableStateOf("") }
    val documentValid = if (documentType == "CI") Regex("[0-9]{7,8}").matches(document) else document.isNotBlank()
    val emailValid = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim())
    val phoneValid = Regex("[0-9]{9,15}").matches(phone)
    val name = "${given.trim()} ${family.trim()}".trim().take(80)
    val profile = PayerProfile(id, if (mine) name else label.trim().ifBlank { given.trim() }.take(80), given.trim(), family.trim(), document.trim(),
        email.trim(), phone, documentType, personal = mine)
    BackHandler(onBack = onClose)
    // Opaque over whatever is under it: taps on its empty parts must not reach that screen.
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0D13)).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}) {
        LabAmbient()
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LabCircle(onClose) { AppGlyph(Glyph.Back, Modifier.size(22.dp), tint = LabColors.On, label = "Volver") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                Text(when { existing != null -> "Editar datos"; askMine || mine -> "Datos para Prex"; else -> "Agregar otra persona" },
                    style = type.labStep, color = LabColors.On, modifier = Modifier.padding(start = 4.dp, top = 4.dp).semantics { heading() })
                Text(if (existing == null && !askMine && !mine) "Para pagar con la tarjeta de alguien más, con su permiso. Sus datos quedan en este celular."
                    else "Prex los pide en cada pago. Quedan solo en este celular.",
                    style = type.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 18.dp))
                if (askMine && existing == null) {
                    Text("¿Son tus datos?", style = type.titleSmall.copy(fontSize = 14.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    LabSegmented(listOf("Sí, son míos", "De otra persona"), if (mine) 0 else 1, { mine = it == 0 }, Modifier.padding(bottom = 14.dp))
                }
                val shape = RoundedCornerShape(16.dp)
                val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(given, { given = it.take(80) }, label = { Text("Nombre") }, singleLine = true, shape = shape, keyboardOptions = words,
                        isError = validate && given.isBlank(), modifier = Modifier.weight(1f).semantics { if (mine) contentType = ContentType.PersonFirstName })
                    OutlinedTextField(family, { family = it.take(80) }, label = { Text("Apellido") }, singleLine = true, shape = shape, keyboardOptions = words,
                        isError = validate && family.isBlank(), modifier = Modifier.weight(1f).semantics { if (mine) contentType = ContentType.PersonLastName })
                }
                if (!mine) OutlinedTextField(label, { label = it.take(80) }, label = { Text("Apodo (por ejemplo, Mamá)") },
                    singleLine = true, shape = shape, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                LabSegmented(listOf("Cédula", "Pasaporte"), if (documentType == "CI") 0 else 1, { documentType = if (it == 0) "CI" else "PAS" },
                    Modifier.padding(top = 14.dp))
                OutlinedTextField(document, { if (documentType == "CI") cedula = it.filter(Char::isDigit).take(8) else passport = it.take(40) },
                    label = { Text(if (documentType == "CI") "Número de cédula" else "Número de pasaporte") },
                    supportingText = if (documentType == "CI") { { Text("Sin puntos ni guion") } } else null,
                    isError = validate && !documentValid, singleLine = true, shape = shape, modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = if (documentType == "CI") KeyboardType.Number else KeyboardType.Text))
                OutlinedTextField(email, { email = it.take(120) }, label = { Text("Correo electrónico") }, isError = validate && !emailValid, singleLine = true, shape = shape,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { if (mine) contentType = ContentType.EmailAddress },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(15) }, label = { Text("Celular") },
                    supportingText = { Text("Por ejemplo, 099123456") }, isError = validate && !phoneValid, singleLine = true, shape = shape,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).semantics { if (mine) contentType = ContentType.PhoneNumber },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(Modifier.height(12.dp))
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                val invalid = when {
                    given.isBlank() || family.isBlank() -> "Completá el nombre y el apellido."
                    !documentValid -> "El documento no parece correcto."
                    !emailValid -> "El correo no parece correcto."
                    !phoneValid -> "El celular lleva al menos 9 números."
                    else -> ""
                }
                if (error.isNotBlank() || validate && invalid.isNotBlank())
                    Text(error.ifBlank { invalid }, style = type.bodySmall.copy(fontSize = 13.sp), color = LabErrorText, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                LabPrimary(actionLabel, onClick = {
                    validate = true; error = ""
                    if (profile.valid()) { if (onSave(profile)) onClose() else error = failure().ifBlank { "No se pudieron guardar los datos. Probá de nuevo." } }
                })
            }
        }
    }
}
