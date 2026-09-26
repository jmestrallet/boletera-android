package uy.boletera.prueba

import java.util.Locale

/** One in-memory journey, using elapsed time. No identifiers, URLs, amounts or field values. */
internal class ExpressTiming(private val clock: () -> Long) {
    enum class Phase(val label: String) {
        STM("Preparación en STM"), PREX("Preparación en Prex"),
        VERIFICATION("Verificación del titular"), CARD("Tarjeta y autocompletado"),
        CARD_VERIFICATION("Tarjeta con verificación"), CONFIRMATION("Confirmación"),
        REVIEW("Revisión manual"), RETURN("Resultado y regreso al saldo"),
        RECOVERY("Recuperación de sesión"), BACKGROUND("En segundo plano")
    }
    enum class End(val label: String) {
        BALANCE("Regreso al saldo"), CANCELLED("Recorrido interrumpido"), ERROR("Detenido por un error")
    }
    data class Snapshot(val elapsedMs: Long, val durations: Map<Phase, Long>, val nativeActions: Int, val end: End?)
    private val durations = linkedMapOf<Phase, Long>()
    private var startedAt: Long? = null
    private var updatedAt = 0L
    private var finishedAt: Long? = null
    private var paused = false
    private var actions = 0
    private var end: End? = null
    var phase = Phase.STM
        private set
    val active get() = startedAt != null && finishedAt == null

    fun start() {
        clear()
        val now=clock();startedAt=now;updatedAt=now
        actions=1 // The accepted tap on Carga Express.
    }
    private fun checkpoint(now: Long) {
        val bucket=if(paused)Phase.BACKGROUND else phase
        durations[bucket]=(durations[bucket]?:0L)+(now-updatedAt).coerceAtLeast(0)
        updatedAt=now
    }
    fun move(next: Phase) {
        if(!active || next==phase)return
        checkpoint(clock());phase=next
    }
    fun pause(value: Boolean) {
        if(!active || paused==value)return
        checkpoint(clock());paused=value
    }
    fun interaction() { if(active)actions++ }
    fun finish(reason: End) {
        if(!active)return
        val now=clock();checkpoint(now);finishedAt=now;end=reason
    }
    fun clear() {
        durations.clear();startedAt=null;finishedAt=null;updatedAt=0;paused=false;actions=0;end=null;phase=Phase.STM
    }
    fun snapshot(): Snapshot? {
        val start=startedAt?:return null
        val now=finishedAt?:clock()
        val values=LinkedHashMap(durations)
        if(active) {
            val bucket=if(paused)Phase.BACKGROUND else phase
            values[bucket]=(values[bucket]?:0L)+(now-updatedAt).coerceAtLeast(0)
        }
        return Snapshot((now-start).coerceAtLeast(0),values.toMap(),actions,end)
    }
    fun report(): String {
        val value=snapshot()?:return ""
        fun seconds(ms: Long)=String.format(Locale.forLanguageTag("es-UY"),"%.1f s",ms/1000.0)
        return buildString {
            append("Última Carga Express · ").append(value.end?.label?:"En curso")
            append("\nTiempo total: ").append(seconds(value.elapsedMs))
            value.durations.filterValues {it>0}.forEach {(step,time)->append("\n").append(step.label).append(": ").append(seconds(time))}
            append("\nInicio y avances manuales desde Boletera: ").append(value.nativeActions)
            append("\nNo cuenta escritura, toques en Google, navegación ni controles de la página original. Los tiempos incluyen la espera del sitio y de la persona; no miden solo la red. Medición local de esta apertura, sin datos personales.")
        }
    }
}
