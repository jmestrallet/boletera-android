package uy.boletera.prueba

import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Records the actual app and preference activation using fictitious account data. No recarga. */
class ExpressDemo {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun activateFromWallet() {
        lateinit var engine:StmEngine
        compose.runOnIdle { compose.activity.getSharedPreferences("appearance",0).edit().putString("theme","dark").commit() }
        compose.activityRule.scenario.recreate()
        compose.runOnIdle {
            engine=MainActivity::class.java.getDeclaredField("engine").apply { isAccessible=true }.get(compose.activity) as StmEngine
            engine.forgetChoices()
            StmEngine::class.java.getDeclaredField("accountVerified").apply { isAccessible=true }.setBoolean(engine,true)
            (StmEngine::class.java.getDeclaredField("choices").apply { isAccessible=true }.get(engine) as JourneyPreferences).useAccount("00000000")
            assertTrue(engine.savePayer(PayerProfile("express-demo","Mi Prex","Persona","Ficticia","00000000","persona@example.invalid","099123456")))
            @Suppress("UNCHECKED_CAST")
            val state=StmEngine::class.java.getDeclaredField("state\$delegate").apply { isAccessible=true }.get(engine) as MutableState<UiState>
            state.value=UiState(stage="balance",selectedCard="DEMO1234",minimum=26000,balance=124000)
        }
        try {
            android.os.SystemClock.sleep(900)
            compose.onNodeWithText("Activar").performScrollTo().performClick()
            android.os.SystemClock.sleep(1300)
            compose.onNodeWithText("Mantené para activar").performScrollTo()
            android.os.SystemClock.sleep(700)
            compose.mainClock.autoAdvance=false
            compose.onNodeWithText("Mantené para activar").performTouchInput { down(center) }
            repeat(95) { compose.mainClock.advanceTimeBy(16);android.os.SystemClock.sleep(16) }
            compose.onNodeWithText("Express activado").performTouchInput { up() }
            compose.mainClock.autoAdvance=true
            compose.onNodeWithText("Express activado").performScrollTo().assertIsDisplayed()
            android.os.SystemClock.sleep(1800)
            compose.onNodeWithText("Listo, vamos").performScrollTo().performClick()
            compose.onNodeWithText("Recarga express · $ 260").performScrollTo().assertIsDisplayed()
            compose.runOnIdle { assertNotNull(engine.state.express);assertNull(engine.state.activePayment) }
            android.os.SystemClock.sleep(2000)
        } finally { compose.mainClock.autoAdvance=true;compose.runOnIdle {engine.cancel();engine.forgetChoices()} }
    }
}
