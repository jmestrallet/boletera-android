package uy.boletera.prueba

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real gestures on production controls with fictitious account data. */
@RunWith(AndroidJUnit4::class)
class TouchResponseTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun longPressOpensOnceAndScrollDoesNotSelect() {
        var changes=0
        val feedback=mutableListOf<HapticFeedbackType>()
        var busy by mutableStateOf(false)
        compose.runOnIdle { compose.activity.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides object:HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType:HapticFeedbackType) { feedback.add(hapticFeedbackType) }
            }) { BoleteraTheme { Surface { WalletHome(UiState(stage="balance",selectedCard="DEMO1234",balance=124000,minimum=26000,busy=busy),{changes++},{},{}) } } }
        } }
        compose.onNodeWithText("Saldo disponible").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(1,changes); assertEquals(1,feedback.count { it==HapticFeedbackType.LongPress }) }
        compose.onNodeWithText("Saldo disponible").performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(1,changes); busy=true }
        compose.onNodeWithText("Saldo disponible").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(1,changes) }
    }
    @Test fun primaryReleaseInvokesOnceCancelAndDisabledDoNothing() {
        var calls=0
        var pulses=0
        var enabled by mutableStateOf(true)
        compose.runOnIdle { compose.activity.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides object:HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType:HapticFeedbackType) { pulses++ }
            }) { BoleteraTheme { Column(Modifier.fillMaxSize()) { Primary("Acción",enabled){calls++} } } }
        } }
        compose.onNodeWithText("Acción").performTouchInput { down(center); advanceEventTime(900); up() }
        compose.runOnIdle { assertEquals(1,calls); assertEquals(1,pulses) }
        compose.onNodeWithText("Acción").performTouchInput { down(center); advanceEventTime(100); cancel() }
        compose.runOnIdle { assertEquals(1,calls); assertEquals(1,pulses); enabled=false }
        compose.onNodeWithText("Acción").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1,calls); assertEquals(1,pulses) }
    }
}
