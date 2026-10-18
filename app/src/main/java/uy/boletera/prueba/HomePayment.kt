package uy.boletera.prueba

/**
 * Lab: decisions of the home that depend on the payment record and the balance STM reports. Pure on purpose (no WebView,
 * no Context, no clock of its own) so they are tested on the JVM; StmEngine only applies what these return.
 */
internal object HomePayment {
    /** A record older than this is dropped: nobody waits half an hour for a payment (Lab). */
    const val STALE_MS = 30 * 60 * 1000L
    /** Coming back to the app re-reads a «Recarga en camino» that was last read more than this long ago. */
    const val ON_THE_WAY_REREAD_MS = 60_000L

    enum class Verdict {
        /** Nothing to settle (no record, other card, or still unconfirmed). */
        NONE,
        /** STM says «credited» but the balance has not moved yet: keep the record, read the balance again. */
        AWAIT_BALANCE,
        /** The balance shows the payment: close the record (when the journal can be cleared) with «Recarga hecha». */
        CLOSE_CREDITED,
        /** Prex rejected it: close the record. */
        CLOSE_REJECTED
    }

    /**
     * @property record the record to keep in memory (null if dropped as stale; «credited» if the balance now shows it).
     * @property forgetStale the record was dropped as stale: the journal must be cleared too.
     */
    data class Settlement(val record: PaymentRecord?, val forgetStale: Boolean, val verdict: Verdict)

    fun settle(record: PaymentRecord?, card: String?, balance: Long?, now: Long, isLab: Boolean): Settlement {
        var current = record
        var stale = false
        if (isLab && current != null && now - current.startedAt > STALE_MS) { current = null; stale = true }
        val seen = current
        // A payment left without Prex's answer («authorizing») that shows up in the balance also loaded (Lab 21).
        if (isLab && seen != null && seen.phase in setOf("confirmed", "pending", "authorizing") && seen.card == card &&
            seen.balanceBefore != null && (balance ?: Long.MIN_VALUE) >= seen.balanceBefore + seen.amount)
            current = seen.copy(phase = "credited")
        val r = current
        val verdict = when {
            r == null || r.phase !in setOf("credited", "rejected") || r.card != card -> Verdict.NONE
            r.phase == "credited" && r.balanceBefore != null && balance == r.balanceBefore -> Verdict.AWAIT_BALANCE
            r.phase == "credited" -> Verdict.CLOSE_CREDITED
            else -> Verdict.CLOSE_REJECTED
        }
        return Settlement(current, stale, verdict)
    }

    /**
     * Lab 18: a payment Prex (or STM's receipt) confirmed, not yet in the balance, is said on the home so nobody pays
     * twice; never a doubtful one («pending» stays silent, as the owner asked). Returns the amount, or null.
     */
    fun onTheWay(record: PaymentRecord?, card: String?, balance: Long?, isLab: Boolean): Long? =
        if (!isLab) null
        else record?.takeIf {
            it.card == card && (it.phase == "confirmed" || it.phase == "credited" && it.balanceBefore != null && balance == it.balanceBefore)
        }?.amount

    /** What the home keeps when the phone has no internet: the «en camino» of a confirmed payment survives the old balance. */
    fun offlineOnTheWay(record: PaymentRecord?, card: String?, balance: Long?, isLab: Boolean, current: Long?): Long? =
        onTheWay(record, card, balance, isLab) ?: current

    /** Lab: the page is left mid-step by the cut, so the old minimum would make «Recargar» wait forever; it is read again. */
    fun offlineMinimum(isLab: Boolean, current: Long?): Long? = if (isLab) null else current

    /** What to do when the app comes back to the front. */
    data class ResumePlan(val restartPoll: Boolean, val refresh: Boolean)

    /**
     * Back on the home, a «Recarga en camino» is read again (so it says when STM added it), and so is a page left out of
     * step by a cut. The poll keeps running on the home, so the refresh is independent of [restartPoll] (Lab 21 had it as
     * an «else» of the poll and never ran).
     */
    fun resumePlan(
        stage: String, pollActive: Boolean, isLab: Boolean, accountVerified: Boolean, busy: Boolean, credited: Long?,
        noInternet: Boolean, onTheWay: Long?, consultedAt: Long?, now: Long, pageStage: String
    ): ResumePlan {
        if (stage == "externalPayment") return ResumePlan(restartPoll = false, refresh = true)
        val refresh = isLab && accountVerified && stage == "balance" && !busy && credited == null && !noInternet &&
            (onTheWay != null && now - (consultedAt ?: 0L) > ON_THE_WAY_REREAD_MS || pageStage != "amount")
        return ResumePlan(restartPoll = pollActive, refresh = refresh)
    }
}
