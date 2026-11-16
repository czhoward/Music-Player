package org.fossify.musicplayer.fgstest;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionResult;
import androidx.media3.session.SessionToken;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public final class FgsProbeActivity extends Activity {
    private static final String TAG = "FgsProbeActivity";
    private static final String TARGET_PACKAGE = "org.fossify.musicplayer.debug";
    private static final String TARGET_SERVICE = "org.fossify.musicplayer.playback.PlaybackService";
    private static final String TEST_DIRECT_PLAY_COMMAND = "org.fossify.musicplayer.DEBUG_PLAY_DIRECT";
    private static final long ALLOWLIST_EXPIRY_DELAY_MS = 15_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView statusView;
    private ListenableFuture<MediaBrowser> browserFuture;
    private MediaBrowser browser;
    private long itemWaitStartedAtMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statusView = new TextView(this);
        statusView.setText("Connecting to the player session...");
        statusView.setTextSize(20);
        setContentView(statusView);

        SessionToken token = new SessionToken(
                this,
                new ComponentName(TARGET_PACKAGE, TARGET_SERVICE)
        );
        browserFuture = new MediaBrowser.Builder(this, token).buildAsync();
        browserFuture.addListener(this::onBrowserConnected, getMainExecutor());
    }

    private void onBrowserConnected() {
        try {
            browser = browserFuture.get();
            browser.addListener(new Player.Listener() {
                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                    Log.i(TAG, "PLAY_WHEN_READY_CHANGED=" + playWhenReady + " reason=" + reason);
                }

                @Override
                public void onPlayerError(androidx.media3.common.PlaybackException error) {
                    Log.e(TAG, "PLAYER_ERROR", error);
                }
            });
            statusView.setText("Connected. Loading the target app's track library...");
            ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> tracksFuture =
                    browser.getChildren("__TRACKS__", 0, 100, null);
            tracksFuture.addListener(() -> onTracksLoaded(tracksFuture), getMainExecutor());
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            showFailure("Could not connect to the player: " + exception.getMessage());
        }
    }

    private void onTracksLoaded(ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> tracksFuture) {
        try {
            LibraryResult<ImmutableList<MediaItem>> result = tracksFuture.get();
            if (result.resultCode != LibraryResult.RESULT_SUCCESS || result.value == null || result.value.isEmpty()) {
                showFailure("The target app did not return any tracks from its library.");
                return;
            }

            browser.setMediaItem(result.value.get(0));
            browser.prepare();
            browser.pause();
            statusView.setText("Prepared a track. Waiting for the temporary allowlist to expire...");
            itemWaitStartedAtMs = System.currentTimeMillis();
            waitForPreparedItem();
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            showFailure("Could not load a track: " + exception.getMessage());
        }
    }

    private void waitForPreparedItem() {
        if (browser.getCurrentMediaItem() != null) {
            handler.postDelayed(this::backgroundAndSendPlay, ALLOWLIST_EXPIRY_DELAY_MS);
            return;
        }

        if (System.currentTimeMillis() - itemWaitStartedAtMs > 10_000L) {
            showFailure("The player did not prepare the selected track.");
            return;
        }

        handler.postDelayed(this::waitForPreparedItem, 250L);
    }

    private void backgroundAndSendPlay() {
        moveTaskToBack(true);
        handler.postDelayed(this::sendPlay, 2_000L);
    }

    private void sendPlay() {
        if (browser == null || !browser.isConnected()) {
            showFailure("The media controller disconnected before play.");
            return;
        }

        ListenableFuture<SessionResult> result =
                browser.sendCustomCommand(
                        new SessionCommand(TEST_DIRECT_PLAY_COMMAND, Bundle.EMPTY),
                        Bundle.EMPTY
                );
        result.addListener(() -> {
            try {
                Log.i(TAG, "DIRECT_PLAY_COMMAND_RESULT=" + result.get().resultCode);
            } catch (ExecutionException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                showFailure("The direct-play test command failed: " + exception.getMessage());
            }
        }, getMainExecutor());
        statusView.setText("Direct play command sent. Check fallback notification ID 42.");
        Log.i(TAG, "DIRECT_PLAY_COMMAND_SENT_AFTER_ALLOWLIST_EXPIRY");
    }

    private void showFailure(String message) {
        statusView.setText(message);
        Log.e(TAG, message);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (browserFuture != null) {
            MediaBrowser.releaseFuture(browserFuture);
        }
        super.onDestroy();
    }
}
