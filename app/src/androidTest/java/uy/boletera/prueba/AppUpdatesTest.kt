package uy.boletera.prueba

import android.content.pm.PackageManager
import androidx.activity.compose.setContent
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
    @Test fun popupUsesOnlyShortAuthoredNotesForSelectedVersion() {
        val body = "Intro técnica\n## Novedades en la app\n- **Mejora** del CAPTCHA.\n- Leé [las novedades](https://example.com).\n- " + "x".repeat(250) + "\n- Cuarto punto\n## Verificación\n- SHA-256: secreto"
        val parsed = UpdatePolicy.notes(body)
        assertEquals(4, parsed.size)
        assertEquals("Mejora del CAPTCHA.", parsed[0])
        assertEquals("Leé las novedades.", parsed[1])
        assertEquals(250,parsed[2].length)
        assertTrue(UpdatePolicy.notes("SHA-256: aaa").isEmpty())
        val json = org.json.JSONObject(fixture("0.2.25")).put("body", body)
        assertEquals(parsed, UpdatePolicy.select("[$json]", "0.2.24-prueba")!!.notes)
    }
    @Test fun skippedVersionsAccumulateWithoutInstalledOrDraftNotes() {
        fun release(v:String,body:String,draft:Boolean=false)=org.json.JSONObject(fixture(v,draft)).put("body",body)
        val entries=listOf(release("0.2.21","Instalada"),release("0.2.22","- Uno\n- Dos\n- Tres\n- Cuatro"),
            release("0.2.23","Mejora anterior.\n\nSHA-256: aaa"),release("0.2.24","## Novedades en la app\n- Última\n## Verificación\n- Técnica"),release("0.2.99","Borrador",true))
        val found=UpdatePolicy.select(entries.joinToString(",","[","]"),"0.2.21-prueba")!!
        assertEquals(listOf("0.2.24","0.2.23","0.2.22"),found.history.map{it.version})
        assertEquals(listOf("Mejora anterior."),found.history[1].notes)
        assertEquals(4,found.history.last().notes.size)
        assertEquals(listOf("Última"),found.notes)
    }
    @Test fun automaticChecksAreThrottledAndKeepDownloadedUpdate() {
        val updates=androidx.lifecycle.ViewModelProvider(compose.activity)[AppUpdates::class.java]
        compose.waitUntil(45000){!updates.busy}
        val prefs=compose.activity.getSharedPreferences("updates",android.content.Context.MODE_PRIVATE)
        val key="last_attempt.${updates.channel.key}"
        val previous=prefs.getLong(key,0)
        try {
            prefs.edit().putLong(key,1000).commit()
            assertFalse(updates.automaticCheckDue(1001))
            assertFalse(updates.automaticCheckDue(1000+6*60*60*1000L-1))
            assertTrue(updates.automaticCheckDue(1000+6*60*60*1000L))
            compose.runOnIdle {updates.downloadReady();updates.dismissReview()}
            assertFalse(updates.automaticCheckDue(1000+12*60*60*1000L))
            compose.runOnIdle {updates.checkAutomatic()}
            assertTrue(updates.ready);assertFalse(updates.busy);assertFalse(updates.installRequested)
        } finally {prefs.edit().putLong(key,previous).commit()}
    }
    @Test fun accumulatedPopupScrollsAndKeepsInstallActionsVisible() {
        val history=(25 downTo 21).map { UpdateNews("0.2.$it",listOf("Primera mejora de esta versión.","Segunda mejora de esta versión.","Tercera mejora de esta versión.")) }
        compose.activity.setContent {BoleteraTheme(appearance="dark") {
            UpdateReviewDialog(UpdateRelease("0.2.25","",1,"",history=history),{}, {})
        }}
        compose.onNodeWithText("Novedades de la versión 0.2.25").assertIsDisplayed()
        compose.onNodeWithText("Instalar").assertIsDisplayed()
        compose.onNodeWithText("Ahora no").assertIsDisplayed()
        compose.onNodeWithText("Novedades de la versión 0.2.21").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Instalar").assertIsDisplayed()
        android.os.SystemClock.sleep(400)
        val screenshot=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null),"accumulated-updates.png").outputStream().use {screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        screenshot.recycle()
    }
    @Test fun automaticNoticeOpensSettingsWithoutDownloading() {
        val updates=androidx.lifecycle.ViewModelProvider(compose.activity)[AppUpdates::class.java]
        compose.waitUntil(45000){!updates.busy}
        compose.runOnIdle {
            @Suppress("UNCHECKED_CAST")
            val state=AppUpdates::class.java.getDeclaredField("automaticNotice\$delegate").apply{isAccessible=true}.get(updates) as androidx.compose.runtime.MutableState<Boolean>
            state.value=true
        }
        compose.onNodeWithText("Hay una nueva versión de Boletera.").assertIsDisplayed()
        compose.onNodeWithText("Ver").performClick()
        compose.onNodeWithText("Buscar actualizaciones").performScrollTo().assertIsDisplayed()
        assertFalse(updates.automaticNotice);assertFalse(updates.ready);assertFalse(updates.installRequested)
    }
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
        """{"tag_name":"v$version","draft":$draft,"prerelease":false,"assets":[{"name":"boletera-$version.apk","size":8000,"digest":"$digest","browser_download_url":"https://$host/jmestrallet/boletera-android/releases/download/v$version/boletera-$version.apk"}]}"""
    private fun betaFixture(version:String) =
        """{"tag_name":"v$version","draft":false,"prerelease":true,"assets":[{"name":"boletera-beta-$version.apk","size":8000,"digest":"sha256:${"b".repeat(64)}","browser_download_url":"https://github.com/jmestrallet/boletera-android/releases/download/v$version/boletera-beta-$version.apk"}]}"""

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

    @Test fun publicAndBetaFeedsStaySeparateAndAllowAChannelSwitchAtTheSameBaseVersion() {
        val json="[${fixture("0.2.31")},${betaFixture("0.2.31-beta.1")},${betaFixture("0.2.32-beta.2")}]"
        assertEquals("0.2.31",UpdatePolicy.select(json,"0.2.30-prueba",UpdateChannel.STABLE)?.version)
        assertEquals("0.2.32-beta.2",UpdatePolicy.select(json,"0.2.31-publica",UpdateChannel.BETA)?.version)
        val publicSwitch=UpdatePolicy.select(json,"0.2.31-beta.1",UpdateChannel.STABLE)
        assertEquals("0.2.31",publicSwitch?.version)
        assertEquals(listOf("0.2.31"),publicSwitch?.history?.map {it.version})
        assertNull(UpdatePolicy.select("[${betaFixture("0.2.31-beta.1")} ]","0.2.31-beta.1",UpdateChannel.BETA))
        assertTrue(UpdatePolicy.newer("0.2.31-beta.2","0.2.31-beta.1"))
        assertEquals("0.2.31 Beta 2",UpdatePolicy.display("0.2.31-beta.2"))
    }

    @Suppress("DEPRECATION")
    @Test fun archiveRequiresSamePackageSignatureAndHigherVersionCode() {
        val context = compose.activity
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val source = context.applicationInfo.sourceDir
        val candidate = pm.getPackageArchiveInfo(source, flags)!!
        val baseline = pm.getPackageArchiveInfo(source, flags)!!
        baseline.versionName = "0.2.30-prueba"; baseline.longVersionCode = BuildConfig.VERSION_CODE.toLong() - 1
        val publicRelease=UpdateRelease("0.2.31","",1,"",packageVersion=candidate.versionName!!)
        UpdateFiles.verifyArchive(candidate, baseline, context.packageName, publicRelease)
        baseline.versionName="0.2.31";baseline.longVersionCode=BuildConfig.VERSION_CODE.toLong()
        candidate.versionName="0.2.31-beta.1";candidate.longVersionCode=BuildConfig.VERSION_CODE.toLong()
        UpdateFiles.verifyArchive(candidate,baseline,context.packageName,
            UpdateRelease("0.2.31-beta.1","",1,"",channel=UpdateChannel.BETA,packageVersion="0.2.31-beta.1"))
        candidate.versionName=publicRelease.packageVersion;baseline.versionName="0.2.30-prueba";baseline.longVersionCode=BuildConfig.VERSION_CODE.toLong()-1
        candidate.longVersionCode = BuildConfig.VERSION_CODE.toLong() - 1
        assertThrows(IllegalStateException::class.java) { UpdateFiles.verifyArchive(candidate, baseline, context.packageName, publicRelease) }
        candidate.longVersionCode = BuildConfig.VERSION_CODE.toLong()
        candidate.packageName = "another.app"
        assertThrows(IllegalStateException::class.java) { UpdateFiles.verifyArchive(candidate, baseline, context.packageName, publicRelease) }
        candidate.packageName = context.packageName
        candidate.signingInfo = null
        assertThrows(IllegalStateException::class.java) { UpdateFiles.verifyArchive(candidate, baseline, context.packageName, publicRelease) }
    }

    @Test fun settingsCanCheckPublicGithubAndKeepInstalledNewerVersion() {
        compose.onNodeWithContentDescription("Configuración").performClick()
        compose.onNodeWithText("Buscar actualizaciones").performScrollTo().performClick()
        compose.waitUntil(45_000) { compose.onAllNodesWithText("Tenés la última versión estable.").fetchSemanticsNodes().isNotEmpty() }
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
            assertThrows(IllegalStateException::class.java) { UpdateFiles.verify(compose.activity, file, release) }
            assertThrows(IllegalStateException::class.java) { UpdateFiles.download(release.copy(sha256 = "0".repeat(64)), file) {} }
            assertFalse(file.exists())
        } finally { file.delete() }
    }
}
