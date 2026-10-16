package uy.boletera.prueba

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Lab sign-in, card picker and the amount panel with example data; screenshots for review. */
@RunWith(AndroidJUnit4::class)
class LabScreensTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun capture(name: String) {
        compose.waitForIdle(); android.os.SystemClock.sleep(900)
        val shot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        shot.recycle()
    }
    /** Like the app: an error or a result is a moment, so it has the way back and no settings. */
    @Composable private fun Frame(back: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
        BoleteraTheme("dark") {
            Box(Modifier.fillMaxSize()) {
                LabAmbient()
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    LabTopBar(onBack = if (back) ({}) else null, onSettings = if (back) null else ({}))
                    Column(Modifier.padding(horizontal = 24.dp), content = content)
                }
            }
        }
    }
    @Test fun signInWithFingerprintAndPassword() {
        var manual by mutableStateOf(false)
        var unlocks = 0
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                Frame {
                    LabWelcome(UiState(stage = "welcome", hasSavedAccess = true), savedAccess = true, authenticating = false,
                        onUnlock = { unlocks++ }, onManual = { manual = true }, onAccessGuide = {})
                }
                BoleteraTheme("dark") { LabSignInSheet(open = manual, document = "", onDocument = {}, password = "", onPassword = {}, reveal = false, onReveal = {},
                    save = true, onSave = {}, canSave = true, enabled = true, onClose = { manual = false }, onSubmit = {}) }
            }
        }
        compose.onNodeWithText("Entrar con huella").performClick()
        assertEquals(1, unlocks)
        compose.onNodeWithText("Proyecto independiente y no oficial de STM").assertIsDisplayed()
        compose.onNodeWithText("¿Es tu primer ingreso a STM?").assertDoesNotExist()
        capture("lab-welcome-fingerprint.png")
        compose.onNodeWithText("Ingresar con contraseña").performClick()
        compose.onNodeWithText("Tu cuenta de gub.uy").assertIsDisplayed()
        compose.onNodeWithText("Entrar con huella la próxima vez").assertIsDisplayed()
        capture("lab-welcome-password.png")
    }
    @Test fun firstAccessShowsGuideLink() {
        compose.setContent { Frame {
            LabWelcome(UiState(stage = "welcome"), savedAccess = false, authenticating = false, onUnlock = {}, onManual = {}, onAccessGuide = {})
        } }
        compose.onNodeWithText("Con tu cuenta de gub.uy.").assertIsDisplayed()
        compose.onNodeWithText("¿Es tu primer ingreso a STM?").assertIsDisplayed()
        capture("lab-welcome-first.png")
    }
    @Test fun signInCardShowsLoadingAndSuccess() {
        var phase by mutableStateOf(HeroPhase.IDLE)
        compose.setContent { Frame {
            LabWelcome(UiState(stage = "welcome", hasSavedAccess = true), savedAccess = true, authenticating = false,
                onUnlock = {}, onManual = {}, onAccessGuide = {}, heroPhase = phase, opening = false)
        } }
        android.os.SystemClock.sleep(1600)
        capture("lab-signin-idle.png")
        phase = HeroPhase.LOADING
        compose.onNodeWithText("Conectando con STM…").assertIsDisplayed()
        android.os.SystemClock.sleep(500)
        capture("lab-signin-loading.png")
        phase = HeroPhase.SUCCESS
        android.os.SystemClock.sleep(250)
        capture("lab-signin-success.png")
        compose.onNodeWithText("Listo.").assertIsDisplayed()
    }
    @Test fun signInErrorStaysWithSiteLink() {
        var opened = 0
        compose.setContent { Frame {
            LabWelcome(UiState(stage = "welcome", hasSavedAccess = true), savedAccess = true, authenticating = false,
                onUnlock = {}, onManual = {}, onAccessGuide = {}, heroPhase = HeroPhase.ERROR, opening = false,
                errorText = "STM no respondió. Probá de nuevo en un momento.", onOpenSite = { opened++ })
        } }
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("STM no responde").assertIsDisplayed()
        compose.onNodeWithText("Volver a intentar").assertIsDisplayed()
        capture("lab-signin-error.png")
        compose.onNodeWithText("Probar en el sitio de STM ↗").performClick()
        assertEquals(1, opened)
    }
    @Test fun errorScreenOffersRetryAndSite() {
        var retried = 0
        compose.setContent { Frame(back = true) {
            AppErrorScreen("STM no está respondiendo (error 503). La página oficial parece caída o con problemas.", "ABC123",
                onOpenSite = {}, onRetry = { retried++ }, onExit = {})
        } }
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("STM no responde").assertIsDisplayed()
        // Same shape as the sign-in error: retry is the button, the site is a link in the text, one exit (the arrow).
        compose.onNodeWithText("Probar en el sitio de STM ↗").assertIsDisplayed()
        compose.onNodeWithText("Volver al inicio").assertDoesNotExist()
        capture("lab-error-outage.png")
        compose.onNodeWithText("Volver a intentar").performClick()
        assertEquals(1, retried)
    }
    @Test fun missingDataSaysBalanceUnavailable() {
        compose.setContent { Frame(back = true) {
            AppErrorScreen("No pudimos leer el saldo de STM.", onOpenSite = {}, onRetry = {}, onExit = {})
        } }
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("Saldo no disponible").assertIsDisplayed()
        compose.onNodeWithText("STM no terminó de mostrar la información.").assertIsDisplayed()
        capture("lab-error-data.png")
    }
    @Test fun paymentCutGoesBackToTheBalance() {
        var exits = 0
        compose.setContent { Frame(back = true) {
            AppErrorScreen("La conexión se interrumpió durante el pago.", payment = true, paymentAmount = 50000, onExit = { exits++ })
        } }
        android.os.SystemClock.sleep(1200)
        // Says what happened and nothing about charges; never offers the site in the middle of a payment.
        compose.onNodeWithText("El pago se interrumpió").assertIsDisplayed()
        compose.onNodeWithText("Mirá tu saldo en el inicio antes de volver a cargar.").assertIsDisplayed()
        compose.onNodeWithText("Enviar reporte").assertIsDisplayed()
        compose.onNodeWithText("Revisá el pago").assertDoesNotExist()
        compose.onNodeWithText("Probar en el sitio de STM ↗").assertDoesNotExist()
        capture("lab-error-payment.png")
        compose.onNodeWithText("Volver al inicio").performClick()
        assertEquals(1, exits)
    }
    @Test fun expiredSessionAsksToComeBackIn() {
        compose.setContent { Frame {
            LabWelcome(UiState(stage = "welcome", hasSavedAccess = true, sessionExpired = true, paymentNeedsReview = true), savedAccess = true,
                authenticating = false, onUnlock = {}, onManual = {}, onAccessGuide = {})
        } }
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("Volvé a entrar").assertIsDisplayed()
        compose.onNodeWithText("STM cierra la sesión después de un rato.").assertIsDisplayed()
        compose.onNodeWithText("No se repite la recarga", substring = true).assertDoesNotExist()
        capture("lab-session-expired.png")
    }
    @Test fun rechargeDoneShowsReportedBalance() {
        var done = 0
        compose.setContent { Frame {
            LabRechargeDone(UiState(stage = "balance", selectedCard = "ABCD1234", balance = 174000, consultedAt = System.currentTimeMillis()), 50000,
                listOf("Número de autorización:" to "123456", "Importe:" to "$ 500,00")) { done++ }
        } }
        android.os.SystemClock.sleep(1200)
        compose.onNodeWithText("Recarga hecha").assertIsDisplayed()
        compose.onNodeWithText("Cargaste $ 500 en STM ···· 1234.").assertIsDisplayed()
        compose.onNodeWithText("Ver detalle del pago").assertIsDisplayed()
        capture("lab-recharge-done.png")
        compose.onNodeWithText("Listo").performClick()
        assertEquals(1, done)
    }
    @Test fun firstTimeWithTwoCardsAsksWhichOne() {
        var chosen = ""
        val state = UiState(stage = "cards", cards = listOf(CardInfo("ABCD1234", true, "Operativa"), CardInfo("DEAD5678", false, "Pte. Anular (Caducidad G.U.)")))
        compose.setContent { Frame { LabCards(state) { chosen = it } } }
        compose.onNodeWithText("¿Qué boletera usás?").assertIsDisplayed()
        // STM's reason is not shown: just «No operativa».
        compose.onNodeWithText("No operativa").assertIsDisplayed()
        compose.onNodeWithText("Pte. Anular", substring = true).assertDoesNotExist()
        capture("lab-cards.png")
        compose.onNodeWithText("STM ···· 1234").performClick()
        assertEquals("ABCD1234", chosen)
    }
    @Test fun connectingAndSwitchingAreWaits() {
        var title by mutableStateOf("Conectando con STM")
        var cancels = 0
        compose.setContent { Frame { LabConnecting(title, if (title.startsWith("Cambiando")) "Consultando el saldo de STM ···· 9012." else "Tu saldo aparece en un momento.",
            if (title.startsWith("Cambiando")) null else ({ cancels++ })) } }
        compose.onNodeWithText("Cancelar").performClick()
        assertEquals(1, cancels)
        capture("lab-connecting.png")
        title = "Cambiando de boletera"
        compose.onNodeWithText("Cancelar").assertDoesNotExist()
        capture("lab-switching.png")
    }
    @Test fun accessHelpIsAmberWithTheThreeSteps() {
        compose.setContent { Frame(back = true) { LabAccessHelp(onOpenSite = {}, onDone = {}) } }
        compose.onNodeWithText("Falta un paso en STM").assertIsDisplayed()
        compose.onNodeWithText("Abrí el sitio de STM y entrá con tu usuario gub.uy.").assertIsDisplayed()
        compose.onNodeWithText("Ya lo hice, entrar").assertIsDisplayed()
        capture("lab-access-help.png")
    }
    @Test fun cardsSheetAndNames() {
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("lab_card_names", 0).edit().clear().putString("ABCD9012", "Mamá").commit()
        var naming by mutableStateOf<String?>(null)
        var chosen = ""
        val state = UiState(stage = "balance", selectedCard = "ABCD1234", balance = 124000,
            cards = listOf(CardInfo("ABCD1234", true, "Operativa"), CardInfo("ABCD9012", true, "Operativa"), CardInfo("DEAD5678", false, "Pte. Anular")))
        compose.setContent { BoleteraTheme("dark") { Box(Modifier.fillMaxSize()) { LabAmbient()
            if (naming == null) LabCardsSheet(state, onChoose = { chosen = it }, onEdit = { naming = it }, onClose = {}) else LabCardNameSheet(naming!!) { naming = null }
        } } }
        compose.onNodeWithText("Tus boleteras").assertIsDisplayed()
        compose.onNodeWithText("Mamá").assertIsDisplayed()
        capture("lab-cards-sheet.png")
        compose.onNodeWithText("Mamá").performClick()
        assertEquals("ABCD9012", chosen)
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithText("Nombre de la boletera").assertIsDisplayed()
        compose.onNodeWithText("Nombre").performTextInput("Mía")
        capture("lab-card-name.png")
        compose.onNodeWithText("Guardar").performClick()
        assertEquals("Mía", InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("lab_card_names", 0).getString("ABCD1234", null))
    }
    @Test fun connectingShowsTheLastBalance() {
        compose.setContent { Frame { LabConnecting("Conectando con STM", "Tu saldo aparece en un momento.", {}, last = LastBalance(124000, System.currentTimeMillis())) } }
        compose.onNodeWithText("Último saldo").assertIsDisplayed()
        compose.onNodeWithText("$ 1.240").assertIsDisplayed()
        compose.onNodeWithText("Tu saldo aparece en un momento.").assertDoesNotExist()
        capture("lab-connecting-last.png")
    }
    @Test fun paymentWaitConfirmAndVerification() {
        var which by mutableStateOf(0)
        var paid = 0
        val rows = listOf("Comercio:" to "STM Recargas", "Medio de pago:" to "PREX **** 8842", "Importe:" to "$ 500,00")
        compose.setContent { Frame { Box(Modifier.fillMaxSize()) { when (which) {
            0 -> LabPaymentWait(50000, "ABCD1234", express = false, onCancel = {})
            1 -> LabPaymentWait(26000, "ABCD1234", express = true, onCancel = {})
            2 -> LabConfirmPayment(50000, "ABCD1234", rows, "Ana Pérez", canPay = true, onPay = { paid++ }, onDetail = {})
            else -> LabVerification(50000, "ABCD1234", afterPay = true, onCancel = null) { Box(Modifier.size(304.dp, 78.dp).background(androidx.compose.ui.graphics.Color.White)) }
        } } } }
        compose.onNodeWithText("Abriendo Prex").assertIsDisplayed(); capture("lab-pay-wait.png")
        which = 1; compose.onNodeWithText("Carga rápida").assertIsDisplayed(); capture("lab-pay-express.png")
        which = 2; compose.onNodeWithText("Prex ···· 8842").assertIsDisplayed(); compose.onNodeWithText("Titular").assertIsDisplayed()
        capture("lab-pay-confirm.png")
        compose.onNodeWithText("Pagar $ 500").performClick(); assertEquals(1, paid)
        which = 3; compose.onNodeWithText("Una verificación y seguimos").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertDoesNotExist()
        capture("lab-pay-verification.png")
    }
    @Test fun expressLightRunsTheEdgeAndLandsOnTheCard() {
        var t by mutableFloatStateOf(0f)
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                Frame(back = true) { LabPaymentWait(26000, "1234", express = true, onCancel = {}) }
                LabExpressLightFrame(t)
            }
        }
        for (at in listOf(260f, 420f, 520f, 580f, 640f)) { t = at; capture("lab-express-light-${at.toInt()}.png") }
    }
    @Test fun cardStepLooksLikeTheRest() {
        compose.setContent {
            Frame(back = true) {
                LabPaymentLine(50000, "1234")
                NativeCardForm(busy = false, canContinue = true, providerError = false, expandedChallenge = false, onSubmit = { _, _, _ -> }, onOriginal = {})
            }
        }
        capture("lab-pay-card.png")
    }
    @Test fun settingsLikeAndroidWithForgetAtTheEnd() {
        val app = compose.activity.application
        var session by mutableStateOf(true)
        var forgot = 0
        compose.setContent {
            BoleteraTheme("dark") {
                LabSettingsScreen(AppUpdates(app), hasSavedAccess = true, canLogout = session, diagnostic = "prueba", effectsMode = EffectsMode.AUTO,
                    onEffectsMode = {}, onLogout = {}, onForget = { forgot++ }, onTicketGuide = {}, onInstall = {}, onClose = {})
            }
        }
        compose.onNodeWithText("Tu huella sigue guardada").assertIsDisplayed()
        compose.onNodeWithText("Enviar reporte").assertIsDisplayed()
        capture("lab-settings.png")
        compose.onNodeWithText("Olvidar este celular").performScrollTo().performClick()
        compose.onNodeWithText("¿Olvidar este celular?").assertIsDisplayed()
        capture("lab-settings-forget.png")
        compose.onAllNodesWithText("Olvidar este celular").onLast().performClick()
        compose.waitForIdle()
        assertEquals(1, forgot)
        session = false
        compose.onNodeWithText("Cerrar sesión").assertDoesNotExist()
        capture("lab-settings-no-session.png")
    }
    @Test fun beforeEnteringAndNewsAreSheets() {
        var step by mutableIntStateOf(0)
        var ready = 0
        compose.setContent {
            Frame {
                LabWelcome(UiState(stage = "welcome"), savedAccess = false, authenticating = false, onUnlock = {}, onManual = {}, onAccessGuide = {})
            }
            BoleteraTheme("dark") {
                when (step) {
                    0 -> LabAccessGuideSheet(onReady = { ready++ }, onFirstTime = {}, onClose = {})
                    1 -> LabNewsSheet("Lab 7", labNewsFrom(listOf("Pago más corto — Del importe a «Pagar» sin pasos repetidos.", "Carga Express: la luz cae en tu tarjeta.")), installed = false, onPrimary = {}, onLater = {})
                    else -> LabNewsSheet("Lab 7", LabWhatsNewItems, installed = true, onPrimary = {}, onLater = {})
                }
            }
        }
        compose.onNodeWithText("Ya entré a STM con mi usuario gub.uy").assertIsDisplayed()
        compose.onNodeWithText("Es mi primera vez: abrir STM ↗").assertIsDisplayed()
        capture("lab-before-entering.png")
        compose.onNodeWithText("Ya entré a STM con mi usuario gub.uy").performClick()
        assertEquals(1, ready)
        step = 1
        compose.onNodeWithText("Lab 7 está lista").assertIsDisplayed()
        capture("lab-update-ready.png")
        step = 2
        compose.onNodeWithText("Novedades").assertIsDisplayed()
        capture("lab-news.png")
    }
    @Test fun pictureCheckGetsTheWholeHeight() {
        compose.setContent {
            Frame(back = true) {
                // Google's picture grid on a phone: about 400 × 580 CSS px, taller than wide.
                LabVerification(26000, "ABCD4A4A", afterPay = false, onCancel = {}, expanded = true) {
                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        val scale = minOf(maxWidth.value / 400f, maxHeight.value / 580f)
                        Box(Modifier.size((400 * scale).dp, (580 * scale).dp).background(androidx.compose.ui.graphics.Color.White))
                    }
                }
            }
        }
        compose.onNodeWithText("Elegí las imágenes").assertIsDisplayed()
        capture("lab-pay-picture-check.png")
    }
    @Test fun checkLoadingAfterComingBack() {
        var expanded by mutableStateOf(true)
        compose.setContent {
            Frame(back = true) {
                LabVerification(26000, "ABCD4A4A", afterPay = false, onCancel = {}, expanded = expanded) { LabCheckLoading(onLight = !expanded) }
            }
        }
        compose.onNodeWithText("Cargando la verificación…").assertIsDisplayed()
        capture("lab-pay-check-loading-pictures.png")
        expanded = false
        compose.onNodeWithText("Cargando la verificación…").assertIsDisplayed()
        capture("lab-pay-check-loading-box.png")
    }
}
