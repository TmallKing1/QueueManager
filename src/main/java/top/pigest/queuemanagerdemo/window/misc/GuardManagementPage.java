package top.pigest.queuemanagerdemo.window.misc;

import javafx.scene.layout.Pane;
import top.pigest.queuemanagerdemo.window.main.MultiMenuProvider;
import top.pigest.queuemanagerdemo.window.main.NamedPage;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class GuardManagementPage extends MultiMenuProvider<Pane> implements NamedPage {
    @Override
    public LinkedHashMap<String, Supplier<Pane>> getMenus() {
        return new LinkedHashMap<>();
    }

    @Override
    public int getMenuIndex(Pane innerContainer) {
        return 0;
    }

    @Override
    public String getName() {
        return "大航海";
    }
}
