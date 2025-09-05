package top.pigest.queuemanagerdemo.window.misc;

import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.window.main.ChildPage;
import top.pigest.queuemanagerdemo.window.main.NamedPage;

public class MiscPage extends VBox implements ChildPage, NamedPage {
    private Pane parentPage;

    public MiscPage() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(30);
        GridPane gridPane = new GridPane(10, 30);
        gridPane.setAlignment(Pos.CENTER);
        int rowIndex = 0;
        int columnIndex = 0;
        for (MiscFunction registry : MiscRegistryManager.getRegistries()) {
            gridPane.add(QueueManager.INSTANCE.getMainScene().createMiscFunctionButton(registry.backgroundColor(), registry.name(), registry.iconCode(), registry.supplier()), columnIndex, rowIndex);
            if (columnIndex == 2) {
                rowIndex++;
                columnIndex = 0;
            } else {
                columnIndex++;
            }
        }
        this.getChildren().addAll(gridPane);
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
        return "工具箱";
    }
}
