package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class ExpressModeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val person=PayerProfile("express-person","Mi Prex","Persona","Ficticia","00000000","persona@example.invalid","099123456")

    @Test fun releaseCancelsAndFullHoldActivatesOnceWithoutPayment() {
        var activations=0
        compose.runOnIdle { compose.activity.setContent { BoleteraTheme("dark") { Surface {
            ExpressSheet(UiState(stage="balance",selectedCard="DEMO1234",minimum=26000),listOf(person),
                onActivate={provider,payer -> assertEquals("1033",provider);assertEquals(person.id,payer);activations++;true},onDisable={},onAddPayer={},onClose={})
        } } } }
        compose.onNodeWithText("Mantené para activar").performScrollTo().performTouchInput { down(center);advanceEventTime(600);up() }
        compose.runOnIdle { assertEquals(0,activations) }
        compose.onNodeWithText("Mantené para activar").performTouchInput { longClick(durationMillis=1500) }
        compose.onNodeWithText("Express activado").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1,activations) }
        compose.onNodeWithText("Todavía no se inició ninguna recarga.").performScrollTo().assertIsDisplayed()
        val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"express-activated.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
        screenshot.recycle()
    }

    @Test fun preferencesAreAccountScopedAndInvalidOrInterruptedRequestsStop() {
        compose.runOnIdle {
            val engine=MainActivity::class.java.getDeclaredField("engine").apply { isAccessible=true }.get(compose.activity) as StmEngine
            val choices=StmEngine::class.java.getDeclaredField("choices").apply { isAccessible=true }.get(engine) as JourneyPreferences
            @Suppress("UNCHECKED_CAST")
            val state=StmEngine::class.java.getDeclaredField("state\$delegate").apply { isAccessible=true }.get(engine) as MutableState<UiState>
            val request=StmEngine::class.java.getDeclaredField("expressRequest").apply { isAccessible=true }
            val snapshot=StmEngine::class.java.getDeclaredMethod("applySnapshot",org.json.JSONObject::class.java).apply { isAccessible=true }
            engine.forgetChoices();choices.useAccount("00000000")
            StmEngine::class.java.getDeclaredField("accountVerified").apply { isAccessible=true }.setBoolean(engine,true)
            StmEngine::class.java.getDeclaredField("pageStage").apply { isAccessible=true }.set(engine,"amount")
            state.value=UiState(stage="balance",selectedCard="DEMO1234",minimum=26000,balance=124000)
            assertFalse(engine.configureExpress("1033",person.id))
            assertTrue(engine.savePayer(person))
            assertTrue(engine.configureExpress("1033",person.id))
            val saved=choices.express
            choices.useAccount("11111111");assertNull(choices.express)
            choices.useAccount("00000000");assertEquals(saved,choices.express)
            state.value=state.value.copy(selectedCard="OTHER5678")
            engine.startExpress();assertNull(request.get(engine))
            state.value=state.value.copy(selectedCard="DEMO1234",message="")
            engine.startExpress();assertNotNull(request.get(engine))
            engine.pause();assertNull(request.get(engine))
            snapshot.invoke(engine,org.json.JSONObject("""{"stage":"paymentBoundary","providers":[{"id":"1033","name":"Prex"}]}"""))
            assertNull(engine.state.activePayment);assertEquals("paymentBoundary",engine.state.stage)
            state.value=state.value.copy(stage="balance")
            request.set(engine,saved)
            snapshot.invoke(engine,org.json.JSONObject("""{"stage":"paymentBoundary","providers":[]}"""))
            assertNull(engine.state.activePayment);assertNull(request.get(engine));assertTrue(engine.state.message.contains("Express"))
            engine.cancel();engine.forgetChoices()
            choices.useAccount("00000000");assertNull(choices.express)
        }
    }
}
