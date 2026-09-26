package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test

class ExpressTimingTest {
    @Test fun timeIsConservedAcrossStagesBackgroundAndCompletion() {
        var now=0L
        val trace=ExpressTiming {now}
        assertNull(trace.snapshot())
        trace.start();now=120;trace.move(ExpressTiming.Phase.PREX)
        now=300;trace.move(ExpressTiming.Phase.VERIFICATION)
        now=500;trace.pause(true);trace.pause(true)
        now=1500;trace.move(ExpressTiming.Phase.CARD);trace.pause(false)
        now=2000;trace.interaction();trace.move(ExpressTiming.Phase.RETURN)
        now=2400;trace.finish(ExpressTiming.End.BALANCE)
        val result=trace.snapshot()!!
        assertEquals(2400L,result.elapsedMs)
        assertEquals(result.elapsedMs,result.durations.values.sum())
        assertEquals(1000L,result.durations[ExpressTiming.Phase.BACKGROUND])
        assertEquals(200L,result.durations[ExpressTiming.Phase.VERIFICATION])
        assertEquals(2,result.nativeActions)
        now=9999;trace.interaction();trace.move(ExpressTiming.Phase.CARD);trace.finish(ExpressTiming.End.ERROR)
        assertEquals(result,trace.snapshot())
    }
    @Test fun readingMetricsDoesNotChangeTimingAndNewJourneyForgetsTheLastOne() {
        var now=100L
        val trace=ExpressTiming {now}
        trace.start();now=600
        val first=trace.snapshot()
        assertEquals(first,trace.snapshot())
        assertTrue(trace.report().contains("Tiempo total: 0,5 s"))
        now=1100;trace.move(ExpressTiming.Phase.PREX)
        assertEquals(1000L,trace.snapshot()!!.durations[ExpressTiming.Phase.STM])
        trace.finish(ExpressTiming.End.ERROR);trace.start()
        assertEquals(0L,trace.snapshot()!!.elapsedMs)
        assertEquals(1,trace.snapshot()!!.nativeActions)
        assertNull(trace.snapshot()!!.end)
        trace.clear();assertEquals("",trace.report())
        trace.pause(true);trace.move(ExpressTiming.Phase.RETURN);trace.finish(ExpressTiming.End.CANCELLED)
        assertNull(trace.snapshot())
    }
}
