package uy.boletera.prueba

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/** Synthetic ready state exercises the real installer handoff. Installation intents are intercepted. */
class AutomaticInstallTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun permission(mode: String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("appops set uy.boletera.prueba REQUEST_INSTALL_PACKAGES $mode")).use { it.readBytes() }
        instrumentation.waitForIdleSync()
    }
    private fun set(updates: AppUpdates, name: String, value: Boolean) {
        @Suppress("UNCHECKED_CAST")
        val state = AppUpdates::class.java.getDeclaredField("${name}\$delegate").apply { isAccessible = true }.get(updates) as MutableState<Boolean>
        state.value = value
    }
    private fun fixture(): AppUpdates {
        val activity = compose.activity
        val file = File(activity.cacheDir, "updates/boletera.apk")
        file.parentFile!!.mkdirs()
        File(activity.applicationInfo.sourceDir).copyTo(file, overwrite = true)
        val updates = ViewModelProvider(activity)[AppUpdates::class.java]
        @Suppress("UNCHECKED_CAST")
        val releaseState = AppUpdates::class.java.getDeclaredField("release\$delegate").apply { isAccessible = true }.get(updates) as MutableState<UpdateRelease?>
        releaseState.value = UpdateRelease("0.2.24", "", 1, "", listOf("Se evita el destello de la página de fondo al cerrar el CAPTCHA.", "Las actualizaciones muestran sus novedades antes de instalar."))
        return updates
    }
    @Test fun downloadedUpdateNeedsReviewAndCancelKeepsItReady() {
        val intents = CopyOnWriteArrayList<Intent>()
        val monitor = monitor(intents)
        instrumentation.addMonitor(monitor)
        try {
            lateinit var updates: AppUpdates
            compose.runOnIdle { updates = fixture(); updates.downloadReady() }
            compose.onNodeWithText("Actualización lista").assertIsDisplayed()
            compose.onNodeWithText("Novedades de la versión 0.2.24").assertIsDisplayed()
            compose.onNodeWithText("Se evita el destello de la página de fondo al cerrar el CAPTCHA.").assertIsDisplayed()
            assertTrue(intents.isEmpty())
            android.os.SystemClock.sleep(400) // Let the Android dialog window finish its entrance before the visual capture.
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                File(compose.activity.getExternalFilesDir(null), "update-review.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
            }
            compose.onNodeWithText("Ahora no").performClick()
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            assertTrue(updates.ready)
            assertTrue(intents.isEmpty())
            compose.onNodeWithText("Actualización lista").assertDoesNotExist()
            compose.onNodeWithContentDescription("Configuración").performClick()
            compose.onNodeWithText("Instalar actualización").performScrollTo().performClick()
            compose.onNodeWithText("Actualización lista").assertIsDisplayed()
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            compose.waitUntil(5000) { !updates.reviewRequested }
            compose.waitForIdle()
            assertFalse(updates.reviewRequested)
            assertTrue(updates.ready)
            assertTrue(intents.isEmpty())
        } finally { instrumentation.removeMonitor(monitor) }
    }
    private fun monitor(intents: MutableList<Intent>) = object : Instrumentation.ActivityMonitor() {
        override fun onStartActivity(intent: Intent?): Instrumentation.ActivityResult? {
            if (intent?.type == "application/vnd.android.package-archive" || intent?.action == Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES) {
                intents.add(Intent(intent))
                return if (intent.action == Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES) null else Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
            return null
        }
    }
    @Test fun verifiedCompletionWaitsForForegroundAndOpensInstallerOnlyOnce() {
        val intents = CopyOnWriteArrayList<Intent>()
        val monitor = monitor(intents)
        instrumentation.addMonitor(monitor)
        try {
            assertTrue(compose.activity.packageManager.canRequestPackageInstalls())
            lateinit var updates: AppUpdates
            compose.runOnIdle { updates = fixture(); set(updates, "busy", true); updates.downloadReady() }
            compose.waitForIdle()
            assertTrue(intents.isEmpty())
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.onActivity { set(updates, "busy", false) }
            instrumentation.waitForIdleSync()
            assertTrue(intents.isEmpty())
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Actualización lista").assertIsDisplayed()
            assertTrue(intents.isEmpty())
            compose.onNodeWithText("Instalar").performClick()
            compose.waitUntil(5000) { intents.size == 1 }
            assertEquals("application/vnd.android.package-archive", intents.single().type)
            assertEquals("content", intents.single().data?.scheme)
            assertTrue(intents.single().flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitForIdle()
            assertEquals(1, intents.size)
            assertFalse(updates.installRequested)
        } finally { instrumentation.removeMonitor(monitor) }
    }
    @Test fun permissionReturnContinuesAutomaticallyAndDenialDoesNotLoop() {
        val intents = CopyOnWriteArrayList<Intent>()
        val monitor = monitor(intents)
        instrumentation.addMonitor(monitor)
        try {
            assertFalse(compose.activity.packageManager.canRequestPackageInstalls())
            lateinit var updates: AppUpdates
            compose.runOnIdle { updates = fixture(); updates.downloadReady() }
            compose.onNodeWithText("Instalar").performClick()
            compose.waitUntil(5000) { intents.size == 1 }
            assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intents.single().action)
            val activity = compose.activity
            compose.waitUntil(5000) { !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            compose.waitUntil(5000) { activity.lifecycle.currentState == Lifecycle.State.RESUMED }
            compose.waitForIdle()
            assertEquals(1, intents.size)
            assertFalse(updates.installRequested)
            compose.runOnIdle { updates.requestReview() }
            compose.onNodeWithText("Instalar").performClick()
            compose.waitUntil(5000) { intents.size == 2 && !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
            permission("allow")
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            try { compose.waitUntil(5000) { intents.size >= 3 } } catch (error: Throwable) {
                throw AssertionError("Installer return: count=${intents.size}, allowed=${activity.packageManager.canRequestPackageInstalls()}, requested=${updates.installRequested}, ready=${updates.ready}, message=${updates.message}", error)
            }
            assertEquals("application/vnd.android.package-archive", intents.last().type)
            assertFalse(updates.installRequested)
        } finally { instrumentation.removeMonitor(monitor) }
    }
}
