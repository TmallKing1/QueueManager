package top.pigest.queuemanagerdemo.system.settings;

import com.google.gson.annotations.SerializedName;
import top.pigest.queuemanagerdemo.Settings;

public class SaveSettings {
    @SerializedName("refresh_token")
    public String refreshToken = null;
    @SerializedName("last_refresh_time")
    public long lastRefreshTime = 0;
    @SerializedName("danmaku_service")
    public DanmakuServiceSettings danmakuServiceSettings = new DanmakuServiceSettings();

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        Settings.saveSettings();
    }

    public long getLastRefreshTime() {
        return lastRefreshTime;
    }

    public void setLastRefreshTime(long lastRefreshTime) {
        this.lastRefreshTime = lastRefreshTime;
        Settings.saveSettings();
    }

    public DanmakuServiceSettings getDanmakuServiceSettings() {
        return danmakuServiceSettings;
    }
}
