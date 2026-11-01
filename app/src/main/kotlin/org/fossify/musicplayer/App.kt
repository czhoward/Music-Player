package org.fossify.musicplayer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.fossify.commons.extensions.checkUseEnglish
import org.fossify.musicplayer.helpers.SimpleMediaController

class App : Application(), DefaultLifecycleObserver {
    override fun onCreate() {
        super<Application>.onCreate()
        checkUseEnglish()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        SimpleMediaController.destroyInstance()
    }
}
