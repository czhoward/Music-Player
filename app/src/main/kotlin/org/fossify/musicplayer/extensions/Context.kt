package org.fossify.musicplayer.extensions

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.util.Size
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.musicplayer.databases.SongsDatabase
import org.fossify.musicplayer.helpers.*
import org.fossify.musicplayer.interfaces.*
import org.fossify.musicplayer.models.Album
import org.fossify.musicplayer.models.Artist
import org.fossify.musicplayer.models.Genre
import org.fossify.musicplayer.models.Track
import java.io.File

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.playlistDAO: PlaylistsDao get() = getTracksDB().PlaylistsDao()

val Context.tracksDAO: SongsDao get() = getTracksDB().SongsDao()

val Context.queueDAO: QueueItemsDao get() = getTracksDB().QueueItemsDao()

val Context.artistDAO: ArtistsDao get() = getTracksDB().ArtistsDao()

val Context.albumsDAO: AlbumsDao get() = getTracksDB().AlbumsDao()

val Context.genresDAO: GenresDao get() = getTracksDB().GenresDao()

val Context.audioHelper: AudioHelper get() = AudioHelper(this)

val Context.mediaScanner: SimpleMediaScanner get() = SimpleMediaScanner.getInstance(applicationContext as Application)

fun Context.getTracksDB() = SongsDatabase.getInstance(this)

fun Context.getPlaylistIdWithTitle(title: String) = playlistDAO.getPlaylistWithTitle(title)?.id ?: -1

fun Context.broadcastUpdateWidgetState() {
    Intent(this, MyWidgetProvider::class.java).apply {
        action = TRACK_STATE_CHANGED
        sendBroadcast(this)
    }
}

fun Context.getMediaStoreIdFromPath(path: String): Long {
    var id = 0L
    val projection = arrayOf(
        Audio.Media._ID
    )

    val uri = getFileUri(path)
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    val selectionArgs = arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                id = cursor.getLongValue(Audio.Media._ID)
            }
        }
    } catch (ignored: Exception) {
    }

    return id
}

fun Context.getFolderTracks(path: String, rescanWrongPaths: Boolean, callback: (tracks: ArrayList<Track>) -> Unit) {
    val folderTracks = getFolderTrackPaths(File(path))
    val allTracks = audioHelper.getAllTracks()
    val wantedTracks = ArrayList<Track>()
    val wrongPaths = ArrayList<String>()    // rescan paths that are not present in the MediaStore

    folderTracks.forEach { trackPath ->
        var trackAdded = false
        val mediaStoreId = getMediaStoreIdFromPath(trackPath)
        if (mediaStoreId != 0L) {
            allTracks.firstOrNull { it.mediaStoreId == mediaStoreId }?.apply {
                id = 0
                wantedTracks.add(this)
                trackAdded = true
            }
        }

        if (!trackAdded) {
            val track = RoomHelper(this).getTrackFromPath(trackPath)
            if (track != null && track.mediaStoreId != 0L) {
                wantedTracks.add(track)
            } else {
                wrongPaths.add(trackPath)
            }
        }
    }

    if (wrongPaths.isEmpty() || !rescanWrongPaths) {
        callback(wantedTracks)
    } else {
        rescanPaths(wrongPaths) {
            getFolderTracks(path, false) { tracks ->
                callback(tracks)
            }
        }
    }
}

private fun getFolderTrackPaths(folder: File): ArrayList<String> {
    val trackFiles = ArrayList<String>()
    val files = folder.listFiles() ?: return trackFiles
    files.forEach {
        if (it.isDirectory) {
            trackFiles.addAll(getFolderTrackPaths(it))
        } else if (it.isAudioFast()) {
            trackFiles.add(it.absolutePath)
        }
    }
    return trackFiles
}

fun BaseSimpleActivity.getArtistCoverArt(artist: Artist, callback: (coverArt: Any?) -> Unit) {
    loadCategoryCoverArt(artist.albumArt, { audioHelper.getArtistTracks(artist.id).firstOrNull() }, callback)
}

fun BaseSimpleActivity.getAlbumCoverArt(album: Album, callback: (coverArt: Any?) -> Unit) {
    loadCategoryCoverArt(album.coverArt, { audioHelper.getAlbumTracks(album.id).firstOrNull() }, callback)
}

fun BaseSimpleActivity.getGenreCoverArt(genre: Genre, callback: (coverArt: Any?) -> Unit) {
    loadCategoryCoverArt(genre.albumArt, { audioHelper.getGenreTracks(genre.id).firstOrNull() }, callback)
}

private fun BaseSimpleActivity.loadCategoryCoverArt(
    coverArt: String,
    getTrack: () -> Track?,
    callback: (coverArt: Any?) -> Unit
) {
    lifecycleScope.launch {
        callback(withContext(Dispatchers.IO) {
            if (coverArt.isEmpty()) resolveTrackCoverArt(getTrack()) else coverArt
        })
    }
}

fun BaseSimpleActivity.getTrackCoverArt(track: Track?, callback: (coverArt: Any?) -> Unit) {
    lifecycleScope.launch {
        callback(withContext(Dispatchers.IO) { resolveTrackCoverArt(track) })
    }
}

private fun Context.resolveTrackCoverArt(track: Track?): Any? {
    if (track == null) return null
    return track.coverArt.ifEmpty { loadTrackCoverArt(track) }
}

fun Context.getTrackCoverArt(track: Track?, callback: (coverArt: Any?) -> Unit) {
    ensureBackgroundThread {
        if (track == null) {
            Handler(Looper.getMainLooper()).post {
                callback(null)
            }
            return@ensureBackgroundThread
        }

        val coverArt = resolveTrackCoverArt(track)

        Handler(Looper.getMainLooper()).post {
            callback(coverArt)
        }
    }
}

fun Context.loadTrackCoverArt(track: Track?): Bitmap? {
    if (track == null) {
        return null
    }

    val artworkUri = track.coverArt
    if (artworkUri.startsWith("content://")) {
        try {
            return MediaStore.Images.Media.getBitmap(contentResolver, artworkUri.toUri())
        } catch (ignored: Exception) {
        }
    }

    val coverArtHeight = resources.getCoverArtHeight()
    val size = Size(coverArtHeight, coverArtHeight)
    if (artworkUri.startsWith("content://")) {
        try {
            return contentResolver.loadThumbnail(artworkUri.toUri(), size, null)
        } catch (ignored: Exception) {
        }
    }

    val path = track.path
    if (path.isNotEmpty() && File(path).exists()) {
        try {
            return ThumbnailUtils.createAudioThumbnail(File(track.path), size, null)
        } catch (ignored: OutOfMemoryError) {
        } catch (ignored: Exception) {
        }
    }

    return null
}

fun Context.loadGlideResource(
    model: Any?,
    options: RequestOptions,
    size: Size,
    onLoadFailed: (e: Exception?) -> Unit,
    onResourceReady: (resource: Drawable) -> Unit,
) {
    val onLoadFailedCallback = onLoadFailed
    val onResourceReadyCallback = onResourceReady
    Glide.with(this)
        .load(model)
        .apply(options)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                onLoadFailedCallback(e)
                return true
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any?,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                onResourceReadyCallback(resource)
                return true
            }
        })
        .into(object : CustomTarget<Drawable>(size.width, size.height) {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) = Unit

            override fun onLoadCleared(placeholder: Drawable?) = Unit
        })
}

suspend fun Context.getTrackFromUri(uri: Uri?): Track? {
    if (uri == null) return null

    return withContext(Dispatchers.IO) {
        val path = getRealPathFromURI(uri) ?: return@withContext null
        val allTracks = audioHelper.getAllTracks()
        allTracks.find { it.path == path } ?: RoomHelper(this@getTrackFromUri).getTrackFromPath(path)
    }
}

fun Context.isTabVisible(flag: Int) = config.showTabs and flag != 0

fun Context.getVisibleTabs() = tabsList.filter { isTabVisible(it) }

fun Context.getPlaybackSetting(repeatMode: @Player.RepeatMode Int): PlaybackSetting {
    return when (repeatMode) {
        Player.REPEAT_MODE_OFF -> PlaybackSetting.REPEAT_OFF
        Player.REPEAT_MODE_ONE -> PlaybackSetting.REPEAT_TRACK
        Player.REPEAT_MODE_ALL -> PlaybackSetting.REPEAT_PLAYLIST
        else -> config.playbackSetting
    }
}

fun Context.getFriendlyFolder(path: String): String {
    return when (val parentPath = path.getParentPath()) {
        internalStoragePath -> getString(org.fossify.commons.R.string.internal)
        sdCardPath -> getString(org.fossify.commons.R.string.sd_card)
        else -> parentPath.getFilenameFromPath()
    }
}
