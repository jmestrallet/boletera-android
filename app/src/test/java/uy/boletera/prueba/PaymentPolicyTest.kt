package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test

class PaymentPolicyTest {
    private fun fields() = PaymentPolicy.brouFields.associateWith { "synthetic" }.toMutableMap().apply {
        put("Monto", "26000"); put("Moneda", "98")
        put("urlVueltaOK", "https://spf.sistarbanc.com.uy/spfws/UrlVueltaOK")
        put("urlVueltaERROR", "https://spf.sistarbanc.com.uy/spfws/UrlVueltaERROR")
        put("urlCONTROL", "https://spf.sistarbanc.com.uy/spfe/RetornoBROU.jsp")
    }
    @Test fun prexHandoffRejectsOtherOriginsPortsPathsAndExtraParameters() {
        assertTrue(PaymentPolicy.prexLink("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=synthetic"))
        listOf("http://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=test", "https://pasarelaspe.sistarbanc.com.uy.evil.test/v2/confirmarPago?id=test",
            "https://evil@pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=test", "https://pasarelaspe.sistarbanc.com.uy:444/v2/confirmarPago?id=test",
            "https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=test&redirect=evil", "https://pasarelaspe.sistarbanc.com.uy/v2/other?id=test").forEach { assertFalse(it, PaymentPolicy.prexLink(it)) }
    }
    @Test fun brouPreservesExactCentsCurrencyFieldsAndBankDestination() {
        assertTrue(PaymentPolicy.validBrou(PaymentPolicy.BROU_ACTION, fields(), 26000))
        assertFalse(PaymentPolicy.validBrou(PaymentPolicy.BROU_ACTION, fields(), 260))
        assertFalse(PaymentPolicy.validBrou("https://evil.test", fields(), 26000))
        assertFalse(PaymentPolicy.validBrou(PaymentPolicy.BROU_ACTION, fields().apply { put("Moneda", "840") }, 26000))
        assertFalse(PaymentPolicy.validBrou(PaymentPolicy.BROU_ACTION, fields().apply { put("password", "never-allowed") }, 26000))
        assertFalse(PaymentPolicy.validBrou(PaymentPolicy.BROU_ACTION, fields().apply { put("urlVueltaOK", "https://evil.test/spfws/UrlVueltaOK") }, 26000))
        assertFalse(PaymentPolicy.validBrou(PaymentPolicy.BROU_ACTION, fields().apply { remove("idOrganismo") }, 26000))
    }
}
