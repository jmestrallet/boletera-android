package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test

class PayerDocumentTest {
    private val payer = PayerProfile("example", "Ejemplo", "Persona", "Ficticia", "00000000", "test@example.invalid", "000000000")

    @Test fun defaultsToUruguayanIdAndForeignDocumentsKeepLetters() {
        assertEquals("CI", payer.documentType)
        assertTrue(payer.valid())
        assertFalse(payer.copy(document = "AB12345").valid())
        assertTrue(payer.copy(documentType = "PAS", document = "AB12345").valid())
        assertFalse(payer.copy(documentType = "UNKNOWN").valid())
        assertFalse(payer.copy(documentType = "PAS", document = " ").valid())
        assertFalse(payer.copy(documentType = "PAS", document = "AB\n123").valid())
    }
}
