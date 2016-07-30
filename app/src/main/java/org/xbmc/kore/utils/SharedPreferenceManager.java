package org.xbmc.kore.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Keith on 2016/07/29.
 * Simple SharedPreference singleton.
 */
public class SharedPreferenceManager {

    //URI's
    private static final String PACKAGED_NAME = "org.xbmc.kore";

    //stream media
    private static final String KEY_CURRENT_STREAM_URL = "current_stream_Url";
    private static final String KEY_CURRENT_STREAM_TITLE = "current_stream_title";
    private static final String KEY_CURRENT_STREAM_TAGLINE = "current_stream_tagline";
    private static final String KEY_CURRENT_STREAM_ART = "current_stream_art";
    private static final String KEY_CURRENT_PLAYLIST_ID = "current_playlist_id";
    private static final String KEY_CURRENT_STREAM_ITEM_ID = "current_stream_item_id";
    private static final String KEY_CURRENT_STREAM_PLAYBACK_POSTION = "current_playback_position";

    //Variables
    public static SharedPreferenceManager instance;
    public SharedPreferences settings;

    public static SharedPreferenceManager getInstance(Context context){
        if(instance == null){
            instance = new SharedPreferenceManager(context);
        }
        return instance;
    }

    private SharedPreferenceManager(Context context){
        settings = context.getSharedPreferences(PACKAGED_NAME,Context.MODE_PRIVATE);
    }

    public void setKeyCurrentStreamUrl(String url){
        SharedPreferences.Editor edit = settings.edit();
        edit.putString(KEY_CURRENT_STREAM_URL,url);
        edit.apply();
    }

    public String getKeyCurrentStreamUrl(){
        return settings.getString(KEY_CURRENT_STREAM_URL,"");
    }


    public void setKeyCurrentStreamTitle(String title){
        SharedPreferences.Editor edit = settings.edit();
        edit.putString(KEY_CURRENT_STREAM_TITLE,title);
        edit.apply();
    }

    public String getKeyCurrentStreamTitle(){
        return settings.getString(KEY_CURRENT_STREAM_TITLE,"");
    }


    public void setKeyCurrentStreamTagline(String tagline){
        SharedPreferences.Editor edit = settings.edit();
        edit.putString(KEY_CURRENT_STREAM_TAGLINE,tagline);
        edit.apply();
    }

    public String getKeyCurrentStreamTagline(){
        return settings.getString(KEY_CURRENT_STREAM_TAGLINE,"");
    }

    public void setKeyCurrentStreamArt(String art){
        SharedPreferences.Editor edit = settings.edit();
        edit.putString(KEY_CURRENT_STREAM_ART, art);
        edit.apply();
    }

    public String getKeyCurrentStreamArt(){
        return settings.getString(KEY_CURRENT_STREAM_ART,"");
    }

    public void setKeyCurrentPlaylistId(int playlistId){
        SharedPreferences.Editor edit = settings.edit();
        edit.putInt(KEY_CURRENT_PLAYLIST_ID, playlistId);
        edit.apply();
    }

    public int getKeyCurrentPlaylistId(){
        return settings.getInt(KEY_CURRENT_PLAYLIST_ID, -1);
    }

    public void setKeyCurrentStreamItemId(int itemId){
        SharedPreferences.Editor edit = settings.edit();
        edit.putInt(KEY_CURRENT_STREAM_ITEM_ID, itemId);
        edit.apply();
    }

    public int getKeyCurrentStreamItemId(){
        return settings.getInt(KEY_CURRENT_STREAM_ITEM_ID, -1);
    }

    public void setKeyCurrentPlaybackPosition(int seekPosition){
        SharedPreferences.Editor edit = settings.edit();
        edit.putInt(KEY_CURRENT_STREAM_PLAYBACK_POSTION, seekPosition);
        edit.apply();
    }

    public int getKeyCurrentPlaybackPosition(){
        return settings.getInt(KEY_CURRENT_STREAM_PLAYBACK_POSTION, -1);
    }

    public void clear() {
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();
    }
}
