package uy.boletera.prueba

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF101D2A)
private val Lime = Color(0xFFB9F375)
private val Paper = Color(0xFFF4F5EF)
private val Muted = Color(0xFF64716E)

class MainActivity : FragmentActivity() {
    private lateinit var engine: StmEngine
    private lateinit var vault: AccessVault
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        // Screenshots are enabled at the owner's request to report problems in this prototype.
        engine = StmEngine(this)
        vault = AccessVault(this)
        engine.savedAccess(vault.exists)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = Color.White,
                secondary = Lime, background = Paper, surface = Color.White, onSurface = Ink)) {
                App()
            }
        }
    }
    override fun onStop() { super.onStop(); if (::engine.isInitialized) engine.pause() }
    override fun onStart() { super.onStart(); if (::engine.isInitialized) engine.resume() }
    override fun onDestroy() { if (::vault.isInitialized) vault.cancel(); if (::engine.isInitialized) engine.destroy(); super.onDestroy() }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun App() {
        val state = engine.state
        val pullState = rememberPullToRefreshState()
        if (state.stage == "embeddedPrex") {
            EmbeddedPrexScreen(engine.prexPayment, state.pendingPayment, engine::leavePrexPayment)
            return
        }
        var manual by remember { mutableStateOf(false) }
        var document by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var save by remember { mutableStateOf(false) }
        var authenticating by remember { mutableStateOf(false) }
        var amountText by remember(state.selectedCard, state.minimum) { mutableStateOf("") }
        var showProbe by remember { mutableStateOf(false) }
        var showForget by remember { mutableStateOf(false) }
        var changeProvider by remember(state.stage) { mutableStateOf(false) }
        var reviewPayment by remember { mutableStateOf(false) }
        var reopenPrex by remember { mutableStateOf(false) }
        var showPayerPicker by remember { mutableStateOf(false) }
        var showPayerEditor by remember { mutableStateOf(false) }
        var editingPayer by remember { mutableStateOf<PayerProfile?>(null) }
        BackHandler(enabled = state.stage != "welcome" || showProbe) {
            showProbe = false; document = ""; password = ""; engine.cancel()
        }
        Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("boletera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 25.sp)
                    Spacer(Modifier.weight(1f))
                    Text(BuildConfig.VERSION_NAME, color = Lime, fontSize = 11.sp, letterSpacing = 1.sp)
                }
                Box(Modifier.weight(1f).fillMaxWidth().pullToRefresh(
                    isRefreshing = state.stage == "balance" && state.busy,
                    state = pullState,
                    enabled = state.stage == "balance" && !state.busy,
                    onRefresh = { if (engine.state.stage == "balance" && !engine.state.busy) engine.refresh() }
                )) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (state.message.isNotBlank()) Notice(state.message)
                    when (state.stage) {
                        "welcome" -> {
                            Text("Tu próxima carga,\nsin las vueltas.", fontSize = 33.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Tu saldo, tu boletera y tu medio habitual.", color = Muted, fontSize = 16.sp)
                            WhiteCard {
                                Text("Entrar a tu STM", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                if (state.hasSavedAccess && !manual) {
                                    Text("Tu acceso está cifrado en este celular.", color = Muted)
                                    Primary("Entrar con huella", enabled = !authenticating && !state.busy) {
                                        authenticating = true
                                        vault.unlock { doc, pass ->
                                            authenticating = false
                                            if (doc != null && pass != null) engine.connect(doc, pass)
                                            else engine.notice("No se desbloqueó el acceso. Si cambiaste las huellas del celular, olvidá el acceso y guardalo de nuevo.")
                                        }
                                    }
                                    TextButton(onClick = { manual = true }) { Text("Ingresar manualmente") }
                                } else {
                                    OutlinedTextField(document, { document = it.filter(Char::isDigit).take(8) },
                                        label = { Text("Documento uruguayo") }, singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(password, { password = it }, label = { Text("Contraseña de gub.uy") }, singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(save, { save = it }, enabled = vault.available)
                                        Text("Guardar acceso con huella", fontSize = 14.sp)
                                    }
                                    Text(if (vault.available) "Solo se guarda si activás esta opción. Android exige tu biometría para descifrarlo."
                                        else "Este celular no tiene biometría compatible configurada. Podés ingresar sin guardar el acceso.", color = Muted, fontSize = 12.sp)
                                    Primary(if (save) "Guardar e ingresar" else "Ingresar", enabled = document.length == 8 && password.isNotBlank() && !authenticating && !state.busy) {
                                        val doc = document; val pass = password
                                        document = ""; password = ""
                                        if (save) {
                                            authenticating = true
                                            vault.save(doc, pass) { ok ->
                                                authenticating = false; engine.savedAccess(vault.exists)
                                                if (ok) engine.connect(doc, pass)
                                                else engine.notice("No se guardó el acceso. Podés intentar de nuevo o ingresar sin guardarlo.")
                                            }
                                        } else engine.connect(doc, pass)
                                    }
                                }
                                Text("Por ahora: Usuario gub.uy. Otros métodos quedan para una próxima prueba.", color = Muted, fontSize = 12.sp)
                            }
                            Text("Aplicación independiente, no oficial de STM. Prex y eBROU autorizan el pago en su pantalla oficial de Chrome.", color = Muted, fontSize = 12.sp)
                        }
                        "connecting" -> {
                            Title("Entrando a STM")
                            Text("Estamos recorriendo los pasos del sitio por vos.", color = Muted)
                        }
                        "openingPayment" -> {
                            Title("Abriendo el pago")
                            Text("Estamos preparando ${Amounts.format(state.amount)} con ${if (state.selectedProvider == "1033") "Prex" else "eBROU"}.", color = Muted)
                        }
                        "cards" -> {
                            Title("Elegí tu boletera")
                            Text("Vamos a recordar tu elección para las próximas cargas.", color = Muted, fontSize = 14.sp)
                            state.cards.forEach { card ->
                                WhiteCard {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("STM · ${card.id.takeLast(4)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text(card.status, color = if (card.active) Color(0xFF386B20) else Muted, fontSize = 13.sp)
                                    }
                                    if (card.active) Primary("Usar esta boletera", enabled = !state.busy) { engine.chooseCard(card.id) }
                                    else Text("No está habilitada para esta prueba.", color = Muted)
                                }
                            }
                            if (state.cards.isEmpty()) Notice("Todavía no pudimos leer las boleteras.")
                        }
                        "balance" -> {
                            Text("TU BOLETERA ${state.selectedCard?.takeLast(4) ?: ""}", color = Muted, fontSize = 12.sp, letterSpacing = 1.sp)
                            TextButton(onClick = { engine.changeCard() }, enabled = !state.busy) { Text("Cambiar boletera") }
                            Surface(color = Ink, shape = RoundedCornerShape(26.dp)) {
                                Column(Modifier.fillMaxWidth().padding(25.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text("Saldo informado por STM", color = Color(0xFFBEC8CE), fontSize = 14.sp)
                                    Text(Amounts.format(state.balance), color = Lime, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                    if ((state.balance ?: 0) < 0) Text("Tenés ${Amounts.format(-(state.balance ?: 0))} de deuda.", color = Color.White)
                                    state.consultedAt?.let { Text("Consultado ${SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-UY")).format(Date(it))}", color = Color(0xFFBEC8CE), fontSize = 12.sp) }
                                }
                            }
                            Text("Puede haber viajes de las últimas 72 horas todavía sin descontar.", color = Muted, fontSize = 12.sp)
                            if (state.pendingPayment != null) {
                                val pending = state.pendingPayment
                                Notice("Hay un pago por revisar: ${Amounts.format(pending.amount)} con ${if (pending.provider == "1033") "Prex" else "eBROU"}, para la boletera ${pending.card.takeLast(4)}. Revisá su resultado antes de iniciar otra carga.")
                                if (state.canReopenPrex) OutlinedButton(onClick = { reopenPrex = true }, modifier = Modifier.fillMaxWidth()) { Text("Volver al pago de Prex") }
                                OutlinedButton(onClick = { reviewPayment = true }, modifier = Modifier.fillMaxWidth()) { Text("Ya revisé el pago anterior") }
                            }
                            WhiteCard {
                                Text("¿Cuánto querés cargar?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                Text("Mínimo informado: ${Amounts.format(state.minimum)}", color = Muted)
                                Primary("Elegir mínimo · ${Amounts.format(state.minimum)}", enabled = !state.busy && state.minimum != null && state.pendingPayment == null) {
                                    state.minimum?.let { engine.prepare(it) }
                                }
                                OutlinedTextField(amountText, { amountText = it }, label = { Text("Otro monto en pesos") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                                val parsed = Amounts.parse(amountText)
                                val valid = Amounts.valid(parsed, state.minimum)
                                if (valid && state.balance != null) Text("Te quedarían ${Amounts.format(state.balance + parsed!!)} antes de viajes pendientes.", color = Muted, fontSize = 13.sp)
                                if (amountText.isNotEmpty() && !valid) Text("Ingresá un monto igual o mayor al mínimo, con hasta 2 decimales.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                OutlinedButton(onClick = { parsed?.let { engine.prepare(it) } }, enabled = valid && !state.busy && state.pendingPayment == null, modifier = Modifier.fillMaxWidth()) { Text("Elegir este monto") }
                            }
                        }
                        "captcha" -> {
                            Title("Una verificación")
                            Text("Este es el desafío original del sitio. Completalo para seguir.", color = Muted)
                        }
                        "paymentBoundary" -> {
                            Title("Tu recarga")
                            WhiteCard {
                                Text(Amounts.format(state.amount), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                                Text("Boletera · ${state.selectedCard?.takeLast(4) ?: ""}", color = Muted)
                                val preferred = state.providers.find { it.id == state.selectedProvider }
                                if (preferred != null && !changeProvider) {
                                    Text("Tu medio habitual: ${preferred.name}", fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { changeProvider = true }) { Text("Cambiar medio de pago") }
                                } else {
                                    Text("¿Con qué querés pagar?", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Recordaremos tu elección en este celular. Podés cambiarla cuando quieras.", color = Muted, fontSize = 13.sp)
                                    state.providers.filter { it.id in PaymentPolicy.supported }.sortedBy { if (it.id == "1033") 0 else 1 }.forEach { provider ->
                                        OutlinedButton(onClick = { engine.chooseProvider(provider.id); changeProvider = false }, modifier = Modifier.fillMaxWidth()) {
                                            Text(if (provider.id == "1002") "eBROU" else provider.name)
                                        }
                                    }
                                    if (state.providers.isEmpty()) Text("No pudimos leer los medios disponibles de STM. Volvé al saldo para consultar de nuevo.", color = Muted)
                                }
                            }
                            if (state.selectedProvider == "1033") {
                                val payer = engine.payerProfiles.find { it.id == state.payerProfileId }
                                WhiteCard {
                                    Text("Datos para esta Prex", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                    if (payer == null) {
                                        Text("Guardalos una vez. Si usás otra Prex, elegí un perfil distinto.", color = Muted)
                                        Primary("Elegir titular") { showPayerPicker = true }
                                    } else {
                                        Text(payer.label, fontWeight = FontWeight.Bold)
                                        Text("${payer.givenName} ${payer.familyName}", color = Muted)
                                        TextButton(onClick = { showPayerPicker = true }) { Text("Elegir otra Prex o editar datos") }
                                    }
                                }
                            }
                            Text(if (state.selectedProvider == "1033") "Seguís con Prex dentro de Boletera. La verificación y autorización corresponden a Sistarbanc." else "La autorización de eBROU se abre en Chrome.", color = Muted, fontSize = 14.sp)
                            Primary("Pagar ${Amounts.format(state.amount)}", enabled = !state.busy && state.selectedProvider in PaymentPolicy.supported && state.pendingPayment == null &&
                                (state.selectedProvider != "1033" || engine.payerProfiles.any { it.id == state.payerProfileId })) { engine.beginPayment() }
                            Primary("Volver al saldo") { engine.refresh() }
                        }
                        "paymentReview" -> {
                            Title("Tu pago en ${if (state.pendingPayment?.provider == "1033") "Prex" else "eBROU"}")
                            Text("La autorización y el resultado se muestran en la pantalla del proveedor. Salir de esa pantalla no confirma ni cancela un pago autorizado.", color = Muted)
                            Text(Amounts.format(state.pendingPayment?.amount), fontSize = 38.sp, fontWeight = FontWeight.Bold)
                            Text("Podés consultar el saldo sin volver a enviar la recarga.", color = Muted)
                            if (state.canReopenPrex) Primary("Volver al pago de Prex") { reopenPrex = true }
                            Primary("Consultar saldo") { engine.refresh() }
                            OutlinedButton(onClick = { reviewPayment = true }, modifier = Modifier.fillMaxWidth()) { Text("Ya revisé el resultado") }
                        }
                        "blocked" -> {
                            Title("Nos detenemos acá")
                            Text(if (state.pendingPayment == null) "No se inició un pago. Podés volver a intentar el acceso." else "Hay una solicitud por revisar. Comprobá su resultado en el proveedor antes de iniciar otra carga.", color = Muted)
                            if (state.pendingPayment != null) OutlinedButton(onClick = { reviewPayment = true }) { Text("Ya revisé el resultado") }
                            if (state.canReopenPrex) OutlinedButton(onClick = { reopenPrex = true }) { Text("Volver al pago de Prex") }
                            Primary("Volver al inicio") { engine.cancel() }
                        }
                    }
                    // Keep a single WebView attached. Only the original CAPTCHA's rectangle becomes visible.
                    val cap = state.captcha
                    val screenHeight = LocalConfiguration.current.screenHeightDp
                    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val captchaScale = if (cap == null) 1f else minOf(1f, maxWidth.value / cap.width,
                            (screenHeight - 260).coerceAtLeast(180).toFloat() / cap.height)
                        AndroidView(factory = { engine.host },
                            modifier = if (cap == null) Modifier.size(1.dp) else Modifier
                                .width((cap.width * captchaScale).dp).height((cap.height * captchaScale).dp).clip(RoundedCornerShape(4.dp)),
                            update = { it.crop = cap })
                    }
                    if (cap != null) {
                        Primary("Ya completé la verificación") { engine.continueCaptcha() }
                        Text("Si no entra el desafío completo o no responde, cancelá. Esta función está en prueba.", color = Muted, fontSize = 12.sp)
                    }
                    if (state.busy || authenticating) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(if (authenticating) "Esperando tu huella…" else "Consultando el sitio…", color = Muted)
                    }
                    if (state.stage != "welcome") TextButton(onClick = { engine.logout() }) { Text("Cerrar sesión local") }
                    if (state.hasSavedAccess) TextButton(onClick = { showForget = true }) { Text("Olvidar acceso guardado") }
                    if (state.diagnostic.isNotBlank()) Text("Referencia: ${state.diagnostic}", color = Muted, fontSize = 11.sp)
                }
                if (state.stage == "balance") PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = state.busy,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Lime,
                    color = Ink
                )
                }
            }
        }
        if (showProbe) AutofillProbe { showProbe = false }
        if (showPayerPicker) AlertDialog(onDismissRequest = { showPayerPicker = false }, title = { Text("¿Qué Prex vas a usar?") },
            text = {
                Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cada perfil conserva los datos de su titular. El número y el código de la tarjeta no se guardan acá.")
                    engine.payerProfiles.forEach { payer ->
                        OutlinedButton(onClick = { engine.choosePayer(payer.id); showPayerPicker = false }, modifier = Modifier.fillMaxWidth()) { Text(payer.label) }
                        TextButton(onClick = { editingPayer = payer; showPayerPicker = false; showPayerEditor = true }) { Text("Editar ${payer.label}") }
                    }
                }
            }, confirmButton = { TextButton(onClick = { editingPayer = null; showPayerPicker = false; showPayerEditor = true }) { Text("Agregar otra Prex") } },
            dismissButton = { TextButton(onClick = { showPayerPicker = false }) { Text("Volver") } })
        if (showPayerEditor) PayerProfileEditor(editingPayer, onSave = engine::savePayer, onDelete = engine::deletePayer, onClose = { showPayerEditor = false })
        if (reopenPrex) AlertDialog(onDismissRequest = { reopenPrex = false }, title = { Text("Volver a la solicitud anterior") },
            text = { Text("Se abrirá el mismo enlace de Prex. Si ya autorizaste el pago, revisá su resultado y no vuelvas a autorizarlo. Si el enlace venció, comprobá el estado del pago antes de iniciar otra carga.") },
            confirmButton = { TextButton(onClick = { reopenPrex = false; engine.reopenPrexPayment() }) { Text("Abrir Prex") } },
            dismissButton = { TextButton(onClick = { reopenPrex = false }) { Text("Volver") } })
        if (reviewPayment) AlertDialog(onDismissRequest = { reviewPayment = false }, title = { Text("Antes de otra recarga") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Si autorizaste el pago, verificá su resultado en Prex o eBROU. Si sigue pendiente o no sabés cómo terminó, no lo repitas.")
                    OutlinedButton(onClick = { reviewPayment = false; engine.acknowledgePayment() }, modifier = Modifier.fillMaxWidth()) { Text("Salí sin autorizar el pago") }
                }
            },
            confirmButton = { TextButton(onClick = { reviewPayment = false; engine.acknowledgePayment() }) { Text("Ya verifiqué que terminó") } },
            dismissButton = { TextButton(onClick = { reviewPayment = false }) { Text("Volver") } })
        if (showForget) AlertDialog(onDismissRequest = { showForget = false }, title = { Text("¿Olvidar este acceso?") },
            text = { Text("Se eliminan las credenciales cifradas, las preferencias y la sesión local. Esto no cancela un pago en curso; su aviso se conserva para cuando vuelvas a ingresar.") },
            confirmButton = { TextButton(onClick = {
                if (engine.forgetChoices()) { vault.forget(); engine.savedAccess(false); engine.logout(); engine.notice("Acceso guardado, perfiles, preferencias y sesión local eliminados.") }
                showForget = false
            }) { Text("Olvidar") } },
            dismissButton = { TextButton(onClick = { showForget = false }) { Text("Cancelar") } })
    }
}

@Composable private fun Title(text: String) { Text(text, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = Ink) }
@Composable private fun Notice(text: String) {
    Surface(color = Color(0xFFE7ECDD), shape = RoundedCornerShape(14.dp)) {
        Text(text, Modifier.padding(16.dp), fontSize = 14.sp, color = Ink)
    }
}
@Composable private fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}
@Composable private fun Primary(label: String, enabled: Boolean = true, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable internal fun EmbeddedPrexScreen(payment: EmbeddedPrexPayment, pending: PendingPayment?, onClose: () -> Unit) {
    var showOriginal by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(payment.nativeStage) { showOriginal = false }
    val native = payment.nativeStage in listOf("summary", "payer", "loading") && !showOriginal
    LaunchedEffect(native, payment.nativeStage, payment.expandedChallenge, payment.challenge != null) {
        if (native && payment.nativeStage == "payer") payment.positionVerification() else payment.restoreVerification()
    }
    BackHandler(onBack = onClose)
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("Volver", color = Lime) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Pago con Prex", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Recarga · ${Amounts.format(pending?.amount)}", color = Color(0xFFBEC8CE), fontSize = 13.sp)
                }
                Text("boletera", color = Lime, fontWeight = FontWeight.Bold)
            }
            Text("Pago seguro · Sistarbanc", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            if (payment.payerNotice.isNotBlank()) {
                Text(payment.payerNotice, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                if (payment.payerConflict) TextButton(onClick = payment::applyChosenPayer, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Usar los datos del perfil elegido") }
            }
            if (payment.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (payment.message.isNotBlank()) {
                Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Notice(payment.message)
                    Text("Volver conserva la solicitud pendiente. No se vuelve a enviar el pago.", color = Muted)
                    Primary("Volver a Boletera", action = onClose)
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val browserWidth = constraints.maxWidth
                    val browserHeight = constraints.maxHeight
                    val pixelsPerDp = LocalDensity.current.density
                    val cssPixelsToDp = if (payment.cssViewportWidth > 0f) browserWidth / payment.cssViewportWidth / pixelsPerDp else 1f
                    if (!native) PaymentBrowserView(payment, false, null, browserWidth, browserHeight, Modifier.fillMaxSize())
                    if (native) Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        if (payment.nativeStage != "payer") PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.fillMaxWidth().height(1.dp))
                        Text(if (payment.nativeStage == "payer") "02 / TUS DATOS" else "01 / TU RECARGA", color = Muted, fontSize = 12.sp)
                        Text(if (payment.nativeStage == "payer") "Datos del titular" else "Revisá tu recarga", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                        if (payment.nativeStage == "loading") {
                            CircularProgressIndicator(color = Ink)
                            Text("Estamos recuperando tu solicitud…", color = Muted)
                        } else if (payment.nativeStage == "summary") {
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(payment.summaryRows.singleOrNull { it.first.trim().equals("Total:", ignoreCase = true) }?.second ?: Amounts.format(pending?.amount), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    payment.summaryRows.filterNot { it.first.trim().equals("Total:", ignoreCase = true) }.forEach { (label, value) ->
                                        Column { Text(label, color = Muted, fontSize = 12.sp); Text(value, color = Ink, fontSize = 16.sp) }
                                    }
                                }
                            }
                            Primary("Continuar", payment.canContinue, payment::advance)
                        } else {
                            if (!payment.expandedChallenge) Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val values = payment.payerValues
                                    val rows = if (values.size == 5) listOf("Titular" to "${values[0]} ${values[1]}", "Documento" to values[2], "Correo" to values[3], "Celular" to values[4]) else emptyList()
                                    rows.forEach { (label, value) ->
                                        Column { Text(label, color = Muted, fontSize = 12.sp); Text(value.ifBlank { "Sin completar" }, color = Ink, fontSize = 16.sp) }
                                    }
                                }
                            }
                            Text("Sistarbanc necesita verificar que sos vos antes de pasar a la tarjeta.", color = Muted)
                            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                val cap = payment.challenge
                                val maxPanelHeight = (browserHeight / pixelsPerDp - 140f).coerceAtLeast(140f)
                                val scale = if (cap == null) 1f else minOf(cssPixelsToDp, maxWidth.value / cap.width, maxPanelHeight / cap.height)
                                PaymentBrowserView(payment, cap == null, cap, browserWidth, browserHeight, if (cap == null) Modifier.size(1.dp) else Modifier.width((cap.width * scale).dp).height((cap.height * scale).dp))
                            }
                            Primary("Continuar a la tarjeta", payment.canContinue, payment::advance)
                            TextButton(onClick = { editing = true }, enabled = payment.chosenPayer != null) { Text("Editar datos para este pago") }
                        }
                        TextButton(onClick = { showOriginal = true }) { Text("Ver pantalla de Sistarbanc") }
                    }
                    if (!native && payment.nativeStage in listOf("summary", "payer")) TextButton(onClick = { showOriginal = false }, modifier = Modifier.align(Alignment.TopEnd).background(Paper)) { Text("Volver a mis datos") }
                }
            }
        }
    }
    if (editing) payment.chosenPayer?.let { profile ->
        val values = payment.payerValues
        val current = if (values.size == 5) profile.copy(givenName = values[0], familyName = values[1], document = values[2], email = values[3], phone = values[4]) else profile
        PayerProfileEditor(current, onSave = payment::editPayer, onDelete = { false }, onClose = { editing = false }, paymentOnly = true)
    }
}

/** Keep the real page laid out while native content owns display and accessibility. */
@Composable internal fun PaymentBrowserView(payment: EmbeddedPrexPayment, hidden: Boolean, crop: CaptchaRect?, browserWidth: Int, browserHeight: Int, modifier: Modifier) {
    AndroidView(factory = {
        (payment.web.parent as? android.view.ViewGroup)?.removeView(payment.web)
        PaymentPageHost(it, payment.web)
    }, update = {
        it.viewportWidth = browserWidth; it.viewportHeight = browserHeight
        it.cssViewportWidth = payment.cssViewportWidth
        it.nativeHidden = hidden; it.crop = crop
    }, modifier = modifier.clip(RoundedCornerShape(4.dp)))
}

internal class PaymentPageHost(context: android.content.Context, private val browser: android.webkit.WebView) : android.view.ViewGroup(context) {
    var viewportWidth = 1
        set(value) { if (field != value) { field = value.coerceAtLeast(1); requestLayout() } }
    var viewportHeight = 1
        set(value) { if (field != value) { field = value.coerceAtLeast(1); requestLayout() } }
    var cssViewportWidth = 0f
        set(value) { if (field != value) { field = value; requestLayout() } }
    var crop: CaptchaRect? = null
        set(value) { if (field != value) { field = value; requestLayout() } }
    var nativeHidden: Boolean = false
        set(value) {
            field = value
            browser.alpha = if (value) 0f else 1f
            browser.importantForAccessibility = if (value) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            browser.isEnabled = !value
            requestLayout()
        }
    init { clipChildren = true; clipToPadding = true; addView(browser) }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        browser.measure(MeasureSpec.makeMeasureSpec(viewportWidth, MeasureSpec.EXACTLY),
            if (crop != null || nativeHidden) MeasureSpec.makeMeasureSpec(viewportHeight, MeasureSpec.EXACTLY) else heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val rect = crop
        val density = if (cssViewportWidth > 0f) browser.measuredWidth / cssViewportWidth else resources.displayMetrics.density
        val scale = if (rect == null) 1f else minOf(1f, width / (rect.width * density), height / (rect.height * density))
        browser.pivotX = 0f; browser.pivotY = 0f; browser.scaleX = scale; browser.scaleY = scale
        val x = ((rect?.x ?: 0f) * density * scale).toInt()
        val y = ((rect?.y ?: 0f) * density * scale).toInt()
        browser.layout(-x, -y, browser.measuredWidth-x, browser.measuredHeight-y)
    }
}

@Composable private fun PayerProfileEditor(existing: PayerProfile?, onSave: (PayerProfile) -> Boolean, onDelete: (String) -> Boolean, onClose: () -> Unit, paymentOnly: Boolean = false) {
    val id = remember(existing?.id) { existing?.id ?: java.util.UUID.randomUUID().toString() }
    var label by remember(id) { mutableStateOf(existing?.label.orEmpty()) }
    var given by remember(id) { mutableStateOf(existing?.givenName.orEmpty()) }
    var family by remember(id) { mutableStateOf(existing?.familyName.orEmpty()) }
    var document by remember(id) { mutableStateOf(existing?.document.orEmpty()) }
    var documentType by remember(id) { mutableStateOf(existing?.documentType ?: "CI") }
    var email by remember(id) { mutableStateOf(existing?.email.orEmpty()) }
    var phone by remember(id) { mutableStateOf(existing?.phone.orEmpty()) }
    var error by remember(id) { mutableStateOf("") }
    val profile = PayerProfile(id, label.trim(), given.trim(), family.trim(), document.trim(), email.trim(), phone, documentType)
    AlertDialog(onDismissRequest = onClose, title = { Text(if (existing == null) "Nueva Prex" else "Datos del titular") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Estos datos se guardan cifrados en este celular. No incluyen el número ni el código de la tarjeta.", color = Muted)
            OutlinedTextField(label, { label = it.take(80) }, label = { Text("Nombre del perfil · por ejemplo, Mi Prex") }, singleLine = true)
            OutlinedTextField(given, { given = it.take(80) }, label = { Text("Nombre del titular") }, singleLine = true)
            OutlinedTextField(family, { family = it.take(80) }, label = { Text("Apellido del titular") }, singleLine = true)
            Text("Tipo de documento")
            listOf("CI" to "Cédula uruguaya", "PAS" to "Pasaporte").forEach { (type, title) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = documentType == type, onClick = { documentType = type })
                    TextButton(onClick = { documentType = type }) { Text(title) }
                }
            }
            OutlinedTextField(document, { document = if (documentType == "CI") it.filter(Char::isDigit).take(8) else it.take(40) }, label = { Text(if (documentType == "CI") "Cédula uruguaya, sin puntos ni guion" else "Número de pasaporte") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (documentType == "CI") KeyboardType.Number else KeyboardType.Text))
            OutlinedTextField(email, { email = it.take(120) }, label = { Text("Correo electrónico") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(15) }, label = { Text("Celular, con código de país si corresponde") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if (existing != null && !paymentOnly) TextButton(onClick = {
                if (onDelete(existing.id)) onClose() else error = "No se pudo borrar. Revisá si este perfil tiene un pago pendiente."
            }) { Text("Eliminar este perfil") }
        }
    }, confirmButton = { TextButton(enabled = profile.valid(), onClick = {
        if (onSave(profile)) onClose() else error = "No se pudieron guardar los datos. Revisá si hay un pago pendiente."
    }) { Text(if (paymentOnly) "Usar en este pago" else "Guardar y usar") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } })
}

@Composable private fun AutofillProbe(onClose: () -> Unit) {
    // Native fields ONLY. No readback to Kotlin/JS, no submit, no disk storage, no network.
    var fields by remember { mutableStateOf<LinearLayout?>(null) }
    fun clear() {
        fields?.let { group ->
            group.context.getSystemService(android.view.autofill.AutofillManager::class.java)?.cancel()
            (0 until group.childCount).forEach { (group.getChildAt(it) as? EditText)?.text?.clear() }
        }
    }
    DisposableEffect(Unit) { onDispose { clear() } }
    AlertDialog(onDismissRequest = { clear(); onClose() }, title = { Text("Prueba local de autocompletado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tocá el campo para ver si Android ofrece tu tarjeta y pide la huella. Esto no paga, no se envía y no se guarda. No demuestra compatibilidad con Sistarbanc.")
                AndroidView(factory = { context ->
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        fun field(label: String, hint: String, type: Int) {
                            addView(EditText(context).apply {
                                this.hint = label; inputType = type
                                setAutofillHints(hint); importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                                isSaveEnabled = false
                                layoutParams = LinearLayout.LayoutParams(-1, -2)
                            })
                        }
                        field("Número de tarjeta", View.AUTOFILL_HINT_CREDIT_CARD_NUMBER, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                        field("Vencimiento", View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE, InputType.TYPE_CLASS_DATETIME)
                        fields = this
                    }
                })
            }
        }, confirmButton = { TextButton(onClick = { clear(); onClose() }) { Text("Borrar y cerrar") } })
}
