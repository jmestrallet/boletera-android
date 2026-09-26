package uy.boletera.prueba

import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Verifies the real Android service receives a manual request for the card's virtual ID. */
class CardAutofillProviderTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText().trim() }
    @Test fun arrivalAndRetryRequestTheCardFieldFromTheSystemService() {
        val previous = shell("settings get secure autofill_service")
        val testPackage = InstrumentationRegistry.getInstrumentation().context.packageName
        val proof = "run-as $testPackage cat files/card-autofill-proof"
        var submissions = 0
        try {
            shell("run-as $testPackage rm -f files/card-autofill-proof")
            shell("settings put secure autofill_service $testPackage/uy.boletera.prueba.FixtureCardAutofillService")
            compose.activity.setContent { BoleteraTheme { Surface {
                NativeCardForm(false,true,false,false,{_,_,_->submissions++},{},focusCard=true)
            } } }
            compose.waitUntil(10000) { shell(proof).contains("fields=3;manual=true;focusedCard=true") }
            val before = shell(proof).lines().size
            compose.onNodeWithTag("requestCardAutofill").performClick()
            compose.waitUntil(10000) { shell(proof).lines().size > before }
            assertTrue(shell(proof).lines().last().contains("fields=3;manual=true;focusedCard=true"))
            assertEquals(0,submissions)
            val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            java.io.File(compose.activity.getExternalFilesDir(null),"express-card-autofill.png").outputStream().use {
                shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
            shot.recycle()
        } finally {
            if (previous == "null" || previous.isBlank()) shell("settings delete secure autofill_service")
            else shell("settings put secure autofill_service $previous")
            shell("run-as $testPackage rm -f files/card-autofill-proof")
        }
    }
}
