package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Visual demonstration of production controls with fictitious data and no financial engine. */
class ExpressDemo {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun shortcutBelowOrdinaryRecharge() {
        var started by mutableStateOf(false)
        compose.activity.setContent { BoleteraTheme("dark") { Surface {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                AppTopBar()
                if(started)NativeCardForm(false,true,false,false,{_,_,_->},{})
                else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=24.dp)) {
                    WalletHome(UiState(stage="balance",selectedCard="DEMO1234",balance=124000,minimum=26000),{},{},{},onExpressCharge={started=true})
                }
            }
        } } }
        compose.onNodeWithText("Carga Express").performScrollTo()
        compose.onNodeWithText("Recargar boletera").assertIsDisplayed()
        compose.waitForIdle()
        android.os.SystemClock.sleep(700)
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"express-shortcut.png").outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
        compose.mainClock.autoAdvance=false
        compose.onNodeWithText("Carga Express").performTouchInput { down(center) }
        repeat(50){compose.mainClock.advanceTimeBy(16);android.os.SystemClock.sleep(16)}
        val held=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"express-shortcut-held.png").outputStream().use {held.compress(Bitmap.CompressFormat.PNG,100,it)};held.recycle()
        repeat(45){compose.mainClock.advanceTimeBy(16);android.os.SystemClock.sleep(16)}
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.autoAdvance=true
        compose.onNodeWithText("Número de tarjeta").assertExists()
        assertTrue(started)
    }
}
