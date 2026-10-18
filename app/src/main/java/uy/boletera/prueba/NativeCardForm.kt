package uy.boletera.prueba

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.onAutofillText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object CardInput {
    fun panValid(value: String): Boolean = value.length in 13..16 && value.all { it in '0'..'9' } &&
        value.reversed().mapIndexed { i, c -> (c-'0').let { if(i%2==0) it else (it*2).let { n -> if(n>9)n-9 else n } } }.sum()%10==0
    fun expiryValid(value: String, now: YearMonth = YearMonth.now()): Boolean =
        value.length==4 && value.all { it in '0'..'9' } && runCatching { YearMonth.of(2000+value.takeLast(2).toInt(),value.take(2).toInt()) >= now }.getOrDefault(false)
    fun cvvValid(value: String) = value.length in 3..4 && value.all { it in '0'..'9' }
}

private class CardGrouping(private val group: Int, private val separator: Char) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val raw=text.text
        val display=raw.chunked(group).joinToString(separator.toString())
        return TransformedText(androidx.compose.ui.text.AnnotatedString(display), object: OffsetMapping {
            override fun originalToTransformed(offset: Int) = (offset+(if(offset==0)0 else (offset-1)/group)).coerceAtMost(display.length)
            override fun transformedToOriginal(offset: Int) = (offset-offset/(group+1)).coerceAtMost(raw.length)
        })
    }
}

/** Temporary values only. A complete autofill selection can continue to the provider's next step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun NativeCardForm(busy: Boolean, canContinue: Boolean, providerError: Boolean,
    expandedChallenge: Boolean, onSubmit: (String,String,String)->Unit, onOriginal: ()->Unit,
    focusCard: Boolean = false, verificationRequired: Boolean = false, onManualContinue: () -> Unit = {}, onVerificationShown: () -> Unit = {},
    onAutomaticSubmit: ((String,String,String)->Unit)? = null, onOtherCard: (() -> Unit)? = null,
    autoContinue: Boolean = true, express: Boolean = false, notSent: Boolean = false,
    verification: @Composable ()->Unit = {}) {
    var pan by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var filledFields by remember { mutableIntStateOf(0) }
    var autoConsumed by remember { mutableStateOf(false) }
    var pendingContinue by remember { mutableStateOf(false) }
    val autofill=LocalAutofillManager.current
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val cardAutofill=remember { CardAutofillRequester() }
    var autofillRequested by remember { mutableStateOf(false) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val numberFocus=remember { FocusRequester() }
    val expiryFocus=remember { FocusRequester() }
    val codeFocus=remember { FocusRequester() }
    val numberView=remember { BringIntoViewRequester() }
    val expiryView=remember { BringIntoViewRequester() }
    val codeView=remember { BringIntoViewRequester() }
    val verificationView=remember { BringIntoViewRequester() }
    val scope=rememberCoroutineScope()
    // Lab: the card typed (or filled) is handed to Android's autofill on «Continuar», so Google can offer «Guardar»;
    // once committed, leaving the screen must not cancel that.
    var committed by remember { mutableStateOf(false) }
    fun submit(manual: Boolean = false, checkVerification: Boolean = false) {
        if(busy || !canContinue || providerError || expandedChallenge) {
            if(IsLab && manual)LabLog.add("tarjeta","Continuar sin efecto: ${if(busy)"enviándose" else if(!canContinue)"Prex no puede seguir" else if(providerError)"error de Prex" else "verificación abierta"}")
            return
        }
        if(manual)onManualContinue()
        autoConsumed=true;filledFields=0;pendingContinue=false
        if(IsLab) {autofill?.commit();committed=true} else autofill?.cancel()
        keyboard?.hide();focus.clearFocus()
        val send=(if(manual&&!checkVerification)onSubmit else onAutomaticSubmit?:onSubmit)
        if(IsLab)LabLog.add("tarjeta",if(manual&&!checkVerification)"Continuar: se envía a Prex" else "se envía sola a Prex")
        send(pan,expiry.take(2)+"/"+expiry.takeLast(2),cvv)
        // Lab keeps the code while the card step is on screen: a check that appears right at sending no longer makes the
        // person type it again. Leaving the step (or the app for 20 s) still wipes it.
        if(!IsLab)cvv="";attempted=false
    }
    fun fill(field: Int, value: String): Boolean {
        if(busy)return false
        pendingContinue=false
        val digits=value.filter { it in '0'..'9' }
        when(field) {
            1->pan=digits.take(16)
            2->expiry=if(digits.length==6)digits.take(2)+digits.takeLast(2) else digits.take(4)
            4->cvv=digits.take(4)
        }
        if(IsLab)LabLog.add("tarjeta","Google completó ${when(field){1->"el número";2->"el vencimiento";else->"el código"}}")
        filledFields=filledFields or field
        return true
    }
    fun showVerification() {
        focus.clearFocus();keyboard?.hide()
        scope.launch { delay(200);verificationView.bringIntoView();onVerificationShown() }
    }
    // A fill that lands while the app is still behind Google's screen waits until it is back in front (Lab).
    val lifeState by lifecycle.currentStateFlow.collectAsState()
    val inFront=!IsLab || lifeState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(filledFields,pan,expiry,cvv,busy,canContinue,providerError,expandedChallenge,verificationRequired,pendingContinue,focusCard,inFront) {
        if(providerError)filledFields=0
        if(providerError||!focusCard)pendingContinue=false
        if(((filledFields==7 && !autoConsumed && autoContinue)||pendingContinue) && inFront && !busy && !providerError && !expandedChallenge &&
            CardInput.panValid(pan) && CardInput.expiryValid(expiry) && CardInput.cvvValid(cvv)) {
            // Typing alone never requests a submission. Express may retain an explicit IME Done
            // while verification is pending; recheck in the provider before sending that request.
            if(verificationRequired)showVerification() else if(canContinue)submit(manual=pendingContinue,checkVerification=true)
        }
    }
    fun focusMissing(): Boolean {
        val target=when {
            !CardInput.panValid(pan)->numberFocus to numberView
            !CardInput.expiryValid(expiry)->expiryFocus to expiryView
            !CardInput.cvvValid(cvv)->codeFocus to codeView
            else->return false
        }
        attempted=true
        // Lab: with nothing filled, «Continuar» brings Google's list back (it is the main way in) and the record says what was missing.
        if(IsLab) {
            val missing=when { !CardInput.panValid(pan)->if(pan.isEmpty())"número vacío" else "número incompleto"; !CardInput.expiryValid(expiry)->"vencimiento"; else->"código" }
            val asked=pan.isEmpty()&&!busy&&cardAutofill.request()
            LabLog.add("tarjeta","Continuar sin datos completos: falta $missing${if(asked)" · se pidió la lista de Google" else ""}")
        }
        target.first.requestFocus();keyboard?.show()
        scope.launch { target.second.bringIntoView();delay(300);target.second.bringIntoView() }
        return true
    }
    LaunchedEffect(focusCard,expandedChallenge) {
        if(focusCard&&!expandedChallenge&&!autofillRequested) {
            autofillRequested=true
            // Request the same virtual field that receives focus and the returned dataset.
            numberFocus.requestFocus();numberView.bringIntoView();delay(120)
            val asked=cardAutofill.request()
            if(IsLab)LabLog.add("tarjeta",if(asked)"lista de Google pedida" else "lista de Google: no hay campo de número para pedirla")
            keyboard?.show()
        }
    }
    LaunchedEffect(filledFields) {
        // A partial system fill commonly omits CVV. Focus it without selecting a different card.
        if(focusCard && filledFields and 3 == 3 && CardInput.panValid(pan) && CardInput.expiryValid(expiry) && cvv.isBlank()) {
            codeFocus.requestFocus();keyboard?.show();codeView.bringIntoView()
        }
    }
    LaunchedEffect(expandedChallenge) { if(expandedChallenge) {focus.clearFocus();keyboard?.hide()} }
    DisposableEffect(lifecycle,autofill) {
        fun clear() { filledFields=0;pendingContinue=false;if(!committed)autofill?.cancel();pan="";expiry="";cvv="";attempted=false;focus.clearFocus() }
        // Lab: Google's fingerprint screen also stops the app, and cancelling there left only the number filled (owner 17/10).
        // So a stop wipes the security code at once but keeps Google's fill alive; number and expiry go if the app is not
        // back within 20 s of that stop (locking the phone or leaving for real still wipes the card).
        var stoppedAt=0L
        var wipe: kotlinx.coroutines.Job?=null
        val observer=LifecycleEventObserver { _, event ->
            when {
                event==Lifecycle.Event.ON_STOP && IsLab -> {
                    cvv="";filledFields=filledFields and 3;pendingContinue=false;stoppedAt=android.os.SystemClock.elapsedRealtime()
                    LabLog.add("tarjeta","salió de pantalla: se borra el código; el resto, si no vuelve en 20 s")
                    wipe?.cancel();wipe=scope.launch { delay(20_000);clear() }
                }
                event==Lifecycle.Event.ON_STOP -> clear()
                event==Lifecycle.Event.ON_START && IsLab && stoppedAt>0L -> {
                    wipe?.cancel();wipe=null
                    if(android.os.SystemClock.elapsedRealtime()-stoppedAt>=20_000)clear()
                    stoppedAt=0L
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { wipe?.cancel();lifecycle.removeObserver(observer);clear() }
    }
    if(expandedChallenge) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {verification()}
            TextButton(onClick=onOriginal,modifier=Modifier.fillMaxWidth()) {Text("Ver página original")}
        }
        return
    }
    val shape=if(IsLab)androidx.compose.foundation.shape.RoundedCornerShape(16.dp) else OutlinedTextFieldDefaults.shape
    // Lab: Google's list hides when the person taps elsewhere or types; a tap on the empty number brings it back.
    val numberTouch=remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    LaunchedEffect(numberTouch) {
        numberTouch.interactions.collect { if(IsLab && it is androidx.compose.foundation.interaction.PressInteraction.Release && pan.isEmpty() && !busy) { cardAutofill.request() } }
    }
    val numberField: @Composable (Modifier)->Unit = { modifier ->
        OutlinedTextField(value=pan,onValueChange={filledFields=0;pendingContinue=false;committed=false;pan=it.filter { c -> c in '0'..'9' }.take(16)},
                    label={Text("Número de tarjeta")},shape=shape,singleLine=true,enabled=!busy,interactionSource=numberTouch,
                    modifier=modifier.bringIntoViewRequester(numberView).focusRequester(numberFocus).cardAutofill(cardAutofill).testTag("cardNumber").semantics { contentType=ContentType.CreditCardNumber;onAutofillText { fill(1,it.text) } },
                    visualTransformation=remember { CardGrouping(4,' ') },
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
                    keyboardActions=KeyboardActions(onNext={if(!focusMissing())keyboard?.hide()}),
                    isError=attempted&&!CardInput.panValid(pan),
                    supportingText=if(attempted&&!CardInput.panValid(pan)) {{Text(if(IsLab)(if(pan.isEmpty())"Te faltó completar el número de la tarjeta." else "El número no parece correcto.") else "Revisá el número de tarjeta.")}} else null)
    }
    val expiryField: @Composable (Modifier)->Unit = { modifier ->
        OutlinedTextField(value=expiry,onValueChange={ filledFields=0;pendingContinue=false;committed=false;val digits=it.filter { c->c in '0'..'9' };expiry=if(digits.length==6)digits.take(2)+digits.takeLast(2) else digits.take(4) },
                        label={Text("Vencimiento")},shape=shape,placeholder={Text("MM/AA")},singleLine=true,enabled=!busy,
                        modifier=modifier.bringIntoViewRequester(expiryView).focusRequester(expiryFocus).semantics { contentType=ContentType.CreditCardExpirationDate;onAutofillText { fill(2,it.text) } },
                        visualTransformation=remember { CardGrouping(2,'/') },
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
                        keyboardActions=KeyboardActions(onNext={if(!focusMissing())keyboard?.hide()}),
                        isError=attempted&&!CardInput.expiryValid(expiry),
                        supportingText=if(attempted&&!CardInput.expiryValid(expiry)) {{Text(if(IsLab)(if(expiry.isEmpty())"Falta el vencimiento." else "Fecha vencida o no válida.") else "Usá mes y año vigentes.")}} else null)
    }
    val cvvField: @Composable (Modifier)->Unit = { modifier ->
        OutlinedTextField(value=cvv,onValueChange={filledFields=0;pendingContinue=false;committed=false;cvv=it.filter { c->c in '0'..'9' }.take(4)},
                        label={Text(if(IsLab)"Código de seguridad" else "CVV")},shape=shape,singleLine=true,enabled=!busy,
                        modifier=modifier.bringIntoViewRequester(codeView).focusRequester(codeFocus).semantics { contentType=ContentType.CreditCardSecurityCode;onAutofillText { fill(4,it.text) } },
                        visualTransformation=PasswordVisualTransformation(),
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,imeAction=ImeAction.Done),
                        keyboardActions=KeyboardActions(onDone={if(!focusMissing()) {
                            if(verificationRequired) {pendingContinue=focusCard&&!providerError&&!busy;showVerification()}
                            else submit(manual=true)
                        }}),
                        isError=attempted&&!CardInput.cvvValid(cvv),
                        supportingText=if(attempted&&!CardInput.cvvValid(cvv)) {{Text(if(cvv.isBlank())(if(IsLab)"Falta el código del dorso." else "Falta el código de seguridad de tu tarjeta.") else "Ingresá 3 o 4 dígitos.")}} else null)
    }
    if(IsLab) {
        // What Google filled is shown before going on (never «a ciegas»): the last four, and the code if it is missing.
        val filledNote=if(filledFields and 1 != 0 && CardInput.panValid(pan))
            if(CardInput.cvvValid(cvv)) "Tarjeta ···· ${pan.takeLast(4)} lista." else "Tarjeta ···· ${pan.takeLast(4)}. Escribí el código de seguridad."
            else null
        LabCardFields(busy,canContinue,providerError,express,pendingContinue,verificationRequired,numberField,expiryField,cvvField,verification,verificationView,
            onContinue={ if(!focusMissing())submit(manual=true) },onOriginal={ autofill?.cancel();onOriginal() },onOtherCard=onOtherCard,filledNote=filledNote,notSent=notSent)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            ProgressSteps(2,listOf("Recarga","Titular","Tarjeta"))
            Text("Tu tarjeta",style=MaterialTheme.typography.headlineMedium,color=Ink)
            if(!expandedChallenge) {
                Text(if(pendingContinue)"Completá la verificación. Después seguimos automáticamente." else if(verificationRequired)"Completá los datos de tu tarjeta y la verificación para continuar." else if(focusCard)"Elegí tu tarjeta guardada. Si falta el CVV, completalo y tocá Listo en el teclado." else "Si Android completa todos los datos, avanzás automáticamente. También podés ingresarlos a mano.",color=Muted,
                    modifier=Modifier.testTag("cardAutofillPrompt"))
                TextButton(enabled=!busy,onClick={
                    pendingContinue=false
                    numberFocus.requestFocus()
                    scope.launch { numberView.bringIntoView();cardAutofill.request();keyboard?.show() }
                },modifier=Modifier.testTag("requestCardAutofill")) { Text("Tarjetas guardadas") }
                numberField(Modifier.fillMaxWidth())
                FlowRow(maxItemsInEachRow=if(LocalDensity.current.fontScale>1.2f)1 else 2,horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    expiryField(Modifier.weight(1f))
                    cvvField(Modifier.weight(1f))
                }
                Text("Boletera no guarda estos datos. El CVV se borra del formulario al continuar.",style=MaterialTheme.typography.bodySmall,color=Muted)
            }
            if(providerError) Notice("No se pudo avanzar. Revisá la página original para ver la respuesta de Sistarbanc antes de intentar de nuevo.")
            Box(Modifier.fillMaxWidth().bringIntoViewRequester(verificationView)) { verification() }
        }
        Surface(color=Paper,shadowElevation=if(IsLab)0.dp else 6.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp)) {
                Primary(if(providerError)"Revisar respuesta" else if(busy)"Procesando…" else "Continuar",(providerError||canContinue)&&!busy&&!expandedChallenge) {
                    if(providerError) {autofill?.cancel();onOriginal()}
                    else if(!focusMissing()) {
                        submit(manual=true)
                    }
                }
                TextButton(onClick={autofill?.cancel();onOriginal()},modifier=Modifier.fillMaxWidth()) { Text("Ver página original") }
            }
        }
    }
}
