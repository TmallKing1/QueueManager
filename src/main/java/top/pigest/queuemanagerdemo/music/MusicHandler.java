package top.pigest.queuemanagerdemo.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.liveroom.LiveRoomApi;
import top.pigest.queuemanagerdemo.settings.MusicServiceSettings;
import top.pigest.queuemanagerdemo.util.ArrayObservableList;
import top.pigest.queuemanagerdemo.util.Three;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.window.music.MusicPlayerScene;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;

public class MusicHandler {
    public static final MusicHandler INSTANCE = new MusicHandler();
    public static final Path MUSIC_CACHE = Settings.DATA_DIRECTORY.toPath().resolve("musicCache");

    private final Image noPlaying;
    private final MusicDownloader musicDownloader = new MusicDownloader(this);
    private final Stage musicPlayerStage;
    final MusicPlayerScene player;
    private final ObservableList<Song> history = new ArrayObservableList<>();
    private final List<Song> songs = new ArrayList<>();
    private final List<String> playlist = new ArrayList<>();
    private final Map<Long, Timer> cooldown = new HashMap<>();
    private boolean selfPlaylist = false;

    private MediaPlayer media = null;

    private MusicHandler() {
        this.noPlaying = new Image(Objects.requireNonNull(QueueManager.class.getResourceAsStream("default_album.jpg")), 250, 250, true, true);
        this.musicPlayerStage = new Stage(StageStyle.TRANSPARENT);
        this.player = new MusicPlayerScene(this);
    }

    public void addSong(Song song) {
        songs.add(song);
        Platform.runLater(() -> this.player.addQueueSong(songs, -1));
    }

    public void addSong(Song song, int index) {
        songs.add(index, song);
        Platform.runLater(() -> this.player.addQueueSong(songs, index));
    }

    public void removeSong(Song song) {
        if (this.songs.contains(song)) {
            List<Song> songs1 = List.copyOf(this.songs);
            Platform.runLater(() -> this.player.removeQueueSong(songs1, songs1.indexOf(song)));
            songs.remove(song);
        }
    }

    public void topSong(Song song) {
        if (this.songs.contains(song)) {
            int i = this.songs.indexOf(song);
            if (i != 0) {
                this.songs.remove(song);
                this.songs.addFirst(song);
                this.player.moveQueueSong(songs, i, 0);
            }
        } else {
            addSong(song, 0);
        }
    }

    public void moveSong(Song song, int index) {
        if (this.songs.contains(song)) {
            int i = this.songs.indexOf(song);
            this.songs.remove(song);
            this.songs.add(index, song);
            this.player.moveQueueSong(songs, i, index);
        }
    }

    public void playPrevious() {
        if (history.size() < 2) {
            return;
        }
        Song song = history.remove(history.size() - 2);
        play(song, true);
    }

    public void play(Song song) {
        this.play(song, false);
    }

    public void play(Song song, boolean fromHistory) {
        if (!songs.isEmpty() && song.equals(songs.getFirst()) && media != null) {
            media.play();
            return;
        }
        if (media != null) {
            media.dispose();
            media = null;
        }
        topSong(song);
        if (song.isDownloaded()) {
            File f = getFile(songs.getFirst());
            if (f.exists()) {
                media = new MediaPlayer(new Media(f.toURI().toString()));
                media.setOnEndOfMedia(() -> {
                    if (fromHistory) {
                        history.remove(songs.getFirst());
                        history.add(songs.getFirst());
                    }
                    playNext();
                });
                media.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                    if (getMusicServiceSettings().displayLyric) {
                        int cur = (int) newValue.toMillis();
                        this.player.updateLyric(cur);
                    }
                });
                media.play();
                Platform.runLater(() -> {
                    media.setVolume(getMusicServiceSettings().volume);
                    this.player.updateContent();
                });
                song.initLyrics();
                media.statusProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue == MediaPlayer.Status.PLAYING || newValue == MediaPlayer.Status.PAUSED) {
                        this.player.refreshControlButtons(false);
                    }
                });

                if (getMusicServiceSettings().earlyDownload && songs.size() > 1) {
                    this.musicDownloader.download(songs.get(1));
                }
            }
            if (!fromHistory) {
                history.remove(songs.getFirst());
                history.add(songs.getFirst());
            }
        } else if (!musicDownloader.isDownloading(song)) {
            musicDownloader.download(song);
        }
    }

    public void playNext() {
        this.playNext(false);
    }

    public void playNext(boolean bypassSingle) {
        if (endFirst(bypassSingle)) return;
        if (!songs.isEmpty()) { // 第二次检测：若已下载则播放，没有下载则开始下载（下载完成时若下载的刚好是第一首则自动播放）
            if (songs.getFirst().isDownloaded()) {
                play(songs.getFirst());
            } else {
                this.musicDownloader.downloadIfNotDownloading(songs.getFirst());
            }
        } else { // 歌单里歌已经放完，填充空闲歌单
            if (playlist.isEmpty()) {
                if (!getMusicServiceSettings().defaultPlaylist.equals("0")) {
                    fillPlayList();
                } else {
                    stop();
                }
            } else {
                boolean errorFlag = false;
                while (!addSelfPlaylistSong(playlist.removeFirst())) {
                    if (playlist.isEmpty()) {
                        if (!errorFlag) {
                            if (!getMusicServiceSettings().defaultPlaylist.equals("0")) {
                                fillPlayList();
                                errorFlag = true;
                            } else {
                                stop();
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean endFirst(boolean bypassSingle) {
        if (!songs.isEmpty()) {
            Utils.onPresent(media, MediaPlayer::dispose);
            media = null;
            File f = getFile(songs.getFirst());
            if (f.exists()) {
                if (getMusicServiceSettings().playMode == MusicServiceSettings.PlayMode.SINGLE && !bypassSingle) {
                    media = new MediaPlayer(new Media(f.toURI().toString()));
                    media.setOnEndOfMedia(this::playNext);
                    media.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                        if (getMusicServiceSettings().displayLyric) {
                            int cur = (int) newValue.toMillis();
                            this.player.updateLyric(cur);
                        }
                    });
                    media.play();
                    Platform.runLater(() -> media.setVolume(getMusicServiceSettings().volume));
                    this.getCurrentSong().getLyrics().reset();
                    media.statusProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue == MediaPlayer.Status.PLAYING || newValue == MediaPlayer.Status.PAUSED) {
                            this.player.refreshControlButtons(false);
                        }
                    });
                    return true;
                }
                clean();
            }
            removeSong(songs.getFirst());
        }
        return false;
    }

    public void fillPlayList() {
        String defaultPlaylist = getMusicServiceSettings().defaultPlaylist;
        if (defaultPlaylist.equals("0")) {
            return;
        }
        try {
            JsonObject o = Music163Api.playlist(defaultPlaylist);
            for (JsonElement track : o.getAsJsonObject("playlist").getAsJsonArray("trackIds")) {
                this.playlist.add(track.getAsJsonObject().get("id").getAsString());
            }
            if (getMusicServiceSettings().playMode == MusicServiceSettings.PlayMode.RANDOM) {
                Collections.shuffle(playlist);
            }
            playNext();
        } catch (Exception e) {
            if (getMusicServiceSettings().displayHint) {
                Platform.runLater(() -> Utils.showDialogMessage("获取歌单信息失败", true, this.player.getRootStackPane(), 4, 300));
            }
        }
    }

    protected static File getFile(Song song, String format) {
        if (!MUSIC_CACHE.toFile().exists() || !MUSIC_CACHE.toFile().isDirectory()) {
            MUSIC_CACHE.toFile().mkdir();
        }
        return MUSIC_CACHE.resolve(song.getId() + "." + format).toFile();
    }

    protected static File getFile(Song song) {
        if (!MUSIC_CACHE.toFile().exists() || !MUSIC_CACHE.toFile().isDirectory()) {
            MUSIC_CACHE.toFile().mkdir();
        }
        return MUSIC_CACHE.resolve(song.getId() + "." + song.getFormat()).toFile();
    }

    private void clean() {
        File file = MUSIC_CACHE.toFile();
        if (!file.exists() || !file.isDirectory()) {
            MUSIC_CACHE.toFile().mkdir();
            return;
        }
        for (File listFile : Objects.requireNonNull(file.listFiles())) {
            if (songs.stream().noneMatch(song -> song.getId().equals(listFile.getName().substring(0, listFile.getName().lastIndexOf('.'))))) {
                listFile.delete();
            }
        }
    }

    public boolean addSong(String id, String user) {
        if (!selfPlaylist && this.songs.stream().anyMatch(song -> song.getId().equals(id))) {
            return false;
        }
        try {
            JsonObject o = Music163Api.songDetail(id);
            JsonObject trackObject = o.getAsJsonArray("songs").get(0).getAsJsonObject();
            if (trackObject.get("name").isJsonNull()) {
                if (getMusicServiceSettings().displayHint) {
                    Platform.runLater(() -> Utils.showDialogMessage("指定的歌曲不存在", true, this.player.getRootStackPane(), 4, 300));
                }
                return false;
            }
            if (getMusicServiceSettings().noVipSongs && (trackObject.get("fee").getAsInt() == 1 || trackObject.get("fee").getAsInt() == 4)) {
                if (getMusicServiceSettings().displayHint) {
                    Platform.runLater(() -> Utils.showDialogMessage("主播已设置禁止播放版权歌曲", true, this.player.getRootStackPane(), 4, 300));
                }
                return false;
            }
            Song song = createSong(trackObject, user);
            if (selfPlaylist) {
                if (getMusicServiceSettings().instantPlay) {
                    clear(false);
                    play(song);
                } else {
                    Iterator<Song> iterator = songs.iterator();
                    while (iterator.hasNext()) {
                        iterator.next();
                        iterator.remove();
                    }
                    this.addSong(song);
                    if (getMusicServiceSettings().earlyDownload) {
                        this.musicDownloader.download(song);
                    }
                }
                selfPlaylist = false;
            } else {
                this.addSong(song);
                if (this.media == null && this.songs.indexOf(song) == 0) {
                    play(song);
                }
                if (getMusicServiceSettings().earlyDownload && this.songs.indexOf(song) == 1) {
                    this.musicDownloader.download(song);
                }
            }
            return true;
        } catch (Exception e) {
            if (getMusicServiceSettings().displayHint) {
                Platform.runLater(() -> Utils.showDialogMessage("添加歌曲失败", true, this.player.getRootStackPane(), 4, 300));
            }
            return false;
        }
    }

    public boolean addSelfPlaylistSong(String id) {
        try {
            JsonObject o = Music163Api.songDetail(id);
            JsonObject trackObject = o.getAsJsonArray("songs").get(0).getAsJsonObject();
            if (trackObject.get("name").isJsonNull()) {
                if (getMusicServiceSettings().displayHint) {
                    Platform.runLater(() -> Utils.showDialogMessage("指定的歌曲不存在", true, this.player.getRootStackPane(), 4, 300));
                }
                return false;
            }
            if (getMusicServiceSettings().noVipSongs && (trackObject.get("fee").getAsInt() == 1 || trackObject.get("fee").getAsInt() == 4)) {
                return false;
            }
            Song song = createSong(trackObject, null);
            this.addSong(song);
            this.selfPlaylist = true;
            play(song);
            return true;
        } catch (Exception e) {
            if (getMusicServiceSettings().displayHint) {
                Platform.runLater(() -> Utils.showDialogMessage("添加歌曲失败", true, this.player.getRootStackPane(), 4, 300));
            }
        }
        return false;
    }

    public boolean searchSong(String name, String artist, String user) {
        try {
            JsonObject o = Music163Api.searchMusic(name);
            JsonArray tracksArray = o.getAsJsonObject("result").getAsJsonArray("songs");
            if (getMusicServiceSettings().noVipSongs) {
                Iterator<JsonElement> it = tracksArray.iterator();
                while (it.hasNext()) {
                    JsonObject trackObject = it.next().getAsJsonObject();
                    if (trackObject.get("fee").getAsInt() == 1 || trackObject.get("fee").getAsInt() == 4) {
                        it.remove();
                    }
                }
            }
            if (artist == null) {
                if (!tracksArray.isEmpty()) {
                    return addSong(tracksArray.get(0).getAsJsonObject().get("id").getAsString(), user);
                } else {
                    if (getMusicServiceSettings().displayHint) {
                        Platform.runLater(() -> Utils.showDialogMessage("未搜索到歌曲", true, this.player.getRootStackPane(), 4, 300));
                    }
                    return false;
                }
            } else {
                List<Three<String, String, String>> list = StreamSupport.stream(tracksArray.spliterator(), false).map(JsonElement::getAsJsonObject)
                        .map(jsonObject -> new Three<>(jsonObject.get("id").getAsString(), jsonObject.get("name").getAsString(), getArtistJoined(jsonObject))).toList();
                Optional<String> id1 = list.stream().filter(three -> three.getThird().equals(artist)).map(Three::getFirst).findFirst();
                if (id1.isPresent()) {
                    return addSong(id1.get(), user);
                } else {
                    Optional<String> id2 = list.stream().filter(three -> three.getThird().contains(artist)).map(Three::getFirst).findFirst();
                    return id2.map(s -> addSong(s, user)).orElseGet(() -> searchSong(name + " " + artist, null, user));
                }
            }
        } catch (Exception e) {
            if (getMusicServiceSettings().displayHint) {
                Platform.runLater(() -> Utils.showDialogMessage("添加歌曲失败", true, this.player.getRootStackPane(), 4, 300));
            }
            return false;
        }
    }

    private Song createSong(JsonObject trackObject, String user) {
        String id = trackObject.get("id").getAsString();
        String name = trackObject.get("name").getAsString();
        String artistJoined = getArtistJoined(trackObject);
        String coverUrl;
        try {
            coverUrl = trackObject.getAsJsonObject("al").get("picUrl").getAsString();
        } catch (Exception e) {
            coverUrl = null;
        }
        return new Song(id, name, artistJoined, coverUrl, user);
    }

    private static String getArtistJoined(JsonObject trackObject) {
        List<String> artists = new ArrayList<>();
        trackObject.get("ar").getAsJsonArray().forEach(a -> {
            if (a.getAsJsonObject().get("name").isJsonNull()) {
                artists.add("未知歌手");
                return;
            }
            artists.add(a.getAsJsonObject().get("name").getAsString());
        });
        return String.join(" / ", artists);
    }

    public void handleSingleDanmaku(JsonObject object) {
        if (!Settings.hasCookie("MUSIC_U")) {
            return;
        }
        JsonArray info = object.getAsJsonArray("info");
        String text = info.get(1).getAsString();
        long uid = info.get(2).getAsJsonArray().get(0).getAsLong();
        String userName = info.get(2).getAsJsonArray().get(1).getAsString();
        if (!getMusicServiceSettings().requestHeader.isEmpty() && text.startsWith(getMusicServiceSettings().requestHeader)) {
            if (cooldown.containsKey(uid)) {
                if (getMusicServiceSettings().displayHint) {
                    Platform.runLater(() -> Utils.showDialogMessage("请等待点歌冷却结束", true, this.player.getRootStackPane(), 4, 300));
                }
                return;
            }
            if (getMusicServiceSettings().maxRequestCount > 0 && songs.stream().filter(song -> song.getUser() != null).count() >= getMusicServiceSettings().maxRequestCount) {
                if (getMusicServiceSettings().displayHint) {
                    Platform.runLater(() -> Utils.showDialogMessage("当前歌曲数已达上限", true, this.player.getRootStackPane(), 4, 300));
                }
                return;
            }
            text = text.substring(getMusicServiceSettings().requestHeader.length());
            if (text.charAt(0) == '#') {
                if (text.substring(1).matches("[0-9]+")) {
                    boolean bl = addSong(text.substring(1), userName);
                    requestCooldown(bl, uid);
                } else {
                    if (getMusicServiceSettings().displayHint) {
                        Platform.runLater(() -> Utils.showDialogMessage("点歌ID格式错误", true, this.player.getRootStackPane(), 4, 300));
                    }
                }
            } else if (text.charAt(0) == ' ' || getMusicServiceSettings().allowNoSpace) {
                text = text.trim();
                String artist = null;
                int atIndex = text.indexOf('@');
                if (atIndex != -1) {
                    artist = text.substring(atIndex + 1);
                    text = text.substring(0, atIndex);
                }
                boolean bl = searchSong(text, artist, userName);
                requestCooldown(bl, uid);
            }
            return;
        }
        if (!getMusicServiceSettings().skipHeader.isEmpty() && text.equals(getMusicServiceSettings().skipHeader)) {
            boolean pass = isPass(info);
            if (pass) {
                this.playNext(true);
            }
            return;
        }
        if (!getMusicServiceSettings().removeHeader.isEmpty() && text.startsWith(getMusicServiceSettings().removeHeader)) {
            text = text.substring(getMusicServiceSettings().removeHeader.length());
            if (text.charAt(0) == ' ' || getMusicServiceSettings().allowNoSpace) {
                text = text.trim();
                try {
                    int index = Integer.parseInt(text);
                    if (songs.size() > index && isPass(info)) {
                        removeSong(songs.get(index));
                    }
                } catch (Exception e) {
                    if (getMusicServiceSettings().displayHint) {
                        Platform.runLater(() -> Utils.showDialogMessage("命令参数有误", true, this.player.getRootStackPane(), 4, 300));
                    }
                }
            }
        }
        if (!getMusicServiceSettings().topHeader.isEmpty() && text.startsWith(getMusicServiceSettings().topHeader)) {
            text = text.substring(getMusicServiceSettings().topHeader.length());
            if (text.charAt(0) == ' ' || getMusicServiceSettings().allowNoSpace) {
                text = text.trim();
                try {
                    int index = Integer.parseInt(text);
                    if (songs.size() > index && index >= 2 && isPass(info)) {
                        moveSong(songs.get(index), 1);
                    }
                } catch (Exception e) {
                    if (getMusicServiceSettings().displayHint) {
                        Platform.runLater(() -> Utils.showDialogMessage("命令参数有误", true, this.player.getRootStackPane(), 4, 300));
                    }
                }
            }
        }
        if (!getMusicServiceSettings().playHeader.isEmpty() && text.startsWith(getMusicServiceSettings().playHeader)) {
            text = text.substring(getMusicServiceSettings().playHeader.length());
            if (text.charAt(0) == ' ' || getMusicServiceSettings().allowNoSpace) {
                text = text.trim();
                try {
                    int index = Integer.parseInt(text);
                    if (songs.size() > index && index >= 1 && isPass(info)) {
                        play(songs.get(index));
                    }
                } catch (Exception e) {
                    if (getMusicServiceSettings().displayHint) {
                        Platform.runLater(() -> Utils.showDialogMessage("命令参数有误", true, this.player.getRootStackPane(), 4, 300));
                    }
                }
            }
        }
    }

    private void requestCooldown(boolean bl, long uid) {
        if (bl && Settings.getMusicServiceSettings().requestCooldown > 0) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    MusicHandler.this.cooldown.remove(uid);
                }
            }, Settings.getMusicServiceSettings().requestCooldown * 1000L);
            this.cooldown.put(uid, timer);
        }
    }

    private static boolean isPass(JsonArray info) {
        boolean pass = false;
        long uid = info.get(0).getAsJsonArray().get(15).getAsJsonObject().getAsJsonObject("user").get("uid").getAsLong();
        if (uid == Settings.MID && getMusicServiceSettings().skipUsers.contains(MusicServiceSettings.UserGroups.ANCHOR)) {
            pass = true;
        }
        if (getMusicServiceSettings().skipUsers.contains(MusicServiceSettings.UserGroups.ALL)) {
            pass = true;
        }
        if (!pass && getMusicServiceSettings().skipUsers.contains(MusicServiceSettings.UserGroups.ADMIN)) {
            try {
                Map<Long, String> map = LiveRoomApi.getRoomAdmins();
                pass = map.containsKey(uid);
            } catch (Exception ignored) {
            }
        }
        if (!pass && getMusicServiceSettings().skipUsers.contains(MusicServiceSettings.UserGroups.GUARD)) {
            try {
                Map<Long, String> map = LiveRoomApi.getGuards();
                pass = map.containsKey(uid);
            } catch (Exception ignored) {
            }
        }
        if (!pass && getMusicServiceSettings().skipUsers.contains(MusicServiceSettings.UserGroups.MEDAL)) {
            JsonElement element = info.get(0).getAsJsonArray().get(15).getAsJsonObject().getAsJsonObject("user").get("medal");
            if (element.isJsonObject()) {
                JsonObject medalObject = element.getAsJsonObject();
                pass = medalObject.get("ruid").getAsLong() == Settings.MID;
            }
        }
        return pass;
    }

    private static MusicServiceSettings getMusicServiceSettings() {
        return Settings.getMusicServiceSettings();
    }

    public void showStage() {
        musicPlayerStage.setScene(player);
        musicPlayerStage.setAlwaysOnTop(Settings.getMusicServiceSettings().alwaysOnTop);
        musicPlayerStage.setResizable(false);
        musicPlayerStage.setTitle("Queue Manager 播放器");
        musicPlayerStage.show();
    }

    public MediaPlayer getMedia() {
        return media;
    }

    public MusicPlayerScene getPlayer() {
        return player;
    }

    public Stage getPlayerStage() {
        return musicPlayerStage;
    }

    public void setVolume(double volume) {
        if (this.media != null) {
            this.media.setVolume(volume);
        }
    }

    public void pause() {
        this.media.pause();

    }

    public State getState() {
        if (this.media != null) {
            if (this.media.getStatus() == MediaPlayer.Status.PAUSED) {
                return State.PAUSED;
            }
            if (this.media.getStatus() == MediaPlayer.Status.PLAYING) {
                return State.PLAYING;
            }
        }
        if (this.musicDownloader.isDownloading()) {
            return State.DOWNLOADING;
        }
        return State.STOPPED;
    }

    public boolean isPaused() {
        return this.getState() == State.PAUSED;
    }

    public void stop() {
        if (this.media != null) {
            this.media.dispose();
        }
        this.media = null;
        Utils.onPresent(this.songs.getFirst(), song -> {
            File file = getFile(song);
            if (file.exists()) {
                file.delete();
            }
        });
        this.player.stopPlaying();
    }

    public void clear(boolean clearPlaylist) {
        if (clearPlaylist) {
            playlist.clear();
        } else {
            this.stop();
            songs.clear();
            this.player.clearQueueSong();
        }
    }

    public void clear() {
        this.clear(true);
        this.clear(false);
    }

    public boolean isSelfPlaylist() {
        return selfPlaylist;
    }

    public Song getCurrentSong() {
        return songs.isEmpty() ? null : songs.getFirst();
    }

    public Image getNoPlayingImage() {
        return noPlaying;
    }

    public ObservableList<Song> getHistory() {
        return history;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public enum State {
        DOWNLOADING,
        PLAYING,
        PAUSED,
        STOPPED
    }
}
