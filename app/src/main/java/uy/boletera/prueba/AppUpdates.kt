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

enum class UpdateChannel(val key: String, val label: String) {
    PUBLIC("public", "Pública"), BETA("beta", "Beta");
    companion object { fun from(value: String?) = entries.firstOrNull { it.key == value } ?: PUBLIC }
}
internal data class UpdateNews(val version: String, val notes: List<String>)
internal data class UpdateRelease(val version: String, val url: String, val size: Long, val sha256: String,
    val notes: List<String> = emptyList(), val history: List<UpdateNews> = emptyList(),
    val channel: UpdateChannel = UpdateChannel.PUBLIC, val packageVersion: String = "$version-publica")

internal object UpdatePolicy {
    const val API = "https://api.github.com/repos/jmestrallet/boletera-android/releases?per_page=100"
    const val MAX_APK = 100L * 1024 * 1024
    fun notes(body: String): List<String> {
        // New releases have a concise app section. Older public releases used bullets or paragraphs.
        val normalized=body.take(32_000).replace("\r\n","\n")
        val curated=normalized.contains("## Novedades en la app")
        val section=if(curated)normalized.substringAfter("## Novedades en la app").substringBefore("\n## ") else normalized
        val lines=section.lines().map(String::trim).filter { it.isNotBlank() && !it.startsWith("SHA-256:",true) && !it.startsWith("#") }
        val items=if(curated || lines.any {it.startsWith("- ")})lines.filter {it.startsWith("- ")} else lines
        return items.map { it.removePrefix("- ").replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
                .replace(Regex("[*`_]"), "").replace(Regex("\\s+"), " ").trim() }.filter(String::isNotBlank)
    }
    private data class ParsedVersion(val core: List<Int>, val beta: Int?)
    private fun parsed(value: String): ParsedVersion? {
        val match=Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-(?:prueba|publica)|-beta\\.(\\d+))?$").matchEntire(value) ?: return null
        val core=match.groupValues.slice(1..3).map {it.toIntOrNull() ?: return null}
        return ParsedVersion(core,match.groupValues[4].takeIf(String::isNotBlank)?.toIntOrNull())
    }
    fun version(value: String): List<Int>? = parsed(value)?.core
    fun channel(value: String): UpdateChannel = if(parsed(value)?.beta!=null)UpdateChannel.BETA else UpdateChannel.PUBLIC
    fun newer(candidate: String, installed: String): Boolean {
        val a = parsed(candidate) ?: return false
        val b = parsed(installed) ?: return false
        for (i in a.core.indices) if (a.core[i] != b.core[i]) return a.core[i] > b.core[i]
        return a.beta!=null && b.beta!=null && a.beta>b.beta
    }
    fun select(json: String, installed: String, wanted: UpdateChannel = UpdateChannel.PUBLIC): UpdateRelease? {
        val releases = JSONArray(json)
        val switching=channel(installed)!=wanted
        val candidates = (0 until releases.length()).mapNotNull { i ->
            val release = releases.getJSONObject(i)
            if (release.optBoolean("draft")) return@mapNotNull null
            val tag = release.optString("tag_name")
            if (!tag.startsWith("v") || channel(tag)!=wanted || (!switching && !newer(tag, installed))) return@mapNotNull null
            val v = tag.removePrefix("v")
            if (version(v) == null || v.endsWith("-prueba") || v.endsWith("-publica")) return@mapNotNull null
            val assets = release.optJSONArray("assets") ?: return@mapNotNull null
            val acceptedNames=if(wanted==UpdateChannel.BETA)setOf("boletera-beta-$v.apk")
                else setOf("boletera-publica-$v.apk","boletera-prueba-$v.apk")
            val matching = (0 until assets.length()).map { assets.getJSONObject(it) }.filter {it.optString("name") in acceptedNames}
            if (matching.size != 1) return@mapNotNull null
            val asset = matching.single()
            val name=asset.optString("name")
            val url = "https://github.com/jmestrallet/boletera-android/releases/download/$tag/$name"
            val digest = asset.optString("digest")
            val size = asset.optLong("size")
            if (asset.optString("browser_download_url") != url || size !in 1..MAX_APK ||
                !Regex("sha256:[a-fA-F0-9]{64}").matches(digest)) return@mapNotNull null
            val packageVersion=if(wanted==UpdateChannel.BETA)v else "$v-prueba"
            UpdateRelease(v, url, size, digest.substringAfter(':').lowercase(), notes(release.optString("body")),
                channel=wanted,packageVersion=packageVersion)
        }
        val selected=candidates.reduceOrNull { best, next -> if (newer(next.version, best.version)) next else best } ?: return null
        val historySource=if(switching)listOf(selected) else candidates
        val history=historySource.distinctBy { it.version }.sortedWith { a,b -> when {newer(a.version,b.version)->-1;newer(b.version,a.version)->1;else->0} }
            .map { UpdateNews(it.version,it.notes) }
        return selected.copy(history=history)
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
    fun verify(context: Context, file: File, release: UpdateRelease) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("El archivo no es una APK válida.")
        val installed = pm.getPackageInfo(context.packageName, flags)
        verifyArchive(candidate, installed, context.packageName, release)
    }

    @Suppress("DEPRECATION")
    fun verifyArchive(candidate: android.content.pm.PackageInfo, installed: android.content.pm.PackageInfo, packageName: String, release: UpdateRelease) {
        check(candidate.packageName == packageName) { "La descarga no es Boletera." }
        val installedName=installed.versionName ?: ""
        val switching=UpdatePolicy.channel(installedName)!=release.channel
        check(candidate.versionName == release.packageVersion && (switching || UpdatePolicy.newer(release.version,installedName))) { "La versión descargada no corresponde al canal elegido." }
        val candidateCode = if (Build.VERSION.SDK_INT >= 28) candidate.longVersionCode else candidate.versionCode.toLong()
        val installedCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        check(candidateCode > installedCode || switching && candidateCode == installedCode) { "La descarga no es compatible con la app instalada." }
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
    var installRequested by mutableStateOf(false); private set
    var reviewRequested by mutableStateOf(false); private set
    var automaticNotice by mutableStateOf(false); private set
    private val preferences=application.getSharedPreferences("updates",Context.MODE_PRIVATE)
    val installedChannel=UpdateChannel.from(BuildConfig.DISTRIBUTION_CHANNEL)
    var channel by mutableStateOf(installedChannel); private set
    private var awaitingInstallPermission = false
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private val apk get() = File(getApplication<Application>().cacheDir, "updates/boletera-${channel.key}.apk")
    init {
        val priorArtifact=preferences.getString("installed_artifact",null)
        channel=if(priorArtifact!=null && priorArtifact!=installedChannel.key)installedChannel
            else UpdateChannel.from(preferences.getString("channel",installedChannel.key))
        preferences.edit().putString("installed_artifact",installedChannel.key).putString("channel",channel.key).apply()
        message="Canal ${channel.label}. Buscá una versión nueva sin salir de la app."
    }
    private fun publish(action: () -> Unit) { main.post { if (!closed) action() } }
    private fun work(action: () -> Unit) {
        busy = true
        worker.execute {
            try { action() } catch (_: Exception) {
                publish { message = "No se pudo completar la actualización. Revisá la conexión y volvé a intentar." }
            } finally { publish { busy = false } }
        }
    }
    fun check() = checkRelease(false)
    fun selectChannel(value: UpdateChannel) {
        if(channel==value || busy)return
        channel=value
        preferences.edit().putString("channel",value.key).remove("last_attempt.${value.key}").apply()
        installRequested=false;awaitingInstallPermission=false;reviewRequested=false;ready=false;release=null;automaticNotice=false
        message="Buscando la versión ${value.label}…"
        checkRelease(false)
    }
    internal fun automaticCheckDue(now: Long = System.currentTimeMillis()): Boolean {
        val previous=preferences.getLong("last_attempt.${channel.key}",0)
        return !busy && !ready && !awaitingInstallPermission && (previous==0L || now<previous || now-previous>=6*60*60*1000L)
    }
    fun checkAutomatic() {
        if(automaticCheckDue())checkRelease(true)
    }
    fun dismissAutomaticNotice() { automaticNotice=false }
    private fun checkRelease(automatic: Boolean) {
        if (busy) return
        val requested=channel
        preferences.edit().putLong("last_attempt.${channel.key}",System.currentTimeMillis()).apply()
        installRequested = false; awaitingInstallPermission = false; reviewRequested = false
        ready = false; release = null; message = "Buscando actualizaciones…"
        work {
            val connection = UpdateFiles.open(UpdatePolicy.API)
            val json = try { connection.inputStream.use {
                val bytes = it.readBytesLimited(2 * 1024 * 1024)
                String(bytes, Charsets.UTF_8)
            } } finally { connection.disconnect() }
            val found = UpdatePolicy.select(json, BuildConfig.VERSION_NAME,requested)
            publish {
                if(channel!=requested)return@publish
                release = found
                automaticNotice = automatic && found!=null
                message = if (found == null && channel==installedChannel) "Ya tenés la versión más nueva del canal ${channel.label}."
                    else if(found==null)"Todavía no hay una versión ${channel.label} compatible para cambiar de canal."
                    else if(channel!=installedChannel)"Está lista la versión ${channel.label} ${found.version} para cambiar de canal."
                    else "Está disponible la versión ${found.version} del canal ${channel.label}."
            }
        }
    }
    fun download() {
        val selected = release ?: return
        if (busy) return
        val destination=apk
        installRequested = false; awaitingInstallPermission = false; reviewRequested = false
        ready = false; message = "Descargando…"
        work {
            destination.parentFile!!.mkdirs()
            val partial = File(destination.parentFile, "boletera-${selected.channel.key}.part")
            try {
                UpdateFiles.download(selected, partial) { percent -> publish { message = "Descargando… $percent %" } }
                UpdateFiles.verify(getApplication(), partial, selected)
                check(partial.renameTo(destination)) { "No se pudo guardar la descarga." }
                publish { downloadReady() }
            } finally { partial.delete() }
        }
    }
    internal fun downloadReady() {
        ready = true; installRequested = false; reviewRequested = true
        message = "Descarga lista. Revisá las novedades antes de instalar."
    }
    fun requestReview() {
        if (ready && !busy) reviewRequested = true
    }
    fun dismissReview() {
        reviewRequested = false; installRequested = false
        message = "Descarga lista. Podés instalarla desde Ajustes cuando quieras."
    }
    fun acceptReview() {
        if (!ready || busy || !reviewRequested) return
        reviewRequested = false; installRequested = true
    }
    fun onForeground(context: Context) {
        checkAutomatic()
        if (!awaitingInstallPermission) return
        awaitingInstallPermission = false
        if (context.packageManager.canRequestPackageInstalls() && ready) installRequested = true
        else message = "Falta habilitar la instalación desde Boletera. Podés volver a intentarlo."
    }
    fun install(context: Context) {
        if (!ready || busy || !installRequested) return
        installRequested = false
        try {
            if (!context.packageManager.canRequestPackageInstalls()) {
                awaitingInstallPermission = true
                message = "Activá «Permitir desde esta fuente» y volvé. El instalador se abrirá automáticamente."
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                return
            }
            context.startActivity(UpdateFiles.installIntent(context, apk))
            message = "Confirmá la instalación en Android. Si la cancelás, podés volver a intentarlo."
        } catch (_: Exception) { awaitingInstallPermission = false; message = "Android no pudo abrir el instalador. Volvé a intentar." }
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
