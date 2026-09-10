package uy.boletera.prueba

import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.URL

@RunWith(AndroidJUnit4::class)
class AppUpdatesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun installerReceivesOnlyPrivateApkWithReadPermission() {
        val context = compose.activity
        val file = File(context.cacheDir, "updates/installer-test.apk")
        file.parentFile!!.mkdirs()
        File(context.applicationInfo.sourceDir).copyTo(file, overwrite = true)
        try {
            val intent = UpdateFiles.installIntent(context, file)
            assertEquals("content", intent.data!!.scheme)
            assertEquals("application/vnd.android.package-archive", intent.type)
            assertTrue(intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            context.contentResolver.openInputStream(intent.data!!)!!.use { assertEquals('P'.code, it.read()) }
            assertThrows(IllegalArgumentException::class.java) {
                UpdateFiles.installIntent(context, File(context.filesDir, "private-profile"))
            }
            // Opens Android's confirmation only. Does not accept installation.
            compose.runOnIdle { context.startActivity(intent) }
            android.os.SystemClock.sleep(1500)
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            File(context.getExternalFilesDir(null), "installer-check.png").outputStream().use {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        } finally { file.delete() }
    }
    private fun fixture(version: String, draft: Boolean = false, digest: String = "sha256:" + "a".repeat(64), host: String = "github.com") =
        """{"tag_name":"v$version","draft":$draft,"prerelease":true,"assets":[{"name":"boletera-prueba-$version.apk","size":8000,"digest":"$digest","browser_download_url":"https://$host/jmestrallet/boletera-android/releases/download/v$version/boletera-prueba-$version.apk"}]}"""

    @Test fun selectsNewerPrereleasesWithoutDowngradesOrForeignAssets() {
        assertTrue(UpdatePolicy.newer("0.1.15", "0.1.9-prueba"))
        assertFalse(UpdatePolicy.newer("0.1.9", "0.1.15-prueba"))
        assertFalse(UpdatePolicy.newer("0.1.15", "0.1.15-prueba"))
        assertFalse(UpdatePolicy.newer("unknown", "0.1.15-prueba"))
        val json = "[${fixture("0.1.10")},${fixture("0.1.16")},${fixture("0.1.99", draft = true)}]"
        assertEquals("0.1.16", UpdatePolicy.select(json, "0.1.15-prueba")?.version)
        assertNull(UpdatePolicy.select("[${fixture("0.1.16", digest = "")}]", "0.1.15-prueba"))
        assertNull(UpdatePolicy.select("[${fixture("0.1.16", host = "evil.example")}]", "0.1.15-prueba"))
        assertNull(UpdatePolicy.select("[${fixture("0.1.15")}]", "0.1.15-prueba"))
        assertFalse(UpdatePolicy.allowed(URL("http://github.com/test")))
        assertFalse(UpdatePolicy.allowed(URL("https://github.com.evil.example/test")))
        assertFalse(UpdatePolicy.allowed(URL("https://user@github.com/test")))
    }

    @Suppress("DEPRECATION")
    @Test fun archiveRequiresSamePackageSignatureAndHigherVersionCode() {
        val context = compose.activity
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val source = context.applicationInfo.sourceDir
        val candidate = pm.getPackageArchiveInfo(source, flags)!!
        val baseline = pm.getPackageArchiveInfo(source, flags)!!
        baseline.versionName = "0.1.14-prueba"; baseline.longVersionCode = 15
        UpdateFiles.verifyArchive(candidate, baseline, context.packageName, "0.1.15")
        candidate.longVersionCode = 15
        assertThrows(IllegalStateException::class.java) { UpdateFiles.verifyArchive(candidate, baseline, context.packageName, "0.1.15") }
        candidate.longVersionCode = 16
        candidate.packageName = "another.app"
        assertThrows(IllegalStateException::class.java) { UpdateFiles.verifyArchive(candidate, baseline, context.packageName, "0.1.15") }
        candidate.packageName = context.packageName
        candidate.signingInfo = null
        assertThrows(IllegalStateException::class.java) { UpdateFiles.verifyArchive(candidate, baseline, context.packageName, "0.1.15") }
    }

    @Test fun settingsCanCheckPublicGithubAndKeepInstalledNewerVersion() {
        compose.onNodeWithText("Configuración").performClick()
        compose.onNodeWithText("Buscar actualizaciones").performClick()
        compose.waitUntil(45_000) { compose.onAllNodesWithText("Ya tenés la versión más nueva disponible para esta app.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Descargar actualización").assertDoesNotExist()
    }

    @Test fun publicApkDownloadVerifiesDigestAndRejectsTamperingAndDowngrade() {
        val connection = UpdateFiles.open(UpdatePolicy.API)
        val json = try { connection.inputStream.bufferedReader().use { it.readText() } } finally { connection.disconnect() }
        val release = UpdatePolicy.select(json, "0.0.0")!!
        val file = File(compose.activity.cacheDir, "update-test.apk")
        try {
            UpdateFiles.download(release, file) {}
            assertEquals(release.size, file.length())
            assertThrows(IllegalStateException::class.java) { UpdateFiles.verify(compose.activity, file, release.version) }
            assertThrows(IllegalStateException::class.java) { UpdateFiles.download(release.copy(sha256 = "0".repeat(64)), file) {} }
            assertFalse(file.exists())
        } finally { file.delete() }
    }
}
