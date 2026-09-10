package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class CardInputTest {
    @Test fun validatesProviderLengthsAndCurrentExpiryMonth() {
        assertTrue(CardInput.panValid("4111111111111111"))
        assertFalse(CardInput.panValid("4111111111111112"))
        assertFalse(CardInput.panValid("4111111111111111111"))
        assertTrue(CardInput.expiryValid("0926",YearMonth.of(2026,9)))
        assertFalse(CardInput.expiryValid("0826",YearMonth.of(2026,9)))
        assertFalse(CardInput.expiryValid("1326",YearMonth.of(2026,9)))
        assertFalse(CardInput.expiryValid("0029",YearMonth.of(2026,9)))
        assertTrue(CardInput.cvvValid("123"));assertTrue(CardInput.cvvValid("1234"))
        assertFalse(CardInput.cvvValid("12"));assertFalse(CardInput.cvvValid("12a"))
    }
}
