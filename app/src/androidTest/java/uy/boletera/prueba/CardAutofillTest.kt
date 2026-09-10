package uy.boletera.prueba

import android.util.SparseArray
import android.view.View
import android.view.autofill.AutofillValue
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Exercises Android's real virtual-view autofill entry point with fictitious values. */
class CardAutofillTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var host: View
    private var submits=0
    private var allowed by mutableStateOf(true)
    private var challenge by mutableStateOf(false)
    private var error by mutableStateOf(false)
    private fun setup() {
        compose.activity.setContent { BoleteraTheme { Surface {
            host=LocalView.current
            NativeCardForm(false,allowed,error,challenge,{pan,expiry,cvv->
                assertEquals("4111111111111111",pan)
                assertEquals("12/39",expiry)
                assertEquals("123",cvv)
                submits++
            },{})
        } } }
        compose.waitForIdle()
    }
    private fun fill(vararg entries: Pair<String,String>) {
        val values=SparseArray<AutofillValue>()
        entries.forEach { (label,value)->values.put(compose.onNodeWithText(label).fetchSemanticsNode().id,AutofillValue.forText(value)) }
        compose.runOnIdle { host.autofill(values) }
        compose.waitForIdle()
    }
    @Test fun completeAndroidAutofillAdvancesOnceWithoutContinue() {
        setup()
        fill("Número de tarjeta" to "4111 1111 1111 1111","Vencimiento" to "12/2039","CVV" to "123")
        compose.waitUntil(5000) { submits==1 }
        compose.onNodeWithText("CVV").assertTextEquals("CVV","")
        fill("CVV" to "123","Vencimiento" to "12/39","Número de tarjeta" to "4111111111111111")
        assertEquals(1,submits)
    }
    @Test fun partialInvalidAndManualValuesDoNotAutoAdvance() {
        setup()
        fill("Número de tarjeta" to "4111111111111111","Vencimiento" to "12/39")
        assertEquals(0,submits)
        fill("CVV" to "1")
        assertEquals(0,submits)
        compose.onNodeWithText("CVV").performTextClearance()
        compose.onNodeWithText("CVV").performTextInput("123")
        assertEquals(0,submits)
        compose.onNodeWithText("Continuar").performClick()
        assertEquals(1,submits)
    }
    @Test fun providerGateAndBackgroundDoNotReplayAutofill() {
        allowed=false;setup()
        fill("CVV" to "123","Número de tarjeta" to "4111111111111111","Vencimiento" to "12/39")
        assertEquals(0,submits)
        compose.runOnIdle { error=true;allowed=true }
        assertEquals(0,submits)
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle { error=false }
        assertEquals(0,submits)
        compose.onNodeWithText("CVV").assertTextEquals("CVV","")
        fill("CVV" to "123","Vencimiento" to "12/39","Número de tarjeta" to "4111111111111111")
        compose.waitUntil(5000) { submits==1 }
    }
}
