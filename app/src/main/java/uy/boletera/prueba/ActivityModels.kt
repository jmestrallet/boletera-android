package uy.boletera.prueba

import java.time.LocalDateTime
import java.time.YearMonth

enum class ActivityAccess { LOADING, IDENTITY_REQUIRED, UNAVAILABLE, READY }
enum class MovementKind(val label: String) { ALL("Todo"), TRIP("Viajes"), RECHARGE("Recargas"), REFUND("Devoluciones"), OTHER("Otros") }
enum class MovementStatus(val label: String) { RECORDED("Registrado por STM"), PENDING("Pendiente"), REVERSED("Anulado") }

/** Only source-backed fields belong here. UI examples live exclusively in androidTest. */
data class StmMovement(
    val id: String, val card: String, val date: LocalDateTime,
    val kind: MovementKind, val title: String, val amount: Long,
    val status: MovementStatus = MovementStatus.RECORDED,
    val detail: List<Pair<String,String>> = emptyList()
)

data class FrequentProgress(
    val card: String, val month: YearMonth, val eligibleTrips: Int, val estimated: Boolean,
    val updatedAt: Long, val regime: String = "Montevideo · STM común"
) {
    init { require(eligibleTrips >= 0 && eligibleTrips <= 10000) }
    val target = 40
    val remaining get() = (target - eligibleTrips).coerceAtLeast(0)
    // Under the IM regime, all qualifying trips count once the monthly threshold is met.
    val estimatedRefund: Long? get() = if(eligibleTrips >= target) eligibleTrips * 200L else null
}

data class ActivityState(
    val card: String = "", val access: ActivityAccess = ActivityAccess.LOADING,
    val entries: List<StmMovement> = emptyList(),
    val availableMonths: List<YearMonth> = emptyList(),
    val completeMonths: Set<YearMonth> = emptySet(),
    val frequent: List<FrequentProgress> = emptyList(),
    val consultedAt: Long? = null
) {
    fun movements(month: YearMonth, filter: MovementKind = MovementKind.ALL) = entries
        .filter { it.card==card && YearMonth.from(it.date)==month && (filter==MovementKind.ALL || it.kind==filter) }
        .distinctBy { it.id }.sortedByDescending { it.date }
    fun total(month: YearMonth, kind: MovementKind): Long? {
        if(access!=ActivityAccess.READY || month !in completeMonths)return null
        return movements(month,kind).filter {it.status==MovementStatus.RECORDED}.sumOf { it.amount }
    }
    fun progress(month: YearMonth): FrequentProgress? =
        if(access==ActivityAccess.READY)frequent.singleOrNull {it.month==month && it.card==card} else null
}
