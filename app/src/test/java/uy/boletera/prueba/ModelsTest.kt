package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    @Test fun amountsAreExactAndRejectAmbiguity() {
        assertEquals(56400L, Amounts.parse("564"))
        assertEquals(56425L, Amounts.parse("564,25"))
        assertEquals(56425L, Amounts.parse("564.25"))
        listOf("0", "-1", "1.000,00", "NaN", "1e3", "", "260.123", "99999999999999999999").forEach { assertNull(it, Amounts.parse(it)) }
    }
    @Test fun aDebtDoesNotInventMinimum() {
        assertFalse(Amounts.valid(56400L, null))
        assertFalse(Amounts.valid(56399L, 56400L))
        assertTrue(Amounts.valid(56400L, 56400L))
        assertTrue(Amounts.valid(70000L, 56400L))
    }
    @Test fun navigationRejectsOriginConfusion() {
        assertTrue(NavigationPolicy.allowed("https://stm.gub.uy/app/mistm/cuenta/"))
        assertTrue(NavigationPolicy.allowed("https://mi.iduruguay.gub.uy/login?process_state=example"))
        listOf("http://stm.gub.uy/", "https://stm.gub.uy.evil.test/", "https://stm.gub.uy@evil.test/",
            "https://evil@stm.gub.uy/", "https://stm.gub.uy:444/", "file:///data/local/secret", "javascript:alert(1)",
            "https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=example").forEach { assertFalse(it, NavigationPolicy.allowed(it)) }
    }
}
