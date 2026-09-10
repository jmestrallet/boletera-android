package uy.boletera.prueba

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

internal enum class PulseResult { Requested, Disabled, Unavailable, Failed }

/** App-controlled foreground vibration; no privileged flags or system-setting writes. */
internal class AppHaptics(private val context:Context):HapticFeedback {
    private val preferences=context.getSharedPreferences("vibration",Context.MODE_PRIVATE)
    var enabled:Boolean
        get()=preferences.getBoolean("enabled",true)
        set(value) {preferences.edit().putBoolean("enabled",value).apply()}
    private val vibrator=context.getSystemService(Vibrator::class.java)
    private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    override fun performHapticFeedback(hapticFeedbackType:HapticFeedbackType) { pulse(false) }
    fun testPulse()=pulse(true)
    private fun pulse(test:Boolean):PulseResult {
        if(!enabled)return PulseResult.Disabled
        if(vibrator?.hasVibrator()!=true)return PulseResult.Unavailable
        return try {
            val effect=if(test)VibrationEffect.createWaveform(longArrayOf(0,70,100,70),-1)
                else VibrationEffect.createOneShot(25,VibrationEffect.DEFAULT_AMPLITUDE)
            if(Build.VERSION.SDK_INT>=33) vibrator.vibrate(effect,VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_MEDIA).build())
            else vibrator.vibrate(effect,attributes)
            PulseResult.Requested // Request accepted is not proof that the user felt the motor.
        } catch(_:RuntimeException) {PulseResult.Failed}
    }
}
