package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test

class TicketBudgetTest {
    @Test fun noOverdraftOrInsufficientMoneyIsCountedAsTickets() {
        for(balance in listOf(null,Long.MIN_VALUE,-1L,0L,5199L))assertTrue(TicketBudget.combinations(balance).isEmpty())
        assertEquals(listOf(TicketMix(1,0)),TicketBudget.combinations(5200))
        assertEquals(setOf(TicketMix(1,0),TicketMix(0,1)),TicketBudget.combinations(7800).toSet())
    }
    @Test fun thousandPesosGivesAllUsefulMixesAndBothPureAlternatives() {
        val options=TicketBudget.combinations(100000)
        assertEquals(14,options.size)
        assertEquals(TicketMix(7,8),options[2])
        assertEquals(TicketMix(19,0),options[0]);assertEquals(TicketMix(0,12),options[1])
        assertTrue(options.contains(TicketMix(16,2)));assertTrue(options.contains(TicketMix(7,8)))
        assertEquals(options,TicketBudget.combinations(100000))
    }
    @Test fun variedAndExtremeBalancesStayAffordableDistinctAndBounded() {
        val balances=(0L..200000L step 137L).toList()+listOf(5199,5200,7799,7800,15600,Long.MAX_VALUE)
        for(balance in balances) {
            val options=TicketBudget.combinations(balance)
            assertEquals(options.size,options.distinct().size)
            assertTrue(options.size<=49)
            for(mix in options) {
                assertTrue(mix.oneHour>=0&&mix.twoHours>=0)
                assertTrue(mix.oneHour+mix.twoHours>0)
                assertTrue(mix.cost in 1..balance)
                // Every mixed option uses the remaining money as far as another 1 h ticket permits.
                if(mix.oneHour>0)assertTrue(balance-mix.cost<TicketBudget.ONE_HOUR)
            }
        }
    }
}
