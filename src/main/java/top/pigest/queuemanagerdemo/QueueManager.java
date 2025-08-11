package top.pigest.queuemanagerdemo;

import javafx.application.Application;
import javafx.stage.Modality;
import javafx.stage.Stage;
import top.pigest.queuemanagerdemo.system.LiveMessageService;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.window.login.LoginMain;
import top.pigest.queuemanagerdemo.window.main.MainScene;

public class QueueManager extends Application {
    public static QueueManager INSTANCE;

    private Stage primaryStage;
    private Stage login;

    @Override
    public void start(Stage primaryStage) {
        INSTANCE = this;
        this.primaryStage = primaryStage;
        primaryStage.setResizable(false);
        primaryStage.setTitle("Queue Manager Demo");
        primaryStage.setScene(new MainScene());
        primaryStage.setOnCloseRequest(event -> {
            MainScene scene = (MainScene) primaryStage.getScene();
            if (scene.getRootStackPane().getChildren().stream().noneMatch(node -> node.getId() != null && node.getId().equals("close-confirm"))) {
                Utils.showChoosingDialog("确认关闭所有服务并关闭程序？", "确认", "取消", event1 -> System.exit(0), event1 -> {}, scene.getRootStackPane());
            }
            event.consume();
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        Settings.loadSettings();
        launch(args);
    }

    public void openLogin() {
        login = new Stage();
        login.setResizable(false);
        login.setTitle("账号登录");
        login.initOwner(primaryStage);
        login.initModality(Modality.WINDOW_MODAL);
        login.setScene(new LoginMain());
        login.setOnCloseRequest(event -> {
            ((MainScene) (primaryStage.getScene())).switchLoginButtonState();
            ((LoginMain) login.getScene()).stopTimeLine();
        });
        login.show();
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