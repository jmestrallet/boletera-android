package uy.boletera.prueba

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpressModeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val person=PayerProfile("express-person","Mi Prex","Persona","Ficticia","00000000","persona@example.invalid","099123456")

    @Test fun ordinaryChargeStaysPrimaryShortcutIsOptionalAndOnlyFullHoldStartsOnce() {
        var starts=0;var common=0
        var available by mutableStateOf(false)
        var busy by mutableStateOf(false)
        compose.activity.setContent { BoleteraTheme("dark") { Surface {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                WalletHome(UiState(stage="balance",selectedCard="DEMO1234",minimum=26000,balance=120000,busy=busy),{},{common++},{},
                    onExpressCharge=if(available)({starts++}) else null)
            }
        } } }
        compose.onNodeWithText("Modo Express").assertDoesNotExist()
        compose.onNodeWithText("Recargar boletera").performScrollTo().performClick();assertEquals(1,common)
        compose.runOnIdle { available=true }
        compose.onNodeWithText("Modo Express").performScrollTo().performClick();assertEquals(0,starts)
        compose.onNodeWithText("Modo Express").performTouchInput { down(center);advanceEventTime(600);up() };assertEquals(0,starts)
        compose.onNodeWithText("Modo Express").performTouchInput { longClick(durationMillis=1500) };assertEquals(1,starts)
        compose.onNodeWithText("Recargar boletera").assertExists()
        compose.onNodeWithText("Activar").assertDoesNotExist();compose.onNodeWithText("Ajustar").assertDoesNotExist()
        compose.runOnIdle {busy=true}
        compose.onNodeWithText("Modo Express").performTouchInput { longClick(durationMillis=1500) };assertEquals(1,starts)
    }

    @Test fun visibilityRequiresCompleteDefaultsAndInterruptedRequestsCannotContinue() {
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
            state.value=UiState(stage="balance",selectedCard="DEMO1234",cards=listOf(CardInfo("DEMO1234",true,"Operativa")),minimum=26000,balance=124000)
            assertFalse(engine.expressAvailable)
            assertTrue(engine.savePayer(person));assertFalse(engine.expressAvailable)
            choices.provider="1033";assertFalse(engine.expressAvailable)
            choices.card="DEMO1234";assertTrue(engine.expressAvailable)
            choices.provider="1002";assertFalse(engine.expressAvailable);choices.provider="1033"
            state.value=state.value.copy(minimum=null);assertFalse(engine.expressAvailable)
            state.value=state.value.copy(minimum=26000,selectedCard="OTHER5678");assertFalse(engine.expressAvailable)
            engine.startExpress();assertNull(request.get(engine))
            state.value=state.value.copy(selectedCard="DEMO1234")
            val savedKeys=compose.activity.getSharedPreferences("journey_choices",0).all.keys.toSet()
            engine.startExpress();assertNotNull(request.get(engine))
            assertEquals(savedKeys,compose.activity.getSharedPreferences("journey_choices",0).all.keys.toSet())
            engine.pause();assertNull(request.get(engine))
            snapshot.invoke(engine,org.json.JSONObject("""{"stage":"paymentBoundary","providers":[{"id":"1033","name":"Prex"}]}"""))
            assertNull(engine.state.activePayment);assertEquals("paymentBoundary",engine.state.stage)
            request.set(engine,ExpressChoice("DEMO1234","1033",person.id))
            snapshot.invoke(engine,org.json.JSONObject("""{"stage":"paymentBoundary","providers":[]}"""))
            assertNull(engine.state.activePayment);assertNull(request.get(engine))
            choices.useAccount("11111111");assertFalse(engine.expressAvailable)
            engine.cancel();engine.forgetChoices()
        }
    }
}
