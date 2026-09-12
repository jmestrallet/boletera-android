package uy.boletera.prueba

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class PaymentJournalTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun encryptedUnresolvedPaymentSurvivesRecreationAndIsIsolatedByAccount() {
        val account="journal-test-account-a";val other="journal-test-account-b"
        val journal=PaymentJournal(context)
        val record=PaymentRecord("DEMO1234","1033",56400,1000,26000,transaction="DEMO-TRANSACTION")
        try {
            assertTrue(journal.write(account,record))
            val raw=context.getSharedPreferences("payment_journal",0).getString(account,null)!!
            assertFalse(raw.contains("DEMO1234"));assertFalse(raw.contains("DEMO-TRANSACTION"));assertFalse(raw.contains("56400"))
            assertEquals(record,PaymentJournal(context).read(account))
            assertNull(PaymentJournal(context).read(other))
            assertTrue(journal.write(account,record.copy(phase="pending")))
            assertEquals("pending",PaymentJournal(context).read(account)?.phase)
            assertTrue(journal.clear(account));assertNull(journal.read(account))
        } finally {journal.clear(account);journal.clear(other)}
    }
    @Test fun corruptJournalCannotBeSilentlyReplaced() {
        val account="journal-test-corrupt"
        val prefs=context.getSharedPreferences("payment_journal",0)
        try {
            prefs.edit().putString(account,"invalid encrypted record").commit()
            assertFalse(PaymentJournal(context).write(account,PaymentRecord("DEMO1234","1033",26000,1000,null)))
            assertEquals("invalid encrypted record",prefs.getString(account,null))
        } finally {PaymentJournal(context).clear(account)}
    }
    @Test fun cardIdentityRecognizesTheSameCardWithoutKeepingItsNumberAndCannotCrossAccounts() {
        val choices=JourneyPreferences(context)
        choices.useAccount("99990000")
        val first=choices.cardIdentity("4111111111111111")
        assertNotNull(first);assertFalse(first!!.contains("4111111111111111"))
        assertEquals(first,choices.cardIdentity("4111111111111111"))
        assertNotEquals(first,choices.cardIdentity("5555555555554444"))
        choices.useAccount("99990001");assertNotEquals(first,choices.cardIdentity("4111111111111111"))
    }
}
