package uy.boletera.prueba

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.withResumed
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private lateinit var engine: StmEngine
    private lateinit var vault: AccessVault
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Screenshots remain available for the owner's prototype feedback.
        engine = StmEngine(this)
        vault = AccessVault(this)
        engine.savedAccess(vault.exists)
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            var appearance by remember { mutableStateOf(preferences.getString("theme", "system") ?: "system") }
            BoleteraTheme(appearance) {
                App(appearance) { appearance = it; preferences.edit().putString("theme", it).apply() }
            }
        }
    }
    override fun onStop() { super.onStop(); if (::engine.isInitialized) engine.pause() }
    override fun onStart() { super.onStart(); if (::engine.isInitialized) engine.resume() }
    override fun onDestroy() { if (::vault.isInitialized) vault.cancel(); if (::engine.isInitialized) engine.destroy(); super.onDestroy() }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun App(appearance: String, onAppearance: (String) -> Unit) {
        val state = engine.state
        val updates: AppUpdates = viewModel()
        DisposableEffect(updates) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) updates.onForeground(this@MainActivity)
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(updates.installRequested, updates.busy) {
            if (updates.installRequested && !updates.busy) lifecycle.withResumed { updates.install(this@MainActivity) }
        }
        var automaticUnlockHandled by rememberSaveable { mutableIntStateOf(-1) }
        var authenticating by remember { mutableStateOf(false) }
        fun unlockSavedAccess() {
            if (authenticating || engine.state.busy || engine.state.stage != "welcome") return
            val accessId=engine.state.accessRequestId
            authenticating = true
            vault.unlock { doc, pass ->
                authenticating = false
                if (!isDestroyed && !isFinishing && engine.state.stage=="welcome" && engine.state.accessRequestId==accessId) {
                    if (doc != null && pass != null) engine.connect(doc, pass)
                    else engine.notice("No se desbloqueó el acceso. Podés volver a intentar o ingresar manualmente.")
                }
            }
        }
        LaunchedEffect(state.accessRequestId) {
            lifecycle.withResumed {
                val current=engine.state
                if (automaticUnlockHandled != current.accessRequestId) {
                    automaticUnlockHandled = current.accessRequestId
                    if (vault.exists && current.stage == "welcome") unlockSavedAccess()
                }
            }
        }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var showTicketGuide by rememberSaveable { mutableStateOf(false) }
        var showExpressHelp by rememberSaveable { mutableStateOf(false) }
        val helpPreferences=remember { getSharedPreferences("feature_help",MODE_PRIVATE) }
        fun requestExpress() {
            if(!engine.expressAvailable || state.busy)return
            if(helpPreferences.getBoolean("express_skip_intro_v1",false))engine.startExpress()
            else showExpressHelp=true
        }
        if (state.stage == "embeddedPrex") {
            EmbeddedPrexScreen(engine.prexPayment, state.activePayment, engine::leavePrexPayment)
            return
        }
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(state.message, state.stage) {
            if (state.message.isNotBlank() && state.stage != "blocked") {
                snackbar.showSnackbar(state.message, withDismissAction = true, duration = SnackbarDuration.Short)
                engine.dismissNotice(state.message)
            }
        }
        var manual by remember { mutableStateOf(false) }
        var document by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var revealPassword by remember { mutableStateOf(false) }
        var save by remember { mutableStateOf(false) }
        var showForget by remember { mutableStateOf(false) }
        var showAmount by remember { mutableStateOf(false) }
        var showPayerPicker by remember { mutableStateOf(false) }
        var showPayerEditor by remember { mutableStateOf(false) }
        var editingPayer by remember { mutableStateOf<PayerProfile?>(null) }
        LaunchedEffect(state.sessionExpired, state.recoveringSession) {
            if(state.sessionExpired || state.recoveringSession) {
                showAmount=false; showPayerPicker=false; showPayerEditor=false; showExpressHelp=false
                showSettings=false; document=""; password=""; revealPassword=false; manual=false
            }
        }
        val stage = if(state.stage=="connecting" && state.selectedCard!=null && state.balance!=null && state.amount==null) "balance" else state.stage
        val scroll = rememberScrollState()
        val pullState = rememberPullToRefreshState()
        LaunchedEffect(stage) { scroll.scrollTo(0);if(stage!="balance")showExpressHelp=false }
        fun back() {
            document=""; password=""
            if(engine.returnFromCardPicker())return
            if(state.stage in listOf("paymentBoundary", "externalPayment")) engine.refresh() else engine.cancel()
        }
        BackHandler(enabled = state.stage != "welcome") { back() }
        Surface(color=Paper,modifier=Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding(),horizontalAlignment=Alignment.CenterHorizontally) {
                AppTopBar(onSettings={showSettings=true},onBack=if(stage in listOf("paymentBoundary","cards","captcha","blocked","externalPayment"))::back else null,
                    title=when(stage){ "paymentBoundary"->"Tu recarga"; "cards"->"Boleteras"; "captcha"->"Verificación"; "externalPayment"->"Pago en eBROU"; else->null })
                Box(Modifier.weight(1f).widthIn(max=600.dp).fillMaxWidth().pullToRefresh(
                    isRefreshing=stage=="balance"&&state.busy,state=pullState,enabled=stage=="balance"&&!state.busy,
                    onRefresh={if(engine.state.stage=="balance"&&!engine.state.busy)engine.refresh()}
                )) {
                    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal=24.dp).padding(top=if(stage=="balance")4.dp else 12.dp,bottom=if(stage in listOf("balance","blocked"))4.dp else 32.dp)
                        .pageEntrance(stage, state.captcha==null),verticalArrangement=Arrangement.spacedBy(if(stage in listOf("balance","blocked"))0.dp else 24.dp)) {
                        when(stage) {
                            "welcome" -> {
                                if(state.sessionExpired) {
                                    Title("Tu sesión venció")
                                    Text(if(state.hasSavedAccess)"STM cerró el acceso después de un tiempo. Confirmá tu huella para volver a entrar." else "STM cerró el acceso después de un tiempo. Ingresá de nuevo para continuar.",color=Muted)
                                    if(state.paymentNeedsReview)Text("Si ya autorizaste un pago, revisaremos el saldo al entrar. La app no repetirá la recarga.",color=Muted,style=MaterialTheme.typography.bodyMedium)
                                } else WelcomeHero()
                                WhiteCard {
                                    Text(if(state.sessionExpired)"Recuperá el acceso" else if(state.hasSavedAccess&&!manual)"Qué bueno verte de nuevo" else "Entrá a tu STM",style=MaterialTheme.typography.titleLarge)
                                    if(state.hasSavedAccess&&!manual) {
                                        Text("Tu acceso está protegido en este teléfono.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                                        Box(Modifier.fillMaxWidth().padding(vertical=8.dp),contentAlignment=Alignment.Center) { AppGlyph(Glyph.Fingerprint,Modifier.size(56.dp),tint=MaterialTheme.colorScheme.primary) }
                                        Primary(if(authenticating)"Esperando tu huella…" else "Entrar con huella",enabled=!authenticating&&!state.busy) {
                                            unlockSavedAccess()
                                        }
                                        TextButton(onClick={manual=true},modifier=Modifier.align(Alignment.CenterHorizontally)) { Text("Ingresar manualmente") }
                                    } else {
                                        Text("Usá tu Usuario gub.uy.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                                        OutlinedTextField(document,{document=it.filter(Char::isDigit).take(8)},label={Text("Documento uruguayo")},singleLine=true,
                                            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp))
                                        OutlinedTextField(password,{password=it},label={Text("Contraseña de gub.uy")},singleLine=true,
                                            visualTransformation=if(revealPassword)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),
                                            modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),trailingIcon={IconButton(onClick={revealPassword=!revealPassword}) { AppGlyph(if(revealPassword)Glyph.EyeOff else Glyph.Eye,label=if(revealPassword)"Ocultar contraseña" else "Mostrar contraseña",tint=Muted) }})
                                        if(vault.available) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                            Text("Guardar acceso con huella",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
                                            Switch(checked=save,onCheckedChange={save=it})
                                        }
                                        Primary(if(authenticating)"Esperando tu huella…" else if(save)"Guardar e ingresar" else "Ingresar",enabled=document.length==8&&password.isNotBlank()&&!authenticating&&!state.busy) {
                                            val doc=document; val pass=password; document="";password="";revealPassword=false
                                            if(save) {
                                                authenticating=true
                                                vault.save(doc,pass) { ok -> authenticating=false;engine.savedAccess(vault.exists)
                                                    if(ok)engine.connect(doc,pass) else engine.notice("No se guardó el acceso. Podés intentar de nuevo o ingresar sin guardarlo.") }
                                            } else engine.connect(doc,pass)
                                        }
                                    }
                                }
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                    AppGlyph(Glyph.Lock,Modifier.size(16.dp),tint=Muted)
                                    Text("App independiente de STM · Versión de prueba",style=MaterialTheme.typography.bodySmall,color=Muted)
                                }
                            }
                            "balance" -> WalletHome(state,engine::changeCard,{showAmount=true},engine::refresh,
                                onExpressCharge=if(engine.expressAvailable)::requestExpress else null,
                                onExpressHelp={if(!helpPreferences.getBoolean("express_skip_intro_v1",false))showExpressHelp=true},onTicketGuide={showTicketGuide=true},expressPreparing=engine.expressPreparing,expressProvider=engine.expressProviderName)
                            "connecting" -> {
                                LoadingState(if(state.recoveringSession)"Recuperando tu sesión" else if(state.amount!=null)"Preparando tu recarga" else "Conectando con STM", if(state.recoveringSession)"Estamos comprobando si podés volver a entrar sin identificarte otra vez." else if(engine.expressPreparing)"Ya podés soltar. Estamos preparando tu medio de pago." else "Estamos consultando el sitio. Tu información va a aparecer acá.",express=engine.expressPreparing)
                                TextButton(onClick=::back) { Text("Cancelar") }
                            }
                            "openingPayment" -> LoadingState("Un momento…","Abriendo ${if(state.selectedProvider=="1033")"Prex" else "eBROU"} para tu recarga de ${Amounts.format(state.amount)}.",express=engine.expressPreparing)
                            "cards" -> {
                                Title("Elegí tu boletera")
                                Text("Recordamos tu elección para la próxima.",color=Muted)
                                state.cards.forEach { card -> WhiteCard {
                                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                                        AppGlyph(Glyph.Ticket,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                                        Column { Text("STM · ${card.id.takeLast(4)}",style=MaterialTheme.typography.titleLarge);Text(card.status,style=MaterialTheme.typography.bodyMedium,color=Muted) }
                                    }
                                    if(card.active)Primary("Usar esta boletera",!state.busy){engine.chooseCard(card.id)}
                                    else Text("Esta boletera no está habilitada.",style=MaterialTheme.typography.bodyMedium,color=Muted)
                                } }
                                if(state.cards.isEmpty())Notice("Todavía no pudimos leer las boleteras.")
                            }
                            "captcha" -> { Title("Una verificación\ny seguimos.");Text("Completá el desafío de abajo para continuar.",color=Muted) }
                            "paymentBoundary" -> PaymentSetupContent(state,engine.payerProfiles,engine::chooseProvider,
                                onAddPayer={editingPayer=null;showPayerEditor=true},
                                onChoosePayer={showPayerPicker=true},
                                onEditPayer={editingPayer=it;showPayerEditor=true})
                            "externalPayment" -> {
                                Title("Pago abierto en eBROU")
                                Text("Al volver del banco se actualizará el saldo.",color=Muted)
                                Primary("Volver al saldo",action=engine::refresh)
                            }
                            "blocked" -> {
                                AppErrorScreen(state.message,state.diagnostic,payment=state.activePayment!=null,
                                    onExit=if(state.activePayment!=null)engine::refresh else engine::cancel)
                            }
                        }
                        // One stable host: transitions never duplicate or recreate the authenticated browser.
                        val cap=state.captcha
                        val screenHeight=LocalConfiguration.current.screenHeightDp
                        BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                            val captchaScale=if(cap==null)1f else minOf(1f,maxWidth.value/cap.width,(screenHeight-260).coerceAtLeast(180).toFloat()/cap.height)
                            AndroidView(factory={engine.host},modifier=if(cap==null)Modifier.size(1.dp) else Modifier.width((cap.width*captchaScale).dp).height((cap.height*captchaScale).dp).clip(RoundedCornerShape(8.dp)),update={it.crop=cap})
                        }
                        if(cap!=null)Primary("Ya completé la verificación"){engine.continueCaptcha()}
                    }
                    SnackbarHost(hostState=snackbar,modifier=Modifier.align(Alignment.BottomCenter).padding(16.dp))
                    if(stage=="balance")PullToRefreshDefaults.Indicator(state=pullState,isRefreshing=state.busy,modifier=Modifier.align(Alignment.TopCenter),containerColor=Lime,color=MaterialTheme.colorScheme.onPrimaryContainer)
                }
                if(stage=="paymentBoundary")Box(Modifier.widthIn(max=600.dp).fillMaxWidth()) {
                    PaymentSetupFooter(state,engine.payerProfiles,
                        onAddPayer={editingPayer=null;showPayerEditor=true},
                        onChoosePayer={showPayerPicker=true},onContinue=engine::beginPayment)
                }
            }
        }
        if(showAmount)AmountSheet(state,{showAmount=false}){amount->showAmount=false;engine.prepare(amount)}
        if(showTicketGuide)TicketGuideSheet {showTicketGuide=false}
        if(showExpressHelp && stage=="balance")ExpressIntroDialog(state.minimum,
            onSkipChanged={skip->helpPreferences.edit().putBoolean("express_skip_intro_v1",skip).apply()},
            onClose={showExpressHelp=false},onAccept={skip->
                helpPreferences.edit().putBoolean("express_skip_intro_v1",skip).apply()
                showExpressHelp=false
                if(engine.expressAvailable && !state.busy)engine.startExpress()
            })
        if(showSettings)SettingsSheet(updates,appearance,onAppearance,state.hasSavedAccess,state.stage!="welcome",state.diagnostic,
            onForget={showSettings=false;showForget=true},onLogout={showSettings=false;engine.logout()},onInstall={updates.install(this@MainActivity)},onClose={showSettings=false},
            onTicketGuide={showSettings=false;showTicketGuide=true})

        if (showPayerPicker) PayerPicker(engine.payerProfiles,state.payerProfileId,
            onChoose={engine.choosePayer(it);showPayerPicker=false},
            onAdd={editingPayer=null;showPayerPicker=false;showPayerEditor=true},
            onClose={showPayerPicker=false})
        if (showPayerEditor) PayerProfileEditor(editingPayer, onSave = engine::savePayer, onDelete = engine::deletePayer, onClose = {
            showPayerEditor = false
        })
        if (showForget) AlertDialog(onDismissRequest = { showForget = false }, title = { Text("¿Olvidar este acceso?") },
            text = { Text("Se eliminan las credenciales cifradas, las preferencias y la sesión local.") },
            confirmButton = { TextButton(onClick = {
                if (engine.forgetChoices()) { helpPreferences.edit().clear().apply();vault.forget(); engine.savedAccess(false); engine.logout(); engine.notice("Acceso guardado, perfiles, preferencias y sesión local eliminados.") }
                showForget = false
            }) { Text("Olvidar") } },
            dismissButton = { TextButton(onClick = { showForget = false }) { Text("Cancelar") } })
    }
}

@Composable internal fun EmbeddedPrexScreen(payment: EmbeddedPrexPayment, pending: ActivePayment?, onClose: () -> Unit) {
    var showOriginal by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val expressAdvancing=payment.expressPhase=="advancing"
    val expressVerification=payment.expressPhase=="verification"
    LaunchedEffect(payment.nativeStage) { showOriginal = false }
    val completion = payment.nativeStage in listOf("finalConfirmation", "receipt", "paymentRejected", "paymentPending", "stmSuccess", "returnBalance", "sessionExpired")
    LaunchedEffect(payment.nativeStage) { if(payment.nativeStage in listOf("returnBalance", "sessionExpired")) onClose() }
    val native = (completion || payment.nativeStage in listOf("summary", "payer", "card", "loading")) && !showOriginal
    LaunchedEffect(native, payment.nativeStage, payment.expandedChallenge, payment.challenge != null) {
        if (native && payment.nativeStage in listOf("payer","card")) payment.positionVerification() else payment.restoreVerification()
    }
    BackHandler(onBack = onClose)
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            AppTopBar(title="Pago con Prex",onBack=onClose)
            Row(Modifier.padding(horizontal=24.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                AppGlyph(Glyph.Lock,Modifier.size(16.dp),tint=Muted)
                Text("${if(payment.currentHost=="stm.gub.uy")"STM" else "Sistarbanc"} · ${Amounts.format(pending?.amount)}",color=Muted,style=MaterialTheme.typography.bodySmall)
            }
            if (payment.payerNotice.isNotBlank()) {
                Text(payment.payerNotice, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                if (payment.payerConflict) TextButton(onClick = payment::applyChosenPayer, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Usar el titular seleccionado") }
            }
            if (payment.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (payment.message.isNotBlank()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
                    AppErrorScreen(payment.message,payment=true,onExit=onClose)
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val browserWidth = constraints.maxWidth
                    val browserHeight = constraints.maxHeight
                    val pixelsPerDp = LocalDensity.current.density
                    val cssPixelsToDp = if (payment.cssViewportWidth > 0f) browserWidth / payment.cssViewportWidth / pixelsPerDp else 1f
                    if (!native) Column(Modifier.fillMaxSize()) {
                        if(completion || payment.nativeStage in listOf("summary","payer","card"))TextButton(onClick={showOriginal=false},modifier=Modifier.align(Alignment.End)) {
                            Text(when(payment.nativeStage){"summary"->"Ver resumen";"card"->"Ver formulario";else->"Ver datos"})
                        }
                        PaymentBrowserView(payment,false,null,browserWidth,browserHeight,Modifier.weight(1f).fillMaxWidth())
                    }
                    if (native && completion) {
                        PaymentCompletionScreen(payment, pending, onOriginal={showOriginal=true}) {
                            PaymentBrowserView(payment,true,null,browserWidth,browserHeight,Modifier.size(1.dp))
                        }
                    }
                    if (native && payment.nativeStage=="card") NativeCardForm(payment.cardBusy,payment.canContinue,payment.cardError,payment.expandedChallenge,payment::submitCard,{payment.stopExpress();showOriginal=true},focusCard=payment.expressPhase=="done") {
                        if(payment.expandedChallenge) ExpandedPaymentChallenge(payment,browserWidth,browserHeight,Modifier.fillMaxSize())
                        else BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                            val cap=payment.challenge
                            val maxPanelHeight=(browserHeight/pixelsPerDp-160f).coerceAtLeast(140f)
                            val scale=if(cap==null)1f else minOf(cssPixelsToDp,maxWidth.value/cap.width,maxPanelHeight/cap.height)
                            PaymentBrowserView(payment,cap==null,cap,browserWidth,browserHeight,if(cap==null)Modifier.size(1.dp) else Modifier.width((cap.width*scale).dp).height((cap.height*scale).dp))
                        }
                    }
                    if(native && !completion && payment.nativeStage!="card" && payment.expandedChallenge)Column(Modifier.fillMaxSize()) {
                        ExpandedPaymentChallenge(payment,browserWidth,browserHeight,Modifier.weight(1f).fillMaxWidth().padding(12.dp))
                        TextButton(onClick={payment.stopExpress();showOriginal=true},modifier=Modifier.fillMaxWidth()){Text("Ver página original")}
                    }
                    if (native && !completion && payment.nativeStage!="card" && !payment.expandedChallenge) Column(Modifier.fillMaxSize().background(Paper)) {
                      Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (payment.nativeStage != "payer" || expressAdvancing) PaymentBrowserView(payment, true, null, browserWidth, browserHeight, Modifier.fillMaxWidth().height(1.dp))
                        if(expressAdvancing) LoadingState("Preparando Prex", "Usando el mínimo y tus datos guardados.",express=true) else {
                        ProgressSteps(if(payment.nativeStage=="payer")1 else 0,listOf("Recarga","Titular","Tarjeta"))
                        Text(if(expressVerification)"Una verificación\ny seguimos" else if (payment.nativeStage == "payer") "Datos del titular" else "Revisá tu recarga",style=MaterialTheme.typography.headlineMedium,color=Ink)
                        if (payment.nativeStage == "loading") {
                            LoadingState("Un momento…","Estamos recuperando tu solicitud.")
                        } else if (payment.nativeStage == "summary") {
                            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(payment.summaryRows.singleOrNull { it.first.trim().equals("Total:", ignoreCase = true) }?.second ?: Amounts.format(pending?.amount),style=MaterialTheme.typography.displayMedium,color=Ink)
                                    payment.summaryRows.filterNot { it.first.trim().equals("Total:", ignoreCase = true) }.forEach { (label, value) ->
                                        Column { Text(label, color = Muted, fontSize = 12.sp); Text(value, color = Ink, fontSize = 16.sp) }
                                    }
                                }
                            }
                        } else {
                            if (!payment.expandedChallenge && !expressVerification) Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val values = payment.payerValues
                                    val rows = if (values.size == 5) listOf("Titular" to "${values[0]} ${values[1]}", "Documento" to values[2], "Correo" to values[3], "Celular" to values[4]) else emptyList()
                                    rows.forEach { (label, value) ->
                                        Column { Text(label, color = Muted, fontSize = 12.sp); Text(value.ifBlank { "Sin completar" }, color = Ink, fontSize = 16.sp) }
                                    }
                                }
                            }
                            Text(if(payment.challenge!=null)"Completá la verificación para continuar." else "Revisá los datos antes de seguir.", color = Muted)
                            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                val cap = payment.challenge
                                val maxPanelHeight = (browserHeight / pixelsPerDp - 140f).coerceAtLeast(140f)
                                val scale = if (cap == null) 1f else minOf(cssPixelsToDp, maxWidth.value / cap.width, maxPanelHeight / cap.height)
                                PaymentBrowserView(payment, cap == null, cap, browserWidth, browserHeight, if (cap == null) Modifier.size(1.dp) else Modifier.width((cap.width * scale).dp).height((cap.height * scale).dp))
                            }
                        }
                        }
                      }
                      if(payment.nativeStage in listOf("summary","payer") && !expressAdvancing) Surface(color=Paper,shadowElevation=6.dp) {
                        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Primary(if(payment.nativeStage=="payer")"Continuar a la tarjeta" else "Continuar a los datos",payment.canContinue,payment::advance)
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                if(payment.nativeStage=="payer" && !expressVerification)TextButton(onClick={payment.stopExpress();editing=true},enabled=payment.chosenPayer!=null){Text("Editar datos")}
                                TextButton(onClick={payment.stopExpress();showOriginal=true}){Text("Ver página original")}
                            }
                        }
                      }
                    }
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
@Composable internal fun ExpandedPaymentChallenge(payment: EmbeddedPrexPayment, browserWidth: Int, browserHeight: Int, modifier: Modifier) {
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {keyboard?.hide()}
    BoxWithConstraints(modifier,contentAlignment=Alignment.Center) {
        val cap=payment.challenge
        val cssScale=if(payment.cssViewportWidth>0f)browserWidth/payment.cssViewportWidth/LocalDensity.current.density else 1f
        val scale=if(cap==null)1f else minOf(cssScale,maxWidth.value/cap.width,maxHeight.value/cap.height)
        PaymentBrowserView(payment,cap==null,cap,browserWidth,browserHeight,
            if(cap==null)Modifier.size(1.dp) else Modifier.width((cap.width*scale).dp).height((cap.height*scale).dp))
    }
}

@Composable internal fun PaymentBrowserView(payment: EmbeddedPrexPayment, hidden: Boolean, crop: CaptchaRect?, browserWidth: Int, browserHeight: Int, modifier: Modifier) {
    AndroidView(factory = {
        (payment.web.parent as? android.view.ViewGroup)?.removeView(payment.web)
        PaymentPageHost(it, payment.web)
    }, update = {
        it.viewportWidth = browserWidth
        // A translated crop cannot reveal pixels clipped by the WebView's own viewport.
        // Keep tall provider challenges fully laid out, then fit their complete rectangle.
        val cssScale=if(payment.cssViewportWidth>0f)browserWidth/payment.cssViewportWidth else it.resources.displayMetrics.density
        it.viewportHeight = maxOf(browserHeight,if(crop!=null)kotlin.math.ceil((crop.height+32f)*cssScale).toInt() else browserHeight)
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
