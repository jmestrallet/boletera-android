package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

class ActivityModelsTest {
    private val month=YearMonth.of(2026,9)
    private fun trip(id:String,card:String="TEST0001",amount:Long=-5200,status:MovementStatus=MovementStatus.RECORDED)=
        StmMovement(id,card,LocalDateTime.of(2026,9,20,10,0),MovementKind.TRIP,"Viaje de prueba",amount,status)

    @Test fun unknownAndPartialMonthsNeverBecomeZeroTotalsOrFrequentProgress() {
        val blocked=ActivityState(card="TEST0001",access=ActivityAccess.IDENTITY_REQUIRED)
        assertNull(blocked.total(month,MovementKind.TRIP));assertNull(blocked.progress(month))
        val partial=blocked.copy(access=ActivityAccess.READY,entries=listOf(trip("a")))
        assertNull(partial.total(month,MovementKind.TRIP));assertNull(partial.progress(month))
        assertEquals(1,partial.movements(month).size)
    }
    @Test fun totalsExcludeOtherCardsMonthsDuplicatesPendingAndReversedRecords() {
        val entries=listOf(trip("a"),trip("a"),trip("other","OTHER001"),trip("b",status=MovementStatus.PENDING),trip("c",status=MovementStatus.REVERSED),trip("august").copy(date=LocalDateTime.of(2026,8,20,10,0)))
        val state=ActivityState(card="TEST0001",access=ActivityAccess.READY,entries=entries,completeMonths=setOf(month))
        assertEquals(-5200L,state.total(month,MovementKind.TRIP))
        assertEquals(3,state.movements(month).size)
        assertEquals(0L,state.total(month,MovementKind.RECHARGE))
    }
    @Test fun frequentThresholdCountsAllEligibleTripsAndNeverShowsUnearnedRefund() {
        val before=FrequentProgress("TEST0001",month,39,true,0)
        assertEquals(1,before.remaining);assertNull(before.estimatedRefund)
        assertEquals(8000L,before.copy(eligibleTrips=40).estimatedRefund)
        assertEquals(8400L,before.copy(eligibleTrips=42).estimatedRefund)
        assertEquals(0,before.copy(eligibleTrips=42).remaining)
        val state=ActivityState(card="TEST0001",access=ActivityAccess.READY,frequent=listOf(before.copy(card="OTHER001")))
        assertNull(state.progress(month))
    }
}
