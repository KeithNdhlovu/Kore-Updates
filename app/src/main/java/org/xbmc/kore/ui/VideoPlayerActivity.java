package org.xbmc.kore.ui;

import android.app.Activity;
import android.content.Intent;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;
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
import org.xbmc.kore.jsonrpc.type.PlaylistType;
import org.xbmc.kore.playlist.PlaylistManager;
import org.xbmc.kore.playlist.VideoApi;
import org.xbmc.kore.service.ConnectionObserversManagerService;
import org.xbmc.kore.service.NotificationObserver;
import org.xbmc.kore.ui.hosts.AddHostActivity;
import org.xbmc.kore.utils.FileDownloadHelper;
import org.xbmc.kore.utils.LogUtils;
import org.xbmc.kore.utils.SharedPreferenceManager;
import org.xbmc.kore.utils.data.MediaItem;
import org.xbmc.kore.utils.data.StreamItems;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class VideoPlayerActivity extends Activity implements PlaylistListener<MediaItem>, HostConnectionObserver.PlayerEventsObserver{
    private static final String TAG = VideoPlayerActivity.class.getSimpleName();

    public static final int PLAYLIST_ID = 6; //Arbitrary, for the example (different from audio)
    private SharedPreferenceManager sharedPreferenceManager;

    protected EMVideoView emVideoView;
    protected PlaylistManager playlistManager;

    protected int selectedIndex;
    protected String title;
    protected String tagline;
    protected String fanArt;
    protected String url;
    protected boolean pausedInOnStop = false;

    /**
     * Host manager singleton
     */
    private HostManager hostManager = null;

    private Handler callbackHandler = new Handler();

    /**
     * To register for observing host events
     */
    private HostConnectionObserver hostConnectionObserver;

    /**
     * The current active player id
     */
    private int currentActivePlayerId = -1;

    private int mediaTotalTime = 0,
            mediaCurrentTime = 0; // s
    /**
     * Default callback for methods that don't return anything
     */
    private ApiCallback<String> defaultStringActionCallback = ApiMethod.getDefaultActionCallback();
    private ApiCallback<Integer> defaultIntActionCallback = ApiMethod.getDefaultActionCallback();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);

        sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        hostManager = HostManager.getInstance(this);

        // Check if we have any hosts setup
        if (hostManager.getHostInfo() == null) {
            final Intent intent = new Intent(this, AddHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

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

        if (hostConnectionObserver != null) hostConnectionObserver.unregisterPlayerObserver(this);
        hostConnectionObserver = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        playlistManager = App.getPlaylistManager();
        playlistManager.registerPlaylistListener(this);

        hostConnectionObserver = hostManager.getHostConnectionObserver();
        hostConnectionObserver.registerPlayerObserver(this, true);
        // Force a refresh, mainly to update the time elapsed on the fragments
        hostConnectionObserver.forceRefreshResults();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerOnStop();
        sharedPreferenceManager.clear();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        playerOnStop();
        sharedPreferenceManager.clear();
    }

    @Override
    public boolean onPlaylistItemChanged(MediaItem currentItem, boolean hasNext, boolean hasPrevious) {
        return false;
    }

    @Override
    public boolean onPlaybackStateChanged(@NonNull final PlaylistServiceCore.PlaybackState playbackState) {
        if (playbackState == PlaylistServiceCore.PlaybackState.STOPPED) {
            finish();
            return true;
        }

        if (playbackState == PlaylistServiceCore.PlaybackState.PLAYING) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);

            int hours = mediaCurrentTime / 3600;
            int minutes = (mediaCurrentTime % 3600) / 60;
            int seconds = (mediaCurrentTime % 3600) % 60;
            int milliseconds = ( (mediaCurrentTime % 3600) % 60 ) * 1000;

            PlayerType.PositionTime positionTime = new PlayerType.PositionTime(hours, minutes, seconds, milliseconds);
            Player.Seek seekAction = new Player.Seek(currentActivePlayerId, positionTime);
            seekAction.execute(hostManager.getConnection(), new ApiCallback<PlayerType.SeekReturnType>() {
                @Override
                public void onSuccess(PlayerType.SeekReturnType result) {
                    playlistManager.invokeSeekEnded(result.time.milliseconds);
                }

                @Override
                public void onError(int errorCode, String description) {
                    LogUtils.LOGD(TAG, "Got an error calling Player.Seek. Error code: " + errorCode + ", description: " + description);
                }
            }, callbackHandler);
            return true;
        }

        if (playbackState == PlaylistServiceCore.PlaybackState.ERROR) {
            //@TODO: show error dialog
            Toast.makeText(VideoPlayerActivity.this, "An error has occured while trying to play media", Toast.LENGTH_SHORT)
                    .show();
            playerOnStop();
            return true;
        }

        if (playbackState == PlaylistServiceCore.PlaybackState.PAUSED) {
            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
            action.execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);
            return true;
        }

//        if (playbackState == PlaylistServiceCore.PlaybackState.PREPARING) {
//            Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
//            action.execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);
//            return true;
//        }

        return false;
    }

    protected void init() {

        // Check if any video player is active and clear the playlist before queuing if so
        final HostConnection connection = hostManager.getConnection();
        Player.GetActivePlayers getActivePlayers = new Player.GetActivePlayers();
        getActivePlayers.execute(connection, new ApiCallback<ArrayList<PlayerType.GetActivePlayersReturnType>>() {
            @Override
            public void onSuccess(ArrayList<PlayerType.GetActivePlayersReturnType> result) {
                boolean videoIsPlaying = false;

                for (PlayerType.GetActivePlayersReturnType player : result) {
                    currentActivePlayerId = player.playerid;
                }

                Player.PlayPause action = new Player.PlayPause(currentActivePlayerId);
                action.execute(hostManager.getConnection(), defaultIntActionCallback, callbackHandler);
            }

            @Override
            public void onError(int errorCode, String description) {
                LogUtils.LOGD(TAG, "Couldn't get active player when start init.");
                Toast.makeText(VideoPlayerActivity.this,
                        String.format(getString(R.string.error_get_active_player), description),
                        Toast.LENGTH_SHORT).show();
            }
        }, callbackHandler);

        setupPlaylistManager();

        emVideoView = (EMVideoView)findViewById(R.id.video_play_activity_video_view);
        playlistManager.setVideoPlayer(new VideoApi(emVideoView));

        SeekBar seekBar = (SeekBar) emVideoView.findViewById(R.id.exomedia_controls_video_seek);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    mediaCurrentTime = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //nothing
            }
        });
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

        StreamItems.StreamItem item = new StreamItems.StreamItem(title, url, fanArt, tagline);
        List<MediaItem> mediaItems = new LinkedList<>();
        MediaItem mediaItem = new MediaItem(item, false);
        mediaItems.add(mediaItem);

        playlistManager.setAllowedMediaType(BasePlaylistManager.AUDIO | BasePlaylistManager.VIDEO);
        playlistManager.setParameters(mediaItems, selectedIndex);
        playlistManager.setId(PLAYLIST_ID);
        playlistManager.play(0, false);
    }

    @Override
    public void playerOnPlay(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                             PlayerType.PropertyValue getPropertiesResult,
                             ListType.ItemsAll getItemResult) {
        currentActivePlayerId = getActivePlayerResult.playerid;
        // Start service that manages connection observers
        LogUtils.LOGD(TAG, "Starting observer service");
        startService(new Intent(this, ConnectionObserversManagerService.class));
    }

    @Override
    public void playerOnPause(PlayerType.GetActivePlayersReturnType getActivePlayerResult,
                              PlayerType.PropertyValue getPropertiesResult,
                              ListType.ItemsAll getItemResult) {
        currentActivePlayerId = getActivePlayerResult.playerid;
        //playerOnPlay(getActivePlayerResult, getPropertiesResult, getItemResult);
    }

    @Override
    public void playerOnStop() {
        playlistManager.invokeStop();
    }

    @Override
    public void playerOnConnectionError(int errorCode, String description) {
        //@TODO nothing
    }

    @Override
    public void playerNoResultsYet() {
        //@TODO nothing
    }

    @Override
    public void systemOnQuit() {
        //@TODO nothing
    }

    @Override
    public void inputOnInputRequested(String title, String type, String value) {
        //@TODO nothing
    }

    @Override
    public void observerOnStopObserving() {
        //@TODO nothing
    }
}
