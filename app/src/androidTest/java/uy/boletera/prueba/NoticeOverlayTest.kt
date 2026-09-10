package uy.boletera.prueba

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NoticeOverlayTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun noticeDoesNotMoveWelcomeAndCanAppearAgainAfterDismissal() {
        lateinit var engine: StmEngine
        compose.runOnIdle {
            engine = MainActivity::class.java.getDeclaredField("engine").apply { isAccessible = true }.get(compose.activity) as StmEngine
            engine.savedAccess(true)
        }
        val hero = compose.onNodeWithText("Tu próximo viaje\nempieza acá.")
        val before = hero.fetchSemanticsNode().boundsInRoot
        val message = "No se desbloqueó el acceso. Podés volver a intentar o ingresar manualmente."
        compose.runOnIdle { engine.notice(message) }
        compose.onNodeWithText(message).assertIsDisplayed()
        assertEquals(before, hero.fetchSemanticsNode().boundsInRoot)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(compose.activity.getExternalFilesDir(null), "notice-overlay.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        compose.waitUntil(10000) { engine.state.message.isEmpty() }
        compose.onNodeWithText(message).assertDoesNotExist()
        compose.runOnIdle { engine.notice(message) }
        compose.onNodeWithText(message).assertIsDisplayed()
        assertEquals(before, hero.fetchSemanticsNode().boundsInRoot)
    }
}
