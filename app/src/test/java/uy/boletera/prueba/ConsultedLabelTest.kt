package uy.boletera.prueba

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ConsultedLabelTest {
    private fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, day, hour, minute, 0) }.timeInMillis
    @Test fun todayShowsOnlyTheTime() = assertEquals("14:32", consultedLabel(at(15, 14, 32), at(15, 20, 0)))
    @Test fun yesterdayIsNamed() = assertEquals("ayer 14:32", consultedLabel(at(14, 14, 32), at(15, 9, 0)))
    @Test fun olderShowsTheDate() = assertTrue(consultedLabel(at(12, 14, 32), at(15, 9, 0)).startsWith("12 "))
}
