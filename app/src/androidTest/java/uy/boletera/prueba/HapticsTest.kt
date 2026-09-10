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
    private fun shell(command:String):String {
        return InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use {input->input.readBytes().toString(Charsets.UTF_8)}
        }
    }
    @Test fun appSwitchControlsVibrationWithAndroidTouchFeedbackOff() {
        assertTrue("Emulator only",android.os.Build.FINGERPRINT.contains("generic")||android.os.Build.MODEL.contains("sdk"))
        val original=Settings.System.getString(compose.activity.contentResolver,Settings.System.HAPTIC_FEEDBACK_ENABLED)
        val prefs=compose.activity.getSharedPreferences("vibration",0)
        val originalApp=if(prefs.contains("enabled"))prefs.getBoolean("enabled",true) else null
        val existing=AppHaptics(compose.activity)
        try {
            existing.enabled=true
            shell("settings put system haptic_feedback_enabled 0")
            compose.onNodeWithContentDescription("Configuración").performClick()
            compose.onNodeWithText("Probar vibración").performScrollTo().performClick()
            compose.onNodeWithText("Se pidieron dos pulsos.",substring=true).assertIsDisplayed()
            compose.waitForIdle()
            val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            java.io.File(compose.activity.getExternalFilesDir(null),"haptics-0.2.29.png").outputStream().use {shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            shot.recycle()
            compose.runOnIdle {existing.performHapticFeedback(HapticFeedbackType.ContextClick)}
            Thread.sleep(400)
            val dump=shell("dumpsys vibrator_manager")
            assertTrue("System must execute the app vibration: $dump",dump.lineSequence().any {it.contains("uy.boletera.prueba")&&it.contains("FINISHED",ignoreCase=true)&&it.contains("MEDIA")})
            compose.onNodeWithContentDescription("Vibración de Boletera").performScrollTo().performClick()
            compose.runOnIdle {
                assertFalse(AppHaptics(compose.activity).enabled)
                assertEquals(PulseResult.Disabled,existing.testPulse())
            }
            compose.onNodeWithText("Probar vibración").performScrollTo().performClick()
            compose.onNodeWithText("La vibración de Boletera está apagada.",substring=true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Vibración de Boletera").performScrollTo().performClick()
            compose.runOnIdle {assertEquals(PulseResult.Requested,existing.testPulse())}
            assertEquals(0,Settings.System.getInt(compose.activity.contentResolver,Settings.System.HAPTIC_FEEDBACK_ENABLED))
        } finally {
            if(original==null)shell("settings delete system haptic_feedback_enabled") else shell("settings put system haptic_feedback_enabled $original")
            if(originalApp==null)prefs.edit().remove("enabled").commit() else prefs.edit().putBoolean("enabled",originalApp).commit()
        }
    }
}
