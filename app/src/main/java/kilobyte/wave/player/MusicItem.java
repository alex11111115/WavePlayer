package kilobyte.wave.player;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class MusicItem {
    private final String songTitle;
    private final String artistName;
    @DrawableRes
    private final int albumArtResId;
    private final String description;
    private final String data;
    private final int duration; // Duration in seconds

    private MusicItem(Builder builder) {
        this.songTitle = builder.songTitle;
        this.artistName = builder.artistName;
        this.albumArtResId = builder.albumArtResId;
        this.description = builder.description;
        this.data = builder.data;
        this.duration = builder.duration;
    }

    @NonNull
    public String getSongTitle() {
        return songTitle;
    }

    @NonNull
    public String getArtistName() {
        return artistName;
    }

    @DrawableRes
    public int getAlbumArtResId() {
        return albumArtResId;
    }

    @NonNull
    public String getDescription() {
        return description;
    }
    
    @NonNull
    public String setFilePath() {
        return data;
    }

    public int getDuration() {
        return duration;
    }

    public static class Builder {
        private String songTitle = "";
        private String artistName = "";
        @DrawableRes
        private int albumArtResId = 0;
        private String description = "";
        private int duration = 0;
        private String data;

        public Builder setSongTitle(@NonNull String songTitle) {
            this.songTitle = songTitle;
            return this;
        }

        public Builder setArtistName(@NonNull String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder setAlbumArtResId(@DrawableRes int albumArtResId) {
            this.albumArtResId = albumArtResId;
            return this;
        }

        public Builder setDescription(@NonNull String description) {
            this.description = description;
            return this;
        }
        
        public Builder setFilePath(@NonNull String data) {
            this.data = data;
            return this;
        }

        public Builder setDuration(int duration) {
            if (duration < 0) {
                throw new IllegalArgumentException("Duration must be non-negative");
            }
            this.duration = duration;
            return this;
        }

        @NonNull
        public MusicItem build() {
            return new MusicItem(this);
        }
    }
}