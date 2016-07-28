package org.xbmc.kore;

import android.app.Application;
import android.os.StrictMode;

import org.xbmc.kore.playlist.PlaylistManager;

public class App extends Application {

    private static App application;
    private static PlaylistManager playlistManager;

    @Override
    public void onCreate() {
        enableStrictMode();
        super.onCreate();

        application = this;
        playlistManager = new PlaylistManager();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        application = null;
        playlistManager = null;
    }

    public static PlaylistManager getPlaylistManager() {
        return playlistManager;
    }

    public static App getApplication() {
        return application;
    }

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }
}
