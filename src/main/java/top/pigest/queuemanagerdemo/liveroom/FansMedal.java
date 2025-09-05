package top.pigest.queuemanagerdemo.liveroom;

import com.google.gson.JsonObject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Text;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;

import java.util.concurrent.CompletableFuture;

/**
 * 粉丝勋章类。
 * 不同途径获得的代表粉丝勋章的 Json 对象基本相同，可以使用 {@link FansMedal#deserializeMedalInfo(JsonObject)}
 * 方法直接从这些对象得到具有粉丝勋章的主要信息的 {@link FansMedal} 对象。
 *
 * @see LiveRoomApi#getFansUInfoMedal(long)
 * @see LiveRoomApi#getFansUInfoMedal(long, long)
 */
public class FansMedal {
    private String medalName;
    private String level;
    private int exp;
    private int nextExp;
    private int todayExp;
    private int dayLimitExp;
    private Boolean isLighted;
    private GuardType guardType;
    private String guardIcon;
    private FansMedalStyleOld oldStyle;
    private FansMedalStyle style;

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

    public int getTodayExp() {
        return todayExp;
    }

    public FansMedal setTodayExp(int todayExp) {
        this.todayExp = todayExp;
        return this;
    }

    public int getDayLimitExp() {
        return dayLimitExp;
    }

    public FansMedal setDayLimitExp(int dayLimitExp) {
        this.dayLimitExp = dayLimitExp;
        return this;
    }

    public Boolean getLighted() {
        return isLighted;
    }

    public FansMedal setLighted(Boolean lighted) {
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

    public String getGuardIcon() {
        return guardIcon;
    }

    public FansMedal setGuardIcon(String guardIcon) {
        this.guardIcon = guardIcon;
        return this;
    }

    public FansMedalStyleOld getOldStyle() {
        return oldStyle;
    }

    public FansMedal setOldStyle(FansMedalStyleOld oldStyle) {
        this.oldStyle = oldStyle;
        return this;
    }

    public FansMedalStyle getStyle() {
        return style;
    }

    public FansMedal setStyle(FansMedalStyle style) {
        this.style = style;
        return this;
    }

    /**
     * 获取粉丝牌显示节点（旧版）
     */
    public Node getDisplayOld() {
        StackPane root = new StackPane();
        root.setPrefHeight(50);
        root.setAlignment(Pos.CENTER_LEFT);
        HBox hBox = new HBox();
        hBox.setMaxHeight(40);
        hBox.setFillHeight(false);
        hBox.setBorder(new Border(new BorderStroke(
                getOldStyle().medalColorBorder,
                BorderStrokeStyle.SOLID,
                new CornerRadii(3, 3, 3, 3 ,false),
                new BorderWidths(2)
        )));
        StackPane namePane = new StackPane();
        namePane.setPrefHeight(40);
        namePane.setAlignment(Pos.CENTER);
        namePane.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, getOldStyle().medalColorStart), new Stop(1, getOldStyle().medalColorEnd)),
                new CornerRadii(3, 0, 0, 3 ,false),
                null
        )));
        namePane.setPadding(new Insets(0, 5, 0, 5));
        Text name = new Text(getMedalName());
        name.setFont(Settings.DEFAULT_FONT);
        name.setFill(Color.WHITE);
        namePane.getChildren().add(name);
        StackPane levelPane = new StackPane();
        levelPane.setPrefHeight(40);
        levelPane.setAlignment(Pos.CENTER);
        levelPane.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0, 3, 0, 3 ,false), null)));
        levelPane.setPadding(new Insets(0, 5, 0, 5));
        Text level = new Text(getLevel());
        level.setFont(Settings.DEFAULT_FONT);
        level.setFill(getOldStyle().medalColor);
        levelPane.getChildren().add(level);
        hBox.getChildren().addAll(namePane, levelPane);
        root.getChildren().add(hBox);
        if (this.guardIcon != null && !this.guardIcon.isEmpty()) {
            ImageView imageView = new ImageView();
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);
            CompletableFuture.supplyAsync(() -> new Image(this.guardIcon))
                    .thenAccept(imageView::setImage).join();
            namePane.setPadding(new Insets(0, 5, 0, 25));
            StackPane.setMargin(hBox, new Insets(0, 0, 0, 25));
            root.getChildren().add(imageView);
        }
        return root;
    }

    /**
     * 用于将 API 返回的 {@code medal_info} Json 对象转换为 {@link FansMedal} 对象<br>
     * 此种对象仅有旧版配色方案，且不包含灯牌是否点亮的信息
     * @param object API 返回的 Json 对象
     * @return 反序列化得到的 {@link FansMedal} 对象。
     */
    public static FansMedal deserializeMedalInfo(JsonObject object) {
        return deserializeMedalInfo(new FansMedal(), object);
    }

    public static FansMedal deserializeMedalInfo(FansMedal fansMedal, JsonObject object) {
        return fansMedal
                .setMedalName(object.get("medal_name").getAsString())
                .setLevel(object.get("level").getAsString())
                .setExp(object.get("intimacy").getAsInt())
                .setNextExp(object.get("next_intimacy").getAsInt())
                .setTodayExp(object.get("today_feed").getAsInt())
                .setDayLimitExp(object.get("day_limit").getAsInt())
                // 有个 API 有 is_lighted 这个字段所以还是加上了
                .setLighted(object.has("is_lighted") ? object.get("is_lighted").getAsInt() != 0 : null)
                .setGuardType(GuardType.valueOf(object.get("guard_level").getAsInt()))
                .setGuardIcon(object.has("guard_icon") ? object.get("guard_icon").getAsString() : null)
                .setOldStyle(FansMedalStyleOld.deserializeMedalInfo(object));
    }

    /**
     * 用于将 API 返回的 {@code uinfo_medal} Json 对象转换为 {@link FansMedal} 对象<br>
     * 此种对象不包含灯牌经验相关信息
     * @param object API 返回的 Json 对象
     * @return 反序列化得到的 {@link FansMedal} 对象。
     */
    public static FansMedal deserializeUInfoMedal(JsonObject object) {
        return deserializeUInfoMedal(new FansMedal(), object);
    }

    public static FansMedal deserializeUInfoMedal(FansMedal fansMedal, JsonObject object) {
        return fansMedal
                .setMedalName(object.get("name").getAsString())
                .setLevel(object.get("level").getAsString())
                .setLighted(object.get("is_light").getAsInt() != 0)
                .setGuardType(GuardType.valueOf(object.get("guard_level").getAsInt()))
                .setGuardIcon(object.has("guard_icon") ? object.get("guard_icon").getAsString() : null)
                .setOldStyle(FansMedalStyleOld.deserializeUInfoMedal(object))
                .setStyle(FansMedalStyle.deserializeUInfoMedal(object));
    }

    public record FansMedalStyleOld(Color medalColor, Color medalColorStart, Color medalColorEnd,
                                    Color medalColorBorder) {
        public static FansMedalStyleOld deserializeMedalInfo(JsonObject object) {
            Color medalColor = Utils.toColor(object.get("medal_color_start").getAsInt());
            Color medalColorStart = Utils.toColor(object.get("medal_color_start").getAsInt());
            Color medalColorEnd = Utils.toColor(object.get("medal_color_end").getAsInt());
            Color medalColorBorder = Utils.toColor(object.get("medal_color_border").getAsInt());
            return new FansMedalStyleOld(medalColor, medalColorStart, medalColorEnd, medalColorBorder);
        }


        public static FansMedalStyleOld deserializeUInfoMedal(JsonObject object) {
            Color medalColor = Utils.toColor(object.get("color").getAsInt());
            Color medalColorStart = Utils.toColor(object.get("color_start").getAsInt());
            Color medalColorEnd = Utils.toColor(object.get("color_end").getAsInt());
            Color medalColorBorder = Utils.toColor(object.get("color_border").getAsInt());
            return new FansMedalStyleOld(medalColor, medalColorStart, medalColorEnd, medalColorBorder);
        }
    }

    public record FansMedalStyle(Color start, Color end, Color border, Color text, Color level) {
        public static FansMedalStyle deserializeUInfoMedal(JsonObject object) {
            Color start = Color.valueOf(object.get("v2_medal_color_start").getAsString());
            Color end = Color.valueOf(object.get("v2_medal_color_end").getAsString());
            Color border = Color.valueOf(object.get("v2_medal_color_border").getAsString());
            Color text = Color.valueOf(object.get("v2_medal_color_text").getAsString());
            Color level = Color.valueOf(object.get("v2_medal_color_level").getAsString());
            return new FansMedalStyle(start, end, border, text, level);
        }
    }
}
