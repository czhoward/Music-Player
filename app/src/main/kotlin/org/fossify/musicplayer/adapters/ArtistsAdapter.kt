package org.fossify.musicplayer.adapters

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.views.MyRecyclerView
import org.fossify.musicplayer.R
import org.fossify.musicplayer.databinding.ItemArtistBinding
import org.fossify.musicplayer.extensions.audioHelper
import org.fossify.musicplayer.extensions.config
import org.fossify.musicplayer.extensions.getArtistCoverArt
import org.fossify.musicplayer.inlines.indexOfFirstOrNull
import org.fossify.musicplayer.models.Artist
import org.fossify.musicplayer.models.Track

class ArtistsAdapter(activity: BaseSimpleActivity, items: ArrayList<Artist>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    BaseMusicAdapter<Artist>(items, activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    override fun getActionMenuId() = R.menu.cab_artists

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArtistBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = items.getOrNull(position) ?: return
        holder.bindView(artist, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, artist)
        }
        bindViewHolder(holder)
    }

    override fun actionItemPressed(id: Int) {
        handleCategoryAction(id, ::askConfirmDelete)
    }

    override fun getSelectedTracks(): ArrayList<Track> {
        val albums = context.audioHelper.getArtistAlbums(getSelectedItems())
        return context.audioHelper.getAlbumTracks(albums)
    }

    private fun askConfirmDelete() {
        confirmDeleteSelectedItems(
            getItemPosition = { artist -> items.indexOfFirstOrNull { it.id == artist.id } },
            loadAndDeleteTracks = { selectedArtists ->
                context.audioHelper.getArtistTracks(selectedArtists).also {
                    context.audioHelper.deleteArtists(selectedArtists)
                }
            }
        )
    }

    private fun setupView(view: View, artist: Artist) {
        ItemArtistBinding.bind(view).apply {
            bindItemTitle(root, artistFrame, artistTitle, artist.title, artist)

            val albums = resources.getQuantityString(R.plurals.albums_plural, artist.albumCnt, artist.albumCnt)
            val tracks = resources.getQuantityString(R.plurals.tracks_plural, artist.trackCnt, artist.trackCnt)
            @SuppressLint("SetTextI18n")
            artistAlbumsTracks.text = "$albums, $tracks"
            artistAlbumsTracks.setTextColor(textColor)

            context.getArtistCoverArt(artist) { coverArt ->
                loadImage(artistImage, coverArt, placeholder)
            }
        }
    }

    override fun onChange(position: Int) = items.getOrNull(position)?.getBubbleText(context.config.artistSorting) ?: ""
}
