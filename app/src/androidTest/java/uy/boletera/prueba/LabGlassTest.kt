package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Same Lab home with the glass forced on and off, for a side-by-side look. */
@RunWith(AndroidJUnit4::class)
class LabGlassTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(800)
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shot.recycle()
    }
    @Test fun glassOnAndOff() {
        var blur by mutableStateOf(true)
        val state = UiState(stage = "balance", selectedCard = "DEMO1234", balance = 124000, minimum = 26000, consultedAt = 1_760_000_000_000)
        compose.setContent {
            BoleteraTheme("dark") {
                val haze = remember { HazeState() }
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().hazeSource(haze)) {
                        LabAmbient()
                        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
                            Spacer(Modifier.height(64.dp))
                            LabHomeContent(state, offline = false, onTicketGuide = {})
                        }
                    }
                    LabHomeChrome(state, haze, blur = blur, onChangeCard = {}, onSettings = {}, onCharge = {},
                        onExpressCharge = {}, expressPreparing = false, expressProvider = "Prex")
                }
            }
        }
        capture("lab-glass-on.png")
        blur = false
        capture("lab-glass-off.png")
    }
}
