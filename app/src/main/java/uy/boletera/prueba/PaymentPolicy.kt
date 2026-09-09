package uy.boletera.prueba

import java.net.URI

/** Payment destinations observed in the real STM journey. Never accepts bank login data. */
object PaymentPolicy {
    val supported = setOf("1033", "1002")
    const val BROU_ACTION = "https://ebanking.brou.com.uy/multipagos/billetera"
    private val gatewayHosts = setOf("spf.sistarbanc.com.uy", "pasarelaspe.sistarbanc.com.uy")
    val brouFields = setOf("FORMSCROLLLEFThtmlPageTopContainer_pageForm", "FORMSCROLLTOPhtmlPageTopContainer_pageForm",
        "SALMONFORMhtmlPageTopContainer_pageForm", "Page_refIndex_hidden", "codComercio", "nroTrxComercio", "Fecha", "Monto", "Moneda",
        "urlVueltaOK", "urlVueltaERROR", "urlCONTROL", "IDBanco", "importeDevolucion", "idCuenta", "idOrganismo")

    fun gateway(url: String): Boolean = try {
        val u = URI(url)
        u.scheme == "https" && u.host in gatewayHosts && u.userInfo == null && (u.port == -1 || u.port == 443)
    } catch (_: Exception) { false }

    fun prexLink(url: String): Boolean = try {
        val u = URI(url)
        gateway(url) && u.host == "pasarelaspe.sistarbanc.com.uy" &&
            u.path in setOf("/v2/seleccionBanco", "/v2/confirmarPago") && u.fragment == null &&
            Regex("id=[A-Za-z0-9%_+./=-]{1,1024}").matches(u.rawQuery ?: "")
    } catch (_: Exception) { false }

    fun validBrou(action: String, fields: Map<String, String>, cents: Long): Boolean {
        if (action != BROU_ACTION || fields.keys != brouFields || fields.values.any { it.length > 4096 }) return false
        // BROU's Monto is integer cents. Currency 98 was observed for the UYU STM charge.
        if (cents <= 0 || fields["Monto"]?.toLongOrNull() != cents || fields["Moneda"] != "98") return false
        if (fields["nroTrxComercio"].isNullOrBlank() || fields["codComercio"].isNullOrBlank()) return false
        val destinations = mapOf("urlVueltaOK" to "/spfws/UrlVueltaOK", "urlVueltaERROR" to "/spfws/UrlVueltaERROR", "urlCONTROL" to "/spfe/RetornoBROU.jsp")
        return destinations.all { (key, path) ->
            try {
                val u = URI(fields[key] ?: "")
                gateway(u.toString()) && u.host == "spf.sistarbanc.com.uy" && u.path == path
            } catch (_: Exception) { false }
        }
    }
}
