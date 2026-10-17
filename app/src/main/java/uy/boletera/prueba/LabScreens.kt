package uy.boletera.prueba

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

import kotlinx.coroutines.launch

import androidx.compose.ui.layout.boundsInRoot

import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.animation.core.Animatable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription

/** Lab bar for screens other than the home: optional back, title, optional settings. */
/** [step] draws the payment's two dashes (card, confirmation) centred in the bar (pago.html). */
@Composable internal fun LabTopBar(title: String? = null, onBack: (() -> Unit)? = null, onSettings: (() -> Unit)? = null, step: Int? = null) {
    Box(Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onBack != null) LabCircle({ LabLog.tap("Volver (flecha)"); onBack() }) { AppGlyph(Glyph.Back, Modifier.size(22.dp), tint = LabColors.On, label = "Volver") }
            Text(title.orEmpty(), style = MaterialTheme.typography.titleLarge, color = LabColors.On, modifier = Modifier.weight(1f))
            if (onSettings != null) LabCircle({ LabLog.tap("Configuración"); onSettings() }) { AppGlyph(Glyph.Settings, Modifier.size(22.dp), tint = LabColors.On, label = "Configuración") }
        }
        if (step != null) Row(Modifier.align(Alignment.Center).semantics(mergeDescendants = true) { contentDescription = "Paso ${step + 1} de 2" },
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { Box(Modifier.size(22.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (it == step) LabColors.Primary else Color.White.copy(alpha = .18f))) }
        }
    }
}

@Composable internal fun LabCircle(onClick: () -> Unit, content: @Composable () -> Unit) {
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(Modifier.size(48.dp).labPress(press).clip(RoundedCornerShape(24.dp)).background(LabColors.GlassTint).border(1.dp, LabColors.GlassLine, RoundedCornerShape(24.dp))
        .clickable(press, indication = androidx.compose.material3.ripple(), onClick = onClick), contentAlignment = Alignment.Center) { content() }
}

/** The one main action: 64 dp on a screen, 60 dp inside a sheet ([sheet]). */
@Composable internal fun LabPrimary(label: String, enabled: Boolean = true, onClick: () -> Unit, glyph: Glyph? = null, sheet: Boolean = false,
    logLabel: String = label) {
    val haptic = LocalHapticFeedback.current
    val height = if (sheet) 60.dp else 64.dp
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    // With large text a long label wraps to two lines and the button grows; it never cuts the words.
    Row(Modifier.fillMaxWidth().heightIn(min = height).labPress(interaction).clip(RoundedCornerShape(height / 2)).background(if (enabled) LabColors.Primary else LabColors.Primary.copy(alpha = 0.38f))
        .clickable(interaction, indication = androidx.compose.material3.ripple(color = LabColors.OnPrimary), enabled = enabled) { LabLog.tap(logLabel); haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onClick() },
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (glyph != null) { AppGlyph(glyph, Modifier.size(22.dp), tint = LabColors.OnPrimary); Spacer(Modifier.width(10.dp)) }
        Text(label, style = MaterialTheme.typography.titleMedium.let { if (sheet) it.copy(fontSize = 17.sp) else it }, color = LabColors.OnPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/**
 * A text-only action under the main button. Blue is another way forward («Ingresar con contraseña»); [quiet] grey is
 * leaving, not going on, or something technical («Cancelar», «Volver al inicio», «Ver detalle del pago»).
 */
@Composable internal fun LabTextLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, quiet: Boolean = false, logLabel: String = label) {
    val type = MaterialTheme.typography
    Box(modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp)).clickable(role = androidx.compose.ui.semantics.Role.Button) { LabLog.tap(logLabel); onClick() }
        .padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (quiet) LabColors.Faint else LabColors.Primary,
            style = if (quiet) type.bodyMedium.copy(fontSize = 14.sp) else type.titleSmall.copy(fontSize = 15.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
    }
}

/** A link inside an explanation, small and blue, that still answers in 48 dp (it never grows to look like a button). */
@Composable internal fun LabInlineLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(label, style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        color = LabColors.Primary, textAlign = TextAlign.Center,
        modifier = modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(12.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Button) { LabLog.tap(label); onClick() }.padding(horizontal = 10.dp, vertical = 6.dp))
}

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

/**
 * Sign-in for Lab, centred: the card owns the free height above, the text sits under it, and the actions stay
 * at the same place at the bottom whatever happens. Document and password live in [LabSignInSheet].
 */
@Composable internal fun LabWelcome(state: UiState, savedAccess: Boolean, authenticating: Boolean, onUnlock: () -> Unit, onManual: () -> Unit,
    onAccessGuide: () -> Unit, heroPhase: HeroPhase = HeroPhase.IDLE, opening: Boolean = false, errorText: String = "", onOpenSite: (() -> Unit)? = null,
    onReport: (() -> Unit)? = null) {
    val type = MaterialTheme.typography
    val intro = introStates(5, opening)
    val working = heroPhase == HeroPhase.LOADING || heroPhase == HeroPhase.SUCCESS
    val failed = !working && (heroPhase == HeroPhase.ERROR || errorText.isNotBlank())
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // One card size everywhere; only the air around it changes from phone to phone.
        // The card sits a little below the middle of the free space, as in entrada.html and every other moment.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = androidx.compose.ui.BiasAlignment(0f, .35f)) {
            LabCardSlot(heroPhase, CardSpot.MOMENT, Modifier.requiredSize(LabSizes.HeroMoment), opening = opening)
        }
        // A failure is told by the title in plain words; the red stays on the card, the text stays calm.
        val problem = if (failed) labEntryProblem(errorText) else null
        // Title and line are announced together, so a change like «STM no responde» is read whole.
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(problem?.first ?: if (state.sessionExpired) "Volvé a entrar" else "Entrá a tu STM", style = type.labMoment,
                color = LabColors.On, textAlign = TextAlign.Center, modifier = Modifier.intro(intro[0]))
            val subtitle = when {
                heroPhase == HeroPhase.LOADING -> if (authenticating) "Confirmá con tu huella." else "Conectando con STM…"
                heroPhase == HeroPhase.SUCCESS -> "Listo."
                problem != null -> problem.second
                state.sessionExpired -> "STM cerró la sesión. Entrá de nuevo."
                savedAccess -> "Tu acceso está guardado en este celular."
                else -> "Con tu cuenta de gub.uy."
            }
            Text(subtitle, style = type.bodyLarge, color = LabColors.Muted, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp).intro(intro[1]))
        }
        // When STM failed, the official site is one quiet tap away: a link in the text, not another button. Its room
        // exists only then; the card above gives way, the buttons below stay put.
        if (failed && (onOpenSite != null || onReport != null)) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            if (onOpenSite != null) LabInlineLink("Probar en el sitio de STM ↗", onOpenSite)
            if (onReport != null) LabInlineLink("Enviar reporte", onReport)
        }
        Spacer(Modifier.height(32.dp))
        Box(Modifier.intro(intro[2])) {
            if (savedAccess) LabPrimary(if (authenticating) "Esperando tu huella…" else if (working) "Entrando…" else if (failed) "Volver a intentar" else "Entrar con huella",
                !authenticating && !state.busy && !working, onUnlock, Glyph.Fingerprint)
            else LabPrimary(if (working) "Entrando…" else if (failed) "Volver a intentar" else "Ingresar", !state.busy && !working, onManual)
        }
        Box(Modifier.height(52.dp).intro(intro[3]), contentAlignment = Alignment.Center) {
            if (savedAccess) LabTextLink("Ingresar con contraseña", onManual)
            // Only someone who never got in can be on a first access.
            else LabTextLink("¿Es tu primer ingreso a STM?", onAccessGuide)
        }
        Row(Modifier.padding(top = 12.dp, bottom = 12.dp).intro(intro[4]), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            AppGlyph(Glyph.Lock, Modifier.size(14.dp), tint = LabColors.Faint)
            Text("Proyecto independiente y no oficial de STM", style = type.bodySmall, color = LabColors.Faint)
        }
    }
}

internal val LabErrorText = Color(0xFFFFB4AB)

/** Short title and one calm line for a sign-in problem, from the engine's longer message. */
internal fun labEntryProblem(message: String): Pair<String, String> {
    val text = message.lowercase()
    return when {
        "no hay internet" in text -> "Sin internet" to "Cuando vuelva la conexión, probá de nuevo."
        "stm no está respondiendo" in text || "stm no respond" in text -> "STM no responde" to "Probá de nuevo en un momento."
        "cambiaron las huellas" in text -> "Cambiaron las huellas" to "Entrá con tu contraseña y la volvemos a guardar."
        "no se desbloqueó" in text -> "No se confirmó la huella" to "Probá de nuevo o entrá con tu contraseña."
        "dígitos" in text -> "Faltan datos" to "Ingresá tu cédula (sin puntos ni guion) y la contraseña."
        "no se guardó el acceso" in text -> "Entrás sin huella" to "No la pudimos guardar. Probá la próxima vez."
        message.isNotBlank() && message.length <= 80 -> "No pudimos entrar" to message
        else -> "No pudimos entrar" to "Probá de nuevo en un momento."
    }
}

/** Document and password for gub.uy, in a panel that rises from the bottom with the keyboard; the entry behind stays as it is. */
@Composable internal fun LabSignInSheet(open: Boolean, document: String, onDocument: (String) -> Unit, password: String, onPassword: (String) -> Unit,
    reveal: Boolean, onReveal: () -> Unit, save: Boolean, onSave: (Boolean) -> Unit, canSave: Boolean, enabled: Boolean,
    onClose: () -> Unit, onSubmit: () -> Unit) {
    val type = MaterialTheme.typography
    val documentFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    LaunchedEffect(open) { if (open) { kotlinx.coroutines.delay(120); runCatching { if (document.length < 8) documentFocus.requestFocus() else passwordFocus.requestFocus() } } }
    val pull = rememberLabPull(onClose, settleOnClose = false)
    LaunchedEffect(open) { if (open) pull.offset.snapTo(0f) }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(open, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = (1f - pull.offset.value.coerceAtLeast(0f) / pull.height).coerceIn(0f, 1f) }
                .background(Color(0x9E040609)).clickable(onClick = onClose))
        }
        AnimatedVisibility(open, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = .82f, stiffness = 380f)) { it } + fadeIn(tween(120)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180))) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)).padding(8.dp)
                // Pulled down it follows the finger and closes; pulled up it stretches (LabPull).
                .onSizeChanged { pull.height = it.height.toFloat().coerceAtLeast(1f) }
                .graphicsLayer { translationY = pull.offset.value }
                .nestedScroll(pull.connection)
                .clip(RoundedCornerShape(32.dp)).background(Color(0xFF151B27)).border(1.dp, LabColors.GlassLine, RoundedCornerShape(32.dp))
                .verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 24.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .22f)))
                Text("Tu cuenta de gub.uy", style = type.labSheet, color = LabColors.On)
                Text("La misma que usás para los trámites del Estado.", style = type.bodyMedium, color = LabColors.Muted, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
                OutlinedTextField(document, onDocument, label = { Text("Documento") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).focusRequester(documentFocus).semantics { contentType = ContentType.Username })
                OutlinedTextField(password, onPassword, label = { Text("Contraseña") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { if (enabled && document.length in 7..8 && password.isNotBlank()) onSubmit() }),
                    trailingIcon = { IconButton(onClick = onReveal) { AppGlyph(if (reveal) Glyph.EyeOff else Glyph.Eye, label = if (reveal) "Ocultar contraseña" else "Mostrar contraseña", tint = LabColors.Muted) } },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).focusRequester(passwordFocus).semantics { contentType = ContentType.Password })
                if (canSave) Row(Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(16.dp))
                    .toggleable(save, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onSave).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Entrar con huella la próxima vez", style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.On)
                        Text("Se guarda cifrado en este celular.", style = type.bodyMedium, color = LabColors.Muted)
                    }
                    Switch(checked = save, onCheckedChange = null)
                }
                // Many people never typed this password: Chrome or Google keeps it for the gub.uy site. It can be looked up there.
                val context = androidx.compose.ui.platform.LocalContext.current
                LabInlineLink("¿Google te guarda la contraseña? Verla ↗", {
                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://passwords.google.com/?q=gub.uy"))) }
                }, Modifier.padding(top = 10.dp))
                Spacer(Modifier.height(18.dp))
                LabPrimary("Ingresar", enabled && document.length in 7..8 && password.isNotBlank(), onSubmit, sheet = true)
            }
        }
    }
}

/** Names people give their cards («Mía», «Mamá»), kept only on this phone, so the right card is recharged. */
internal class LabCardNames(context: android.content.Context) {
    private val store = context.getSharedPreferences("lab_card_names", android.content.Context.MODE_PRIVATE)
    fun get(id: String?): String? = id?.let { store.getString(it, null) }?.takeIf { it.isNotBlank() }
    fun set(id: String, name: String) { store.edit().apply { if (name.isBlank()) remove(id) else putString(id, name.trim().take(24)) }.apply() }
}
@Composable internal fun rememberLabCardNames(): LabCardNames { val context = androidx.compose.ui.platform.LocalContext.current; return remember { LabCardNames(context) } }

/** One card: the mini card, its name (or number) and a line; the chosen one painted whole; one that cannot be used dimmed. */
@Composable private fun LabCardRow(card: CardInfo, name: String?, chosen: Boolean, onClick: (() -> Unit)?, onEdit: (() -> Unit)? = null) {
    val type = MaterialTheme.typography
    val number = "STM ···· ${card.id.takeLast(4)}"
    Row(Modifier.padding(bottom = 8.dp).fillMaxWidth().height(76.dp).clip(RoundedCornerShape(22.dp))
        .background(if (chosen) LabColors.Primary else Color.White.copy(alpha = .06f))
        .graphicsLayer { alpha = if (card.active) 1f else .5f }
        .then(if (onClick != null && card.active) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier)
        .semantics(mergeDescendants = true) { selected = chosen }.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LabMiniCard(Modifier.size(40.dp), dim = !card.active)
        Column(Modifier.weight(1f)) {
            Text(name ?: number, style = type.titleMedium.copy(fontSize = 17.sp), color = if (chosen) LabColors.OnPrimary else LabColors.On)
            Text(if (name != null) number else if (card.active) "Operativa" else "No operativa", style = type.bodyMedium.copy(fontSize = 13.5.sp),
                color = if (chosen) LabColors.OnPrimary.copy(alpha = .7f) else LabColors.Muted)
        }
        if (onEdit != null) Text("Editar", style = type.titleSmall.copy(fontSize = 14.sp), color = LabColors.OnPrimary.copy(alpha = .75f),
            modifier = Modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onEdit).padding(horizontal = 6.dp))
    }
}

/**
 * The first time an account has two or more cards (faltantes.html): «¿Qué boletera usás?». Tapping a row chooses it.
 * One that cannot be used shows dimmed and says so, without STM's reason.
 */
@Composable internal fun LabCards(state: UiState, onChoose: (String) -> Unit) {
    val type = MaterialTheme.typography
    val names = rememberLabCardNames()
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text("¿Qué boletera usás?", style = type.labSheet, color = LabColors.On, modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() })
        Text("Después la cambiás desde el inicio.", style = type.bodyLarge.copy(fontSize = 15.sp), color = LabColors.Muted, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 22.dp))
        state.cards.forEach { card -> LabCardRow(card, names.get(card.id), chosen = false, onClick = { if (!state.busy) onChoose(card.id) }) }
        if (state.cards.isEmpty()) Text("Todavía no pudimos leer las boleteras.", color = LabColors.Muted)
    }
}

/** A Lab sheet: dark, rounded, with its handle; it never climbs over the status bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LabSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    // The sheet's surface is drawn here (not by Material) so it can stretch when pulled up; Material still closes it
    // when pulled down.
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), tonalElevation = 0.dp, dragHandle = null,
        contentWindowInsets = { WindowInsets(0) }) {
        Column(Modifier.fillMaxWidth().labStretchUp(Color(0xFF151B27)).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Color(0xFF151B27))
            .navigationBarsPadding()) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 18.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .22f)))
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp), content = content)
        }
    }
}

/** «Tus boleteras» over the home (boleteras.html), from the list already read: the chosen one painted, «Editar» names it. */
@Composable internal fun LabCardsSheet(state: UiState, onChoose: (String) -> Unit, onEdit: (String) -> Unit, onClose: () -> Unit) {
    val names = rememberLabCardNames()
    LabSheet(onClose) {
        Text("Tus boleteras", style = MaterialTheme.typography.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp, bottom = 16.dp).semantics { heading() })
        state.cards.forEach { card ->
            val chosen = card.id == state.selectedCard
            LabCardRow(card, names.get(card.id), chosen, onClick = { onChoose(card.id) }, onEdit = if (chosen) ({ onEdit(card.id) }) else null)
        }
    }
}

/** «Nombre de la boletera»: a name only this phone knows, so nobody recharges the wrong card. */
@Composable internal fun LabCardNameSheet(cardId: String, onClose: () -> Unit) {
    val names = rememberLabCardNames()
    val type = MaterialTheme.typography
    var name by remember(cardId) { mutableStateOf(names.get(cardId).orEmpty()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(150); runCatching { focus.requestFocus() } }
    LabSheet(onClose) {
        Column(Modifier.imePadding()) {
            Text("Nombre de la boletera", style = type.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp).semantics { heading() })
            Text("STM ···· ${cardId.takeLast(4)} · queda solo en este celular.", style = type.bodyMedium.copy(fontSize = 14.5.sp), color = LabColors.Muted,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 16.dp))
            OutlinedTextField(name, { name = it.take(24) }, label = { Text("Nombre") }, singleLine = true, shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { names.set(cardId, name); onClose() }),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).focusRequester(focus))
            Spacer(Modifier.height(14.dp))
            LabPrimary("Guardar", onClick = { names.set(cardId, name); onClose() }, sheet = true)
            if (names.get(cardId) != null) LabTextLink("Quitar el nombre", { names.set(cardId, ""); onClose() }, Modifier.align(Alignment.CenterHorizontally), quiet = true)
        }
    }
}

/**
 * «¿Cuánto cargás?» (inicio.html): the Recargar pill grows into this panel and shrinks back. A vertical list where the
 * chosen amount is painted whole, how much you keep on the right, «Otro importe» typed in place, and under it what you
 * pay with (Prex and the payer, tapped only to change). «Continuar con Prex» goes straight to Prex: there is no
 * «Tu recarga» step any more. Nothing is charged until «Pagar» at the end.
 */
@Composable internal fun LabAmountPanel(state: UiState, open: Boolean, onClose: () -> Unit,
    onChoose: (Long) -> Unit, modifier: Modifier = Modifier, expressShown: Boolean = true) {
    val progress by animateFloatAsState(if (open) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 380f), label = "amount morph")
    if (progress < 0.01f && !open) return
    val pull = rememberLabPull(onClose)
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress.coerceIn(0f, 1f) * (1f - pull.offset.value.coerceAtLeast(0f) / pull.height).coerceIn(0f, 1f) }
            .background(Color(0x9E040609)).clickable(enabled = open, onClick = onClose))
        // Rises with the keyboard so the typed amount stays visible.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(8.dp)
            // Pulled down it follows the finger and closes back into the button; pulled up it stretches (LabPull).
            .onSizeChanged { pull.height = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer { translationY = pull.offset.value }
            .nestedScroll(pull.connection)
            // The panel is laid out at its final size from the first frame and shown through a window shaped like the
            // Recargar pill (inside its ring, left of the bolt), which grows into the whole panel: the button becomes the
            // panel. Its blue turns dark early; the content fades in once there is room for it.
            .graphicsLayer { clip = true; shape = amountMorphShape(progress.coerceIn(0f, 1f), expressShown) }
            .drawBehind { drawRect(lerpColor(LabColors.Primary, Color(0xFF151B27), (progress / .3f).coerceIn(0f, 1f))) }
            .border(1.dp, LabColors.GlassLine.copy(alpha = LabColors.GlassLine.alpha * ((progress - .7f) / .3f).coerceIn(0f, 1f)), RoundedCornerShape(32.dp))
            .animateContentSize()) {
            Box(Modifier.graphicsLayer { alpha = ((progress - .35f) / .5f).coerceIn(0f, 1f) }) {
                LabAmountChoices(state, open, onChoose)
            }
        }
    }
}

/** What the amount panel holds: the amounts, «Otro importe» and Continuar (kept apart from the panel's motion). */
@Composable private fun LabAmountChoices(state: UiState, open: Boolean, onChoose: (Long) -> Unit) {
    val type = MaterialTheme.typography
    val haptic = LocalHapticFeedback.current
    var custom by remember(open) { mutableStateOf(false) }
    var text by remember(open) { mutableStateOf("") }
    var chosen by remember(open, state.minimum) { mutableStateOf(state.minimum) }
    val amount = if (custom) Amounts.parse(text) else chosen
    val valid = Amounts.valid(amount, state.minimum)
    val customFocus = remember { FocusRequester() }
    LaunchedEffect(custom) { if (custom) { kotlinx.coroutines.delay(60); runCatching { customFocus.requestFocus() } } }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .22f)))
        Text("¿Cuánto cargás?", style = type.labSheet, color = LabColors.On, modifier = Modifier.padding(start = 8.dp).semantics { heading() })
        // Today the only way to pay (owner, 17/10): said once, quietly, where the recharge starts.
        Text("Por ahora, solo con tarjeta Prex.", style = type.bodyMedium.copy(fontSize = 14.sp), color = LabColors.Muted,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 16.dp))
        val choices = (listOfNotNull(state.minimum) + listOf(50000L, 100000L).filter { it > (state.minimum ?: Long.MAX_VALUE) }).distinct().take(3)
        choices.forEachIndexed { i, value ->
            val active = !custom && chosen == value
            LabAmountRow(value, state.balance, active, if (i == 0) "Mínimo" else null, Modifier.labEnter(i, base = 80)) {
                LabLog.tap("monto ${Amounts.format(value)}"); if (!active) { custom = false; chosen = value; haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) }
            }
        }
        if (custom) {
            // The typed amount becomes the chosen one and is painted the same; under the minimum it stays unpainted and says why.
            val typed = Amounts.parse(text)
            Row(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(20.dp))
                .background(if (valid) LabColors.Primary else Color.Transparent)
                .border(if (valid) 0.dp else 2.dp, if (valid) Color.Transparent else LabColors.Primary, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                val ink = if (valid) LabColors.OnPrimary else LabColors.On
                Text("$", style = type.bodyMedium.copy(fontSize = 15.sp), color = ink.copy(alpha = .7f), modifier = Modifier.padding(end = 3.dp).offset(y = (-6).dp))
                androidx.compose.foundation.text.BasicTextField(text, { text = it.filter { c -> c.isDigit() }.take(6) }, singleLine = true,
                    textStyle = type.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = ink),
                    cursorBrush = SolidColor(ink),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).focusRequester(customFocus).semantics { contentDescription = "Otro importe en pesos" })
                val after = typed?.let { a -> state.balance?.let { it + a } }
                if (after != null) Column(horizontalAlignment = Alignment.End) {
                    Text("Quedás con", style = type.bodySmall.copy(fontSize = 12.5.sp), color = ink.copy(alpha = .7f))
                    Text(Amounts.format(after), style = type.bodyMedium.copy(fontSize = 15.sp), color = ink)
                }
            }
            if (text.isNotEmpty() && !valid) Text("El mínimo es ${Amounts.format(state.minimum)}", style = type.bodySmall.copy(fontSize = 13.sp),
                color = LabErrorText, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
        } else Row(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(20.dp)).border(1.5.dp, LabColors.GlassLine, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button) { LabLog.tap("Otro importe"); custom = true }.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = null, tint = LabColors.Primary, modifier = Modifier.size(22.dp))
            Text("Otro importe", style = type.titleMedium.copy(fontSize = 16.sp), color = LabColors.On)
        }
        Spacer(Modifier.height(14.dp))
        // Whose card it is (only with two or more people) and the card itself come after (Lab 18).
        LabPrimary("Continuar", valid && !state.busy, onClick = { amount?.let(onChoose) }, sheet = true)
    }
}

/** The window of the amount panel at [p]: the Recargar pill at 0, the whole panel at 1 (sizes match LabHomeChrome). */
private fun amountMorphShape(p: Float, expressShown: Boolean) = object : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val dp = density.density
        // The pill sits 22 dp from the bottom and the left (16 dp bar + 6 dp ring), left of the bolt (76 dp + 12 dp gap);
        // the panel itself has 8 dp around it.
        val bottom0 = size.height - 14f * dp; val top0 = bottom0 - 64f * dp
        val left0 = 14f * dp; val right0 = size.width - (if (expressShown) 102f else 14f) * dp
        fun mix(a: Float, b: Float) = a + (b - a) * p
        return androidx.compose.ui.graphics.Outline.Rounded(androidx.compose.ui.geometry.RoundRect(mix(left0, 0f), mix(top0, 0f), mix(right0, size.width),
            mix(bottom0, size.height), androidx.compose.ui.geometry.CornerRadius(32f * dp)))
    }
}

/** One amount: painted whole when chosen; «Quedás con» on the right (not «te quedan», which read like what is left to pay). */
@Composable private fun LabAmountRow(value: Long, balance: Long?, active: Boolean, tag: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val type = MaterialTheme.typography
    // The blue drains out of the old row and fills the new one from where the finger landed (movimiento.html v2).
    val fill by animateFloatAsState(if (active) 1f else 0f, tween(if (active) 300 else 200, easing = LabEmphasized), label = "row fill")
    var tapX by remember { mutableFloatStateOf(-1f) }
    val ink by androidx.compose.animation.animateColorAsState(if (active) LabColors.OnPrimary else LabColors.On, tween(220), label = "row ink")
    val soft by androidx.compose.animation.animateColorAsState(if (active) LabColors.OnPrimary.copy(alpha = .7f) else LabColors.Muted, tween(220), label = "row soft")
    Row(modifier.padding(bottom = 8.dp).fillMaxWidth().height(66.dp).clip(RoundedCornerShape(20.dp))
        .background(Color.White.copy(alpha = .06f))
        .pointerInput(Unit) { awaitEachGesture { tapX = awaitFirstDown(requireUnconsumed = false).position.x } }
        .drawBehind {
            if (fill <= 0f) return@drawBehind
            if (active) {
                val x = if (tapX >= 0f) tapX else size.width * .3f
                val reach = maxOf(x, size.width - x) * 1.1f
                drawCircle(LabColors.Primary, reach * fill, Offset(x, size.height / 2))
            } else drawRect(LabColors.Primary.copy(alpha = fill))
        }
        .clickable(role = Role.RadioButton, onClick = onClick).semantics { selected = active }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text("$", style = type.bodyMedium.copy(fontSize = 15.sp), color = soft, modifier = Modifier.padding(end = 3.dp, top = 2.dp))
            Text(Amounts.format(value).removePrefix("$ "), style = type.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp), color = ink)
        }
        if (tag != null) Text(tag, style = type.labelMedium.copy(fontSize = 12.sp), color = soft,
            modifier = Modifier.border(1.dp, if (active) LabColors.OnPrimary.copy(alpha = .25f) else LabColors.GlassLine, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        Spacer(Modifier.weight(1f))
        if (balance != null) Column(horizontalAlignment = Alignment.End) {
            Text("Quedás con", style = type.bodySmall.copy(fontSize = 12.5.sp), color = soft)
            Text(Amounts.format(balance + value), style = type.bodyMedium.copy(fontSize = 15.sp), color = ink)
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t)
