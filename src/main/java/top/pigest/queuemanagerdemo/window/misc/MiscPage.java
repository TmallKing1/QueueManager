package top.pigest.queuemanagerdemo.window.misc;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.window.main.ChildPage;
import top.pigest.queuemanagerdemo.window.main.NamedPage;

public class MiscPage extends BorderPane implements ChildPage, NamedPage {
    private Pane parentPage;

    public MiscPage() {
        Label label = Utils.createLabel("杂项功能列表");
        BorderPane.setAlignment(label, Pos.CENTER);
        BorderPane.setMargin(label, new Insets(30, 0, 30, 0));
        this.setTop(label);
        GridPane gridPane = new GridPane(10, 10);
        int rowIndex = 0;
        int columnIndex = 0;
        for (MiscFunction registry : MiscRegistryManager.getRegistries()) {
            gridPane.add(QueueManager.INSTANCE.getMainScene().createMiscFunctionButton(registry.backgroundColor(), registry.name(), registry.iconCode(), registry.supplier()), rowIndex, columnIndex);
            if (columnIndex == 2) {
                rowIndex++;
                columnIndex = 0;
            }
            columnIndex++;
        }
        BorderPane.setAlignment(gridPane, Pos.CENTER);
        this.setCenter(gridPane);

    }


    @Override
    public Pane getParentPage() {
        return parentPage;
    }

    @Override
    public void setParentPage(Pane parentPage) {
        this.parentPage = parentPage;
    }

    @Override
    public String getName() {
        return "更多功能";
    }
}
