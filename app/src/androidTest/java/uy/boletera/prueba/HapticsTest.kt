package uy.boletera.prueba

import android.provider.Settings
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run only on the test emulator: restore its system preference after the check. */
class HapticsTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun shell(command:String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use {input->input.readBytes()}
        }
    }
    @Test fun settingsButtonRequestsTwoPulsesAndHonorsAndroidTouchFeedbackOff() {
        assertTrue("This test changes settings only on an emulator",android.os.Build.FINGERPRINT.contains("generic")||android.os.Build.MODEL.contains("sdk"))
        val original=Settings.System.getInt(compose.activity.contentResolver,Settings.System.HAPTIC_FEEDBACK_ENABLED,1)
        try {
            shell("settings put system haptic_feedback_enabled 1")
            compose.onNodeWithContentDescription("Configuración").performClick()
            compose.onNodeWithText("Probar vibración").performScrollTo().performClick()
            compose.onNodeWithText("Se pidieron dos pulsos.",substring=true).assertIsDisplayed()
            compose.runOnIdle {AppHaptics(compose.activity).performHapticFeedback(HapticFeedbackType.ContextClick)}
            shell("settings put system haptic_feedback_enabled 0")
            compose.onNodeWithText("Probar vibración").performClick()
            compose.onNodeWithText("La respuesta táctil está apagada en Android.",substring=true).assertIsDisplayed()
            compose.runOnIdle {assertEquals(PulseResult.Disabled,AppHaptics(compose.activity).testPulse())}
        } finally {shell("settings put system haptic_feedback_enabled $original")}
    }
}
