package top.pigest.queuemanagerdemo;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.window.login.LoginMain;
import top.pigest.queuemanagerdemo.window.main.MainScene;

import java.util.Objects;

public class QueueManager extends Application {
    public static QueueManager INSTANCE;

    private Stage primaryStage;
    private Stage login;

    @Override
    public void start(Stage primaryStage) {
        System.setProperty("prism.lcdtext", "true");
        INSTANCE = this;
        this.primaryStage = primaryStage;
        primaryStage.setResizable(false);
        primaryStage.setTitle("Queue Manager");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png"))));
        primaryStage.setScene(new MainScene());
        primaryStage.setOnCloseRequest(event -> {
            MainScene scene = (MainScene) primaryStage.getScene();
            if (scene.getRootDrawer().getChildren().stream().noneMatch(node -> node.getId() != null && node.getId().equals("close-confirm"))) {
                Utils.showChoosingDialog("确认关闭程序？", "确认", "取消", event1 -> System.exit(0), event1 -> {}, scene.getRootDrawer());
            }
            event.consume();
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        Settings.loadSettings();
        launch(args);
    }

    public void openLoginMain() {
        this.login = new Stage();
        this.login.setResizable(false);
        this.login.setTitle("账号登录");
        this.login.initOwner(primaryStage);
        this.login.initModality(Modality.WINDOW_MODAL);
        this.login.setScene(new LoginMain());
        this.login.setOnCloseRequest(event -> {
            ((MainScene) (primaryStage.getScene())).switchLoginButtonState();
            ((LoginMain) this.login.getScene()).stopTimeline();
        });
        this.login.show();
    }

    public void closeLogin() {
        login.close();
    }

    public void refreshLoginState() {
        ((MainScene) this.primaryStage.getScene()).refreshLoginState();
    }

    public boolean isLoginOpen() {
        return login != null && login.isShowing();
    }

    public void close() {
        primaryStage.close();
    }

    public MainScene getMainScene() {
        return ((MainScene) primaryStage.getScene());
    }
}