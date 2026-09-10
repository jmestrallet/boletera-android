package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Every profile and screen value is synthetic. No provider request is made. */
class PaymentUxTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun appearance() {
        val theme = InstrumentationRegistry.getArguments().getString("appearance") ?: "light"
        compose.runOnIdle { compose.activity.getSharedPreferences("appearance", 0).edit().putString("theme", theme).commit() }
        compose.activityRule.scenario.recreate()
    }
    private val person = PayerProfile("ux-one", "Mi Prex", "Persona", "Ficticia", "00000000", "persona@example.invalid", "099123456")
    private lateinit var engine: StmEngine
    private fun setup(profiles: List<PayerProfile>, selected: String? = profiles.firstOrNull()?.id) {
        compose.runOnIdle {
            engine = MainActivity::class.java.getDeclaredField("engine").apply { isAccessible = true }.get(compose.activity) as StmEngine
            engine.forgetChoices()
            StmEngine::class.java.getDeclaredField("accountVerified").apply { isAccessible = true }.setBoolean(engine, true)
            (StmEngine::class.java.getDeclaredField("choices").apply { isAccessible = true }.get(engine) as JourneyPreferences).useAccount("00000000")
            profiles.forEach { assertTrue(engine.savePayer(it)) }
            @Suppress("UNCHECKED_CAST")
            val state = StmEngine::class.java.getDeclaredField("state\$delegate").apply { isAccessible = true }.get(engine) as MutableState<UiState>
            state.value = UiState(stage = "paymentBoundary", selectedCard = "DEMO1234", amount = 26000, minimum = 26000,
                selectedProvider = "1033", payerProfileId = selected, providers = listOf(ProviderInfo("1033", "Prex"), ProviderInfo("1002", "eBROU")))
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(500)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), "payment-ux-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun firstUseOpensTheFormDirectlyAndSavesWithoutAnExtraProfileName() {
        setup(emptyList())
        compose.onNodeWithText("Agregar datos").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Tu medio habitual").assertDoesNotExist()
        capture("first-use")
        compose.onNodeWithText("Agregar datos").performClick()
        compose.onNodeWithText("Agregar datos del titular").assertIsDisplayed()
        compose.onNodeWithText("Elegí un titular").assertDoesNotExist()
        compose.onNodeWithText("Guardar datos").assertIsDisplayed().performClick()
        compose.onNodeWithText("Completá el nombre y el apellido.").assertIsDisplayed()
        listOf("Nombre" to "Persona", "Apellido" to "Ficticia", "Número de cédula" to "00000000", "Correo electrónico" to "persona@example.invalid", "Celular" to "099123456").forEach { (label, value) ->
            compose.onNodeWithText(label).performScrollTo().performTextInput(value)
        }
        compose.onNodeWithText("Guardar datos").assertIsDisplayed().performClick()
        compose.onNodeWithText("Continuar · $ 260").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle {
            assertEquals(1, engine.payerProfiles.size)
            assertEquals("Persona Ficticia", engine.payerProfiles.single().label)
            assertEquals(engine.payerProfiles.single().id, engine.state.payerProfileId)
            assertNull(engine.state.activePayment)
        }
    }
    @Test fun onePayerKeepsTheNextActionVisibleAndEditingIsDirect() {
        setup(listOf(person))
        val button = compose.onNodeWithText("Continuar · $ 260")
        button.assertIsDisplayed().assertIsEnabled()
        assertTrue(button.getUnclippedBoundsInRoot().bottom <= compose.onRoot().getUnclippedBoundsInRoot().bottom)
        compose.onNodeWithText("Persona Ficticia").assertIsDisplayed()
        capture("one-payer")
        compose.onNodeWithText("Editar").performScrollTo().performClick()
        compose.onNodeWithText("Editar datos del titular").assertIsDisplayed()
        compose.onNodeWithText("Guardar datos").assertIsDisplayed()
        capture("editor")
    }
    @Test fun multiplePayersOnlyOpenAListWhenChangingSelection() {
        setup((1..12).map { person.copy(id = "ux-$it", givenName = "Persona $it", label = "Titular $it") })
        compose.onNodeWithText("Continuar · $ 260").assertIsDisplayed()
        compose.onNodeWithText("Persona 12 Ficticia").assertDoesNotExist()
        compose.onNodeWithText("Cambiar titular").performScrollTo().performClick()
        compose.onNodeWithText("Elegí un titular").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(13)
        compose.onNodeWithText("Persona 12 Ficticia").performClick()
        compose.onNodeWithText("Persona 12 Ficticia").assertExists()
        compose.onNodeWithText("Continuar · $ 260").assertIsDisplayed()
        compose.runOnIdle { assertEquals("ux-12", engine.state.payerProfileId); assertNull(engine.state.activePayment) }
    }
    @Test fun brouDoesNotAskForPrexPayerDetails() {
        setup(emptyList())
        compose.onNodeWithText("eBROU").performClick()
        compose.onNodeWithText("Datos del titular").assertDoesNotExist()
        compose.onNodeWithText("Se abre eBROU en Chrome.").assertIsDisplayed()
        compose.onNodeWithText("Continuar · $ 260").assertIsDisplayed().assertIsEnabled()
        capture("brou")
    }
}
