package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class TicketGuideTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(500)
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),name).outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)}
        shot.recycle()
    }
    @Test fun guideExpandsExplanationsAndClosesWithoutStartingAnything() {
        var show by mutableStateOf(true)
        compose.activity.setContent { BoleteraTheme("dark") { Surface { if(show)TicketGuideSheet {show=false} } } }
        compose.onNodeWithText("Boletos y tarifas").assertIsDisplayed()
        screenshot("ticket-guide-dark.png")
        compose.onNodeWithText("Combinación metropolitana").performScrollTo().performClick()
        compose.onNodeWithText("Combina un urbano",substring=true).performScrollTo().assertIsDisplayed()
        screenshot("ticket-guide-metropolitana.png")
        compose.onNodeWithText("Paradas de intercambio").performScrollTo().performClick()
        compose.onNodeWithText("Son paradas habilitadas",substring=true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Combina un urbano",substring=true).assertDoesNotExist()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Boletos y tarifas").assertDoesNotExist()
        assertFalse(show)
    }
    @Test fun acceptingWithoutOptOutDoesNotSilentlySelectIt() {
        var accepted: Boolean?=null
        compose.activity.setContent { BoleteraTheme("light") { ExpressIntroDialog(26000,{}, {accepted=it}) } }
        screenshot("express-intro-light.png")
        compose.onNodeWithText("No volver a mostrar").performScrollTo().assertIsOff()
        compose.onNodeWithText("Aceptar y continuar").performClick()
        assertEquals(false,accepted)
    }
    @Test fun guideIsAvailableFromSettingsWithoutLoginAndReturnsToSettingsAccess() {
        compose.onNodeWithContentDescription("Configuración").performClick()
        compose.onNodeWithText("Boletos y tarifas").performScrollTo().performClick()
        compose.onNodeWithText("1 hora").assertExists()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithContentDescription("Configuración").assertIsDisplayed()
    }
}
