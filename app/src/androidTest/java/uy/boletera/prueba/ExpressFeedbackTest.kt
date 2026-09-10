package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ExpressFeedbackTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name:String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)}
        shot.recycle()
    }
    @Test fun completionAcknowledgesImmediatelyAndReleaseDoesNotRestart() {
        var starts=0
        var preparing by mutableStateOf(false)
        compose.activity.setContent {BoleteraTheme("dark") {Surface {
            Box(Modifier.padding(24.dp)) {
                ExpressShortcut(26000,!preparing,preparing=preparing) {starts++;preparing=true}
            }
        }}}
        compose.onNodeWithTag("expressShortcut").performTouchInput {down(center);advanceEventTime(700)}
        assertEquals(0,starts)
        assertTrue(compose.onNodeWithTag("expressShortcut").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current<1f)
        compose.onNodeWithTag("expressShortcut").performTouchInput {up()}
        assertEquals(0,starts)
        compose.onNodeWithTag("expressShortcut").performTouchInput {longClick(durationMillis=1250)}
        assertEquals(1,starts)
        compose.onNodeWithText("Preparando tu recarga").assertIsDisplayed()
        compose.onNodeWithText("Ya podés soltar",substring=true).assertIsDisplayed()
        assertEquals(1f,compose.onNodeWithTag("expressShortcut").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current)
        screenshot("express-preparing.png")
        compose.onNodeWithTag("expressShortcut").performTouchInput {longClick(durationMillis=1500)}
        assertEquals(1,starts)
    }
    @Test fun mixesRotateAndTappingThemDoesNotChangeTheCard() {
        var changes=0
        var balance by mutableStateOf(100000L)
        compose.activity.setContent {BoleteraTheme("light") {Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                WalletHome(UiState(stage="balance",selectedCard="DEMO1234",balance=balance,minimum=26000),{changes++},{},{})
            }
        }}}
        val pause = compose.onNodeWithTag("ticketBudget").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions].single().action
        compose.runOnIdle {pause()}
        compose.onNodeWithText("19 de 1 h").performScrollTo().assertIsDisplayed()
        screenshot("ticket-mixes.png")
        compose.onNodeWithTag("ticketBudget").performScrollTo().performClick()
        compose.onNodeWithText("12 de 2 h").assertExists()
        compose.onNodeWithTag("ticketBudget").performClick()
        compose.onNodeWithText("7 de 1 h").assertExists()
        compose.onNodeWithText("8 de 2 h").assertExists()
        assertEquals(0,changes)
        screenshot("ticket-mixed.png")
        val resume = compose.onNodeWithTag("ticketBudget").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsActions.CustomActions].single().action
        compose.runOnIdle {resume()}
        compose.waitUntil(10000) {compose.onAllNodesWithText("4 de 14",substring=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("4 de 14",substring=true).assertExists()
        compose.runOnIdle {balance=-40000}
        compose.onNodeWithTag("ticketBudget").assertDoesNotExist()
    }
}

