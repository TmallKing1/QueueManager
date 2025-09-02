package top.pigest.queuemanagerdemo.window.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.WhiteFontIcon;

public class LoginContainer extends BorderPane {
    private final QMButton loginButton = Utils.make(new QMButton("点击登录哔哩哔哩账号"), button -> {
        button.setPrefSize(350, 40);
        button.setOnAction(actionEvent -> login());
        button.setDefaultButton(true);
        button.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
    });

    public LoginContainer() {
        super();
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        Label label = Utils.createLabel("登录你的哔哩哔哩账号以开始使用");
        vBox.getChildren().addAll(label, loginButton);
        this.setCenter(vBox);
        QMButton exitButton = Utils.make(new QMButton("退出", "#bb5555"), button -> {
            button.setPrefSize(200, 40);
            button.setOnAction(actionEvent -> QueueManager.INSTANCE.close());
            BorderPane.setAlignment(button, Pos.CENTER);
            BorderPane.setMargin(button, new Insets(0, 0, 30, 0));
        });
        this.setBottom(exitButton);
    }

    public void login() {
        QueueManager.INSTANCE.openLoginMain();
        switchLoginButtonState();
    }

    public void switchLoginButtonState() {
        if (!loginButton.isDisable()) {
            loginButton.disable(true);
            loginButton.setText("请在弹出窗口中完成登录");
            loginButton.setGraphic(new WhiteFontIcon("fas-bullseye"));
        }  else {
            loginButton.disable(false);
            loginButton.setText("点击登录哔哩哔哩账号");
            loginButton.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
        }
    }

    public QMButton getLoginButton() {
        return loginButton;
    }
}
