package top.pigest.queuemanagerdemo.window.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.widget.QMButton;

public class MainPageContainer extends BorderPane {
    private final GridPane functions = Utils.make(new GridPane(10, 10), gridPane -> {
        gridPane.setAlignment(Pos.CENTER);
    });
    private final QMButton exitButton = Utils.make(new QMButton("退出", "#bb5555"), button -> {
        button.setPrefSize(200, 40);
        button.setOnAction(actionEvent -> System.exit(0));
        BorderPane.setAlignment(button, Pos.CENTER);
        BorderPane.setMargin(button, new Insets(0, 0, 30, 0));
    });
    private final MainScene parent;

    public MainPageContainer(MainScene parent) {
        this.parent = parent;
        functions.add(this.parent.createMainFunctionButton("#357c56", "#73be95", "弹幕服务", "fas-hamburger", DanmakuServiceContainer::new), 0, 0);
        functions.add(this.parent.createMainFunctionButton("#ae5220", "#cf9d81", "排队系统", "fas-list-ol", QueueSystemContainer::new), 1, 0);
        functions.add(this.parent.createMainFunctionButton("#932121", "#c15757", "点歌系统", "fas-music", MusicSystemContainer::new), 0, 1);
        functions.add(this.parent.createMainFunctionButton("#0d608f", "#1a8bcc", "更多功能", "fas-dice-d6", () -> null), 1, 1);
        this.setCenter(functions);
        this.setBottom(exitButton);
    }
}
