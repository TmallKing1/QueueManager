package top.pigest.queuemanagerdemo.settings;

import com.google.gson.annotations.SerializedName;
import top.pigest.queuemanagerdemo.Settings;

import java.util.*;

public class DanmakuServiceSettings{
    public static Map<NarratableElement, String> DEFAULT_NARRATOR_TEXT = new HashMap<>(Map.of(
            NarratableElement.ENTER, "{user}进入直播间",
            NarratableElement.DANMAKU, "{user}说{comment}",
            NarratableElement.GIFT, "{user}投喂了{amount}个{gift}",
            NarratableElement.GUARD, "{user}开通了{guard}",
            NarratableElement.SUPER_CHAT, "{user}开通了{amount}个月{guard}"
    ));

    @SerializedName("auto_connect")
    public boolean autoConnect = false;

    @SerializedName("narrator_enabled")
    public boolean narratorEnabled = false;
    @SerializedName("narrator_rate")
    public int narratorRate = 0;
    @SerializedName("narrator_volume")
    public int narratorVolume = 100;
    @SerializedName("narrator_voice_name")
    public String narratorVoiceName = "Microsoft Huihui Desktop";
    @SerializedName("narrator_type")
    public NarratorType narratorType = NarratorType.DEFAULT;
    @SerializedName("accepted_types")
    public Set<NarratableElement> acceptedTypes = new HashSet<>(List.of(NarratableElement.DANMAKU));
    @SerializedName("narrator_text")
    private Map<NarratableElement, String> narratorText = new HashMap<>(Map.of(
            NarratableElement.ENTER, "{user}进入直播间",
            NarratableElement.DANMAKU, "{user}说{comment}",
            NarratableElement.GIFT, "{user}投喂了{amount}个{gift}",
            NarratableElement.GUARD, "{user}开通了{guard}",
            NarratableElement.SUPER_CHAT, "{user}发送了一条SC：{comment}"
    ));
    @SerializedName("gift_combo_optimization")
    public boolean giftComboOptimization = false;
    @SerializedName("gift_combo_end_text")
    public String giftComboEndText = "{user}共投喂了{amount}个{gift}";
    @SerializedName("multi_guard_optimization")
    public boolean multiGuardOptimization = false;
    @SerializedName("multi_guard_text")
    public String multiGuardText = "{user}开通了{amount}个月{guard}";

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
        Settings.saveSettings();
    }

    public void setNarratorEnabled(boolean narratorEnabled) {
        this.narratorEnabled = narratorEnabled;
        Settings.saveSettings();
    }

    public void setNarratorRate(int narratorRate) {
        this.narratorRate = narratorRate;
        Settings.saveSettings();
    }

    public void setNarratorVolume(int narratorVolume) {
        this.narratorVolume = narratorVolume;
        Settings.saveSettings();
    }

    public void setNarratorVoiceName(String narratorVoiceName) {
        this.narratorVoiceName = narratorVoiceName;
        Settings.saveSettings();
    }

    public void setNarratorType(NarratorType narratorType) {
        this.narratorType = narratorType;
        Settings.saveSettings();
    }

    public void modifyAcceptedType(NarratableElement type, boolean enable) {
        if (enable) {
            acceptedTypes.add(type);
        } else {
            acceptedTypes.remove(type);
        }
        Settings.saveSettings();
    }

    public Map<NarratableElement, String> getNarratorText() {
        boolean save = false;
        Map<NarratableElement, String> narratorText = this.narratorText;
        for (NarratableElement element : NarratableElement.values()) {
            if (!narratorText.containsKey(element)) {
                narratorText.put(element, DEFAULT_NARRATOR_TEXT.get(element));
                save = true;
            }
        }
        if (save) {
            Settings.saveSettings();
        }
        return narratorText;
    }

    public String getNarratorText(NarratableElement narratableElement) {
        return getNarratorText().get(narratableElement);
    }

    public void setNarratorText(NarratableElement element, String narratorText) {
        this.narratorText.put(element, narratorText);
        Settings.saveSettings();
    }

    public void setGiftComboOptimization(boolean giftComboOptimization) {
        this.giftComboOptimization = giftComboOptimization;
        Settings.saveSettings();
    }

    public void setGiftComboEndText(String giftComboEndText) {
        this.giftComboEndText = giftComboEndText;
        Settings.saveSettings();
    }

    public void setMultiGuardOptimization(boolean multiGuardOptimization) {
        this.multiGuardOptimization = multiGuardOptimization;
        Settings.saveSettings();
    }

    public void setMultiGuardText(String multiGuardText) {
        this.multiGuardText = multiGuardText;
        Settings.saveSettings();
    }


    public enum NarratableElement{
        ENTER("进房", Map.of("user", "进房用户名")),
        DANMAKU("弹幕", Map.of("user", "发送者名称", "comment", "弹幕内容")),
        GIFT("礼物", Map.of("user", "送礼用户名", "amount", "送礼数量", "gift", "礼物名称")),
        GUARD("上舰", Map.of("user", "上舰用户名", "guard", "舰长/提督/总督")),
        SUPER_CHAT("SC", Map.of("user", "发送者名称", "comment", "SC内容"));

        private final String message;
        private final Map<String, String> arguments;

        NarratableElement(String message, Map<String, String> arguments) {
            this.message = message;
            this.arguments = arguments;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, String> getArguments() {
            return arguments;
        }
    }

    public enum NarratorType{
        DEFAULT("按序朗读（读完一条读下一条）"),
        INTERRUPTED("打断朗读（停止当前朗读并读下一条）"),
        STACKABLE("叠加朗读（不停止当前朗读并读下一条）");

        private final String message;

        NarratorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return this.message;
        }
    }

}
