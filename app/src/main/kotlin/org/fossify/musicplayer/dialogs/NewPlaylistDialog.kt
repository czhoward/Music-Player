package org.fossify.musicplayer.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.musicplayer.R
import org.fossify.musicplayer.databinding.DialogNewPlaylistBinding
import org.fossify.musicplayer.extensions.audioHelper
import org.fossify.musicplayer.extensions.getPlaylistIdWithTitle
import org.fossify.musicplayer.models.Playlist

class NewPlaylistDialog(val activity: BaseSimpleActivity, var playlist: Playlist? = null, val callback: (playlistId: Int) -> Unit) {
    private var isNewPlaylist = playlist == null
    private val binding by activity.viewBinding(DialogNewPlaylistBinding::inflate)

    init {
        if (playlist == null) {
            playlist = Playlist(0, "")
        }

        binding.newPlaylistTitle.setText(playlist!!.title)
        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                val dialogTitle = if (isNewPlaylist) R.string.create_new_playlist else R.string.rename_playlist
                activity.setupDialogStuff(binding.root, this, dialogTitle) { alertDialog ->
                    alertDialog.showKeyboard(binding.newPlaylistTitle)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val title = binding.newPlaylistTitle.value
                        if (title.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.empty_name)
                            return@setOnClickListener
                        }

                        activity.lifecycleScope.launch {
                            val playlistIdWithTitle = withContext(Dispatchers.IO) {
                                activity.getPlaylistIdWithTitle(title)
                            }
                            val isPlaylistTitleTaken = if (isNewPlaylist) {
                                playlistIdWithTitle != -1
                            } else {
                                playlist!!.id != playlistIdWithTitle && playlistIdWithTitle != -1
                            }

                            if (isPlaylistTitleTaken) {
                                activity.toast(R.string.playlist_name_exists)
                                return@launch
                            }

                            playlist!!.title = title
                            val eventTypeId = withContext(Dispatchers.IO) {
                                if (isNewPlaylist) {
                                    activity.audioHelper.insertPlaylist(playlist!!).toInt()
                                } else {
                                    activity.audioHelper.updatePlaylist(playlist!!)
                                    playlist!!.id
                                }
                            }

                            if (eventTypeId != -1) {
                                alertDialog.dismiss()
                                callback(eventTypeId)
                            } else {
                                activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
                            }
                        }
                    }
                }
            }
    }
}
