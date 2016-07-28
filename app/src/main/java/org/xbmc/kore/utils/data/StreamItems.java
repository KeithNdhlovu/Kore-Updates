package org.xbmc.kore.utils.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class StreamItems {
    /**
     * A container for the information associated with a
     * sample media item.
     */
    public static class StreamItem {
        @NonNull
        private String title;
        @NonNull
        private String mediaUrl;
        @Nullable
        private String artworkUrl;
        @Nullable
        private String tagline;

        public StreamItem(@NonNull String title, @NonNull String mediaUrl) {
            this(title, mediaUrl, null, null);
        }

        public StreamItem(@NonNull String title, @NonNull String mediaUrl, @Nullable String artworkUrl, @Nullable String tagline) {
            this.title = title;
            this.mediaUrl = mediaUrl;
            this.artworkUrl = artworkUrl;
            this.tagline = tagline;
        }

        @NonNull
        public String getTitle() {
            return title;
        }

        @NonNull
        public String getMediaUrl() {
            return mediaUrl;
        }

        @Nullable
        public String getArtworkUrl() {
            return artworkUrl;
        }

        @Nullable
        public String getTagline() {
            return tagline;
        }
    }
}
