package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Lab sign-in, card picker and the amount panel with example data; screenshots for review. */
@RunWith(AndroidJUnit4::class)
class LabScreensTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(900)
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shot.recycle()
    }
    @Composable private fun Frame(back: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
        BoleteraTheme("dark") {
            Box(Modifier.fillMaxSize()) {
                LabAmbient()
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    LabTopBar(onBack = if (back) ({}) else null, onSettings = {})
                    Column(Modifier.padding(horizontal = 24.dp), content = content)
                }
            }
        }
    }
    @Test fun signInWithFingerprintAndPassword() {
        var manual by mutableStateOf(false)
        var unlocks = 0
        compose.setContent { Frame {
            LabWelcome(UiState(stage = "welcome", hasSavedAccess = true), savedAccess = true, manual = manual, onManual = { manual = true },
                document = "", onDocument = {}, password = "", onPassword = {}, reveal = false, onReveal = {}, save = true, onSave = {}, canSave = true,
                authenticating = false, onUnlock = { unlocks++ }, onSubmit = {}, onAccessGuide = {})
        } }
        compose.onNodeWithText("Entrar con huella").performClick()
        assertEquals(1, unlocks)
        capture("lab-welcome-fingerprint.png")
        compose.onNodeWithText("Ingresar con contraseña").performClick()
        compose.onNodeWithText("Documento uruguayo").assertIsDisplayed()
        compose.onNodeWithText("Guardar acceso con huella").assertIsDisplayed()
        capture("lab-welcome-password.png")
    }
    @Test fun signInCardShowsLoadingAndSuccess() {
        var phase by mutableStateOf(HeroPhase.IDLE)
        compose.setContent { Frame {
            LabWelcome(UiState(stage = "welcome", hasSavedAccess = true), savedAccess = true, manual = false, onManual = {},
                document = "", onDocument = {}, password = "", onPassword = {}, reveal = false, onReveal = {}, save = true, onSave = {}, canSave = true,
                authenticating = false, onUnlock = {}, onSubmit = {}, onAccessGuide = {}, heroPhase = phase, opening = false)
        } }
        android.os.SystemClock.sleep(1600)
        capture("lab-signin-idle.png")
        phase = HeroPhase.LOADING
        compose.onNodeWithText("Conectando con STM…").assertIsDisplayed()
        android.os.SystemClock.sleep(500)
        capture("lab-signin-loading.png")
        phase = HeroPhase.SUCCESS
        android.os.SystemClock.sleep(250)
        capture("lab-signin-success.png")
        compose.onNodeWithText("Listo.").assertIsDisplayed()
    }
    @Test fun signInErrorStaysWithSiteLink() {
        var opened = 0
        compose.setContent { Frame {
            LabWelcome(UiState(stage = "welcome", hasSavedAccess = true), savedAccess = true, manual = false, onManual = {},
                document = "", onDocument = {}, password = "", onPassword = {}, reveal = false, onReveal = {}, save = true, onSave = {}, canSave = true,
                authenticating = false, onUnlock = {}, onSubmit = {}, onAccessGuide = {}, heroPhase = HeroPhase.ERROR, opening = false,
                errorText = "STM no está respondiendo (sin respuesta del servidor). La página oficial parece caída o con problemas.", onOpenSite = { opened++ })
        } }
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("STM no está respondiendo", substring = true).assertIsDisplayed()
        capture("lab-signin-error.png")
        compose.onNodeWithText("Probar en el sitio de STM ↗").performClick()
        assertEquals(1, opened)
    }
    @Test fun errorScreenOffersRetryAndSite() {
        var retried = 0
        compose.setContent { Frame(back = true) {
            AppErrorScreen("STM no está respondiendo (error 503). La página oficial parece caída o con problemas.", "ABC123",
                onOpenSite = {}, onRetry = { retried++ }, onExit = {})
        } }
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("STM no responde").assertIsDisplayed()
        compose.onNodeWithText("Abrir STM en el navegador").assertIsDisplayed()
        capture("lab-error-outage.png")
        compose.onNodeWithText("Volver a intentar").performClick()
        assertEquals(1, retried)
    }
    @Test fun cardPickerMarksCurrentCard() {
        var chosen = ""
        val state = UiState(stage = "cards", selectedCard = "ABCD1234", cards = listOf(CardInfo("ABCD1234", true, "Operativa"), CardInfo("DEAD5678", false, "Pte. Anular")))
        compose.setContent { Frame(back = true) { LabCards(state) { chosen = it } } }
        compose.onNodeWithText("STM ···· 1234").performClick()
        assertEquals("ABCD1234", chosen)
        capture("lab-cards.png")
    }
    @Test fun rechargePillOpensAmountPanel() {
        var open by mutableStateOf(false)
        var chosen = 0L
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", balance = 124000, minimum = 26000, consultedAt = 1_760_000_000_000)
        compose.setContent {
            BoleteraTheme("dark") {
                val haze = remember { HazeState() }
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().hazeSource(haze)) {
                        LabAmbient()
                        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
                            Spacer(Modifier.height(64.dp)); LabHomeContent(state, null, {}, {})
                        }
                    }
                    LabHomeChrome(state, haze, blur = false, onChangeCard = {}, onSettings = {}, onCharge = { open = true },
                        onExpressCharge = {}, expressPreparing = false, expressProvider = "Prex")
                    LabAmountPanel(state, open, onClose = { open = false }, onChoose = { chosen = it })
                }
            }
        }
        compose.onNodeWithText("Recargar").performClick()
        compose.onNodeWithText("¿Cuánto cargás?").assertIsDisplayed()
        capture("lab-amount.png")
        compose.onNodeWithText("$ 500").performClick()
        compose.onNodeWithText("Continuar").performClick()
        assertEquals(50000L, chosen)
    }
}
