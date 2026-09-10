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
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("Saldo disponible").performTouchInput { down(center) }
        android.os.SystemClock.sleep(350)
        compose.onNodeWithText("Saldo disponible").performTouchInput { up() }
        android.os.SystemClock.sleep(650)
        compose.onNodeWithText("Recargar boletera").performScrollTo().performTouchInput { down(center) }
        android.os.SystemClock.sleep(250)
        compose.onNodeWithText("Recargar boletera").performTouchInput { up() }
        android.os.SystemClock.sleep(900)
        compose.onNodeWithText("$ 500").performClick()
        android.os.SystemClock.sleep(800)
        compose.onNodeWithText("Continuar").performClick()
        android.os.SystemClock.sleep(900)
        compose.onNodeWithContentDescription("Configuración").performClick()
        android.os.SystemClock.sleep(900)
        compose.onNodeWithText("Oscuro").performClick()
        android.os.SystemClock.sleep(1100)
        compose.onNodeWithText("Claro").performClick()
        android.os.SystemClock.sleep(900)
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        android.os.SystemClock.sleep(1200)
    }
}
