package top.pigest.queuemanagerdemo.window.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.window.misc.MiscPage;
import top.pigest.queuemanagerdemo.window.music.MusicSystemPage;

public class MainPage extends BorderPane {

    public MainPage(MainScene parent) {
        GridPane functions = Utils.make(new GridPane(10, 10), gridPane -> gridPane.setAlignment(Pos.CENTER));
        functions.add(parent.createMainFunctionButton("#357c56", "#73be95", "弹幕服务", "fas-hamburger", DanmakuServicePage::new), 0, 0);
        functions.add(parent.createMainFunctionButton("#ae5220", "#cf9d81", "排队系统", "fas-list-ol", QueueSystemPage::new), 1, 0);
        functions.add(parent.createMainFunctionButton("#932121", "#c15757", "点歌系统", "fas-music", MusicSystemPage::new), 0, 1);
        functions.add(parent.createMainFunctionButton("#0d608f", "#1a8bcc", "工具箱", "fas-toolbox", MiscPage::new), 1, 1);
        this.setCenter(functions);
        QMButton exitButton = Utils.make(new QMButton("退出", "#bb5555"), button -> {
            button.setPrefSize(200, 40);
            button.setOnAction(actionEvent -> System.exit(0));
            BorderPane.setAlignment(button, Pos.CENTER);
            BorderPane.setMargin(button, new Insets(0, 0, 30, 0));
        });
        this.setBottom(exitButton);
    }
}
