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

    /** Sends it to GitHub; says «Reporte enviado» or, if it cannot, opens the share sheet. One every 20 seconds at most. */
    fun send(context: Context, what: String) {
        val app = context.applicationContext
        val token = BuildConfig.REPORT_TOKEN
        if (token.isBlank()) { share(context, what); return }
        val now = System.currentTimeMillis()
        if (now - lastSent < 20_000) { Toast.makeText(app, "El reporte ya se envió.", Toast.LENGTH_SHORT).show(); return }
        lastSent = now
        val body = text(what)
        val title = "${what.take(70).ifBlank { "Reporte" }} · ${BuildConfig.DISPLAY_VERSION} · ${Build.MODEL}"
        val main = Handler(Looper.getMainLooper())
        Toast.makeText(app, "Enviando reporte…", Toast.LENGTH_SHORT).show()
        Thread {
            val ok = runCatching {
                val connection = (URL("https://api.github.com/repos/$REPO/issues").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; doOutput = true; connectTimeout = 10_000; readTimeout = 15_000
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                try {
                    connection.outputStream.use { it.write(JSONObject().put("title", title).put("body", body).put("labels", JSONArray().put("reporte")).toString().toByteArray()) }
                    connection.responseCode in 200..299
                } finally { connection.disconnect() }
            }.getOrDefault(false)
            main.post {
                if (ok) Toast.makeText(app, "Reporte enviado. ¡Gracias!", Toast.LENGTH_LONG).show()
                else { lastSent = 0; share(context, what) }
            }
        }.start()
    }
}
