package top.pigest.queuemanagerdemo.liveroom;

import com.google.gson.JsonObject;
import javafx.scene.paint.Color;

/**
 * 粉丝勋章类
 * @see LiveRoomApi#getFansMedal(long)
 * @see LiveRoomApi#getFansMedal(long, long)
 */
public class FansMedal {
    private String medalName;
    private String level;
    private int exp;
    private int nextExp;
    private boolean isLighted;
    private GuardType guardType;
    private Color medalColor;
    private Color medalColorStart;
    private Color medalColorEnd;
    private Color medalColorBorder;

    public String getMedalName() {
        return medalName;
    }

    public FansMedal setMedalName(String medalName) {
        this.medalName = medalName;
        return this;
    }

    public String getLevel() {
        return level;
    }

    public FansMedal setLevel(String level) {
        this.level = level;
        return this;
    }

    public int getExp() {
        return exp;
    }

    public FansMedal setExp(int exp) {
        this.exp = exp;
        return this;
    }

    public int getNextExp() {
        return nextExp;
    }

    public FansMedal setNextExp(int nextExp) {
        this.nextExp = nextExp;
        return this;
    }

    public boolean isLighted() {
        return isLighted;
    }

    public FansMedal setLighted(boolean lighted) {
        isLighted = lighted;
        return this;
    }

    public GuardType getGuardType() {
        return guardType;
    }

    public FansMedal setGuardType(GuardType guardType) {
        this.guardType = guardType;
        return this;
    }

    public Color getMedalColor() {
        return medalColor;
    }

    public FansMedal setMedalColor(Color medalColor) {
        this.medalColor = medalColor;
        return this;
    }

    public Color getMedalColorStart() {
        return medalColorStart;
    }

    public FansMedal setMedalColorStart(Color medalColorStart) {
        this.medalColorStart = medalColorStart;
        return this;
    }

    public Color getMedalColorEnd() {
        return medalColorEnd;
    }

    public FansMedal setMedalColorEnd(Color medalColorEnd) {
        this.medalColorEnd = medalColorEnd;
        return this;
    }

    public Color getMedalColorBorder() {
        return medalColorBorder;
    }

    public FansMedal setMedalColorBorder(Color medalColorBorder) {
        this.medalColorBorder = medalColorBorder;
        return this;
    }

    public static FansMedal deserialize(JsonObject object) {
        return new FansMedal()
                .setMedalName(object.get("medal_name").getAsString())
                .setLevel(object.get("level").getAsString())
                .setExp(object.get("intimacy").getAsInt())
                .setNextExp(object.get("next_intimacy").getAsInt())
                .setLighted(object.get("lighted").getAsInt() != 0)
                .setGuardType(GuardType.valueOf(object.get("guard_level").getAsInt()))
                .setMedalColor(Color.web(object.get("medal_color").getAsString()))
                .setMedalColorStart(Color.web(object.get("medal_color_start").getAsString()))
                .setMedalColorEnd(Color.web(object.get("medal_color_end").getAsString()))
                .setMedalColorBorder(Color.web(object.get("medal_color_border").getAsString()));
    }
}
