package org.fossify.musicplayer.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.provider.MediaStore
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.media3.common.MediaItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.musicplayer.R
import org.fossify.musicplayer.databinding.ViewCurrentTrackBarBinding
import org.fossify.musicplayer.extensions.*
import androidx.core.graphics.drawable.toDrawable

class CurrentTrackBar(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    private val binding by viewBinding(ViewCurrentTrackBarBinding::bind)

    fun initialize(togglePlayback: () -> Unit) {
        binding.currentTrackPlayPause.setOnClickListener {
            togglePlayback()
        }
    }

    fun updateColors() {
        background = context.getProperBackgroundColor().toDrawable()
        binding.currentTrackLabel.setTextColor(context.getProperTextColor())
    }

    fun updateCurrentTrack(mediaItem: MediaItem?) {
        val track = mediaItem?.toTrack()
        if (track == null) {
            fadeOut()
            return
        } else {
            fadeIn()
        }

        val artist = if (track.artist.trim().isNotEmpty() && track.artist != MediaStore.UNKNOWN_STRING) {
            " • ${track.artist}"
        } else {
            ""
        }

        @SuppressLint("SetTextI18n")
        binding.currentTrackLabel.text = "${track.title}$artist"
        val cornerRadius = resources.getDimension(org.fossify.commons.R.dimen.rounded_corner_radius_small).toInt()
        val currentTrackPlaceholder = resources.getColoredDrawableWithColor(R.drawable.ic_headset, context.getProperTextColor())
        val options = RequestOptions()
            .error(currentTrackPlaceholder)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))

        val activity = context.findBaseSimpleActivity()
        val onCoverArtLoaded: (Any?) -> Unit = { coverArt ->
            activity?.ensureActivityNotDestroyed {
                Glide.with(this)
                    .load(coverArt)
                    .apply(options)
                    .into(findViewById(R.id.current_track_image))
            }
        }
        if (activity != null) {
            activity.getTrackCoverArt(track, onCoverArtLoaded)
        } else {
            context.getTrackCoverArt(track, onCoverArtLoaded)
        }
    }

    fun updateTrackState(isPlaying: Boolean) {
        binding.currentTrackPlayPause.updatePlayPauseIcon(isPlaying, context.getProperTextColor())
    }

    private fun Context.findBaseSimpleActivity(): BaseSimpleActivity? {
        var currentContext = this
        while (currentContext !is BaseSimpleActivity) {
            currentContext = (currentContext as? ContextWrapper)?.baseContext ?: return null
        }
        return currentContext
    }
}
