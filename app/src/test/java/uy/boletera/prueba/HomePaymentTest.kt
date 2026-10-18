package uy.boletera.prueba

import org.junit.Assert.*
import org.junit.Test
import uy.boletera.prueba.HomePayment.Verdict

/**
 * Real cases of «registro de pagos + saldo → qué muestra el inicio». Amounts are in cents like STM's: a $ 260 payment on a
 * $ 100 balance. Each test says, in plain words, what the person would see.
 */
class HomePaymentTest {
    private val card = "5042300"
    private val now = 1_000_000_000_000L
    private fun record(phase: String, startedAt: Long = now - 60_000, balanceBefore: Long? = 10_000L, cardId: String = card, amount: Long = 26_000L) =
        PaymentRecord(cardId, "1033", amount, startedAt, balanceBefore, phase)

    private fun settle(r: PaymentRecord?, balance: Long?, isLab: Boolean = true, at: Long = now, forCard: String? = card) =
        HomePayment.settle(r, forCard, balance, at, isLab)

    // ---- registro + saldo -------------------------------------------------------------------------------------------

    @Test fun prexConfirmedAndStmHasNotAddedYet_homeSaysRechargeOnTheWay() {
        val r = record("confirmed")
        val s = settle(r, balance = 10_000L)
        assertEquals(Verdict.NONE, s.verdict)
        assertEquals(r, s.record)
        assertEquals(26_000L, HomePayment.onTheWay(s.record, card, 10_000L, isLab = true))
    }

    @Test fun stmAlreadyAddedIt_confirmedPaymentIsClosedAsCredited() {
        val s = settle(record("confirmed"), balance = 36_000L)
        assertEquals(Verdict.CLOSE_CREDITED, s.verdict)
        assertEquals("credited", s.record?.phase)
    }

    @Test fun balanceHigherThanExpected_stillCountsAsCredited() {
        // Somebody else topped the card up too: it is at least what we paid, so the payment is in.
        assertEquals(Verdict.CLOSE_CREDITED, settle(record("confirmed"), balance = 90_000L).verdict)
    }

    @Test fun paymentLeftWithoutAnswer_butBalanceShowsIt_isPromotedToCredited() {
        // «authorizing» and «pending» are the doubtful ones: the balance is the proof (Lab 21).
        listOf("authorizing", "pending").forEach { phase ->
            val s = settle(record(phase), balance = 36_000L)
            assertEquals(phase, Verdict.CLOSE_CREDITED, s.verdict)
        }
    }

    @Test fun doubtfulPaymentWithBalanceUnchanged_staysSilentOnTheHome() {
        listOf("authorizing", "pending").forEach { phase ->
            val s = settle(record(phase), balance = 10_000L)
            assertEquals(Verdict.NONE, s.verdict)
            assertNull(phase, HomePayment.onTheWay(s.record, card, 10_000L, isLab = true))
        }
    }

    @Test fun balanceMissing_neverPromotesAPayment() {
        assertEquals(Verdict.NONE, settle(record("confirmed"), balance = null).verdict)
    }

    @Test fun creditedByStmButBalanceStillTheOldOne_waitsAndKeepsSayingOnTheWay() {
        val r = record("credited")
        val s = settle(r, balance = 10_000L)
        assertEquals(Verdict.AWAIT_BALANCE, s.verdict)
        assertEquals(26_000L, HomePayment.onTheWay(s.record, card, 10_000L, isLab = true))
    }

    @Test fun creditedWithoutKnownBalanceBefore_closesInsteadOfWaitingForever() {
        assertEquals(Verdict.CLOSE_CREDITED, settle(record("credited", balanceBefore = null), balance = 10_000L).verdict)
    }

    @Test fun prexRejected_isClosedAndNothingIsOnTheWay() {
        val s = settle(record("rejected"), balance = 10_000L)
        assertEquals(Verdict.CLOSE_REJECTED, s.verdict)
        assertNull(HomePayment.onTheWay(s.record, card, 10_000L, isLab = true))
    }

    @Test fun pendingForMoreThanThirtyMinutes_isForgottenAndJournalCleared() {
        val s = settle(record("confirmed", startedAt = now - HomePayment.STALE_MS - 1), balance = 10_000L)
        assertTrue(s.forgetStale)
        assertNull(s.record)
        assertEquals(Verdict.NONE, s.verdict)
        assertNull(HomePayment.onTheWay(s.record, card, 10_000L, isLab = true))
    }

    @Test fun exactlyThirtyMinutes_isStillKept() {
        val s = settle(record("confirmed", startedAt = now - HomePayment.STALE_MS), balance = 10_000L)
        assertFalse(s.forgetStale)
        assertNotNull(s.record)
    }

    @Test fun paymentOfAnotherCard_isIgnoredByThisHome() {
        val other = record("confirmed", cardId = "9999999")
        val s = settle(other, balance = 36_000L)
        assertEquals(Verdict.NONE, s.verdict)
        assertEquals("confirmed", s.record?.phase)
        assertNull(HomePayment.onTheWay(other, card, 36_000L, isLab = true))
        // ...and a rejected/credited record of the other card is not closed from here either.
        assertEquals(Verdict.NONE, settle(record("rejected", cardId = "9999999"), balance = 10_000L).verdict)
    }

    @Test fun nonLab_neverForgetsPromotesOrShowsOnTheWay() {
        val old = record("confirmed", startedAt = now - HomePayment.STALE_MS * 3)
        val s = settle(old, balance = 90_000L, isLab = false)
        assertFalse(s.forgetStale)
        assertEquals(old, s.record)
        assertEquals(Verdict.NONE, s.verdict)
        assertNull(HomePayment.onTheWay(old, card, 10_000L, isLab = false))
        // The old behaviour for credited/rejected stays for the non-Lab apps.
        assertEquals(Verdict.CLOSE_REJECTED, settle(record("rejected"), balance = 10_000L, isLab = false).verdict)
    }

    @Test fun noRecord_nothingToShow() {
        val s = settle(null, balance = 10_000L)
        assertEquals(Verdict.NONE, s.verdict)
        assertNull(s.record)
        assertFalse(s.forgetStale)
    }

    // ---- sin internet tras pagar ------------------------------------------------------------------------------------

    @Test fun noInternetAfterPaying_confirmedPaymentKeepsSayingOnTheWay() {
        val way = HomePayment.offlineOnTheWay(record("confirmed"), card, 10_000L, isLab = true, current = null)
        assertEquals(26_000L, way)
    }

    @Test fun noInternet_withoutConfirmedPayment_keepsWhatTheHomeAlreadySaid() {
        assertEquals(26_000L, HomePayment.offlineOnTheWay(null, card, 10_000L, isLab = true, current = 26_000L))
        assertNull(HomePayment.offlineOnTheWay(record("authorizing"), card, 10_000L, isLab = true, current = null))
    }

    @Test fun noInternet_forgetsTheMinimumInLabButNotInTheOtherApps() {
        // Lab: the page is left mid-step, an old minimum would leave «Recargar» waiting forever.
        assertNull(HomePayment.offlineMinimum(isLab = true, current = 26_000L))
        assertEquals(26_000L, HomePayment.offlineMinimum(isLab = false, current = 26_000L))
    }

    // ---- volver a la app --------------------------------------------------------------------------------------------

    private fun plan(
        stage: String = "balance", pollActive: Boolean = true, isLab: Boolean = true, verified: Boolean = true, busy: Boolean = false,
        credited: Long? = null, noInternet: Boolean = false, onTheWay: Long? = null, consultedAt: Long? = now, pageStage: String = "amount"
    ) = HomePayment.resumePlan(stage, pollActive, isLab, verified, busy, credited, noInternet, onTheWay, consultedAt, now, pageStage)

    @Test fun backToTheApp_pageLeftOutsideAmount_refreshesEvenThoughThePollIsRunning() {
        // Lab 21 regression: the refresh was an «else» of the poll, so with the poll active it never ran.
        val p = plan(pollActive = true, pageStage = "document")
        assertTrue(p.refresh)
        assertTrue(p.restartPoll)
    }

    @Test fun backToTheApp_onTheWayReadMoreThanAMinuteAgo_refreshesEvenThoughThePollIsRunning() {
        val p = plan(pollActive = true, onTheWay = 26_000L, consultedAt = now - 61_000)
        assertTrue(p.refresh)
        assertTrue(p.restartPoll)
    }

    @Test fun backToTheApp_onTheWayReadJustNow_doesNotRefresh() {
        assertFalse(plan(onTheWay = 26_000L, consultedAt = now - 60_000).refresh)
        assertFalse(plan(onTheWay = 26_000L, consultedAt = now - 5_000).refresh)
    }

    @Test fun backToTheApp_neverReadOnTheWay_refreshes() {
        assertTrue(plan(onTheWay = 26_000L, consultedAt = null).refresh)
    }

    @Test fun backToTheApp_pageAtAmountAndNothingOnTheWay_leavesTheHomeAlone() {
        val p = plan()
        assertFalse(p.refresh)
        assertTrue(p.restartPoll)
    }

    @Test fun backToTheApp_doesNotRefreshWhenItWouldBeWrong() {
        assertFalse("busy", plan(busy = true, pageStage = "document").refresh)
        assertFalse("no internet", plan(noInternet = true, pageStage = "document").refresh)
        assertFalse("not verified", plan(verified = false, pageStage = "document").refresh)
        assertFalse("not the home", plan(stage = "openingPayment", pageStage = "document").refresh)
        assertFalse("«Recarga hecha» on screen", plan(credited = 26_000L, pageStage = "document").refresh)
        assertFalse("other apps", plan(isLab = false, pageStage = "document").refresh)
    }

    @Test fun backToTheApp_fromTheExternalPayment_alwaysRefreshesWithoutRestartingThePoll() {
        val p = plan(stage = "externalPayment", pollActive = true, isLab = false)
        assertTrue(p.refresh)
        assertFalse(p.restartPoll)
    }

    @Test fun backToTheApp_pollInactive_doesNotRestartIt() {
        assertFalse(plan(pollActive = false).restartPoll)
    }

    // ---- el motor usa estas funciones (no vuelve a tener la lógica inline) ----------------------------------------

    private fun engineSource(): String {
        val candidates = listOf("src/main/java/uy/boletera/prueba/StmEngine.kt", "app/src/main/java/uy/boletera/prueba/StmEngine.kt")
        return java.io.File(candidates.first { java.io.File(it).exists() }).readText()
    }
    private fun body(source: String, signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("no encuentro $signature en StmEngine.kt", start >= 0)
        // The next member at the same indent ends the body.
        val end = Regex("\\n    (private |internal |override |fun |/\\*\\*)").find(source, start + signature.length)?.range?.first ?: source.length
        return source.substring(start, end)
    }

    @Test fun engineResume_usesThePlanAndHasNoElseOnThePoll() {
        val resume = body(engineSource(), "fun resume()")
        assertTrue(resume.contains("HomePayment.resumePlan("))
        assertFalse("el refresco volvió a ser un «else» del sondeo", Regex("\\belse\\b").containsMatchIn(resume))
    }

    @Test fun engineLeaveOffline_andBalanceBranchUseHomePayment() {
        val source = engineSource()
        val offline = body(source, "private fun leaveOffline()")
        assertTrue(offline.contains("HomePayment.offlineOnTheWay("))
        assertTrue(offline.contains("HomePayment.offlineMinimum("))
        assertTrue(source.contains("HomePayment.settle("))
        assertTrue(source.contains("HomePayment.onTheWay("))
    }
}
