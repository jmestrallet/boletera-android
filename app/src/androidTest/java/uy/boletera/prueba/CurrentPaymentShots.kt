package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Screenshots of today's payment screens with example data, to compare them with the redesign mockups. */
@RunWith(AndroidJUnit4::class)
class CurrentPaymentShots {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(900)
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shot.recycle()
    }
    private val payer = PayerProfile("p1", "Yo", "Ana", "Pérez", "12345678", "ana@example.com", "099123456")
    @Test fun paymentSetup() {
        val state = UiState(stage = "paymentBoundary", selectedCard = "ABCD1234", amount = 50000, balance = 124000, minimum = 26000,
            providers = listOf(ProviderInfo("1033", "Prex"), ProviderInfo("1002", "BROU"), ProviderInfo("1048", "Mastercard"), ProviderInfo("1049", "Visa")),
            selectedProvider = "1033", payerProfileId = "p1")
        compose.setContent {
            BoleteraTheme("dark") { Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    LabTopBar(title = "Tu recarga", onBack = {}, onSettings = {})
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                        PaymentSetupContent(state, listOf(payer), {}, {}, {}, {})
                    }
                    PaymentSetupFooter(state, listOf(payer), {}, {}, {})
                }
            } }
        }
        capture("current-payment-setup.png")
    }
    @Composable private fun LabFrame(content: @Composable ColumnScope.() -> Unit) {
        BoleteraTheme("dark") { Box(Modifier.fillMaxSize()) {
            LabAmbient()
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                LabTopBar(onSettings = {})
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp), content = content)
            }
        } }
    }
    @Test fun connecting() {
        compose.setContent { LabFrame {
            LoadingState("Conectando con STM", "Estamos consultando STM. Tu información va a aparecer acá.")
            androidx.compose.material3.TextButton(onClick = {}) { androidx.compose.material3.Text("Cancelar") }
        } }
        capture("current-connecting.png")
    }
    @Test fun accessHelp() {
        compose.setContent { LabFrame {
            Title("Completá el acceso\nen la web de STM")
            androidx.compose.material3.Text("Después de la contraseña apareció un paso que Boletera todavía no reconoce.", color = Muted)
            StmAccessInstructions()
            Primary("Abrir STM en el navegador") {}
        } }
        capture("current-access-help.png")
    }
    @Test fun settings() {
        compose.setContent { BoleteraTheme("dark") { Box(Modifier.fillMaxSize()) {
            LabAmbient()
            val updates: AppUpdates = androidx.lifecycle.viewmodel.compose.viewModel()
            SettingsSheet(updates, "dark", {}, hasSavedAccess = true, canLogout = true, diagnostic = "", onForget = {}, onLogout = {}, onInstall = {}, onClose = {},
                onTicketGuide = {}, effectsMode = EffectsMode.AUTO, onEffectsMode = {})
        } } }
        android.os.SystemClock.sleep(1500)
        capture("current-settings-1.png")
        compose.onNodeWithText("Efectos visuales").performTouchInput { swipeUp(startY = bottom * 0.9f, endY = top, durationMillis = 400) }
        capture("current-settings-2.png")
        compose.onNodeWithText("Vibración").performTouchInput { swipeUp(startY = bottom, endY = top - 1400f, durationMillis = 400) }
        capture("current-settings-3.png")
    }
    @Composable private fun Dialog(title: String, body: @Composable () -> Unit, confirm: String, dismiss: String?) {
        androidx.compose.material3.AlertDialog(onDismissRequest = {}, title = { androidx.compose.material3.Text(title) }, text = body,
            confirmButton = { androidx.compose.material3.TextButton(onClick = {}) { androidx.compose.material3.Text(confirm) } },
            dismissButton = dismiss?.let { { androidx.compose.material3.TextButton(onClick = {}) { androidx.compose.material3.Text(it) } } })
    }
    @Test fun dialogs() {
        var which by androidx.compose.runtime.mutableIntStateOf(0)
        compose.setContent { BoleteraTheme("dark") { Box(Modifier.fillMaxSize()) {
            LabAmbient()
            when (which) {
                0 -> Dialog("Antes de entrar", { androidx.compose.material3.Text("Boletera entra a STM con tu Usuario gub.uy, igual que la web Mi STM.\n\nSi nunca usaste Mi STM, la primera vez STM te pide autorizar el acceso en su web. Se hace una sola vez: abrís STM, entrás con tu usuario, aceptás y volvés a Boletera.") }, "Ya usé Mi STM", "Abrir STM primero")
                1 -> Dialog("Primer ingreso a STM", { StmAccessInstructions() }, "Abrir STM", "Cerrar")
                2 -> Dialog("Qué hay de nuevo en 0.2.40 Lab 6", { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { LabWhatsNew.forEach { androidx.compose.material3.Text("· $it") } } }, "Listo", null)
                else -> UpdateReviewDialog(UpdateRelease("0.2.40-lab.7", "https://example.invalid/a.apk", 1, "x",
                    history = listOf(UpdateNews("0.2.40 Lab 7", listOf("La entrada nueva, centrada, con la hoja para la contraseña.", "Errores que dicen qué pasó, con la tarjeta en rojo.", "El pago con el encabezado fijo y «Pagar $ 500» al final.")))), {}, {})
            }
        } } }
        for (i in 0..3) { which = i; android.os.SystemClock.sleep(700); capture("current-dialog-$i.png") }
    }
    @Test fun sessionExpired() {
        compose.setContent { BoleteraTheme("dark") { Box(Modifier.fillMaxSize()) {
            LabAmbient()
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                LabTopBar(onSettings = {})
                Box(Modifier.weight(1f).padding(horizontal = 24.dp)) {
                    LabWelcome(UiState(stage = "welcome", hasSavedAccess = true, sessionExpired = true), savedAccess = true, authenticating = false, onUnlock = {}, onManual = {}, onAccessGuide = {})
                }
            }
        } } }
        android.os.SystemClock.sleep(1200)
        capture("current-session-expired.png")
    }
    @Test fun prexScreens() {
        var which by androidx.compose.runtime.mutableIntStateOf(0)
        val ana = PayerProfile("p1", "Yo", "Ana", "Pérez", "12345678", "ana@example.com", "099123456")
        val luis = PayerProfile("p2", "Papá", "Luis", "Pérez", "23456789", "luis@example.com", "098765432")
        compose.setContent { BoleteraTheme("dark") { Box(Modifier.fillMaxSize()) {
            LabAmbient()
            when (which) {
                // The same pieces EmbeddedPrexScreen draws for Prex's summary and payer steps.
                0, 1 -> Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AppTopBar(title = "Pago con Prex", onBack = {})
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProgressSteps(which, listOf("Recarga", "Titular", "Tarjeta"))
                        androidx.compose.material3.Text(if (which == 0) "Revisá tu recarga" else "Datos del titular", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = Ink)
                        androidx.compose.material3.Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Panel), shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                val rows = if (which == 0) listOf("Comercio:" to "STM Recargas", "Tarjeta STM:" to "···· 1234", "Medio de pago:" to "Prex")
                                    else listOf("Titular" to "Ana Pérez", "Documento" to "12345678", "Correo" to "ana@example.com", "Celular" to "099123456")
                                if (which == 0) androidx.compose.material3.Text("$ 500", style = androidx.compose.material3.MaterialTheme.typography.displayMedium, color = Ink)
                                rows.forEach { (l, v) -> Column { androidx.compose.material3.Text(l, color = Muted, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)); androidx.compose.material3.Text(v, color = Ink) } }
                            }
                        }
                    }
                }
                2 -> PayerProfileEditor(null, { true }, { true }, {})
                3 -> PayerPicker(listOf(ana, luis), "p1", {}, {}, {})
                else -> Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) { LoadingState("Preparando Prex", "Usando el mínimo y tus datos guardados.", express = true) }
            }
        } } }
        for (i in 0..4) { which = i; android.os.SystemClock.sleep(900); capture("current-prex-$i.png") }
    }
    @Test fun cardForm() {
        compose.setContent {
            BoleteraTheme("dark") { Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AppTopBar(title = "Pago con Prex", onBack = {})
                    NativeCardForm(busy = false, canContinue = true, providerError = false, expandedChallenge = false, onSubmit = { _, _, _ -> }, onOriginal = {})
                }
            } }
        }
        capture("current-card-form.png")
    }
}
