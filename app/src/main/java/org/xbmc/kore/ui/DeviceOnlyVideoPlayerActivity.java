package org.xbmc.kore.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.SeekBar;
import android.widget.Toast;

import com.devbrackets.android.exomedia.ui.widget.EMVideoView;
import com.devbrackets.android.playlistcore.listener.PlaylistListener;
import com.devbrackets.android.playlistcore.manager.BasePlaylistManager;
import com.devbrackets.android.playlistcore.service.PlaylistServiceCore;

import org.xbmc.kore.App;
import org.xbmc.kore.R;
import org.xbmc.kore.host.HostConnectionObserver;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.ApiCallback;
import org.xbmc.kore.jsonrpc.ApiMethod;
import org.xbmc.kore.jsonrpc.HostConnection;
import org.xbmc.kore.jsonrpc.method.Player;
import org.xbmc.kore.jsonrpc.type.ListType;
import org.xbmc.kore.jsonrpc.type.PlayerType;
import org.xbmc.kore.playlist.PlaylistManager;
import org.xbmc.kore.playlist.VideoApi;
import org.xbmc.kore.service.ConnectionObserversManagerService;
import org.xbmc.kore.ui.hosts.AddHostActivity;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedPreferenceManager;
import org.xbmc.kore.utils.data.MediaItem;
import org.xbmc.kore.utils.data.StreamItems;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class DeviceOnlyVideoPlayerActivity extends Activity implements PlaylistListener<MediaItem>{
    private static final String TAG = DeviceOnlyVideoPlayerActivity.class.getSimpleName();

    public static final int PLAYLIST_ID = 6; //Arbitrary, for the example (different from audio)
    private SharedPreferenceManager sharedPreferenceManager;

    protected EMVideoView emVideoView;
    protected SeekBar emVideoSeekBar;
    protected PlaylistManager playlistManager;

    protected int selectedIndex;
    protected String title;
    protected String tagline;
    protected String fanArt;
    protected String url;
    protected int seekPosition;
    protected boolean pausedInOnStop = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
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
    public void onStart() {
        super.onStart();

        if (pausedInOnStop) {
            emVideoView.start();
            pausedInOnStop = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        playlistManager.unRegisterPlaylistListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        playlistManager = App.getPlaylistManager();
        playlistManager.registerPlaylistListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playlistManager.invokeStop();
        sharedPreferenceManager.clear();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        playlistManager.invokeStop();
        sharedPreferenceManager.clear();
    }

    @Override
    public boolean onPlaylistItemChanged(MediaItem currentItem, boolean hasNext, boolean hasPrevious) {
        return false;
    }

    @Override
    public boolean onPlaybackStateChanged(@NonNull final PlaylistServiceCore.PlaybackState playbackState) {
        if (playbackState == PlaylistServiceCore.PlaybackState.STOPPED) {
            playlistManager.invokeStop();
            finish();
            return true;
        }

        if (playbackState == PlaylistServiceCore.PlaybackState.ERROR) {
            //@TODO: show error dialog
            Toast.makeText(DeviceOnlyVideoPlayerActivity.this, "An error has occured while trying to play media", Toast.LENGTH_SHORT)
                    .show();
            playlistManager.invokeStop();
            finish();
            return true;
        }

        return false;
    }

    protected void init() {
        setupPlaylistManager();
        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
        playlistManager.setVideoPlayer(new VideoApi(emVideoView));
        emVideoSeekBar = (SeekBar) emVideoView.findViewById(R.id.exomedia_controls_video_seek);

//        emVideoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (!fromUser) {
//                    //seekPosition = progress;
//                }
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//                //nothing
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                //nothing
//            }
//        });
    }

    /**
     * Retrieves the playlist instance and performs any generation
     * of content if it hasn't already been performed.
     */
    private void setupPlaylistManager() {
        playlistManager = App.getPlaylistManager();

        title = sharedPreferenceManager.getKeyCurrentStreamTitle();
        url = sharedPreferenceManager.getKeyCurrentStreamUrl();
        fanArt = sharedPreferenceManager.getKeyCurrentStreamArt();
        tagline = sharedPreferenceManager.getKeyCurrentStreamTagline();
        seekPosition = sharedPreferenceManager.getKeyCurrentPlaybackPosition();

        StreamItems.StreamItem item = new StreamItems.StreamItem(title, url, fanArt, tagline);
        List<MediaItem> mediaItems = new LinkedList<>();
        MediaItem mediaItem = new MediaItem(item, false);
        mediaItems.add(mediaItem);

        playlistManager.setAllowedMediaType(BasePlaylistManager.AUDIO | BasePlaylistManager.VIDEO);
        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setId(PLAYLIST_ID);
        playlistManager.play(seekPosition, false);

        //playlistManager.invokeSeekEnded(seekPosition);

    }
}
