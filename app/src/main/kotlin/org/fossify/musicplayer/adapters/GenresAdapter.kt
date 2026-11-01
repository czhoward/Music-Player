package org.fossify.musicplayer.adapters

import android.view.View
import android.view.ViewGroup
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.views.MyRecyclerView
import org.fossify.musicplayer.R
import org.fossify.musicplayer.databinding.ItemGenreBinding
import org.fossify.musicplayer.extensions.audioHelper
import org.fossify.musicplayer.extensions.config
import org.fossify.musicplayer.extensions.getGenreCoverArt
import org.fossify.musicplayer.inlines.indexOfFirstOrNull
import org.fossify.musicplayer.models.Genre
import org.fossify.musicplayer.models.Track

class GenresAdapter(activity: BaseSimpleActivity, items: ArrayList<Genre>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    BaseMusicAdapter<Genre>(items, activity, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    override fun getActionMenuId() = R.menu.cab_genres

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGenreBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val genre = items.getOrNull(position) ?: return
        holder.bindView(genre, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, genre)
        }
        bindViewHolder(holder)
    }

    override fun actionItemPressed(id: Int) {
        handleCategoryAction(id, ::askConfirmDelete)
    }

    override fun getSelectedTracks(): ArrayList<Track> {
        return context.audioHelper.getGenreTracks(getSelectedItems())
    }

    private fun askConfirmDelete() {
        confirmDeleteSelectedItems(
            getItemPosition = { genre -> items.indexOfFirstOrNull { it.id == genre.id } },
            loadAndDeleteTracks = { selectedGenres ->
                context.audioHelper.getGenreTracks(selectedGenres).also {
                    context.audioHelper.deleteGenres(selectedGenres)
                }
            }
        )
    }

    private fun setupView(view: View, genre: Genre) {
        ItemGenreBinding.bind(view).apply {
            bindItemTitle(root, genreFrame, genreTitle, genre.title, genre)
            bindQuantityText(genreTracks, R.plurals.tracks_plural, genre.trackCnt)

            context.getGenreCoverArt(genre) { coverArt ->
                loadImage(genreImage, coverArt, placeholderBig)
            }
        }
    }

    override fun onChange(position: Int) = items.getOrNull(position)?.getBubbleText(context.config.genreSorting) ?: ""
}
