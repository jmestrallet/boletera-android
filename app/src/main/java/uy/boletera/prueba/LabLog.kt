package uy.boletera.prueba

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Lab's step-by-step record (the owner's request, 16/10): screens, taps, what STM and Prex answered, the fingerprint's
 * answer, the app going to the background, so «Enviar reporte» shows exactly where something stalls. It stays in the
 * phone (the last 24 hours, with a size cap) until a report sends it to the private repo.
 *
 * Never written: the document, the password, card number, expiry or code, the captcha. Callers log names of actions
 * and screens, never typed values; and every line goes through [mask], which hides any run of 6 or more digits (a
 * document or a card can never slip through a message).
 */
internal object LabLog {
    private const val MAX_BYTES = 1_500_000L
    private const val KEEP_BYTES = 900_000
    private var file: File? = null
    private val writer = Executors.newSingleThreadExecutor()
    private const val KEEP_MILLIS = 24L * 60 * 60 * 1000
    private val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
    private val longNumber = Regex("\\d[\\d .-]{4,}\\d")

    fun init(context: Context) {
        if (!IsLab || file != null) return
        file = File(context.filesDir, "lab-log.txt")
        writer.execute { dropOld() }
        add("app", "abierta · ${BuildConfig.DISPLAY_VERSION}")
    }

    /** Runs of 6+ digits (documents, cards), even with spaces, dots or dashes between them, become «#». */
    fun mask(text: String) = longNumber.replace(text) { m -> if (m.value.count(Char::isDigit) >= 6) "#" else m.value }

    /** One line: [where] (pantalla, toque, STM, Prex, huella…) and what happened. */
    @Synchronized fun add(where: String, what: String) {
        val target = file ?: return
        val line = "${time.format(Date())}  $where · ${mask(what).replace('\n', ' ').take(400)}"
        writer.execute {
            runCatching {
                target.appendText(line + "\n")
                if (target.length() > MAX_BYTES) {
                    val text = target.readText()
                    target.writeText(text.substring(text.indexOf('\n', text.length - KEEP_BYTES).coerceAtLeast(0) + 1))
                }
            }
        }
    }

    fun tap(label: String) = add("toque", label)

    /** The last 24 hours, oldest first (empty outside Lab). */
    fun read(): String {
        val target = file ?: return ""
        return runCatching { writer.submit<String> { dropOld(); if (target.exists()) target.readText() else "" }.get() }.getOrDefault("")
    }

    /** Only the last 24 hours are kept (the owner sends a report within hours of a problem). Runs on [writer]. */
    private fun dropOld() {
        val target = file ?: return
        if (!target.exists()) return
        val since = System.currentTimeMillis() - KEEP_MILLIS
        val parse = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT)
        val lines = target.readLines()
        val first = lines.indexOfFirst { line -> runCatching { parse.parse(line.take(23))!!.time >= since }.getOrDefault(true) }
        if (first > 0) target.writeText(lines.drop(first).joinToString("\n", postfix = "\n"))
        else if (first < 0) target.delete()
    }

    fun clear() {
        val target = file ?: return
        writer.execute { runCatching { target.delete() } }
        add("app", "registro borrado")
    }
}
