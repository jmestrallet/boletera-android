package uy.boletera.prueba

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

/** Invented data for UI review only. No production fixture, real account or payment. */
class ActivityScreenTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val month=YearMonth.now()
    private fun show(state:ActivityState) {
        compose.runOnIdle {
            (MainActivity::class.java.getDeclaredField("engine").apply {isAccessible=true}.get(compose.activity) as StmEngine).cancel()
            compose.activity.setContent {BoleteraTheme("dark"){ActivityScreen(state,{}, {})}}
        }
    }
    private fun capture(name:String) {
        compose.waitForIdle()
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(compose.activity.getExternalFilesDir(null),name).outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
    @Test fun identityRequirementExplainsAccessWithoutPretendingThereAreNoTrips() {
        show(ActivityState(card="DEMO0001",access=ActivityAccess.IDENTITY_REQUIRED))
        compose.onNodeWithText("Habilitá tus movimientos").assertIsDisplayed()
        compose.onNodeWithText("Cómo habilitar el acceso").assertIsDisplayed()
        compose.onNodeWithText("Usuario frecuente").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("frequentProgress").assertDoesNotExist()
        capture("activity-identity-preview.png")
    }
    private fun ready()=ActivityState(card="DEMO0001",access=ActivityAccess.READY,
        entries=listOf(
            StmMovement("trip","DEMO0001",month.atDay(20).atTime(18,42),MovementKind.TRIP,"Viaje de 1 hora",-5200,detail=listOf("Línea" to "181 · ejemplo")),
            StmMovement("recharge","DEMO0001",month.atDay(20).atTime(10,15),MovementKind.RECHARGE,"Recarga Prex",50000),
            StmMovement("refund","DEMO0001",month.atDay(10).atTime(9,0),MovementKind.REFUND,"Usuario frecuente",8400))+
            (1..33).map {index->StmMovement("example-$index","DEMO0001",month.atDay((index+1)/2).atTime(if(index%2==0)18 else 8,0),MovementKind.TRIP,"Viaje registrado",-5200)},
        availableMonths=listOf(month),completeMonths=setOf(month),
        frequent=listOf(FrequentProgress("DEMO0001",month,34,true,System.currentTimeMillis())))
    @Test fun monthlyActivityFiltersAndOpensTheSourceDetails() {
        show(ready())
        compose.onNodeWithText("34 de 40 viajes").assertIsDisplayed()
        capture("activity-month-preview.png")
        compose.onNodeWithText("Viajes",useUnmergedTree=true).performScrollTo().performClick()
        compose.onNodeWithText("Viaje de 1 hora").performScrollTo().performClick()
        compose.onNodeWithText("Línea: 181 · ejemplo").assertIsDisplayed()
        compose.onNodeWithText("Cerrar detalle").performClick()
        compose.onNodeWithText("Recarga Prex").assertDoesNotExist()
        capture("activity-trips-preview.png")
    }
    @Test fun incompleteMonthAndUnknownEligibilityRemainExplicit() {
        show(ready().copy(completeMonths=emptySet(),frequent=emptyList()))
        compose.onNodeWithTag("frequentProgress").assertDoesNotExist()
        compose.onNodeWithText("El mes todavía no está completo.",substring=true).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("—").assertCountEquals(3)
    }
}
