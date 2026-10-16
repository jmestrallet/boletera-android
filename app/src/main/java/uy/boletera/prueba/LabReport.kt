package uy.boletera.prueba

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * «Enviar reporte»: what went wrong, the phone and the masked trail of STM pages. It goes straight to the private
 * GitHub repo jmestrallet/boletera-reportes as an issue; without the key or without internet, it opens the share
 * sheet instead. The trail never has the document, the password, card data or numbers (digits are masked when it is
 * recorded).
 */
internal object LabReport {
    private const val REPO = "jmestrallet/boletera-reportes"
    /** The engine's masked diagnostic, set by the activity. */
    var diagnostic: () -> String = { "" }
    private var lastSent = 0L

    fun text(what: String): String = buildString {
        append("Reporte de Boletera ${BuildConfig.DISPLAY_VERSION}\n")
        append("Celular: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}\n")
        if (what.isNotBlank()) append("Qué pasó: $what\n")
        val trail = diagnostic().trim()
        if (trail.isNotEmpty()) append("\nRecorrido en STM:\n$trail\n")
        append("\n(No incluye tu documento, tu contraseña ni datos de tarjeta.)")
    }

    fun share(context: Context, what: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "Reporte de Boletera").putExtra(Intent.EXTRA_TEXT, text(what))
        runCatching { context.startActivity(Intent.createChooser(send, "Enviar reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /** POSTs [json]; the answer's text when GitHub took it, else null. */
    private fun post(url: String, token: String, json: JSONObject): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 10_000; readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        return try {
            connection.outputStream.use { it.write(json.toString().toByteArray()) }
            if (connection.responseCode in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else null
        } finally { connection.disconnect() }
    }

    /** [text] cut at line ends into pieces of at most [size] characters. */
    private fun chunks(text: String, size: Int): List<String> {
        val out = mutableListOf<String>(); var rest = text.trimEnd()
        while (rest.isNotEmpty()) {
            if (rest.length <= size) { out += rest; break }
            val cut = rest.lastIndexOf('\n', size).let { if (it <= 0) size else it }
            out += rest.substring(0, cut); rest = rest.substring(cut).trimStart('\n')
        }
        return out
    }

    /** Sends it to GitHub; says «Reporte enviado» or, if it cannot, opens the share sheet. One every 20 seconds at most. */
    fun send(context: Context, what: String) {
        val app = context.applicationContext
        val token = BuildConfig.REPORT_TOKEN
        if (token.isBlank()) { share(context, what); return }
        val now = System.currentTimeMillis()
        if (now - lastSent < 20_000) { Toast.makeText(app, "El reporte ya se envió.", Toast.LENGTH_SHORT).show(); return }
        lastSent = now
        LabLog.add("reporte", "enviado: $what")
        val log = LabLog.read()
        val lines = log.count { it == '\n' }
        val body = text(what) + if (log.isNotBlank()) "\n\nRegistro de uso: últimas 24 h, $lines líneas (en los comentarios, del más viejo al más nuevo)." else ""
        val title = "${what.take(70).ifBlank { "Reporte" }} · ${BuildConfig.DISPLAY_VERSION} · ${Build.MODEL}"
        val main = Handler(Looper.getMainLooper())
        Toast.makeText(app, "Enviando reporte…", Toast.LENGTH_SHORT).show()
        Thread {
            val ok = runCatching {
                val issue = post("https://api.github.com/repos/$REPO/issues", token,
                    JSONObject().put("title", title).put("body", body).put("labels", JSONArray().put("reporte")))
                    ?: return@runCatching false
                // The record goes as comments: GitHub takes up to 65 536 characters per message.
                val number = JSONObject(issue).optInt("number")
                if (number > 0) chunks(log, 60_000).forEachIndexed { i, part ->
                    post("https://api.github.com/repos/$REPO/issues/$number/comments", token,
                        JSONObject().put("body", "Registro ${i + 1}\n```\n$part\n```"))
                }
                true
            }.getOrDefault(false)
            main.post {
                if (ok) Toast.makeText(app, "Reporte enviado. ¡Gracias!", Toast.LENGTH_LONG).show()
                else { lastSent = 0; share(context, what) }
            }
        }.start()
    }
}
