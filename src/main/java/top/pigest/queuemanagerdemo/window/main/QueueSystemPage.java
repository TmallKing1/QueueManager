package top.pigest.queuemanagerdemo.window.main;

import javafx.scene.layout.Pane;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class QueueSystemPage extends MultiMenuProvider<Pane> implements NamedPage {
    @Override
    public LinkedHashMap<String, Supplier<Pane>> getMenus() {
        return new LinkedHashMap<>();
    }

    @Override
    public int getMenuIndex(Pane innerContainer) {
        return -1;
    }

    @Override
    public String getName() {
        return "排队系统";
    }
}
