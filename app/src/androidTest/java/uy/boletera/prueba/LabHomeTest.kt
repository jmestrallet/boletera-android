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

/** Renders the Lab home (inicio.html) with example data; screenshots land in the app's external files folder. */
@RunWith(AndroidJUnit4::class)
class LabHomeTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(600)
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shot.recycle()
    }
    /** The bolt's help only shows the first times: every test starts as a first time. */
    @org.junit.Before fun freshHints() { InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("lab_hints", 0).edit().clear().commit() }
    private val twoCards = listOf(CardInfo("DEMO1234", true, "Operativa"), CardInfo("DEMO5678", false, "Pte. Anular"))
    /** The home as MainActivity draws it: content in the blurred layer, chrome and panel on top. */
    @Composable private fun Home(state: UiState, offline: Boolean = false, express: (() -> Unit)? = {}, panel: Boolean = false,
        onCharge: () -> Unit = {}, onChoose: (Long) -> Unit = {}, onClose: () -> Unit = {}, ring: Boolean = true) {
        BoleteraTheme("dark") {
            val haze = remember { HazeState() }
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().hazeSource(haze)) {
                    LabAmbient()
                    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
                        Spacer(Modifier.height(64.dp))
                        LabHomeContent(state, offline, onTicketGuide = {})
                    }
                }
                LabHomeChrome(state, haze, blur = VisualEffects.capable(compose.activity), onChangeCard = {}, onSettings = {}, onCharge = onCharge,
                    onExpressCharge = express, expressPreparing = false, expressProvider = "Prex", ring = ring)
                LabAmountPanel(state, panel, onClose = onClose, onChoose = onChoose)
            }
        }
    }
    @Test fun homeShowsBalanceTripsAndHoldToStartExpress() {
        var starts = 0
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards, balance = 124000, minimum = 26000, consultedAt = System.currentTimeMillis())
        compose.setContent { Home(state, express = { starts++ }) }
        compose.onNodeWithText("Te alcanza para", substring = true).assertIsDisplayed()
        compose.onNodeWithText("23 viajes", substring = true).assertExists()
        compose.onNodeWithText("Informado por STM · hoy", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Puede no incluir viajes de las últimas 72 horas.").assertIsDisplayed()
        compose.onNodeWithText("Boletos y tarifas").assertIsDisplayed()
        // Activity waits for an account with identity level 2.
        compose.onNodeWithText("Actividad").assertDoesNotExist()
        compose.onNodeWithContentDescription("cambiar boletera", substring = true).assertExists()
        capture("lab-home.png")
        compose.onNodeWithTag("expressShortcut").performTouchInput { down(center); advanceEventTime(700) }
        assertEquals(0, starts)
        capture("lab-home-holding.png")
        compose.onNodeWithTag("expressShortcut").performTouchInput { up() }
        assertEquals(0, starts)
        compose.onNodeWithTag("expressShortcut").performTouchInput { longClick(durationMillis = 1500) }
        assertEquals(1, starts)
    }
    @Test fun oneCardHasNothingToChooseAndNoBoltBeforePrex() {
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards.take(1), balance = 124000, minimum = 26000, consultedAt = System.currentTimeMillis())
        compose.setContent { Home(state, express = null) }
        compose.onNodeWithContentDescription("cambiar boletera", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("expressShortcut").assertDoesNotExist()
        capture("lab-home-single.png")
    }
    @Test fun negativeBalanceIsRoseWithTheMinimum() {
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards, balance = -12000, minimum = 38000, consultedAt = System.currentTimeMillis())
        compose.setContent { Home(state) }
        // The home comes in as a cascade; on a slow phone the lower lines are still fading in (transparent) at first.
        compose.mainClock.advanceTimeBy(1500)
        compose.onNodeWithText("Saldo negativo").assertIsDisplayed()
        compose.onNodeWithText("Recarga mínima", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Mantené el rayo para cargar $ 380 con Prex").assertIsDisplayed()
        capture("lab-home-negative.png")
    }
    @Test fun offlineShowsTheLastBalanceWithItsTime() {
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards, balance = 124000, minimum = 26000, consultedAt = System.currentTimeMillis())
        compose.setContent { Home(state, offline = true) }
        compose.onNodeWithText("Sin internet · saldo de hoy", substring = true).assertIsDisplayed()
        capture("lab-home-offline.png")
    }
    @Test fun rechargePillOpensTheAmountPanel() {
        var open by mutableStateOf(false)
        var chosen = 0L
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards, balance = 124000, minimum = 26000, consultedAt = System.currentTimeMillis(), payerProfileId = "p1")
        compose.setContent { Home(state, panel = open, onCharge = { open = true }, onChoose = { chosen = it }, onClose = { open = false }) }
        compose.onNodeWithText("Recargar").performClick()
        compose.onNodeWithText("¿Cuánto cargás?").assertIsDisplayed()
        // Lab 18: whose card it is comes after, and only with two or more people.
        compose.onNodeWithText("Titular", substring = true).assertDoesNotExist()
        compose.onAllNodesWithText("Quedás con", substring = true).onFirst().assertExists()
        compose.onNodeWithText("Se paga con tu tarjeta Prex").assertExists()
        capture("lab-amount.png")
        compose.onNodeWithText("500").performClick()
        compose.onNodeWithText("Continuar").performClick()
        assertEquals(50000L, chosen)
    }
    @Test fun typedAmountIsPaintedAndTheMinimumIsSaid() {
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards, balance = 124000, minimum = 26000, consultedAt = System.currentTimeMillis(), payerProfileId = "p1")
        compose.setContent { Home(state, panel = true) }
        compose.onNodeWithText("Otro importe").performClick()
        compose.onNodeWithContentDescription("Otro importe en pesos").performTextInput("100")
        compose.onNodeWithText("El mínimo es $ 260").assertIsDisplayed()
        compose.onNodeWithContentDescription("Otro importe en pesos").performTextReplacement("350")
        compose.onNodeWithText("El mínimo es $ 260").assertDoesNotExist()
        capture("lab-amount-typing.png")
    }
    @Test fun aConfirmedRechargeNotYetInTheBalanceIsOnItsWay() {
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards, balance = 124000, minimum = 26000, consultedAt = System.currentTimeMillis(), onTheWay = 26000)
        compose.setContent { Home(state) }
        compose.onNodeWithText("Recarga de $ 260 en camino").assertIsDisplayed()
        compose.onNodeWithText("STM la suma en unos minutos").assertIsDisplayed()
        capture("lab-home-on-the-way.png")
    }
    @Test fun ticketGuideOpensWithTheOneHourTicket() {
        compose.setContent { BoleteraTheme("dark") { Box(Modifier.fillMaxSize()) { LabAmbient(); LabTicketGuideSheet {} } } }
        compose.onNodeWithText("Boletos y tarifas").assertIsDisplayed()
        compose.onNodeWithText("Con este contamos tus viajes en el inicio").assertIsDisplayed()
        capture("lab-ticket-guide.png")
    }
    @Test fun recargarWithAndWithoutRing() {
        var ring by mutableStateOf(true)
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", cards = twoCards, balance = 124000, minimum = 26000, consultedAt = System.currentTimeMillis())
        compose.setContent { Home(state, ring = ring) }
        capture("lab-home-ring.png")
        ring = false
        capture("lab-home-no-ring.png")
    }
}
