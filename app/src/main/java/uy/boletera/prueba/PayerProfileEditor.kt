package uy.boletera.prueba

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun PayerProfileEditor(
    existing: PayerProfile?, onSave: (PayerProfile) -> Boolean, onDelete: (String) -> Boolean,
    onClose: () -> Unit, paymentOnly: Boolean = false
) {
    val id = remember(existing?.id) { existing?.id ?: java.util.UUID.randomUUID().toString() }
    var label by remember(id) { mutableStateOf(existing?.label.orEmpty()) }
    var given by remember(id) { mutableStateOf(existing?.givenName.orEmpty()) }
    var family by remember(id) { mutableStateOf(existing?.familyName.orEmpty()) }
    var document by remember(id) { mutableStateOf(existing?.document.orEmpty()) }
    var documentType by remember(id) { mutableStateOf(existing?.documentType ?: "CI") }
    var email by remember(id) { mutableStateOf(existing?.email.orEmpty()) }
    var phone by remember(id) { mutableStateOf(existing?.phone.orEmpty()) }
    var error by remember(id) { mutableStateOf("") }
    var validate by remember(id) { mutableStateOf(false) }
    var deleting by remember(id) { mutableStateOf(false) }
    val documentValid = if (documentType == "CI") Regex("[0-9]{7,8}").matches(document) else document.isNotBlank()
    val emailValid = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim())
    val phoneValid = Regex("[0-9]{9,15}").matches(phone)
    val profile = PayerProfile(id, label.trim().ifBlank { "$given $family".trim().take(80) }, given.trim(), family.trim(), document.trim(), email.trim(), phone, documentType)
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Paper) {
        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().align(Alignment.CenterHorizontally)
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.9f).dp).imePadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (existing == null) "Agregar datos del titular" else "Editar datos del titular", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Text(if (paymentOnly) "Los cambios se usan solo en esta recarga." else "Usá los datos de la persona a cuyo nombre está la Prex. Se guardan en este teléfono; el número y el código de la tarjeta se ingresan en Prex.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                OutlinedTextField(given, { given = it.take(80) }, label = { Text("Nombre") }, isError = validate && given.isBlank(), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(family, { family = it.take(80) }, label = { Text("Apellido") }, isError = validate && family.isBlank(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("CI" to "Cédula", "PAS" to "Pasaporte").forEach { (type, title) ->
                        FilterChip(selected = documentType == type, onClick = { documentType = type }, label = { Text(title) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                    }
                }
                OutlinedTextField(document, { document = if (documentType == "CI") it.filter(Char::isDigit).take(8) else it.take(40) },
                    label = { Text(if (documentType == "CI") "Número de cédula" else "Número de pasaporte") },
                    supportingText = if (documentType == "CI") { { Text("Sin puntos ni guion") } } else null,
                    isError = validate && !documentValid, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (documentType == "CI") KeyboardType.Number else KeyboardType.Text))
                OutlinedTextField(email, { email = it.take(120) }, label = { Text("Correo electrónico") }, isError = validate && !emailValid,
                    singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(15) }, label = { Text("Celular") },
                    supportingText = { Text("Por ejemplo, 099123456") }, isError = validate && !phoneValid,
                    singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                if (!paymentOnly) OutlinedTextField(label, { label = it.take(80) }, label = { Text("Nombre para guardar (opcional)") },
                    placeholder = { Text("Por defecto, nombre y apellido") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (existing != null && !paymentOnly) TextButton(onClick = { deleting = true }) { Text("Eliminar datos guardados") }
                Spacer(Modifier.height(4.dp))
            }
            Surface(color = Paper, shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val invalidMessage = when {
                        given.isBlank() || family.isBlank() -> "Completá el nombre y el apellido."
                        !documentValid -> "Revisá el número de documento."
                        !emailValid -> "Ingresá un correo electrónico válido."
                        !phoneValid -> "Ingresá el celular con al menos 9 dígitos."
                        else -> "Revisá los datos ingresados."
                    }
                    if (error.isNotBlank() || validate && !profile.valid()) Text(error.ifBlank { invalidMessage }, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Primary(if (paymentOnly) "Usar en esta recarga" else "Guardar datos") {
                        validate = true; error = ""
                        if (profile.valid()) { if (onSave(profile)) onClose() else error = "No se pudieron guardar los datos. Intentá de nuevo." }
                    }
                }
            }
        }
    }
    if (deleting && existing != null) AlertDialog(onDismissRequest = { deleting = false },
        title = { Text("¿Eliminar estos datos?") }, text = { Text("Se borrarán de este teléfono. Podés agregarlos de nuevo cuando los necesites.") },
        confirmButton = { TextButton(onClick = { deleting = false; if (onDelete(existing.id)) onClose() else error = "No se pudieron eliminar los datos. Intentá de nuevo." }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancelar") } })
}
