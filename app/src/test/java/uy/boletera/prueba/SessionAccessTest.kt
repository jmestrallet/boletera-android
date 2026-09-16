package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test

class SessionAccessTest {
    @Test fun keepsAuthorizedAccessOnlyUntilCleared() {
        val access=SessionAccess()
        assertFalse(access.available);assertNull(access.open())
        access.remember("00000000","synthetic-password")
        assertTrue(access.available)
        assertEquals("00000000" to "synthetic-password",access.open())
        access.clear()
        assertFalse(access.available);assertNull(access.open())
    }

    @Test fun replacingAccessRemovesThePreviousValue() {
        val access=SessionAccess()
        access.remember("00000000","first")
        access.remember("11111111","second")
        assertEquals("11111111" to "second",access.open())
    }
}
