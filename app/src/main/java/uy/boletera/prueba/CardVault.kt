package uy.boletera.prueba

import android.content.Context
import java.security.KeyStore

/**
 * Lab 17 and earlier kept a card for Carga Express in the phone (prefs «card_vault», key «boletera.card.v1»). From Lab 18
 * Boletera keeps no card at all (the owner, 17/10: always Google's autofill); whatever was left is erased on start.
 */
internal object OldCardVault {
    fun erase(context: Context) {
        context.getSharedPreferences("card_vault", Context.MODE_PRIVATE).edit().clear().commit()
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias("boletera.card.v1")) deleteEntry("boletera.card.v1") } }
    }
}
