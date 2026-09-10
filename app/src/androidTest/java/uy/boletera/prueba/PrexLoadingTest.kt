package uy.boletera.prueba

import android.webkit.*
import androidx.activity.compose.setContent
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Delayed, bright-magenta provider fixture. Every request is answered locally. */
@RunWith(AndroidJUnit4::class)
class PrexLoadingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun loadingPageNeverFlashesBeforeNativeSummary() {
        lateinit var payment: EmbeddedPrexPayment
        val loaded = CountDownLatch(1)
        compose.runOnIdle {
            payment=EmbeddedPrexPayment(compose.activity)
            val delegate=payment.web.webViewClient
            payment.web.webViewClient=object:WebViewClient() {
                override fun shouldInterceptRequest(view:WebView, request:WebResourceRequest):WebResourceResponse {
                    val html="""<meta name="viewport" content="width=device-width,initial-scale=1"><style>html,body{margin:0;background:#ff00ff;height:100%}stepper-pago,confirmar-pago{display:block}</style><stepper-pago>PROVIDER LOADING FIXTURE</stepper-pago><script>setTimeout(()=>{document.querySelector('stepper-pago').innerHTML='<confirmar-pago><div><b>Total:</b><p>260</p></div><button type="button">Continuar</button></confirmar-pago>'},3500)</script>"""
                    return WebResourceResponse("text/html","UTF-8",html.byteInputStream())
                }
                override fun onPageStarted(view:WebView,url:String?,icon:android.graphics.Bitmap?)=delegate.onPageStarted(view,url,icon)
                override fun onPageFinished(view:WebView,url:String?) {delegate.onPageFinished(view,url);loaded.countDown()}
            }
            compose.activity.setContent { EmbeddedPrexScreen(payment,null,{}) }
            assertTrue(payment.open("https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago?id=SYNTHETIC-LOADING"))
        }
        try {
            assertTrue(loaded.await(10,TimeUnit.SECONDS))
            repeat(6) {
                android.os.SystemClock.sleep(350)
                compose.runOnIdle { assertNotEquals("Must not expose the intermediate website", "original",payment.nativeStage) }
                val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                try {
                    var magenta=0
                    for(y in 0 until bitmap.height step 8) for(x in 0 until bitmap.width step 8) {
                        val pixel=bitmap.getPixel(x,y)
                        if(android.graphics.Color.red(pixel)>240 && android.graphics.Color.blue(pixel)>240 && android.graphics.Color.green(pixel)<20) magenta++
                    }
                    assertEquals("Provider background flashed on screen",0,magenta)
                } finally {bitmap.recycle()}
            }
            compose.waitUntil(8000) {payment.nativeStage=="summary"}
            compose.onNodeWithText("Continuar a los datos").assertIsDisplayed()
            android.os.SystemClock.sleep(500)
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            java.io.File(compose.activity.getExternalFilesDir(null),"prex-summary-compact.png").outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            screenshot.recycle()
        } finally {compose.runOnIdle {payment.destroy()}}
    }
}
