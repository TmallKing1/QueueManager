package top.pigest.queuemanagerdemo.settings;

import com.google.gson.annotations.SerializedName;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.music.MusicHandler;

import java.util.ArrayList;
import java.util.List;

public class MusicServiceSettings {
    @SerializedName("always_on_top")
    public boolean alwaysOnTop = false;
    //page 1
    @SerializedName("auto_play")
    public boolean autoPlay = false;
    @SerializedName("background_color")
    public String backgroundColor = "#1f1e33";
    @SerializedName("background_opacity")
    public double backgroundOpacity = 1.0;
    @SerializedName("text_color")
    public String textColor = "#ffffff";
    @SerializedName("text_font")
    public String textFont = "";
    @SerializedName("display_lyric")
    public boolean displayLyric = true;
    @SerializedName("display_list")
    public boolean displayList = true;
    @SerializedName("list_height")
    public int listHeight = 300;
    @SerializedName("display_hint")
    public boolean displayHint = true;
    @SerializedName("default_playlist")
    public String defaultPlaylist = "0";
    @SerializedName("volume")
    public double volume = 1;
    @SerializedName("music_level")
    public MusicLevel musicLevel = MusicLevel.STANDARD;
    @SerializedName("play_mode")
    public PlayMode playMode = PlayMode.REPEAT;
    @SerializedName("play_previous")
    public boolean playPrevious = false;
    @SerializedName("earlyDownload")
    public boolean earlyDownload = false;

    @SerializedName("request_header")
    public String requestHeader = "点歌";
    @SerializedName("skip_header")
    public String skipHeader = "切歌";
    @SerializedName("remove_header")
    public String removeHeader = "移除歌曲";
    @SerializedName("top_header")
    public String topHeader = "置顶";
    @SerializedName("play_header")
    public String playHeader = "播放";
    @SerializedName("skip_users")
    public List<UserGroups> skipUsers = new ArrayList<>(List.of(UserGroups.ANCHOR));
    @SerializedName("request_cooldown")
    public int requestCooldown = 0;
    @SerializedName("max_request_count")
    public int maxRequestCount = 0;
    @SerializedName("allow_no_space")
    public boolean allowNoSpace = false;
    @SerializedName("instant_play")
    public boolean instantPlay = true;

    @SerializedName("no_vip_songs")
    public boolean noVipSongs = false;

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
        Settings.saveSettings();
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        Settings.saveSettings();
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        Settings.saveSettings();
    }

    public void setBackgroundOpacity(double backgroundOpacity) {
        this.backgroundOpacity = backgroundOpacity;
        Settings.saveSettings();
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
        Settings.saveSettings();
    }

    public void setTextFont(String textFont) {
        this.textFont = textFont;
        Settings.saveSettings();
    }

    public void setDisplayList(boolean displayList) {
        this.displayList = displayList;
        Settings.saveSettings();
    }

    public void setListHeight(int listHeight) {
        this.listHeight = listHeight;
        Settings.saveSettings();
    }

    public void setDisplayLyric(boolean displayLyric) {
        this.displayLyric = displayLyric;
        Settings.saveSettings();
    }

    public void setDefaultPlaylist(String defaultPlaylist) {
        this.defaultPlaylist = defaultPlaylist;
        Settings.saveSettings();
    }

    public void setVolume(double volume) {
        this.volume = volume;
        MusicHandler.INSTANCE.setVolume(volume);
        Settings.saveSettings();
    }

    public void setMusicLevel(MusicLevel musicLevel) {
        this.musicLevel = musicLevel;
        Settings.saveSettings();
    }

    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
        Settings.saveSettings();
    }

    public void setEarlyDownload(boolean earlyDownload) {
        this.earlyDownload = earlyDownload;
        Settings.saveSettings();
    }

    public void setRequestHeader(String requestHeader) {
        this.requestHeader = requestHeader;
        Settings.saveSettings();
    }

    public void setSkipHeader(String skipHeader) {
        this.skipHeader = skipHeader;
        Settings.saveSettings();
    }

    public void setRemoveHeader(String removeHeader) {
        this.removeHeader = removeHeader;
        Settings.saveSettings();
    }

    public void setTopHeader(String topHeader) {
        this.topHeader = topHeader;
    }

    public void setPlayHeader(String playHeader) {
        this.playHeader = playHeader;
    }

    public void modifySkipUsers(UserGroups userGroups, boolean enable) {
        if (enable) {
            skipUsers.add(userGroups);
        } else {
            skipUsers.remove(userGroups);
        }
        Settings.saveSettings();
    }

    public void setAllowNoSpace(boolean allowNoSpace) {
        this.allowNoSpace = allowNoSpace;
        Settings.saveSettings();
    }

    public void setInstantPlay(boolean instantPlay) {
        this.instantPlay = instantPlay;
        Settings.saveSettings();
    }

    public void setPlayPrevious(boolean playPrevious) {
        this.playPrevious = playPrevious;
        Settings.saveSettings();
    }

    public void setDisplayHint(boolean displayHint) {
        this.displayHint = displayHint;
        Settings.saveSettings();
    }

    public void setRequestCooldown(int requestCooldown) {
        this.requestCooldown = requestCooldown;
        Settings.saveSettings();
    }

    public void setMaxRequestCount(int maxRequestCount) {
        this.maxRequestCount = maxRequestCount;
        Settings.saveSettings();
    }

    public void setNoVipSongs(boolean noVipSongs) {
        this.noVipSongs = noVipSongs;
        Settings.saveSettings();
    }

    public enum MusicLevel {
        STANDARD("标准"),
        HIGHER("较高"),
        EXHIGH("极高"),
        LOSSLESS("无损"),
        HIRES("Hi-Res");

        private final String name;

        MusicLevel(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        public String getName() {
            return name;
        }
    }

    public enum PlayMode {
        SINGLE("单曲循环"),
        RANDOM("随机播放"),
        REPEAT("顺序播放");

        private final String name;

        PlayMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum UserGroups {
        ANCHOR("主播"),
        ADMIN("房管"),
        GUARD("舰长"),
        MEDAL("粉丝"),
        ALL("所有观众");

        private final String name;

        UserGroups(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
