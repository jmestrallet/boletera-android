package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class HomeFitTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var state:MutableState<UiState>
    private fun prepare() {
        compose.runOnIdle {compose.activity.getSharedPreferences("appearance",0).edit().putString("theme","dark").commit()}
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            val engine=MainActivity::class.java.getDeclaredField("engine").apply {isAccessible=true}.get(compose.activity) as StmEngine
            val choices=StmEngine::class.java.getDeclaredField("choices").apply {isAccessible=true}.get(engine) as JourneyPreferences
            @Suppress("UNCHECKED_CAST")
            val backing=StmEngine::class.java.getDeclaredField("state\$delegate").apply {isAccessible=true}.get(engine) as MutableState<UiState>
            state=backing
            engine.forgetChoices();choices.useAccount("00000000")
            StmEngine::class.java.getDeclaredField("accountVerified").apply {isAccessible=true}.setBoolean(engine,true)
            engine.savePayer(PayerProfile("home-test","Mi Prex","Persona","Ficticia","00000000","persona@example.invalid","099123456"))
            choices.provider="1033";choices.card="DEMO1234"
            state.value=UiState(stage="balance",selectedCard="DEMO1234",cards=listOf(CardInfo("DEMO1234",true,"Operativa")),minimum=26000,balance=100000,consultedAt=System.currentTimeMillis())
        }
    }
    private fun verifyFit() {
        compose.onNodeWithContentDescription("Boletos y tarifas").assertIsDisplayed()
        compose.onNodeWithText("Carga Express").assertIsDisplayed()
        compose.onNodeWithText("Recargar boletera").assertIsDisplayed()
        val scrolls=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
        for(node in scrolls)assertEquals("La home debe entrar completa sin desplazamiento",0f,node.config[SemanticsProperties.VerticalScrollAxisRange].maxValue(),0f)
    }
    private fun screenshot(name:String) {
        compose.waitForIdle();android.os.SystemClock.sleep(300)
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
    }
    @Test fun realHomeFitsWithExpressAndUsesTheCurrentMinimum() {
        prepare();verifyFit()
        compose.onNodeWithTag("ticketBudget").assertDoesNotExist()
        compose.runOnIdle {state.value=state.value.copy(minimum=56400)}
        compose.onNodeWithText("$ 564").assertExists()
        compose.onNodeWithText("Carga Express").performClick()
        compose.onNodeWithText("Ahora es $ 564",substring=true).assertExists()
        compose.onNodeWithText("Ahora no").performClick()
        compose.runOnIdle {state.value=state.value.copy(minimum=73100)}
        compose.onNodeWithText("$ 731").assertExists()
        verifyFit()
        screenshot("home-fit-positive.png")
        compose.runOnIdle {state.value=state.value.copy(balance=-4500)}
        verifyFit();screenshot("home-fit-negative.png")
        compose.onNodeWithContentDescription("Boletos y tarifas").performClick()
        compose.onNodeWithText("1 hora").assertExists()
    }
    @Test fun hiddenExpressHelpStaysHiddenAfterCancelledHoldAndActivityRecreation() {
        val prefs=compose.activity.getSharedPreferences("feature_help",0)
        compose.runOnIdle {prefs.edit().remove("express_skip_intro_v1").commit()}
        try {
            prepare()
            compose.onNodeWithText("Carga Express").performScrollTo().performClick()
            compose.onNodeWithText("No volver a mostrar").performScrollTo().performClick()
            compose.onNodeWithText("Ahora no").performClick()
            compose.runOnIdle {assertTrue(prefs.getBoolean("express_skip_intro_v1",false))}
            compose.onNodeWithTag("expressShortcut").performScrollTo().performTouchInput {down(center);advanceEventTime(700);up()}
            compose.onNodeWithText("Así funciona Carga Express").assertDoesNotExist()
            prepare() // Recreates the real activity; the choice must survive.
            compose.onNodeWithTag("expressShortcut").performScrollTo().performTouchInput {down(center);advanceEventTime(700);up()}
            compose.onNodeWithText("Así funciona Carga Express").assertDoesNotExist()
            compose.onNodeWithTag("expressShortcut").performTouchInput {click()}
            compose.onNodeWithText("Así funciona Carga Express").assertDoesNotExist()
            compose.runOnIdle {assertEquals("balance",state.value.stage);assertFalse(state.value.busy)}
        } finally {compose.runOnIdle {prefs.edit().remove("express_skip_intro_v1").commit()}}
    }
}
