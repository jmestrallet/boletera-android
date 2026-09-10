package uy.boletera.prueba

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.YearMonth

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

/** Scoped to the payment panel; restore the owner's existing capture policy afterward. */
@Composable internal fun SecurePaymentWindow() {
    val context=LocalContext.current
    DisposableEffect(context) {
        var current: android.content.Context = context
        while(current is android.content.ContextWrapper && current !is ComponentActivity) current=current.baseContext
        val window=(current as? ComponentActivity)?.window
        val alreadySecure=window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE)!=0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if(!alreadySecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** Deliberately not saveable or hoisted into engine/persistence. Only explicit Continue transfers values. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun NativeCardForm(busy: Boolean, canContinue: Boolean, providerError: Boolean,
    expandedChallenge: Boolean, onSubmit: (String,String,String)->Unit, onOriginal: ()->Unit,
    focusCard: Boolean = false,
    verification: @Composable ()->Unit = {}) {
    var pan by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val autofill=LocalAutofillManager.current
    val focus=LocalFocusManager.current
    val keyboard=LocalSoftwareKeyboardController.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val numberFocus=remember { FocusRequester() }
    LaunchedEffect(focusCard,expandedChallenge) { if(focusCard&&!expandedChallenge) { numberFocus.requestFocus();keyboard?.show() } }
    DisposableEffect(lifecycle,autofill) {
        fun clear() { autofill?.cancel();pan="";expiry="";cvv="";attempted=false;focus.clearFocus() }
        val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_STOP) clear() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer);clear() }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            ProgressSteps(2,listOf("Recarga","Titular","Tarjeta"))
            Text("Tu tarjeta",style=MaterialTheme.typography.headlineMedium,color=Ink)
            if(!expandedChallenge) {
                Text("Ingresá los datos para continuar con Sistarbanc.",color=Muted)
                OutlinedTextField(value=pan,onValueChange={pan=it.filter { c -> c in '0'..'9' }.take(16)},
                    label={Text("Número de tarjeta")},singleLine=true,enabled=!busy,
                    modifier=Modifier.fillMaxWidth().focusRequester(numberFocus).semantics { contentType=ContentType.CreditCardNumber },
                    visualTransformation=remember { CardGrouping(4,' ') },
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
                    keyboardActions=KeyboardActions(onNext={focus.moveFocus(FocusDirection.Next)}),
                    isError=attempted&&!CardInput.panValid(pan),
                    supportingText=if(attempted&&!CardInput.panValid(pan)) {{Text("Revisá el número de tarjeta.")}} else null)
                FlowRow(maxItemsInEachRow=if(LocalDensity.current.fontScale>1.2f)1 else 2,horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value=expiry,onValueChange={ val digits=it.filter { c->c in '0'..'9' };expiry=if(digits.length==6)digits.take(2)+digits.takeLast(2) else digits.take(4) },
                        label={Text("Vencimiento")},placeholder={Text("MM/AA")},singleLine=true,enabled=!busy,
                        modifier=Modifier.weight(1f).semantics { contentType=ContentType.CreditCardExpirationDate },
                        visualTransformation=remember { CardGrouping(2,'/') },
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Next),
                        keyboardActions=KeyboardActions(onNext={focus.moveFocus(FocusDirection.Next)}),
                        isError=attempted&&!CardInput.expiryValid(expiry),
                        supportingText=if(attempted&&!CardInput.expiryValid(expiry)) {{Text("Usá mes y año vigentes.")}} else null)
                    OutlinedTextField(value=cvv,onValueChange={cvv=it.filter { c->c in '0'..'9' }.take(4)},
                        label={Text("CVV")},singleLine=true,enabled=!busy,modifier=Modifier.weight(1f),
                        visualTransformation=PasswordVisualTransformation(),
                        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword,imeAction=ImeAction.Done),
                        keyboardActions=KeyboardActions(onDone={keyboard?.hide()}),
                        isError=attempted&&!CardInput.cvvValid(cvv),
                        supportingText=if(attempted&&!CardInput.cvvValid(cvv)) {{Text("3 o 4 dígitos.")}} else null)
                }
                Text("Boletera no guarda estos datos. El CVV se borra del formulario al continuar.",style=MaterialTheme.typography.bodySmall,color=Muted)
            }
            if(providerError) Notice("No se pudo avanzar. Revisá la página original para ver la respuesta de Sistarbanc antes de intentar de nuevo.")
            verification()
        }
        Surface(color=Paper,shadowElevation=6.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp)) {
                Primary(if(busy)"Procesando…" else "Continuar",canContinue&&!busy&&!expandedChallenge) {
                    attempted=true
                    if(CardInput.panValid(pan)&&CardInput.expiryValid(expiry)&&CardInput.cvvValid(cvv)) {
                        autofill?.cancel();keyboard?.hide();focus.clearFocus()
                        onSubmit(pan,expiry.take(2)+"/"+expiry.takeLast(2),cvv)
                        cvv="";attempted=false
                    }
                }
                TextButton(onClick={autofill?.cancel();onOriginal()},modifier=Modifier.fillMaxWidth()) { Text("Ver página original") }
            }
        }
    }
}
