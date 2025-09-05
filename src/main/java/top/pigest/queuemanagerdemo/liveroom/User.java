package top.pigest.queuemanagerdemo.liveroom;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * 用户类，部分场合下使用。
 * 由于通过不同途径获得的 Json 对象中代表用户名、UID 等字段的键名可能不同
 * (例如用户名可能对应 {@code uname}、{@code username}、{@code name}等多个键名），
 * 构造对象时需要先解析用户名、UID 这两个必需字段并传入构造函数，然后根据需要添加其他字段。
 */
public class User {
    /**
     * 用户名
     */
    private String username;
    /**
     * UID
     */
    private final long uid;
    /**
     * 头像链接
     */
    private String face;
    /**
     * 粉丝牌
     */
    private FansMedal fansMedal;
    /**
     * 舰长信息，包含到期时间。需要请求单独的 API。
     * @see LiveRoomApi#getGuardsWithExpireDate(int)
     */
    private GuardInfo guardInfo;

    public User(String username, long uid) {
        this.username = username;
        this.uid = uid;
    }

    public long getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public FansMedal getFansMedal() {
        return fansMedal;
    }

    public User setFansMedal(FansMedal fansMedal) {
        this.fansMedal = fansMedal;
        return this;
    }

    public User setFansMedal(JsonObject jsonObject) {
        return this.setFansMedal(FansMedal.deserializeUInfoMedal(jsonObject));
    }

    public String getFace() {
        return face;
    }

    public User setFace(String face) {
        this.face = face;
        return this;
    }

    public GuardInfo getGuardInfo() {
        return guardInfo;
    }

    public User setGuardInfo(GuardInfo guardInfo) {
        this.guardInfo = guardInfo;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return getUid() == user.getUid();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUid());
    }
}
