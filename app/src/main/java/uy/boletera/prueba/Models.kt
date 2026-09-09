package uy.boletera.prueba

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class CardInfo(val id: String, val active: Boolean, val status: String)
data class CaptchaRect(val x: Float, val y: Float, val width: Float, val height: Float)
data class UiState(
    val stage: String = "welcome", val busy: Boolean = false, val message: String = "",
    val cards: List<CardInfo> = emptyList(), val selectedCard: String? = null,
    val balance: Long? = null, val minimum: Long? = null, val amount: Long? = null,
    val consultedAt: Long? = null, val captcha: CaptchaRect? = null,
    val hasSavedAccess: Boolean = false,
    val diagnostic: String = ""
)

object Amounts {
    fun parse(value: String): Long? = try {
        if (!Regex("[0-9]{1,7}([,.][0-9]{1,2})?").matches(value.trim())) null
        else BigDecimal(value.trim().replace(',', '.')).movePointRight(2).longValueExact().takeIf { it > 0 }
    } catch (_: Exception) { null }
    fun valid(amount: Long?, minimum: Long?) = amount != null && minimum != null && minimum > 0 && amount >= minimum
    fun format(cents: Long?): String = cents?.let {
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-UY"))
        nf.minimumFractionDigits = if (it % 100 == 0L) 0 else 2
        nf.maximumFractionDigits = 2
        "$ " + nf.format(BigDecimal(it).divide(BigDecimal(100), 2, RoundingMode.UNNECESSARY))
    } ?: "—"
}

object NavigationPolicy {
    private val hosts = setOf("stm.gub.uy", "mi.iduruguay.gub.uy", "auth.iduruguay.gub.uy", "ih.montevideo.gub.uy")
    fun allowed(url: String): Boolean = try {
        val uri = java.net.URI(url)
        uri.scheme == "https" && uri.host in hosts && uri.userInfo == null && (uri.port == -1 || uri.port == 443)
    } catch (_: Exception) { false }

    fun snapshotMatches(origin: String, currentUrl: String): Boolean = try {
        val page = java.net.URI(currentUrl)
        val sample = java.net.URI(origin)
        allowed(currentUrl) && allowed(origin) && sample.scheme == page.scheme && sample.host == page.host &&
            (sample.port.takeIf { it != -1 } ?: 443) == (page.port.takeIf { it != -1 } ?: 443)
    } catch (_: Exception) { false }
}
