package top.pigest.queuemanagerdemo.window.login;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.*;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;

public class LoginMain extends Scene {
    private LoginMethod loginMethod;
    private final StackPane root = new StackPane();
    private final BorderPane borderPane = new BorderPane();
    private final HBox loginMethodButtons = Utils.make(new HBox(30), hBox -> {
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().add(Utils.make(new QMButton("账密登录", "#555555"), button -> {
            button.setPrefWidth(120);
            button.setOnAction(actionEvent -> preSetLoginMethod(LoginMethod.PASSWORD));
        }));
        hBox.getChildren().add(Utils.make(new QMButton("短信登录", "#555555"), button -> {
            button.setPrefWidth(120);
            button.setOnAction(actionEvent -> preSetLoginMethod(LoginMethod.SMS));
        }));
        hBox.getChildren().add(Utils.make(new QMButton("扫码登录", "#555555"), button -> {
            button.setPrefWidth(120);
            button.setOnAction(actionEvent -> preSetLoginMethod(LoginMethod.QRCODE));
        }));
    });

    public LoginMain() {
        super(new Pane(), 640, 480, false, SceneAntialiasing.BALANCED);
        this.setRoot(root);
        root.getChildren().add(borderPane);
        BorderPane header = new BorderPane();
        borderPane.setTop(header);
        header.setTop(Utils.make(Utils.createLabel("选择登录方式"), label -> {
            BorderPane.setAlignment(label, Pos.CENTER);
            BorderPane.setMargin(label, new Insets(20, 0, 20, 0));
        }));
        header.setCenter(loginMethodButtons);
        setLoginMethod(LoginMethod.PASSWORD);
    }

    private void preSetLoginMethod(LoginMethod loginMethod) {
        if (this.loginMethod == loginMethod) {
            return;
        }

        LoginMethodLocker loginMethodLocker = ((LoginMethodLocker) this.borderPane.getCenter());
        if (loginMethodLocker.lockLoginMethod()) {
            Utils.showChoosingDialog("确认放弃当前的信息并切换登录方式？", "确认", "取消", actionEvent -> setLoginMethod(loginMethod), actionEvent -> {}, this.getRootStackPane());
        } else {
            setLoginMethod(loginMethod);
        }
    }

    private void setLoginMethod(LoginMethod loginMethod) {
        if (this.loginMethod == loginMethod) {
            return;
        }
        this.stopTimeline();
        this.loginMethodButtons.getChildren().forEach(child -> child.setStyle("-fx-background-color: #999999"));
        this.loginMethod = loginMethod;
        this.loginMethodButtons.getChildren().get(loginMethod.ordinal()).setStyle("-fx-background-color: #1f1e33;");
        switch (loginMethod) {
            case PASSWORD -> this.borderPane.setCenter(new PasswordLogin(this));
            case SMS -> this.borderPane.setCenter(SMSLogin.standalone(this));
            case QRCODE -> this.borderPane.setCenter(new QRLogin(this));
        }
    }

    public void stopTimeline() {
        if (this.loginMethod == LoginMethod.QRCODE) {
            ((QRLogin) this.borderPane.getCenter()).stopTimeline();
        }
    }

    public void switchLoginContainer(Node node) {
        this.borderPane.setCenter(node);
    }

    public void lockLoginMethodButtons(boolean lock) {
        if (lock) {
            this.loginMethodButtons.getChildren().forEach(node -> node.setDisable(true));
        }  else {
            this.loginMethodButtons.getChildren().forEach(node -> node.setDisable(false));
        }
    }

    public void loginSuccess() {
        QueueManager.INSTANCE.refreshLoginState();
    }

    public void loginFail(String failMessage, boolean isError) {
        this.lockLoginMethodButtons(false);
        showDialogMessage(failMessage, isError);
    }

    public void showDialogMessage(String message) {
        this.showDialogMessage(message, false);
    }

    public void showDialogMessage(String message, boolean isError) {
        Utils.showDialogMessage(message, isError, root);

    }

    public StackPane getRootStackPane() {
        return root;
    }
}
