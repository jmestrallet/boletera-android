package uy.boletera.prueba

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorPresentationTest {
    @Test fun stmOutageIsNotPresentedAsTheUsersConnection() {
        val info = errorPresentation("STM no está respondiendo (error 503). La página oficial parece caída o con problemas.")
        assertEquals("STM CAÍDO", info.code)
        assertEquals("STM no responde", info.title)
    }
    @Test fun aRealConnectionProblemStaysAConnectionProblem() {
        assertEquals("CONEXIÓN", errorPresentation("No se pudo conectar. Revisá tu conexión y el estado del pago antes de repetirlo.").code)
    }
}
