package uy.boletera.prueba

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

internal data class UpdateRelease(val version: String, val url: String, val size: Long, val sha256: String)

internal object UpdatePolicy {
    const val API = "https://api.github.com/repos/jmestrallet/boletera-android/releases?per_page=100"
    const val MAX_APK = 100L * 1024 * 1024
    fun version(value: String): List<Int>? = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-prueba)?$")
        .matchEntire(value)?.groupValues?.drop(1)?.map { it.toIntOrNull() ?: return null }
    fun newer(candidate: String, installed: String): Boolean {
        val a = version(candidate) ?: return false
        val b = version(installed) ?: return false
        for (i in a.indices) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }
    fun select(json: String, installed: String): UpdateRelease? {
        val releases = JSONArray(json)
        val candidates = (0 until releases.length()).mapNotNull { i ->
            val release = releases.getJSONObject(i)
            if (release.optBoolean("draft")) return@mapNotNull null
            val tag = release.optString("tag_name")
            if (!tag.startsWith("v") || !newer(tag, installed)) return@mapNotNull null
            val v = tag.removePrefix("v")
            if (version(v) == null || v.endsWith("-prueba")) return@mapNotNull null
            val assets = release.optJSONArray("assets") ?: return@mapNotNull null
            val matching = (0 until assets.length()).map { assets.getJSONObject(it) }
                .filter { it.optString("name") == "boletera-prueba-$v.apk" }
            if (matching.size != 1) return@mapNotNull null
            val asset = matching.single()
            val url = "https://github.com/jmestrallet/boletera-android/releases/download/$tag/boletera-prueba-$v.apk"
            val digest = asset.optString("digest")
            val size = asset.optLong("size")
            if (asset.optString("browser_download_url") != url || size !in 1..MAX_APK ||
                !Regex("sha256:[a-fA-F0-9]{64}").matches(digest)) return@mapNotNull null
            UpdateRelease(v, url, size, digest.substringAfter(':').lowercase())
        }
        return candidates.reduceOrNull { best, next -> if (newer(next.version, best.version)) next else best }
    }
    fun allowed(url: URL): Boolean = url.protocol == "https" && url.userInfo == null && url.port in listOf(-1, 443) &&
        url.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
}

internal object UpdateFiles {
    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    fun open(address: String): HttpURLConnection {
        var url = URL(address)
        repeat(6) {
            check(UpdatePolicy.allowed(url)) { "Destino de descarga no permitido." }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000; readTimeout = 30_000
                setRequestProperty("User-Agent", "Boletera-Android/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val status = connection.responseCode
            if (status == 200) return connection
            if (status in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                check(location != null) { "Descarga sin destino." }
                url = URL(url, location)
            } else {
                connection.disconnect()
                error(if (status == 403 || status == 429) "GitHub limitó las consultas. Probá más tarde." else "GitHub no respondió correctamente ($status).")
            }
        }
        error("Demasiadas redirecciones de descarga.")
    }

    fun download(release: UpdateRelease, destination: File, progress: (Int) -> Unit) {
        val connection = open(release.url)
        try {
            val hash = MessageDigest.getInstance("SHA-256")
            var total = 0L
            var previous = -1
            connection.inputStream.use { input -> destination.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    if (Thread.currentThread().isInterrupted) error("Descarga cancelada.")
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= release.size && total <= UpdatePolicy.MAX_APK) { "Tamaño de descarga inesperado." }
                    hash.update(buffer, 0, count); output.write(buffer, 0, count)
                    val percent = (total * 100 / release.size).toInt()
                    if (percent != previous) { previous = percent; progress(percent) }
                }
            } }
            val actual = hash.digest().joinToString("") { "%02x".format(it) }
            check(total == release.size && actual == release.sha256) { "La descarga está incompleta o no coincide con la publicada." }
        } catch (e: Exception) {
            destination.delete()
            throw e
        } finally { connection.disconnect() }
    }

    @Suppress("DEPRECATION")
    fun verify(context: Context, file: File, expectedVersion: String) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("El archivo no es una APK válida.")
        val installed = pm.getPackageInfo(context.packageName, flags)
        verifyArchive(candidate, installed, context.packageName, expectedVersion)
    }

    @Suppress("DEPRECATION")
    fun verifyArchive(candidate: android.content.pm.PackageInfo, installed: android.content.pm.PackageInfo, packageName: String, expectedVersion: String) {
        check(candidate.packageName == packageName) { "La descarga no es Boletera." }
        check(candidate.versionName == "$expectedVersion-prueba" && UpdatePolicy.newer(expectedVersion, installed.versionName ?: "")) { "La versión descargada no es una actualización." }
        val candidateCode = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        val installedCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        check(candidateCode > installedCode) { "La descarga no es más nueva que la app instalada." }
        val own = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
        val other = if (Build.VERSION.SDK_INT >= 28) candidate.signingInfo?.apkContentsSigners else candidate.signatures
        check(!own.isNullOrEmpty() && !other.isNullOrEmpty() && own.toSet() == other.toSet()) { "La firma de la descarga no coincide con Boletera." }
    }
}

class AppUpdates(application: Application) : AndroidViewModel(application) {
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf("Buscá una versión nueva sin salir de la app."); private set
    internal var release by mutableStateOf<UpdateRelease?>(null); private set
    var ready by mutableStateOf(false); private set
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private val apk get() = File(getApplication<Application>().cacheDir, "updates/boletera.apk")
    private fun publish(action: () -> Unit) { main.post { if (!closed) action() } }
    private fun work(action: () -> Unit) {
        busy = true
        worker.execute {
            try { action() } catch (_: Exception) {
                publish { message = "No se pudo completar la actualización. Revisá la conexión y volvé a intentar." }
            } finally { publish { busy = false } }
        }
    }
    fun check() {
        if (busy) return
        ready = false; release = null; message = "Buscando actualizaciones…"
        work {
            val connection = UpdateFiles.open(UpdatePolicy.API)
            val json = try { connection.inputStream.use {
                val bytes = it.readBytesLimited(2 * 1024 * 1024)
                String(bytes, Charsets.UTF_8)
            } } finally { connection.disconnect() }
            val found = UpdatePolicy.select(json, BuildConfig.VERSION_NAME)
            publish {
                release = found
                message = if (found == null) "Ya tenés la versión más nueva disponible para esta app."
                    else "Está disponible la versión ${found.version}."
            }
        }
    }
    fun download() {
        val selected = release ?: return
        if (busy) return
        ready = false; message = "Descargando…"
        work {
            apk.parentFile!!.mkdirs()
            val partial = File(apk.parentFile, "boletera.part")
            try {
                UpdateFiles.download(selected, partial) { percent -> publish { message = "Descargando… $percent %" } }
                UpdateFiles.verify(getApplication(), partial, selected.version)
                check(partial.renameTo(apk)) { "No se pudo guardar la descarga." }
                publish { ready = true; message = "Descarga lista. Tocá Instalar actualización." }
            } finally { partial.delete() }
        }
    }
    fun install(context: Context) {
        if (!ready || busy) return
        try {
            if (!context.packageManager.canRequestPackageInstalls()) {
                message = "Activá «Permitir desde esta fuente», volvé y tocá Instalar actualización."
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                return
            }
            context.startActivity(UpdateFiles.installIntent(context, apk))
            message = "Confirmá la instalación en Android. Si la cancelás, podés volver a intentarlo."
        } catch (_: Exception) { message = "Android no pudo abrir el instalador. Volvé a intentar." }
    }
    override fun onCleared() { closed = true; worker.shutdownNow(); main.removeCallbacksAndMessages(null) }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        check(output.size() + count <= limit) { "Respuesta demasiado grande." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
