package org.fossify.musicplayer.activities

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.musicplayer.R
import org.fossify.musicplayer.adapters.AlbumsTracksAdapter
import org.fossify.musicplayer.databinding.ActivityAlbumsBinding
import org.fossify.musicplayer.extensions.audioHelper
import org.fossify.musicplayer.helpers.ALBUM
import org.fossify.musicplayer.helpers.ARTIST
import org.fossify.musicplayer.models.*

// Artists -> Albums -> Tracks
class AlbumsActivity : SimpleMusicActivity() {

    private val binding by viewBinding(ActivityAlbumsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupEdgeToEdge(padBottomSystem = listOf(binding.albumsList, binding.currentTrackBar.root))
        setupMaterialScrollListener(binding.albumsList, binding.albumsAppbar)

        binding.albumsFastscroller.updateColors(getProperPrimaryColor())

        val artistType = object : TypeToken<Artist>() {}.type
        val artist = Gson().fromJson<Artist>(intent.getStringExtra(ARTIST), artistType)
        binding.albumsToolbar.title = artist.title

        lifecycleScope.launch {
            val (albums, albumTracks) = withContext(Dispatchers.IO) {
                val albums = audioHelper.getArtistAlbums(artist.id)
                albums to audioHelper.getAlbumTracks(albums)
            }

            val listItems = ArrayList<ListItem>()
            val albumsSectionLabel = resources.getQuantityString(R.plurals.albums_plural, albums.size, albums.size)
            listItems.add(AlbumSection(albumsSectionLabel))
            listItems.addAll(albums)

            val trackFullDuration = albumTracks.sumOf { it.duration }

            var tracksSectionLabel = resources.getQuantityString(R.plurals.tracks_plural, albumTracks.size, albumTracks.size)
            tracksSectionLabel += " • ${trackFullDuration.getFormattedDuration(true)}"
            listItems.add(AlbumSection(tracksSectionLabel))
            listItems.addAll(albumTracks)

            AlbumsTracksAdapter(this@AlbumsActivity, listItems, binding.albumsList) {
                hideKeyboard()
                if (it is Album) {
                    Intent(this@AlbumsActivity, TracksActivity::class.java).apply {
                        putExtra(ALBUM, Gson().toJson(it))
                        startActivity(this)
                    }
                } else {
                    handleNotificationPermission { granted ->
                        if (granted) {
                            val startIndex = albumTracks.indexOf(it as Track)
                            prepareAndPlay(albumTracks, startIndex)
                        } else {
                            PermissionRequiredDialog(
                                this@AlbumsActivity,
                                org.fossify.commons.R.string.allow_notifications_music_player,
                                { openNotificationSettings() }
                            )
                        }
                    }
                }
            }.apply {
                binding.albumsList.adapter = this
            }

            if (areSystemAnimationsEnabled) {
                binding.albumsList.scheduleLayoutAnimation()
            }
        }

        setupCurrentTrackBar(binding.currentTrackBar.root)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.albumsAppbar, NavigationIcon.Arrow)
    }
}
