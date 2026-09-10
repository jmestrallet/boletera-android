package uy.boletera.prueba

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Payment preparation only. The provider still owns the authorization. */
@Composable internal fun PaymentSetupContent(
    state: UiState,
    profiles: List<PayerProfile>,
    onProvider: (String) -> Unit,
    onAddPayer: () -> Unit,
    onChoosePayer: () -> Unit,
    onEditPayer: (PayerProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Importe a recargar", style = MaterialTheme.typography.bodyMedium, color = Muted)
                Text(Amounts.format(state.amount), style = MaterialTheme.typography.headlineLarge, color = Ink)
            }
            Text("STM · ${state.selectedCard?.takeLast(4).orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Medio de pago", style = MaterialTheme.typography.titleMedium, color = Ink)
            val available = state.providers.filter { it.id in PaymentPolicy.supported }.sortedBy { if (it.id == "1033") 0 else 1 }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                available.forEach { provider ->
                    FilterChip(
                        selected = state.selectedProvider == provider.id,
                        onClick = { onProvider(provider.id) },
                        enabled = !state.busy,
                        label = { Text(if (provider.id == "1033") "Prex" else "eBROU") },
                        leadingIcon = { AppGlyph(if (state.selectedProvider == provider.id) Glyph.Check else Glyph.Card, tint = LocalContentColor.current) },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    )
                }
            }
            if (available.isEmpty()) Text("No hay medios de pago disponibles. Volvé al saldo para consultar de nuevo.", color = Muted)
        }
        if (state.selectedProvider == "1033") {
            val payer = profiles.find { it.id == state.payerProfileId }
            Surface(color = Panel, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Datos del titular", style = MaterialTheme.typography.titleMedium, color = Ink)
                    when {
                        profiles.isEmpty() -> Text("Para pagar con Prex, agregá los datos de la persona a cuyo nombre está la tarjeta. Quedan guardados para la próxima.", color = Muted)
                        payer == null -> Text("Tenés ${profiles.size} titulares guardados. Elegí los datos que vas a usar en esta recarga.", color = Muted)
                        else -> {
                            val fullName = "${payer.givenName} ${payer.familyName}"
                            Text(fullName, style = MaterialTheme.typography.titleLarge, color = Ink)
                            if (payer.label != fullName) Text(payer.label, style = MaterialTheme.typography.bodyMedium, color = Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onEditPayer(payer) }, enabled = !state.busy) { Text("Editar") }
                                if (profiles.size > 1) TextButton(onClick = onChoosePayer, enabled = !state.busy) { Text("Cambiar titular") }
                                else TextButton(onClick = onAddPayer, enabled = !state.busy) { Text("Agregar titular") }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Lives outside the scrolling content, so the next action stays visible. */
@Composable internal fun PaymentSetupFooter(
    state: UiState,
    profiles: List<PayerProfile>,
    onAddPayer: () -> Unit,
    onChoosePayer: () -> Unit,
    onContinue: () -> Unit
) {
    val providerAvailable = state.providers.any { it.id == state.selectedProvider && it.id in PaymentPolicy.supported }
    val needsPayer = state.selectedProvider == "1033" && profiles.none { it.id == state.payerProfileId }
    val label = when {
        !providerAvailable -> "Elegí un medio de pago"
        needsPayer && profiles.isEmpty() -> "Agregar datos"
        needsPayer -> "Elegir titular"
        else -> "Continuar · ${Amounts.format(state.amount)}"
    }
    Surface(color = Paper, shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (providerAvailable && !needsPayer) Text(
                if (state.selectedProvider == "1033") "Completás el pago en Prex." else "Se abre eBROU en Chrome.",
                style = MaterialTheme.typography.bodySmall, color = Muted
            )
            Primary(label, enabled = providerAvailable && !state.busy && state.activePayment == null) {
                if (!needsPayer) onContinue() else if (profiles.isEmpty()) onAddPayer() else onChoosePayer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun PayerPicker(
    profiles: List<PayerProfile>, selectedId: String?,
    onChoose: (String) -> Unit, onAdd: () -> Unit, onClose: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Paper) {
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Elegí un titular", style = MaterialTheme.typography.headlineMedium, color = Ink) }
            item { Text("Usá los datos de la persona a cuyo nombre está la tarjeta.", color = Muted) }
            items(profiles.size, key = { profiles[it].id }) { index ->
                val payer = profiles[index]
                OutlinedButton(onClick = { onChoose(payer.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${payer.givenName} ${payer.familyName}", style = MaterialTheme.typography.titleMedium)
                        if (payer.label != "${payer.givenName} ${payer.familyName}") Text(payer.label, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selectedId == payer.id) AppGlyph(Glyph.Check, tint = LocalContentColor.current)
                }
            }
            item { TextButton(onClick = onAdd) { Text("Agregar titular") } }
        }
    }
}
