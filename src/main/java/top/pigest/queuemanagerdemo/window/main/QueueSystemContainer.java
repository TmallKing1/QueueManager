package top.pigest.queuemanagerdemo.window.main;

import javafx.scene.layout.Pane;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class QueueSystemContainer extends MultiMenuProvider<Pane> {
    @Override
    public LinkedHashMap<String, Supplier<Pane>> getMenus() {
        return new LinkedHashMap<>();
    }

    @Override
    public int getMenuIndex(Pane innerContainer) {
        return -1;
    }
}
