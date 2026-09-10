package uy.boletera.prueba

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

internal enum class PulseResult { Requested, Disabled, Unavailable, Failed }

/** Foreground touch effects. Never overrides Android's touch-feedback preference. */
internal class AppHaptics(private val context:Context):HapticFeedback {
    private val vibrator=context.getSystemService(Vibrator::class.java)
    private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    override fun performHapticFeedback(hapticFeedbackType:HapticFeedbackType) { pulse(false) }
    fun testPulse()=pulse(true)
    private fun pulse(test:Boolean):PulseResult {
        if(Settings.System.getInt(context.contentResolver,Settings.System.HAPTIC_FEEDBACK_ENABLED,1)==0)return PulseResult.Disabled
        if(vibrator?.hasVibrator()!=true)return PulseResult.Unavailable
        return try {
            val effect=if(Build.VERSION.SDK_INT>=29)VibrationEffect.createPredefined(if(test)VibrationEffect.EFFECT_DOUBLE_CLICK else VibrationEffect.EFFECT_CLICK)
                else if(test)VibrationEffect.createWaveform(longArrayOf(0,35,80,35),-1)
                else VibrationEffect.createOneShot(15,VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect,attributes)
            PulseResult.Requested // Request accepted is not proof that the user felt the motor.
        } catch(_:RuntimeException) {PulseResult.Failed}
    }
}
