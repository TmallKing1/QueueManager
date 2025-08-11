package top.pigest.queuemanagerdemo.window.main;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXDialog;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.system.LiveMessageService;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.widget.QMButton;
import top.pigest.queuemanagerdemo.widget.TitledDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainScene extends Scene {
    private final QMButton accountButton = Utils.make(new QMButton("正在登录……", null, false), qmButton -> {
        qmButton.setOnAction(event -> {
            if (isLogin()) {
                VBox vBox = new VBox(2);
                vBox.setAlignment(Pos.CENTER);
                TitledDialog dialog = new TitledDialog("操作", this.getRootStackPane(), vBox, JFXDialog.DialogTransition.CENTER, true);
                QMButton accountSettings = new QMButton("账号设置", null, false);
                QMButton exitLogin = new QMButton("退出登录", null, false);
                accountSettings.setOnAction(e -> dialog.close());
                accountSettings.setGraphic(new FontIcon("fas-user-cog"));
                exitLogin.setOnAction(e -> {
                    dialog.close();
                    new Thread(this::logout).start();
                });
                exitLogin.setGraphic(new FontIcon("fas-sign-out-alt"));
                vBox.getChildren().add(accountSettings);
                vBox.getChildren().add(exitLogin);
                vBox.getChildren().forEach(node -> {
                    ((QMButton) node).setPrefWidth(300);
                    ((QMButton) node).setTextAlignment(TextAlignment.LEFT);
                    ((QMButton) node).setAlignment(Pos.CENTER_LEFT);
                });
                dialog.show();
            }
        });
    });
    private final HBox menuItems = Utils.make(new HBox(), hBox -> hBox.setAlignment(Pos.CENTER_LEFT));
    private final QMButton bar = new QMButton("", null, false);
    private final BorderPane top = Utils.make(new BorderPane(), border -> {
        bar.setGraphic(new FontIcon("fas-bars"));
        border.setLeft(new BorderPane(menuItems, null, null, null, bar));
        border.setRight(accountButton);
    });
    private final BorderPane borderPane = Utils.make(new BorderPane(), border -> border.setTop(top));
    private final StackPane rootStackPane = Utils.make(new StackPane(), stackPane -> stackPane.getChildren().add(borderPane));

    private boolean login = false;

    public MainScene() {
        super(new Pane(), 800, 600, false, SceneAntialiasing.BALANCED);
        this.setRoot(rootStackPane);
        refreshLoginState();
    }

    public boolean isLogin() {
        return login;
    }

    public void logout() {
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(Settings.getCookieStore());
            HttpPost httpPost = new HttpPost("https://passport.bilibili.com/login/exit/v2");
            httpPost.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> lp = new ArrayList<>();
            Optional<Cookie> biliJct = Settings.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("bili_jct")).findAny();
            if (biliJct.isPresent()) {
                lp.add(new BasicNameValuePair("biliCSRF", biliJct.get().getValue()));
                httpPost.setEntity(new UrlEncodedFormEntity(lp));
                try (CloseableHttpResponse response = client.execute(httpPost, context)) {
                    JsonObject element = (JsonObject) JsonParser.parseString(EntityUtils.toString(response.getEntity()));
                    if (element.get("code").getAsInt() == 0) {
                        Settings.getCookieStore().clear();
                        Settings.saveCookie(true);
                        if (LiveMessageService.getInstance() != null) {
                            LiveMessageService.getInstance().close();
                        }
                        refreshLoginState();
                    }
                }
            }
        } catch (Exception e) {
            this.showDialogMessage("登出失败\n" + e.getMessage(), "RED");
        }
    }

    public void refreshLoginState() {
        Settings.loadCookie();
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            HttpClientContext context = HttpClientContext.create();
            context.setCookieStore(Settings.getCookieStore());
            HttpGet httpGet = new HttpGet("https://api.bilibili.com/x/space/myinfo");
            httpGet.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
            try (CloseableHttpResponse response = client.execute(httpGet, context)) {
                JsonObject element = (JsonObject) JsonParser.parseString(EntityUtils.toString(response.getEntity()));
                if (element.get("code").getAsInt() == 0) {
                    Settings.setMID(element.getAsJsonObject("data").get("mid").getAsLong());
                    Platform.runLater(() -> {
                        this.login = true;
                        displayFunctions();
                        accountButton.setText(element.getAsJsonObject("data").get("name").getAsString());
                        accountButton.setGraphic(new FontIcon("far-user-circle"));
                    });
                    if (Settings.getDanmakuServiceSettings().autoConnect) {
                        try {
                            LiveMessageService.connect();
                        } catch (LiveMessageService.ConnectFailedException e) {
                            Platform.runLater(() -> this.showDialogMessage("自动连接弹幕服务失败\n" + e.getMessage(), "RED"));
                        }
                    }
                } else {
                    Settings.setMID(-1);
                    Platform.runLater(() -> {
                        this.login = false;
                        this.setMainContainer(new LoginContainer());
                        accountButton.setText("未登录");
                        accountButton.setGraphic(null);
                        if (((LoginContainer) this.borderPane.getCenter()).getLoginButton().isDisable()) {
                            ((LoginContainer) getMainContainer()).switchLoginButtonState();
                        }
                    });
                }
            }
        } catch (Exception e) {
            Settings.setMID(-1);
            Platform.runLater(() -> {
                this.login = false;
                accountButton.setText("未登录");
                accountButton.setGraphic(null);
                this.setMainContainer(new LoginContainer());
            });
        }
        if (QueueManager.INSTANCE.isLoginOpen()) {
            Platform.runLater(() -> QueueManager.INSTANCE.closeLogin());
        }
    }

    private void displayFunctions() {
        this.setMainContainer(new MainPageContainer(this));
    }

    public StackPane getRootStackPane() {
        return rootStackPane;
    }

    public BorderPane getBorderPane() {
        return borderPane;
    }

    public void showDialogMessage(String message, String color) {
        Utils.showDialogMessage(message, color, rootStackPane);
    }

    public void switchLoginButtonState() {
        if (getMainContainer() instanceof LoginContainer) {
            ((LoginContainer) getMainContainer()).switchLoginButtonState();
        }
    }

    private Node getMainContainer() {
        return this.borderPane.getCenter();
    }

    public void setMainContainer(Node mainContainer) {
        this.borderPane.setCenter(mainContainer);
        this.refreshMenuButtons();
    }

    public void refreshMenuButtons() {
        this.menuItems.getChildren().clear();
        if (this.getMainContainer() instanceof ChildContainer childContainer) {
            Pane parent = childContainer.getParentContainer();
            if (parent != null) {
                QMButton back = new QMButton("", null, false);
                back.setGraphic(new FontIcon("far-arrow-alt-circle-left"));
                back.setOnAction(event -> this.setMainContainer(parent));
                this.menuItems.getChildren().add(back);
            }
        }
        if (this.getMainContainer() instanceof MultiMenuProvider<?> multiMenuProvider) {
            this.menuItems.getChildren().addAll(multiMenuProvider.getMenuButtons());
            int currentMenuIndex = multiMenuProvider.getCurrentMenuIndex();
            currentMenuIndex = this.getMainContainer() instanceof ChildContainer ? currentMenuIndex + 1 : currentMenuIndex;
            if (currentMenuIndex != -1) {
                ((QMButton) this.menuItems.getChildren().get(currentMenuIndex)).setTextFill(Paint.valueOf("#1a8bcc"));
            }
        }
        if (this.getMainContainer() instanceof NamedContainer namedContainer) {
            bar.setText(namedContainer.getName());
        } else {
            bar.setText("");
        }
    }

    public void updateMenuButtonTextFill() {
        this.menuItems.getChildren().forEach(node -> ((QMButton) node).setTextFill(Paint.valueOf("BLACK")));
        if (this.getMainContainer() instanceof MultiMenuProvider<?> multiMenuProvider) {
            int currentMenuIndex = multiMenuProvider.getCurrentMenuIndex();
            currentMenuIndex = this.getMainContainer() instanceof ChildContainer ? currentMenuIndex + 1 : currentMenuIndex;
            if (currentMenuIndex != -1) {
                ((QMButton) this.menuItems.getChildren().get(currentMenuIndex)).setTextFill(Paint.valueOf("#1a8bcc"));
            }
        }
    }
}
