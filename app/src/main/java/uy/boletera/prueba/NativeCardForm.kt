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
    focusCard: Boolean = false,
    verification: @Composable ()->Unit = {}) {
    var pan by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    var filledFields by remember { mutableIntStateOf(0) }
    var autoConsumed by remember { mutableStateOf(false) }
    val autofill=LocalAutofillManager.current
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val numberFocus=remember { FocusRequester() }
    val expiryFocus=remember { FocusRequester() }
    val codeFocus=remember { FocusRequester() }
    val numberView=remember { BringIntoViewRequester() }
    val expiryView=remember { BringIntoViewRequester() }
    val codeView=remember { BringIntoViewRequester() }
    val scope=rememberCoroutineScope()
    fun submit() {
        if(busy || !canContinue || providerError || expandedChallenge)return
        autoConsumed=true;filledFields=0
        autofill?.cancel();keyboard?.hide();focus.clearFocus()
        onSubmit(pan,expiry.take(2)+"/"+expiry.takeLast(2),cvv)
        cvv="";attempted=false
    }
    fun fill(field: Int, value: String): Boolean {
        if(busy)return false
        val digits=value.filter { it in '0'..'9' }
        when(field) {
            1->pan=digits.take(16)
            2->expiry=if(digits.length==6)digits.take(2)+digits.takeLast(2) else digits.take(4)
            4->cvv=digits.take(4)
        }
        filledFields=filledFields or field
        return true
    }
    LaunchedEffect(filledFields,pan,expiry,cvv,busy,canContinue,providerError,expandedChallenge) {
        if(providerError)filledFields=0
        if(filledFields==7 && !autoConsumed && !busy && canContinue && !providerError && !expandedChallenge &&
            CardInput.panValid(pan) && CardInput.expiryValid(expiry) && CardInput.cvvValid(cvv)) {
            // Autofill delivers separate fields; wait for the complete batch, never infer it from typing.
            submit()
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
        target.first.requestFocus();keyboard?.show()
        scope.launch { target.second.bringIntoView();delay(300);target.second.bringIntoView() }
        return true
    }
    LaunchedEffect(focusCard,expandedChallenge) { if(focusCard&&!expandedChallenge) { numberFocus.requestFocus();keyboard?.show() } }
    LaunchedEffect(filledFields) {
        // A partial system fill commonly omits CVV. Focus it without selecting a different card.
        if(focusCard && filledFields and 3 == 3 && CardInput.panValid(pan) && CardInput.expiryValid(expiry) && cvv.isBlank()) {
            codeFocus.requestFocus();keyboard?.show();codeView.bringIntoView()
        }
    }
    LaunchedEffect(expandedChallenge) { if(expandedChallenge) {focus.clearFocus();keyboard?.hide()} }
    DisposableEffect(lifecycle,autofill) {
        fun clear() { filledFields=0;autofill?.cancel();pan="";expiry="";cvv="";attempted=false;focus.clearFocus() }
        val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_STOP) clear() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer);clear() }
    }
    if(expandedChallenge) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {verification()}
            TextButton(onClick=onOriginal,modifier=Modifier.fillMaxWidth()) {Text("Ver página original")}
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            ProgressSteps(2,listOf("Recarga","Titular","Tarjeta"))
            Text("Tu tarjeta",style=MaterialTheme.typography.headlineMedium,color=Ink)
            if(!expandedChallenge) {
                Text("Si Google completa todos los datos, avanzás automáticamente. También podés ingresarlos a mano.",color=Muted)
                OutlinedTextField(value=pan,onValueChange={filledFields=0;pan=it.filter { c -> c in '0'..'9' }.take(16)},
                    label={Text("Número de tarjeta")},singleLine=true,enabled=!busy,
                    modifier=Modifier.fillMaxWidth().bringIntoViewRequester(numberView).focusRequester(numberFocus).semantics { contentType=ContentType.CreditCardNumber;onAutofillText { fill(1,it.text) } },
                    visualTransformation=remember { CardGrouping(4,' ') },
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
                    keyboardActions=KeyboardActions(onNext={if(!focusMissing())keyboard?.hide()}),
                    isError=attempted&&!CardInput.panValid(pan),
                    supportingText=if(attempted&&!CardInput.panValid(pan)) {{Text("Revisá el número de tarjeta.")}} else null)
                FlowRow(maxItemsInEachRow=if(LocalDensity.current.fontScale>1.2f)1 else 2,horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value=expiry,onValueChange={ filledFields=0;val digits=it.filter { c->c in '0'..'9' };expiry=if(digits.length==6)digits.take(2)+digits.takeLast(2) else digits.take(4) },
                        label={Text("Vencimiento")},placeholder={Text("MM/AA")},singleLine=true,enabled=!busy,
                        modifier=Modifier.weight(1f).bringIntoViewRequester(expiryView).focusRequester(expiryFocus).semantics { contentType=ContentType.CreditCardExpirationDate;onAutofillText { fill(2,it.text) } },
                        visualTransformation=remember { CardGrouping(2,'/') },
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
                        keyboardActions=KeyboardActions(onNext={if(!focusMissing())keyboard?.hide()}),
                        isError=attempted&&!CardInput.expiryValid(expiry),
                        supportingText=if(attempted&&!CardInput.expiryValid(expiry)) {{Text("Usá mes y año vigentes.")}} else null)
                    OutlinedTextField(value=cvv,onValueChange={filledFields=0;cvv=it.filter { c->c in '0'..'9' }.take(4)},
                        label={Text("CVV")},singleLine=true,enabled=!busy,
                        modifier=Modifier.weight(1f).bringIntoViewRequester(codeView).focusRequester(codeFocus).semantics { contentType=ContentType.CreditCardSecurityCode;onAutofillText { fill(4,it.text) } },
                        visualTransformation=PasswordVisualTransformation(),
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,imeAction=ImeAction.Done),
                        keyboardActions=KeyboardActions(onDone={if(!focusMissing())submit()}),
                        isError=attempted&&!CardInput.cvvValid(cvv),
                        supportingText=if(attempted&&!CardInput.cvvValid(cvv)) {{Text(if(cvv.isBlank())"Falta el código de seguridad de tu tarjeta." else "Ingresá 3 o 4 dígitos.")}} else null)
                }
                Text("Boletera no guarda estos datos. El CVV se borra del formulario al continuar.",style=MaterialTheme.typography.bodySmall,color=Muted)
            }
            if(providerError) Notice("No se pudo avanzar. Revisá la página original para ver la respuesta de Sistarbanc antes de intentar de nuevo.")
            verification()
        }
        Surface(color=Paper,shadowElevation=6.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp)) {
                Primary(if(busy)"Procesando…" else "Continuar",canContinue&&!busy&&!expandedChallenge) {
                    if(!focusMissing()) {
                        submit()
                    }
                }
                TextButton(onClick={autofill?.cancel();onOriginal()},modifier=Modifier.fillMaxWidth()) { Text("Ver página original") }
            }
        }
    }
}
