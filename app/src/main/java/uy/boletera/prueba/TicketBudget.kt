package uy.boletera.prueba

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

internal data class TicketMix(val oneHour: Long, val twoHours: Long) {
    val cost: Long get()=oneHour*TicketBudget.ONE_HOUR+twoHours*TicketBudget.TWO_HOURS
}

/** Common electronic fares verified against IM, effective 5 January 2026. Not an overdraft allowance. */
internal object TicketBudget {
    const val ONE_HOUR=5200L
    const val TWO_HOURS=7800L
    fun combinations(balance: Long?):List<TicketMix> {
        if(balance==null || balance<ONE_HOUR)return emptyList()
        val maximumTwo=balance/TWO_HOURS
        // Small balances get every useful mix. Sample large balances without allocating per ticket.
        val twos=if(maximumTwo<48)(0L..maximumTwo).toList() else (0..47).map {maximumTwo*it/47}.distinct()
        val oneOnly=TicketMix(balance/ONE_HOUR,0)
        val twoOnly=TicketMix(0,maximumTwo)
        val useful=twos.map {two->TicketMix((balance-two*TWO_HOURS)/ONE_HOUR,two)}
        val mixed=useful.filter {it.oneHour>0 && it.twoHours>0}
        val balanced=mixed.minWithOrNull(compareBy<TicketMix> {abs(it.oneHour-it.twoHours)}.thenByDescending {it.cost})
        return (listOfNotNull(oneOnly,twoOnly.takeIf {it.twoHours>0},balanced)+
            useful.shuffled(Random(balance))).distinct()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun TicketBudgetCarousel(balance: Long?, enabled: Boolean = true) {
    val options=remember(balance){TicketBudget.combinations(balance)}
    if(options.isEmpty())return
    var index by remember(balance){mutableIntStateOf(0)}
    var paused by remember(balance){mutableStateOf(false)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val timeout=LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(5000,containsText=true,containsControls=true)?:5000
    LaunchedEffect(options,index,enabled,paused,lifecycle,timeout) {
        if(enabled && !paused && options.size>1)lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(timeout);index=(index+1)%options.size
        }
    }
    val color=MaterialTheme.colorScheme.onPrimaryContainer
    val option=options[index.coerceIn(options.indices)]
    Column(Modifier.fillMaxWidth().testTag("ticketBudget")
        .clickable(enabled=enabled&&options.size>1,onClickLabel="Ver otra combinación de boletos") {index=(index+1)%options.size}
        .semantics {if(options.size>1)customActions=listOf(CustomAccessibilityAction(if(paused)"Reanudar combinaciones" else "Pausar combinaciones"){paused=!paused;true})},
        verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("Con este saldo · tarifa común",style=MaterialTheme.typography.labelMedium,color=color.copy(alpha=0.8f))
        AnimatedContent(option,modifier=Modifier.fillMaxWidth().heightIn(min=if(LocalDensity.current.fontScale>1.2f)100.dp else 36.dp),
            transitionSpec={(slideInVertically(tween(380)){it/2}+fadeIn(tween(280))) togetherWith
                (slideOutVertically(tween(280)){-it/2}+fadeOut(tween(180)))},label="ticket combinations") {mix->
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                if(mix.oneHour>0)Text("${mix.oneHour} de 1 h",style=MaterialTheme.typography.headlineSmall,color=color)
                if(mix.oneHour>0&&mix.twoHours>0)Text("+",style=MaterialTheme.typography.headlineSmall,color=color.copy(alpha=0.5f))
                if(mix.twoHours>0)Text("${mix.twoHours} de 2 h",style=MaterialTheme.typography.headlineSmall,color=color)
            }
        }
        Text("Te ${if(balance!!-option.cost==100L)"sobra" else "sobran"} ${Amounts.format(balance-option.cost)}",style=MaterialTheme.typography.bodySmall,color=color.copy(alpha=0.8f))
        if(options.size>1)Text("${index+1} de ${options.size} · ${if(paused)"En pausa" else "Tocá para ver otra"}",style=MaterialTheme.typography.labelSmall,color=color.copy(alpha=0.65f))
    }
}
