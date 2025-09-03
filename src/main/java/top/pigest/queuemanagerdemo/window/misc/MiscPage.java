package top.pigest.queuemanagerdemo.window.misc;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.window.main.ChildPage;
import top.pigest.queuemanagerdemo.window.main.NamedPage;

public class MiscPage extends VBox implements ChildPage, NamedPage {
    private Pane parentPage;

    public MiscPage() {
        Label label = Utils.createLabel("杂项功能列表");
        this.setAlignment(Pos.CENTER);
        this.setSpacing(50);
        GridPane gridPane = new GridPane(10, 10);
        gridPane.setAlignment(Pos.CENTER);
        int rowIndex = 0;
        int columnIndex = 0;
        for (MiscFunction registry : MiscRegistryManager.getRegistries()) {
            gridPane.add(QueueManager.INSTANCE.getMainScene().createMiscFunctionButton(registry.backgroundColor(), registry.name(), registry.iconCode(), registry.supplier()), columnIndex, rowIndex);
            if (columnIndex == 2) {
                rowIndex++;
                columnIndex = 0;
            }
            columnIndex++;
        }
        this.getChildren().addAll(label, gridPane);
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
