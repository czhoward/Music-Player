package org.fossify.musicplayer.playback

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.fossify.musicplayer.extensions.config
import org.fossify.musicplayer.models.Events
import org.greenrobot.eventbus.EventBus

private var isActive = false
private var sleepTimer: CountDownTimer? = null

internal object SleepTimerState {
    private val mutableSeconds = MutableStateFlow<Int?>(null)
    val seconds: StateFlow<Int?> = mutableSeconds.asStateFlow()

    fun update(seconds: Int?) {
        mutableSeconds.value = seconds
    }
}

internal fun PlaybackService.toggleSleepTimer() {
    if (isActive) {
        stopSleepTimer()
    } else {
        startSleepTimer()
    }
}

internal fun PlaybackService.startSleepTimer() {
    val millisInFuture = config.sleepInTS - System.currentTimeMillis() + 1000L
    sleepTimer?.cancel()
    SleepTimerState.update(null)
    sleepTimer = object : CountDownTimer(millisInFuture, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val seconds = (millisUntilFinished / 1000).toInt()
            SleepTimerState.update(seconds)
        }

        override fun onFinish() {
            config.sleepInTS = 0
            EventBus.getDefault().post(Events.SleepTimerExpired())
            stopSleepTimer()
            stopService()
        }
    }

    sleepTimer?.start()
    isActive = true
}

internal fun PlaybackService.stopSleepTimer() {
    sleepTimer?.cancel()
    sleepTimer = null
    isActive = false
    config.sleepInTS = 0
    SleepTimerState.update(null)
}
