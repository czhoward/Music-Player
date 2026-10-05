package org.fossify.musicplayer.helpers

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Looper
import androidx.media3.common.Player.Listener
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import org.fossify.musicplayer.extensions.addListenerWithResult
import org.fossify.musicplayer.extensions.getOrNull
import org.fossify.musicplayer.extensions.runOnPlayerThread
import org.fossify.musicplayer.playback.PlaybackService

class SimpleMediaController(val context: Application) {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null

    @Synchronized
    fun createControllerAsync() {
        val future = MediaController
            .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .setApplicationLooper(Looper.getMainLooper())
            .buildAsync()
        controllerFuture = future

        future.addListenerWithResult { acquiredController ->
            if (controllerFuture === future) {
                controller = acquiredController
            }
        }
    }

    private fun getControllerSync() = controllerFuture.getOrNull()

    private fun shouldCreateNewController(): Boolean {
        return if (!::controllerFuture.isInitialized) {
            return true
        } else {
            controllerFuture.isCancelled || controllerFuture.isDone && getControllerSync()?.isConnected != true
        }
    }

    @Synchronized
    private fun acquireController(callback: (MediaController) -> Unit) {
        if (shouldCreateNewController()) {
            createControllerAsync()
        }

        val future = controllerFuture
        future.addListenerWithResult { acquiredController ->
            acquiredController?.takeIf { it.isConnected }?.let(callback)
            }
    }

    fun releaseController() {
        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
            controller = null
        }
    }

    fun withController(callback: MediaController.() -> Unit) {
        val controller = controller
        if (controller != null && controller.isConnected) {
            controller.runOnPlayerThread(callback)
        } else {
            acquireController { it.runOnPlayerThread(callback) }
        }
    }

    fun addListener(listener: Listener) {
        withController {
            addListener(listener)
        }
    }

    fun removeListener(listener: Listener) {
        withController {
            removeListener(listener)
        }
    }

    companion object {
        private var instance: SimpleMediaController? = null

        fun getInstance(context: Context): SimpleMediaController {
            if (instance == null) {
                instance = SimpleMediaController(context.applicationContext as Application)
            }

            return instance!!
        }

        fun destroyInstance() {
            instance?.releaseController()
            instance = null
        }
    }
}
