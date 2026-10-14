package org.fossify.musicplayer.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.musicplayer.R
import org.fossify.musicplayer.databinding.DialogRenameSongBinding
import org.fossify.musicplayer.extensions.audioHelper
import org.fossify.musicplayer.helpers.TagHelper
import org.fossify.musicplayer.models.Track

class EditDialog(val activity: BaseSimpleActivity, val track: Track, val callback: (track: Track) -> Unit) {
    private val tagHelper = TagHelper(activity)
    private val binding by activity.viewBinding(DialogRenameSongBinding::inflate)

    init {
        binding.apply {
            title.setText(track.title)
            artist.setText(track.artist)
            album.setText(track.album)
            arrayOf(fileNameHint, extensionHint).forEach {
                it.beGone()
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.rename_song) { alertDialog ->
                    alertDialog.showKeyboard(binding.title)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.title.value
                        val newArtist = binding.artist.value
                        val newAlbum = binding.album.value
                        if (newTitle.isEmpty() || newArtist.isEmpty()) {
                            activity.toast(R.string.rename_song_empty)
                            return@setOnClickListener
                        }

                        if (track.title != newTitle || track.artist != newArtist || track.album != newAlbum) {
                            updateContentResolver(track, newArtist, newTitle, newAlbum) {
                                track.artist = newArtist
                                track.title = newTitle
                                track.album = newAlbum
                                val oldPath = track.path
                                storeEditedSong(track, oldPath, oldPath)
                                callback(track)
                                alertDialog.dismiss()
                            }
                        } else {
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun storeEditedSong(track: Track, oldPath: String, newPath: String) {
        activity.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    activity.audioHelper.updateTrackInfo(newPath, track.artist, track.title, oldPath)
                }
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }
    }

    private fun updateContentResolver(track: Track, newArtist: String, newTitle: String, newAlbum: String, onUpdateMediaStore: () -> Unit) {
        activity.lifecycleScope.launch {
            try {
                activity.handleRecoverableSecurityException { granted ->
                    if (granted) {
                        activity.lifecycleScope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    tagHelper.writeTag(track, newArtist, newTitle, newAlbum)
                                }
                                onUpdateMediaStore()
                            } catch (e: Exception) {
                                activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
            }
        }
    }
}
