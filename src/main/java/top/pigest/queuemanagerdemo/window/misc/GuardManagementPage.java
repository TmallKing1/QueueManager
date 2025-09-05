package top.pigest.queuemanagerdemo.window.misc;

import javafx.scene.layout.Pane;
import top.pigest.queuemanagerdemo.util.DynamicListPagedContainer;
import top.pigest.queuemanagerdemo.util.PagedContainerFactory;
import top.pigest.queuemanagerdemo.window.main.MultiMenuProvider;
import top.pigest.queuemanagerdemo.window.main.NamedPage;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class GuardManagementPage extends MultiMenuProvider<Pane> implements NamedPage {
    private Pane guardList;

    public GuardManagementPage() {
        this.setInnerContainer(this.getMenus().entrySet().iterator().next().getValue().get());
    }

    public Pane initC0() {
        GuardContainer guardContainer = new GuardContainer("c0", 6);
        return guardContainer.build();
    }

    public Pane getC0() {
        return guardList == null ? (guardList = initC0()) : guardList;
    }

    @Override
    public LinkedHashMap<String, Supplier<Pane>> getMenus() {
        LinkedHashMap<String, Supplier<Pane>> map = new LinkedHashMap<>();
        map.put("大航海列表", this::getC0);
        return map;
    }

    @Override
    public int getMenuIndex(Pane innerContainer) {
        String id = innerContainer.getId();
        if (id == null) {
            return -1;
        }
        return id.charAt(1) - '0';
    }

    @Override
    public String getName() {
        return "大航海";
    }
}
