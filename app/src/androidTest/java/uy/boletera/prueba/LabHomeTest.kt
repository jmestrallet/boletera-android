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

/** Renders the Lab home with example data; screenshots land in the app's external files folder. */
@RunWith(AndroidJUnit4::class)
class LabHomeTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(600)
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shot.recycle()
    }
    @Test fun homeShowsBalanceTripsAndHoldToStartExpress() {
        var starts = 0
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", balance = 124000, minimum = 26000, consultedAt = 1_760_000_000_000)
        compose.setContent {
            BoleteraTheme("dark") {
                val haze = remember { HazeState() }
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().hazeSource(haze)) {
                        LabAmbient()
                        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
                            Spacer(Modifier.height(64.dp))
                            LabHomeContent(state, onPaymentReviewed = null, onActivity = {}, onTicketGuide = {})
                        }
                    }
                    LabHomeChrome(state, haze, blur = VisualEffects.capable(compose.activity), onChangeCard = {}, onSettings = {}, onCharge = {},
                        onExpressCharge = { starts++ }, expressPreparing = false, expressProvider = "Prex")
                }
            }
        }
        compose.onNodeWithText("Te alcanza para", substring = true).assertIsDisplayed()
        compose.onNodeWithText("23 viajes", substring = true).assertExists()
        compose.onNodeWithText("Recargar").assertIsDisplayed()
        capture("lab-home.png")
        compose.onNodeWithTag("expressShortcut").performTouchInput { down(center); advanceEventTime(700) }
        assertEquals(0, starts)
        capture("lab-home-holding.png")
        compose.onNodeWithTag("expressShortcut").performTouchInput { up() }
        assertEquals(0, starts)
        compose.onNodeWithTag("expressShortcut").performTouchInput { longClick(durationMillis = 1500) }
        assertEquals(1, starts)
    }
}
