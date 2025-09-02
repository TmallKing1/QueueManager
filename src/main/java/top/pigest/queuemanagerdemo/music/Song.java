package top.pigest.queuemanagerdemo.music;

import com.google.gson.JsonObject;
import javafx.scene.image.Image;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Song {
    private final String id;
    private final String title;
    private final String artist;
    private final String coverUrl;
    private final String user;
    private String format = null;
    private Lyrics lyrics;
    private volatile Image cover;
    public Song(String id, String title, String artist, String coverUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.user = null;
    }

    public Song(String id, String title, String artist, String coverUrl, String user) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.user = user;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.id.equals(((Song) obj).id);
    }

    public String getId() {
        return id;
    }

    public Image getCover(Consumer<Image> consumer) {
        if (cover == null) {
            CompletableFuture.supplyAsync(this::fetchCover)
                    .whenComplete((image, throwable) -> {
                        if (image != null) {
                            this.cover = image;
                            consumer.accept(image);
                        }
                    });
            return MusicHandler.INSTANCE.getNoPlayingImage();
        }
        return cover;
    }

    public Image fetchCover() {
        return new Image(coverUrl, 60, 60, true, true);
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getUser() {
        return user;
    }

    public String getFormat() {
        return Objects.requireNonNullElse(format, "m4a");
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isDownloaded() {
        return format != null;
    }

    public void initLyrics() {
        this.lyrics = Lyrics.LOADING;
        CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject o = Music163Api.lyric(id);
                return new Lyrics(o.getAsJsonObject("lrc").get("lyric").getAsString());
            } catch (Exception e) {
                return Lyrics.NONE;
            }
        }).whenComplete((lyrics, throwable) -> this.lyrics = Objects.requireNonNullElse(lyrics, Lyrics.NONE));
    }

    public Lyrics getLyrics() {
        return lyrics;
    }
}
