package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AppErrorScreenTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun managedErrorExplainsCauseShowsDetailsAndDoesNotPretendToSend() {
        compose.runOnIdle {
            val engine=MainActivity::class.java.getDeclaredField("engine").apply {isAccessible=true}.get(compose.activity) as StmEngine
            @Suppress("UNCHECKED_CAST")
            val state=StmEngine::class.java.getDeclaredField("state\$delegate").apply {isAccessible=true}.get(engine) as MutableState<UiState>
            state.value=UiState(stage="blocked",message="No se pudo conectar. Revisá tu conexión.",diagnostic="DEMO-CONEXION")
        }
        compose.onNodeWithText("CONEXIÓN").assertIsDisplayed()
        compose.onNodeWithText("Enviar error al desarrollador").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Próximamente").assertExists()
        compose.onNodeWithText("Ver qué pasó").performClick()
        compose.onNodeWithText("Referencia: DEMO-CONEXION").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ocultar detalle").performClick()
        compose.onNodeWithText("CONEXIÓN").performScrollTo()
        if(compose.activity.resources.configuration.fontScale<=1f) {
            compose.onNodeWithText("Próximamente").assertIsDisplayed()
            val scrolls=compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNodes()
            for(node in scrolls)assertEquals(0f,node.config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].maxValue(),0f)
        }
        compose.waitForIdle();android.os.SystemClock.sleep(300)
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"error-connection.png").outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle()
        compose.onNodeWithText("Volver al inicio").performScrollTo().performClick()
        compose.onNodeWithText("CONEXIÓN").assertDoesNotExist()
        assertEquals("CARGA DE DATOS",errorPresentation("No pudimos leer el saldo o el mínimo con certeza.").code)
        assertEquals("SERVICIO",errorPresentation("El servicio no respondió correctamente.").code)
        assertEquals("PASO INTERRUMPIDO",errorPresentation("Algo desconocido").code)
    }
}
