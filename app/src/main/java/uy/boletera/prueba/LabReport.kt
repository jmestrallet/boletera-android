package uy.boletera.prueba

import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * «Enviar reporte»: what went wrong, the phone and the masked trail of STM pages, ready to send by WhatsApp or mail.
 * The trail never has the document, the password, card data or numbers (digits are masked when it is recorded).
 */
internal object LabReport {
    /** The engine's masked diagnostic, set by the activity. */
    var diagnostic: () -> String = { "" }
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
}
