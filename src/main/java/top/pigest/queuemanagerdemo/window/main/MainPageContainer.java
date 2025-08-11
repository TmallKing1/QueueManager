package top.pigest.queuemanagerdemo.window.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.widget.QMButton;
import top.pigest.queuemanagerdemo.widget.WhiteFontIcon;

import java.util.function.Supplier;

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
    private MainScene parent;

    public MainPageContainer(MainScene parent) {
        this.parent = parent;
        functions.add(createMainFunctionButton("#357c56", "#73be95", "弹幕服务", "fas-hamburger", DanmakuServiceContainer::new), 0, 0);
        functions.add(createMainFunctionButton("#ae5220", "#cf9d81", "排队系统", "fas-list-ol", QueueSystemContainer::new), 1, 0);
        functions.add(createMainFunctionButton("#7420ae", "#a56ccb", "更多功能", "fas-dice-d6", () -> null), 0, 1);
        functions.add(createMainFunctionButton("#1a8bcc", "#7cbce1", "系统设置", "fas-sliders-h", () -> null), 1, 1);
        this.setCenter(functions);
        this.setBottom(exitButton);
    }

    private QMButton createMainFunctionButton(String backgroundColor, String ripplerColor, String text, String iconCode, Supplier<Pane> supplier) {
        QMButton button = new QMButton(null, backgroundColor, false);
        button.setPrefSize(200, 200);
        FontIcon fontIcon = new WhiteFontIcon(iconCode);
        fontIcon.setIconSize(100);
        Text text1 = new Text(text);
        text1.setFont(Settings.DEFAULT_FONT);
        text1.setFill(Paint.valueOf("WHITE"));
        StackPane stackPane = new StackPane(fontIcon, text1);
        StackPane.setAlignment(fontIcon, Pos.CENTER);
        StackPane.setAlignment(text1, Pos.BOTTOM_LEFT);
        StackPane.setMargin(text1, new Insets(0, 0, 2, 2));
        button.setGraphic(stackPane);
        button.setRipplerFill(Paint.valueOf(ripplerColor));
        button.setOnAction(actionEvent -> this.parent.setMainContainer(((MultiMenuProvider<?>) supplier.get()).withParentContainer(this)));
        return button;
    }
}
