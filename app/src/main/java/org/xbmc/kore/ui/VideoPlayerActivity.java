package org.xbmc.kore.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.devbrackets.android.exomedia.ui.widget.EMVideoView;
import com.devbrackets.android.playlistcore.listener.PlaylistListener;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.devbrackets.android.playlistcore.service.PlaylistServiceCore;

import org.xbmc.kore.App;
import org.xbmc.kore.R;
import org.xbmc.kore.playlist.PlaylistManager;
import org.xbmc.kore.playlist.VideoApi;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.data.MediaItem;
import org.xbmc.kore.utils.data.StreamItems;

import java.util.LinkedList;
import java.util.List;


public class VideoPlayerActivity extends Activity implements PlaylistListener<MediaItem> {
    public static final String EXTRA_TITLE = "EXTRA_TITLE";
    public static final String EXTRA_TAG_LINE = "EXTRA_TAG_LINE";
    public static final String EXTRA_FAN_ART = "EXTRA_FAN_ART";
    public static final String EXTRA_URL = "EXTRA_URL";
    public static final int PLAYLIST_ID = 6; //Arbitrary, for the example (different from audio)

    protected EMVideoView emVideoView;
    protected PlaylistManager playlistManager;

    protected int selectedIndex;
    protected String title;
    protected String tagline;
    protected String fanArt;
    protected String url;
    protected boolean pausedInOnStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        retrieveExtras();
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (emVideoView.isPlaying()) {
            pausedInOnStop = true;
            emVideoView.pause();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (pausedInOnStop) {
            emVideoView.start();
            pausedInOnStop = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        playlistManager.unRegisterPlaylistListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playlistManager = App.getPlaylistManager();
        playlistManager.registerPlaylistListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playlistManager.invokeStop();
    }

    @Override
    public boolean onPlaylistItemChanged(MediaItem currentItem, boolean hasNext, boolean hasPrevious) {
        return false;
    }

    @Override
    public boolean onPlaybackStateChanged(@NonNull PlaylistServiceCore.PlaybackState playbackState) {
        if (playbackState == PlaylistServiceCore.PlaybackState.STOPPED) {
            finish();
            return true;
        }

        return false;
    }

    /**
     * Retrieves the extra associated with the selected playlist index
     * so that we can start playing the correct item.
     */
    protected void retrieveExtras() {
        Bundle extras = getIntent().getExtras();
        selectedIndex = 0;
        //otherwise start from the begining
        title = extras.getString(EXTRA_TITLE, "");
        fanArt = extras.getString(EXTRA_FAN_ART, "");
        tagline = extras.getString(EXTRA_TAG_LINE, "");
        url = extras.getString(EXTRA_URL, "");
    }

    protected void init() {
        setupPlaylistManager();

        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);

        playlistManager.setVideoPlayer(new VideoApi(emVideoView));
        playlistManager.play(0, false);
    }

    /**
     * Retrieves the playlist instance and performs any generation
     * of content if it hasn't already been performed.
     */
    private void setupPlaylistManager() {
        playlistManager = App.getPlaylistManager();

        StreamItems.StreamItem item = new StreamItems.StreamItem(title, url, fanArt, tagline);
        List<MediaItem> mediaItems = new LinkedList<>();
        MediaItem mediaItem = new MediaItem(item, false);
        mediaItems.add(mediaItem);

        playlistManager.setAllowedMediaType(BasePlaylistManager.AUDIO | BasePlaylistManager.VIDEO);
        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setId(PLAYLIST_ID);
    }
}
