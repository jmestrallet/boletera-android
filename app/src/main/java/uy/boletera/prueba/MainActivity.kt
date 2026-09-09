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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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

    @Composable private fun App() {
        val state = engine.state
        var manual by remember { mutableStateOf(false) }
        var document by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var save by remember { mutableStateOf(false) }
        var authenticating by remember { mutableStateOf(false) }
        var amountText by remember(state.selectedCard, state.minimum) { mutableStateOf("") }
        var showProbe by remember { mutableStateOf(false) }
        var showForget by remember { mutableStateOf(false) }
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
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (state.message.isNotBlank()) Notice(state.message)
                    when (state.stage) {
                        "welcome" -> {
                            Text("Tu próxima carga,\nsin las vueltas.", fontSize = 33.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("Consultá tu saldo y prepará el monto justo.", color = Muted, fontSize = 16.sp)
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
                            Text("Aplicación independiente, no oficial de STM. Esta versión consulta y prepara; todavía no cobra.", color = Muted, fontSize = 12.sp)
                        }
                        "connecting" -> {
                            Title("Entrando a STM")
                            Text("Estamos recorriendo los pasos del sitio por vos.", color = Muted)
                        }
                        "cards" -> {
                            Title("Elegí tu boletera")
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
                            if (state.cards.isEmpty()) Notice("No encontramos boleteras en esta pantalla. Volvé a consultar.")
                        }
                        "balance" -> {
                            Text("TU BOLETERA ${state.selectedCard?.takeLast(4) ?: ""}", color = Muted, fontSize = 12.sp, letterSpacing = 1.sp)
                            Surface(color = Ink, shape = RoundedCornerShape(26.dp)) {
                                Column(Modifier.fillMaxWidth().padding(25.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text("Saldo informado por STM", color = Color(0xFFBEC8CE), fontSize = 14.sp)
                                    Text(Amounts.format(state.balance), color = Lime, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                    if ((state.balance ?: 0) < 0) Text("Tenés ${Amounts.format(-(state.balance ?: 0))} de deuda.", color = Color.White)
                                    state.consultedAt?.let { Text("Consultado ${SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-UY")).format(Date(it))}", color = Color(0xFFBEC8CE), fontSize = 12.sp) }
                                }
                            }
                            Text("Puede haber viajes de las últimas 72 horas todavía sin descontar.", color = Muted, fontSize = 12.sp)
                            WhiteCard {
                                Text("¿Cuánto querés cargar?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                Text("Mínimo informado: ${Amounts.format(state.minimum)}", color = Muted)
                                Primary("Elegir mínimo · ${Amounts.format(state.minimum)}", enabled = !state.busy && state.minimum != null) {
                                    state.minimum?.let { engine.prepare(it) }
                                }
                                OutlinedTextField(amountText, { amountText = it }, label = { Text("Otro monto en pesos") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                                val parsed = Amounts.parse(amountText)
                                val valid = Amounts.valid(parsed, state.minimum)
                                if (valid && state.balance != null) Text("Te quedarían ${Amounts.format(state.balance + parsed!!)} antes de viajes pendientes.", color = Muted, fontSize = 13.sp)
                                if (amountText.isNotEmpty() && !valid) Text("Ingresá un monto igual o mayor al mínimo, con hasta 2 decimales.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                OutlinedButton(onClick = { parsed?.let { engine.prepare(it) } }, enabled = valid && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Elegir este monto") }
                            }
                            TextButton(onClick = { engine.refresh() }, enabled = !state.busy) { Text("Actualizar saldo y mínimo") }
                        }
                        "captcha" -> {
                            Title("Una verificación")
                            Text("Este es el desafío original del sitio. Completalo para seguir.", color = Muted)
                        }
                        "paymentBoundary" -> {
                            Title("Monto preparado")
                            WhiteCard {
                                Text(Amounts.format(state.amount), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                                Text("Sin pagar · sin solicitud de cobro", color = Muted)
                                Text("El pago con diseño propio y tarjeta guardada todavía necesita validación. No vamos a sustituirlo por la página completa.")
                            }
                            OutlinedButton(onClick = { showProbe = true }, modifier = Modifier.fillMaxWidth()) { Text("Probar autocompletado de Android") }
                            Primary("Volver al saldo") { engine.refresh() }
                        }
                        "blocked" -> {
                            Title("Nos detenemos acá")
                            Text("No se completó ninguna carga. Podés volver a intentar el acceso sin que se repita un pago.", color = Muted)
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
                }
            }
        }
        if (showProbe) AutofillProbe { showProbe = false }
        if (showForget) AlertDialog(onDismissRequest = { showForget = false }, title = { Text("¿Olvidar este acceso?") },
            text = { Text("Se eliminan las credenciales cifradas y la sesión local. Para volver a entrar tendrás que escribirlas.") },
            confirmButton = { TextButton(onClick = { vault.forget(); engine.savedAccess(false); engine.logout(); engine.notice("Acceso guardado y sesión local eliminados."); showForget = false }) { Text("Olvidar") } },
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
