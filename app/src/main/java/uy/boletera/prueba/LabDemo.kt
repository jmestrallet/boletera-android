package uy.boletera.prueba

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay

/**
 * Modo demo: the whole Lab journey with made-up data, to try Carga Express, the payment and the motion without money.
 * It is a separate little app built from the same screens: it has no engine, no STM page and no Prex page, so nothing
 * in it can sign in, charge or send anything. It says «Modo demo» on every screen and is left from its own sheet.
 */
internal object LabDemo {
    var on by mutableStateOf(false); private set
    /** The next payment ends rejected / asks for the check, to try those screens too. */
    var rejectNext by mutableStateOf(false)
    var askCheck by mutableStateOf(false)
    private fun prefs(context: Context) = context.getSharedPreferences("lab_demo", Context.MODE_PRIVATE)
    fun load(context: Context) { on = prefs(context).getBoolean("on", false) }
    fun set(context: Context, value: Boolean) { on = value; prefs(context).edit().putBoolean("on", value).apply() }
}

private const val DemoCard = "00001234"
private val DemoPayer = PayerProfile("demo", "Ana", "Ana", "Pérez", "", "", "")

@Composable internal fun LabDemoApp() {
    val context = LocalContext.current
    var stage by remember { mutableStateOf("welcome") }
    var balance by remember { mutableLongStateOf(124_000L) }
    var amount by remember { mutableLongStateOf(26_000L) }
    var express by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var credited by remember { mutableStateOf<Long?>(null) }
    var checked by remember { mutableStateOf(false) }
    val now = remember(stage) { System.currentTimeMillis() }
    val state = UiState(stage = if (stage == "home") "balance" else stage, cards = listOf(CardInfo(DemoCard, true, "Activa")), selectedCard = DemoCard,
        balance = balance, minimum = 26_000L, consultedAt = now, hasSavedAccess = true, credited = credited)
    fun pay(value: Long, quick: Boolean) { amount = value; express = quick; checked = false; showAmount = false; stage = "wait" }
    // The made-up timings are close to the real ones, so the waits feel like the real waits.
    LaunchedEffect(stage) {
        when (stage) {
            "connecting" -> { delay(1600); stage = "home" }
            "wait" -> { delay(1400); stage = "card" }
            "card" -> if (express) { delay(1300); stage = if (LabDemo.askCheck) "check" else "processing" }
            "processing" -> { delay(2300)
                if (LabDemo.rejectNext) { LabDemo.rejectNext = false; stage = "rejected" } else { balance += amount; credited = amount; stage = "done" } }
        }
    }
    BackHandler(enabled = stage !in setOf("welcome", "processing")) {
        when (stage) { "home" -> stage = "welcome"; "done" -> { credited = null; stage = "home" }; else -> stage = "home" }
    }
    BackHandler(enabled = showAmount) { showAmount = false }
    val haze = remember { HazeState() }
    BoleteraTheme("dark") {
        Box(Modifier.fillMaxSize().background(LabColors.Base)) {
            LabAmbient()
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (stage) {
                    "home" -> Spacer(Modifier.height(64.dp))
                    "card" -> LabTopBar(onBack = { stage = "home" }, step = 0)
                    "confirm" -> LabTopBar(onBack = { stage = "home" }, step = 1)
                    "welcome" -> LabTopBar(onSettings = { showSheet = true })
                    else -> LabTopBar()
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp)) {
                    when (stage) {
                        "welcome" -> LabWelcome(state, savedAccess = true, authenticating = false, onUnlock = { stage = "connecting" }, onManual = { stage = "connecting" },
                            onAccessGuide = {})
                        "connecting" -> LabConnecting("Conectando con STM", "Tu saldo aparece en un momento.", { stage = "welcome" },
                            last = LastBalance(balance, now - 3_600_000L))
                        "home" -> LabHomeContent(state, offline = false, onTicketGuide = { showGuide = true })
                        "wait" -> LabPaymentWait(amount, DemoCard, express, onCancel = { stage = "home" })
                        "card" -> Column(Modifier.fillMaxSize()) {
                            LabPaymentLine(amount, DemoCard)
                            LabDemoCardFields(express) { stage = if (LabDemo.askCheck) "check" else "confirm" }
                        }
                        "check" -> LabVerification(amount, DemoCard, afterPay = false, onCancel = { stage = "home" }) {
                            LabDemoCheck(checked) { checked = true }
                            LaunchedEffect(checked) { if (checked) { delay(700); LabDemo.askCheck = false; stage = if (express) "processing" else "confirm" } }
                        }
                        "confirm" -> LabConfirmPayment(amount, DemoCard, listOf("Medio de pago" to "PREX **** 8842"), "Ana Pérez", canPay = true,
                            onPay = { stage = "processing" }, onDetail = null)
                        "processing" -> LabStatusScreen(HeroPhase.LOADING, "Procesando tu pago", "Prex está confirmando el pago de ${Amounts.format(amount)}.", null, null)
                        "done" -> LabRechargeDone(state, amount, listOf("Medio de pago" to "PREX **** 8842", "Importe" to Amounts.format(amount), "Número" to "DEMO-0001")) {
                            credited = null; stage = "home" }
                        "rejected" -> LabStatusScreen(HeroPhase.ERROR, "Prex rechazó el pago", "Revisá el saldo de tu Prex o los datos de la tarjeta.",
                            LabAction("Probar de nuevo") { stage = "home"; showAmount = true }, LabAction("Volver al inicio", quiet = true) { stage = "home" })
                    }
                }
            }
            if (stage == "home") {
                LabHomeChrome(state, haze, blur = false, onChangeCard = {}, onSettings = { showSheet = true }, onCharge = { showAmount = true },
                    onExpressCharge = { pay(26_000L, quick = true) }, expressPreparing = false, expressProvider = "Prex")
                LabAmountPanel(state, open = showAmount, onClose = { showAmount = false }, payer = DemoPayer, onPayer = {}, onChoose = { pay(it, quick = false) })
            }
            // Always on screen: this is not the real thing.
            Row(Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.statusBars).padding(top = 14.dp, end = if (stage in setOf("welcome", "home")) 76.dp else 16.dp).clip(RoundedCornerShape(14.dp))
                .background(LabColors.Warn.copy(alpha = .18f)).border(1.dp, LabColors.Warn.copy(alpha = .4f), RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { showSheet = true }.padding(horizontal = 12.dp, vertical = 5.dp)
                .semantics(mergeDescendants = true) { contentDescription = "Modo demo, sin pagos reales. Tocá para opciones." }) {
                Text("Modo demo", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = LabColors.Warn)
            }
        }
        if (showGuide) LabTicketGuideSheet { showGuide = false }
        if (showSheet) LabSheet({ showSheet = false }) {
            Text("Modo demo", style = MaterialTheme.typography.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp).semantics { heading() })
            Text("Todo es de mentira: no entra a STM, no abre Prex y no carga plata. Sirve para probar Carga Express, el pago y las animaciones.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp))
            DemoSwitch("Que Prex rechace el próximo pago", LabDemo.rejectNext) { LabDemo.rejectNext = it }
            DemoSwitch("Pedir la verificación «No soy un robot»", LabDemo.askCheck) { LabDemo.askCheck = it }
            Spacer(Modifier.height(14.dp))
            LabPrimary("Salir del modo demo", onClick = { showSheet = false; LabDemo.set(context, false) }, sheet = true)
            LabTextLink("Empezar de nuevo", { showSheet = false; credited = null; balance = 124_000L; stage = "welcome" }, Modifier.align(Alignment.CenterHorizontally), quiet = true)
        }
    }
}

@Composable private fun DemoSwitch(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Switch) { onChange(!on) }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp), color = LabColors.On, modifier = Modifier.weight(1f))
        Switch(on, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = LabColors.Primary, checkedThumbColor = LabColors.OnPrimary))
    }
}

/** «Tu tarjeta» with a sample card already filled in and nothing to type: there is no place for a real card in the demo. */
@Composable private fun LabDemoCardFields(express: Boolean, onContinue: () -> Unit) {
    val view = remember { BringIntoViewRequester() }
    @Composable fun field(label: String, value: String, modifier: Modifier) =
        Column(modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, Color(0x57ECEEF6), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = LabColors.Muted)
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.5.sp), color = LabColors.On)
        }
    LabCardFields(busy = false, canContinue = true, providerError = false, express = express, waitingCheck = false, checkRequired = false,
        number = { field("Número de tarjeta", "4517 6600 1234 8842 · de muestra", it) }, expiry = { field("Vencimiento", "08/29", it) }, code = { field("Código", "•••", it) },
        verification = {}, verificationView = view, onSaved = {}, onContinue = onContinue, onOriginal = {})
}

/** A stand-in for Google's check: a box to tick, in the same light frame. */
@Composable private fun LabDemoCheck(checked: Boolean, onCheck: () -> Unit) {
    Row(Modifier.width(300.dp).height(76.dp).background(Color(0xFFF9F9F9)).border(1.dp, Color(0xFFD3D3D3)).clickable(enabled = !checked, role = Role.Checkbox, onClick = onCheck)
        .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(if (checked) Color(0xFF2FA35A) else Color.White).border(2.dp, if (checked) Color(0xFF2FA35A) else Color(0xFFC1C1C1)),
            contentAlignment = Alignment.Center) { if (checked) AppGlyph(Glyph.Check, Modifier.size(20.dp), tint = Color.White) }
        Text("No soy un robot", color = Color(0xFF222222), fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text("demo", color = Color(0xFF777777), fontSize = 10.sp)
    }
}
