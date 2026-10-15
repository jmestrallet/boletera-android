package uy.boletera.prueba

import kotlinx.coroutines.launch

import androidx.compose.ui.layout.boundsInRoot

import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.animation.core.Animatable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

/** Lab bar for screens other than the home: optional back, title, optional settings. */
@Composable internal fun LabTopBar(title: String? = null, onBack: (() -> Unit)? = null, onSettings: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) LabCircle(onBack) { AppGlyph(Glyph.Back, Modifier.size(22.dp), tint = LabColors.On, label = "Volver") }
        Text(title.orEmpty(), style = MaterialTheme.typography.titleLarge, color = LabColors.On, modifier = Modifier.weight(1f))
        if (onSettings != null) LabCircle(onSettings) { AppGlyph(Glyph.Settings, Modifier.size(22.dp), tint = LabColors.On, label = "Configuración") }
    }
}

@Composable private fun LabCircle(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).background(LabColors.GlassTint).border(1.dp, LabColors.GlassLine, RoundedCornerShape(22.dp))
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

@Composable internal fun LabPrimary(label: String, enabled: Boolean = true, onClick: () -> Unit, glyph: Glyph? = null) {
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp)).background(if (enabled) LabColors.Primary else LabColors.Primary.copy(alpha = 0.38f))
        .clickable(enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (glyph != null) { AppGlyph(glyph, Modifier.size(22.dp), tint = LabColors.OnPrimary); Spacer(Modifier.width(10.dp)) }
        Text(label, style = MaterialTheme.typography.titleMedium, color = LabColors.OnPrimary)
    }
}

@Composable internal fun LabTextLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) =
    TextButton(onClick = onClick, modifier = modifier) { Text(label, color = LabColors.Primary, style = MaterialTheme.typography.titleSmall) }

/** Sign-in for Lab: fingerprint first when saved, otherwise the gub.uy fields marked for Android autofill. */
/** The app opening plays once per process: the card travels from the launcher icon into place. */
internal object LabLaunch {
    var openingPlayed = false
    /** Uptime until which the opening runs; heavy work (STM page, fingerprint prompt) waits for it. */
    var openingUntil = 0L
}

/** Staggered fade-and-rise for the sign-in content while the card lands; already settled when not opening. */
@Composable private fun introStates(count: Int, opening: Boolean): List<State<Float>> {
    val items = remember { List(count) { Animatable(if (opening) 0f else 1f) } }
    LaunchedEffect(opening) {
        if (opening) items.forEachIndexed { i, a -> launch { kotlinx.coroutines.delay(550L + i * 70L); a.animateTo(1f, spring(dampingRatio = .7f, stiffness = 260f)) } }
    }
    return items.map { it.asState() }
}
private fun Modifier.intro(v: State<Float>) = graphicsLayer { alpha = v.value.coerceIn(0f, 1f); translationY = (1 - v.value) * 16.dp.toPx() }

/** Sign-in for Lab: the card is the progress indicator; fingerprint first when saved, otherwise the gub.uy fields marked for Android autofill. */
@Composable internal fun LabWelcome(state: UiState, savedAccess: Boolean, manual: Boolean, onManual: () -> Unit,
    document: String, onDocument: (String) -> Unit, password: String, onPassword: (String) -> Unit,
    reveal: Boolean, onReveal: () -> Unit, save: Boolean, onSave: (Boolean) -> Unit, canSave: Boolean,
    authenticating: Boolean, onUnlock: () -> Unit, onSubmit: () -> Unit, onAccessGuide: () -> Unit,
    heroPhase: HeroPhase = HeroPhase.IDLE, opening: Boolean = false, errorText: String = "", onOpenSite: (() -> Unit)? = null) {
    val type = MaterialTheme.typography
    val view = androidx.compose.ui.platform.LocalView.current
    var fromY by remember { mutableFloatStateOf(0f) }
    val intro = introStates(6, opening)
    val working = heroPhase == HeroPhase.LOADING || heroPhase == HeroPhase.SUCCESS
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val heroHeight = if (savedAccess && !manual) 260.dp else 190.dp
        LabCardHero(heroPhase, Modifier.fillMaxWidth().height(heroHeight).onGloballyPositioned {
            val r = it.boundsInRoot(); fromY = view.height / 2f - r.center.y }, opening = opening, openingFromY = fromY)
        Text(if (state.sessionExpired) "Tu sesión venció" else "Entrá a tu STM", style = type.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp), color = LabColors.On,
            modifier = Modifier.intro(intro[0]))
        Text(when {
            heroPhase == HeroPhase.LOADING -> if (authenticating) "Confirmá con tu huella." else "Conectando con STM…"
            heroPhase == HeroPhase.SUCCESS -> "Listo."
            errorText.isNotBlank() -> errorText
            heroPhase == HeroPhase.ERROR -> "No pudimos entrar. Probá de nuevo." 
            state.sessionExpired && savedAccess -> "STM cierra el acceso después de un rato. Confirmá tu huella y volvés a donde estabas."
            state.sessionExpired -> "STM cierra el acceso después de un rato. Ingresá de nuevo para seguir."
            else -> "Con tu Usuario gub.uy."
        }, style = type.bodyLarge, color = if (heroPhase == HeroPhase.ERROR || (errorText.isNotBlank() && !working)) Color(0xFFFFB4AB) else LabColors.Muted,
            modifier = Modifier.intro(intro[1]).semantics { liveRegion = LiveRegionMode.Polite })
        // When something failed, the official site is one tap away for anyone in a hurry.
        if (errorText.isNotBlank() && !working && onOpenSite != null) LabTextLink("Probar en el sitio de STM ↗", onOpenSite)
        if (state.sessionExpired && state.paymentNeedsReview) Text("Si ya habías autorizado un pago, revisamos el saldo al entrar. No se repite la recarga.", style = type.bodyMedium, color = LabColors.Faint)
        if (savedAccess && !manual) {
            Box(Modifier.intro(intro[2]).padding(top = 18.dp)) {
                LabPrimary(if (authenticating) "Esperando tu huella…" else if (working) "Entrando…" else "Entrar con huella", !authenticating && !state.busy && !working, onUnlock, Glyph.Fingerprint)
            }
            LabTextLink("Ingresar con contraseña", onManual, Modifier.align(Alignment.CenterHorizontally).intro(intro[3]))
        } else {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(document, onDocument, label = { Text("Documento uruguayo") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username })
            OutlinedTextField(password, onPassword, label = { Text("Contraseña de gub.uy") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { IconButton(onClick = onReveal) { AppGlyph(if (reveal) Glyph.EyeOff else Glyph.Eye, label = if (reveal) "Ocultar contraseña" else "Mostrar contraseña", tint = LabColors.Muted) } },
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password })
            if (canSave) Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Guardar acceso con huella", style = type.bodyLarge, color = LabColors.On, modifier = Modifier.weight(1f))
                Switch(checked = save, onCheckedChange = onSave)
            }
            LabPrimary(if (authenticating) "Esperando tu huella…" else if (save) "Guardar e ingresar" else "Ingresar",
                document.length == 8 && password.isNotBlank() && !authenticating && !state.busy && !working, onSubmit)
        }
        Row(Modifier.intro(intro[4]).padding(top = 8.dp, start = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppGlyph(Glyph.Lock, Modifier.size(16.dp), tint = LabColors.Faint)
            Text("Proyecto independiente y no oficial de STM", style = type.bodySmall, color = LabColors.Faint)
        }
        LabTextLink("¿Es tu primer ingreso a STM?", onAccessGuide, Modifier.intro(intro[5]))
    }
}

/** Card picker for Lab: one tappable glass row per card, the current one marked. */
@Composable internal fun LabCards(state: UiState, onChoose: (String) -> Unit) {
    val type = MaterialTheme.typography
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Elegí tu boletera", style = type.headlineLarge.copy(fontSize = 30.sp), color = LabColors.On, modifier = Modifier.padding(top = 8.dp))
        Text("Recordamos tu elección para la próxima.", style = type.bodyLarge, color = LabColors.Muted, modifier = Modifier.padding(bottom = 16.dp))
        state.cards.forEachIndexed { index, card ->
            val current = card.id == state.selectedCard
            val shape = RoundedCornerShape(topStart = if (index == 0) 24.dp else 6.dp, topEnd = if (index == 0) 24.dp else 6.dp,
                bottomStart = if (index == state.cards.lastIndex) 24.dp else 6.dp, bottomEnd = if (index == state.cards.lastIndex) 24.dp else 6.dp)
            Row(Modifier.fillMaxWidth().clip(shape).background(if (current) LabColors.Primary.copy(alpha = 0.16f) else LabColors.GlassTint)
                .clickable(enabled = card.active && !state.busy) { onChoose(card.id) }.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(44.dp).background(if (card.active) LabColors.Primary else LabColors.Faint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    AppGlyph(Glyph.Card, Modifier.size(22.dp), tint = LabColors.OnPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text("STM ···· ${card.id.takeLast(4)}", style = type.titleMedium, color = if (card.active) LabColors.On else LabColors.Faint)
                    Text(if (card.active) card.status else "${card.status} · no se puede usar", style = type.bodyMedium, color = LabColors.Muted)
                }
                if (current) AppGlyph(Glyph.Check, Modifier.size(22.dp), tint = LabColors.Primary, label = "Boletera actual")
            }
        }
        if (state.cards.isEmpty()) Text("Todavía no pudimos leer las boleteras.", color = LabColors.Muted)
    }
}

/**
 * The Recargar pill grows into the amount panel (and shrinks back), instead of a separate sheet sliding up.
 * [open] drives one spring; the panel shares the pill's colour, position and corner radius at both ends.
 */
@Composable internal fun LabAmountPanel(state: UiState, open: Boolean, onClose: () -> Unit, onChoose: (Long) -> Unit, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (open) 1f else 0f, spring(dampingRatio = 0.78f, stiffness = 260f), label = "amount morph")
    if (progress < 0.01f && !open) return
    val type = MaterialTheme.typography
    val haptic = LocalHapticFeedback.current
    var custom by remember(open) { mutableStateOf(false) }
    var text by remember(open) { mutableStateOf("") }
    var selected by remember(open, state.minimum) { mutableStateOf(state.minimum) }
    val amount = if (custom) Amounts.parse(text) else selected
    val customFocus = remember { FocusRequester() }
    // "Otro importe" opens the keyboard straight away.
    LaunchedEffect(custom) { if (custom) { kotlinx.coroutines.delay(60); runCatching { customFocus.requestFocus() } } }
    Box(modifier.fillMaxSize()) {
        // Scrim fades in with the morph; tapping it closes.
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.coerceIn(0f, 1f) }.background(Color(0x99040609)).clickable(enabled = open, onClick = onClose))
        // Rises with the keyboard so the custom amount stays visible while typing.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(horizontal = lerp(24.dp, 8.dp, progress.coerceIn(0f, 1f)), vertical = lerp(36.dp, 8.dp, progress.coerceIn(0f, 1f)))
            .clip(RoundedCornerShape(lerp(32.dp, 32.dp, progress)))
            .background(lerpColor(LabColors.Primary, Color(0xFF151B27), progress.coerceIn(0f, 1f)))
            .heightIn(min = 64.dp).animateContentSize()) {
            AnimatedVisibility(open, enter = fadeIn(tween(220, delayMillis = 90)), exit = fadeOut(tween(90))) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("¿Cuánto cargás?", style = type.headlineMedium, color = LabColors.On)
                    Text("STM ···· ${state.selectedCard?.takeLast(4).orEmpty()} · mínimo ${Amounts.format(state.minimum)}", style = type.bodyMedium, color = LabColors.Muted)
                    Spacer(Modifier.height(6.dp))
                    val choices = (listOfNotNull(state.minimum) + listOf(50000L, 100000L).filter { it > (state.minimum ?: Long.MAX_VALUE) }).distinct().take(3)
                    choices.forEachIndexed { i, value ->
                        val active = !custom && selected == value
                        val radius by animateDpAsState(if (active) 32.dp else 18.dp, spring(dampingRatio = 0.65f, stiffness = 500f), label = "choice shape")
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(radius))
                            .background(if (active) LabColors.Primary.copy(alpha = 0.18f) else LabColors.GlassTint)
                            .border(if (active) 2.dp else 0.dp, if (active) LabColors.Primary else Color.Transparent, RoundedCornerShape(radius))
                            .clickable { if (!active) { custom = false; selected = value; haptic.performHapticFeedback(HapticFeedbackType.ContextClick) } }
                            .padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(Amounts.format(value), style = type.titleLarge, color = LabColors.On)
                                val after = state.balance?.let { it + value }
                                Text(listOfNotNull(if (i == 0) "Mínimo" else null, after?.let { "te quedan ${Amounts.format(it)}" }).joinToString(" · "),
                                    style = type.bodySmall, color = LabColors.Muted)
                            }
                            if (active) AppGlyph(Glyph.Check, Modifier.size(22.dp), tint = LabColors.Primary)
                        }
                    }
                    if (custom) OutlinedTextField(text, { text = it }, label = { Text("Importe en pesos") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().focusRequester(customFocus),
                        isError = text.isNotEmpty() && !Amounts.valid(amount, state.minimum),
                        supportingText = { if (text.isNotEmpty() && !Amounts.valid(amount, state.minimum)) Text("Ingresá al menos ${Amounts.format(state.minimum)}.") })
                    LabTextLink(if (custom) "Usar un importe sugerido" else "Otro importe", { custom = !custom })
                    LabPrimary("Continuar", Amounts.valid(amount, state.minimum) && !state.busy, onClick = { amount?.let(onChoose) })
                }
            }
            if (!open) Box(Modifier.fillMaxWidth().height(64.dp))
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t)
