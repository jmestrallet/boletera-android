package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Production composables with explicit fictitious values. No authentication or payment. */
@RunWith(AndroidJUnit4::class)
class DesignReviewTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun capture(name:String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(500)
        val nodes=compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),useUnmergedTree=true).fetchSemanticsNodes()
        nodes.forEach { node ->
            val layouts=mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            // MultiParagraph can retain the parent width after Text adopts its intrinsic width.
            // Check the laid-out glyph lines, not that wider paragraph allocation.
            layouts.forEach { layout ->
                assertFalse("Text has hidden lines: ${layout.layoutInput.text}",layout.multiParagraph.didExceedMaxLines)
                for(line in 0 until layout.lineCount) {
                    assertTrue("Text clips horizontally: ${layout.layoutInput.text}",layout.getLineRight(line)-layout.getLineLeft(line)<=layout.size.width+1f)
                    assertTrue("Text clips vertically: ${layout.layoutInput.text}",layout.getLineBottom(line)<=layout.size.height+1f)
                    assertFalse("Text is ellipsized: ${layout.layoutInput.text}",layout.isLineEllipsized(line))
                }
            }
        }
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"design-$name.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) }
        shot.recycle()
    }
    @Test fun captureWelcome() { compose.onNodeWithText("Tu próximo viaje\nempieza acá.").assertIsDisplayed();capture("welcome") }
    @Test fun walletAndAmountSelection() {
        var choice:Long?=null
        compose.runOnIdle {
            compose.activity.setContent {
                var sheet by remember{mutableStateOf(false)}
                val state=UiState(stage="balance",selectedCard="DEMO1234",balance=124000,minimum=26000,consultedAt=1_783_683_600_000)
                BoleteraTheme("light") { Surface(color=Paper) { Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AppTopBar(onSettings={})
                    Column(Modifier.weight(1f).widthIn(max=600.dp).fillMaxWidth().align(androidx.compose.ui.Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).padding(24.dp)) { WalletHome(state,{}, {sheet=true},{},{},{}) }
                    if(sheet)AmountSheet(state,{sheet=false}){choice=it;sheet=false}
                } } }
            }
        }
        capture("wallet-light")
        compose.onNodeWithText("Recargar boletera").performScrollTo().assertIsDisplayed().performClick()
        capture("amount")
        compose.onNodeWithText("$ 500").performScrollTo().performClick()
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(50000L,choice)}
    }
    @Test fun darkWallet() {
        compose.runOnIdle {
            compose.activity.setContent { BoleteraTheme("dark") { Surface(color=Paper) { Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                AppTopBar(onSettings={})
                Column(Modifier.weight(1f).widthIn(max=600.dp).fillMaxWidth().align(androidx.compose.ui.Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).padding(24.dp)) { WalletHome(UiState(stage="balance",selectedCard="DEMO1234",balance=124000,minimum=26000),{},{},{},{},{}) }
            } } } }
        }
        capture("wallet-dark")
    }
    @Test fun settingsThemeIsPersistentAndScrollable() {
        compose.onNodeWithContentDescription("Configuración").performClick()
        compose.onNodeWithText("Oscuro").performClick()
        compose.onNodeWithText("Buscar actualizaciones").performScrollTo().assertIsDisplayed()
        capture("settings-dark")
        compose.onNodeWithText("Claro").performScrollTo().performClick()
        capture("settings-light")
    }
    @Test fun paymentChoiceIsClearWithoutStartingAnOperation() {
        compose.runOnIdle {
            val engine=MainActivity::class.java.getDeclaredField("engine").apply{isAccessible=true}.get(compose.activity) as StmEngine
            @Suppress("UNCHECKED_CAST")
            val state=StmEngine::class.java.getDeclaredField("state\$delegate").apply{isAccessible=true}.get(engine) as MutableState<UiState>
            state.value=UiState(stage="paymentBoundary",selectedCard="DEMO1234",amount=50000,providers=listOf(ProviderInfo("1033","Prex"),ProviderInfo("1002","eBROU")),selectedProvider="1033")
        }
        capture("payment")
        compose.onNodeWithText("Pagar $ 500").assertIsNotEnabled()
    }
}
