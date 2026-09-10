package uy.boletera.prueba

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Short visual demonstration. Uses production UI and fictitious values; no engine or payment. */
@RunWith(AndroidJUnit4::class)
class DesignMotionDemo {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun panelsAndAppearance() {
        compose.runOnIdle {
            val updates=AppUpdates(compose.activity.application)
            compose.activity.setContent {
                var amount by remember{mutableStateOf(false)}
                var settings by remember{mutableStateOf(false)}
                var appearance by remember{mutableStateOf("light")}
                val state=UiState(stage="balance",selectedCard="DEMO1234",balance=124000,minimum=26000)
                BoleteraTheme(appearance) { Surface(color=Paper) { Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AppTopBar(onSettings={settings=true})
                    Column(Modifier.weight(1f).widthIn(max=600.dp).fillMaxWidth().align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).padding(24.dp)) {
                        WalletHome(state,{}, {amount=true},{})
                    }
                    if(amount)AmountSheet(state,{amount=false}){amount=false}
                    if(settings)SettingsSheet(updates,appearance,{appearance=it},false,false,"",{},{},{},{settings=false})
                } } }
            }
        }
        compose.waitForIdle()
        android.os.SystemClock.sleep(1200)
        fun frames(ms:Int) { repeat(ms/16) { compose.mainClock.advanceTimeBy(16);android.os.SystemClock.sleep(16) } }
        fun press(text:String) {
            compose.onNodeWithText(text).performScrollTo().performTouchInput { down(center) }
            frames(220)
            compose.onNodeWithText(text).performTouchInput { up() }
            frames(600)
        }
        compose.mainClock.autoAdvance=false
        try {
            press("Saldo disponible")
            press("Recargar boletera")
            compose.onNodeWithText("$ 500").performClick();frames(650)
            compose.onNodeWithText("$ 1.000").performClick();frames(650)
            press("Continuar")
            compose.onNodeWithContentDescription("Configuración").performClick();frames(700)
            compose.onNodeWithText("Oscuro").performClick();frames(800)
            compose.onNodeWithText("Claro").performClick();frames(800)
            compose.onNodeWithText("Probar vibración").performScrollTo().performClick();frames(700)
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            frames(800)
        } finally { compose.mainClock.autoAdvance=true }
    }
}
