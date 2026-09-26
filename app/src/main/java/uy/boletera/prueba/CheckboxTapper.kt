package uy.boletera.prueba

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.webkit.WebView
import org.json.JSONObject
import org.json.JSONTokener

/** One attempt per stage. Touching the original page gives control to the person for this journey. */
internal class CheckboxTapper(private val web: WebView, private val allowed: (String) -> Boolean) {
    private var generation=0L
    private var pending: Long?=null
    private var userTouched=false
    private var dispatching=false
    private val attempted=mutableSetOf<String>()
    var attempts=0
        private set
    fun userTouched() {if(!dispatching){userTouched=true;cancel()}}
    fun cancel() {generation++;pending=null}
    fun reset() {cancel();attempted.clear();attempts=0;userTouched=false}
    private fun ready(stage: String) = !userTouched && allowed(stage) && web.isAttachedToWindow && web.isShown &&
        web.alpha>0f && web.isEnabled && web.hasWindowFocus() && !web.isLayoutRequested

    fun request(stage: String) {
        if(stage !in setOf("payer","card") || stage in attempted || pending!=null || !ready(stage))return
        val version=++generation
        pending=version
        web.postVisualStateCallback(version,object:WebView.VisualStateCallback() {
            override fun onComplete(requestId:Long) {
                if(version!=generation || !ready(stage)) {if(pending==version)pending=null;return}
                web.evaluateJavascript("JSON.stringify(window.BoleteraCheckbox?.claim(${JSONObject.quote(stage)})??null)") {raw->
                    if(pending==version)pending=null
                    if(version!=generation || !ready(stage))return@evaluateJavascript
                    val point=try {(JSONTokener(raw).nextValue() as? String)?.let(::JSONObject)}catch(_:Exception){null}
                    if(point==null)return@evaluateJavascript
                    attempted.add(stage)
                    val viewport=point.optDouble("viewport",0.0)
                    val cssX=point.optDouble("x",Double.NaN);val cssY=point.optDouble("y",Double.NaN)
                    if(!viewport.isFinite() || viewport<=0 || !cssX.isFinite() || !cssY.isFinite())return@evaluateJavascript
                    val x=(cssX*web.width/viewport).toFloat();val y=(cssY*web.width/viewport).toFloat()
                    val visible=Rect()
                    if(!web.getLocalVisibleRect(visible) || !visible.contains(x.toInt(),y.toInt()))return@evaluateJavascript
                    val time=SystemClock.uptimeMillis()
                    dispatching=true
                    try {
                        MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,x,y,0).also {web.dispatchTouchEvent(it);it.recycle()}
                        MotionEvent.obtain(time,time+60,MotionEvent.ACTION_UP,x,y,0).also {web.dispatchTouchEvent(it);it.recycle()}
                        attempts++
                    } finally {dispatching=false}
                }
            }
        })
    }
}
