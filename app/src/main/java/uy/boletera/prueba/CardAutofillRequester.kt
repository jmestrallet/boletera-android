package uy.boletera.prueba

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requestAutofill
import androidx.compose.ui.platform.InspectorInfo

/** Requests the card's virtual field, not the Compose host view. Contains no card values. */
internal class CardAutofillRequester {
    private var target: CardAutofillNode? = null
    fun request() { target?.takeIf { it.isAttached }?.requestAutofill() }
    internal fun attach(node: CardAutofillNode) { target = node }
    internal fun detach(node: CardAutofillNode) { if (target === node) target = null }
}

internal fun Modifier.cardAutofill(requester: CardAutofillRequester): Modifier =
    this.then(CardAutofillElement(requester))

private data class CardAutofillElement(val requester: CardAutofillRequester) : ModifierNodeElement<CardAutofillNode>() {
    override fun create() = CardAutofillNode(requester)
    override fun update(node: CardAutofillNode) { node.update(requester) }
    override fun InspectorInfo.inspectableProperties() { name = "cardAutofill" }
}

internal class CardAutofillNode(private var requester: CardAutofillRequester) : Modifier.Node() {
    override fun onAttach() { requester.attach(this) }
    override fun onDetach() { requester.detach(this) }
    fun update(next: CardAutofillRequester) {
        requester.detach(this)
        requester = next
        if (isAttached) requester.attach(this)
    }
}
