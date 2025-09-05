package top.pigest.queuemanagerdemo.window.misc;

import java.util.ArrayList;
import java.util.List;

public class MiscRegistryManager {
    private static final List<MiscFunction> REGISTRIES = new ArrayList<>();

    public static void register(MiscFunction miscFunction) {
        REGISTRIES.add(miscFunction);
    }

    public static void registerPrimary() {
        register(new MiscFunction("网页开播", null, "#284CB8", "fas-video"));
        register(new MiscFunction("大航海列表", GuardManagementPage::new, "#F08650", "fas-anchor"));
        register(new MiscFunction("命令测试", CommandTestPage::new, "#003847", "fas-code"));
    }

    public static List<MiscFunction> getRegistries() {
        return REGISTRIES;
    }
}
