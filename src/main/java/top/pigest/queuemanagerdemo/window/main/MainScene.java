package top.pigest.queuemanagerdemo.window.main;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.liveroom.LiveMessageService;
import top.pigest.queuemanagerdemo.music.MusicHandler;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.TitledDialog;
import top.pigest.queuemanagerdemo.control.WhiteFontIcon;
import top.pigest.queuemanagerdemo.window.music.MusicSystemPage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MainScene extends Scene {
    private final QMButton accountButton = Utils.make(new QMButton("正在登录……", null, false), qmButton -> qmButton.setOnAction(event -> {
        if (isLogin()) {
            VBox vBox = new VBox(2);
            vBox.setAlignment(Pos.CENTER);
            TitledDialog dialog = new TitledDialog("操作", this.getRootDrawer(), vBox, JFXDialog.DialogTransition.CENTER, true);
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
    }));
    private final HBox menuItems = Utils.make(new HBox(), hBox -> hBox.setAlignment(Pos.CENTER_LEFT));
    private final QMButton bar = Utils.make(new QMButton("", null, false), qmButton -> qmButton.setOnAction(event -> showSidebar()));
    private final BorderPane top = Utils.make(new BorderPane(), border -> {
        bar.setGraphic(new FontIcon("fas-bars"));
        border.setLeft(new BorderPane(menuItems, null, null, null, bar));
        border.setRight(accountButton);
    });
    private final BorderPane borderPane = Utils.make(new BorderPane(), border -> border.setTop(top));
    private final JFXDrawer drawer = Utils.make(new JFXDrawer(new Duration(300)), drawer1 -> {
        drawer1.setDefaultDrawerSize(250);
        drawer1.setContent(borderPane);
    });
    private final List<QMButton> drawerButtons = new ArrayList<>();

    private boolean login = false;
    private MainPage mainPage;

    public MainScene() {
        super(new Pane(), 800, 600, false, SceneAntialiasing.BALANCED);
        this.setRoot(drawer);
        refreshLoginState();
        autoMethods();
    }

    public void autoMethods() {
        if (Settings.getMusicServiceSettings().autoPlay && Settings.hasCookie("MUSIC_U")) {
            CompletableFuture.runAsync(MusicHandler.INSTANCE::playNext);
        }
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
            if (Settings.hasCookie("bili_jct")) {
                lp.add(new BasicNameValuePair("biliCSRF", Settings.getCookie("bili_jct")));
                httpPost.setEntity(new UrlEncodedFormEntity(lp));
                try (CloseableHttpResponse response = client.execute(httpPost, context)) {
                    JsonObject element = (JsonObject) JsonParser.parseString(EntityUtils.toString(response.getEntity()));
                    if (element.get("code").getAsInt() == 0) {
                        Settings.saveCookie(true);
                        if (LiveMessageService.getInstance() != null) {
                            LiveMessageService.getInstance().close();
                        }
                        refreshLoginState();
                    }
                }
            }
        } catch (Exception e) {
            this.showDialogMessage("登出失败\n" + e.getMessage(), true);
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
                    loggedIn(element.getAsJsonObject("data").get("name").getAsString());
                    if (Settings.getDanmakuServiceSettings().autoConnect) {
                        try {
                            LiveMessageService.connect();
                        } catch (LiveMessageService.ConnectFailedException e) {
                            Platform.runLater(() -> this.showDialogMessage("自动连接弹幕服务失败\n" + e.getMessage(), true));
                        }
                    }
                } else {
                    notLoggedIn();
                }
            }
        } catch (Exception e) {
            notLoggedIn();
        }
        if (QueueManager.INSTANCE.isLoginOpen()) {
            Platform.runLater(() -> QueueManager.INSTANCE.closeLogin());
        }
    }

    private void notLoggedIn() {
        Settings.setMID(-1);
        Platform.runLater(() -> {
            this.login = false;
            this.setMainContainer(new LoginPage(), "登录页");
            accountButton.setText("未登录");
            accountButton.setGraphic(null);
            if (((LoginPage) this.getMainContainer()).getLoginButton().isDisable()) {
                ((LoginPage) getMainContainer()).switchLoginButtonState();
            }
            VBox vbox = new VBox();
            vbox.setAlignment(Pos.CENTER);
            QMButton button = new QMButton("登录账号", null, false);
            button.setPrefWidth(200);
            button.setGraphic(new FontIcon("far-user-circle"));
            button.setOnAction(event -> {
                ((LoginPage) getMainContainer()).login();
                drawer.close();
            });
            vbox.getChildren().add(button);
            drawer.setSidePane(vbox);
        });
    }

    private void loggedIn(String name) {
        Platform.runLater(() -> {
            this.login = true;
            mainPage = new MainPage(this);
            this.setMainContainer(mainPage, "主页");
            accountButton.setText(name);
            accountButton.setGraphic(new FontIcon("far-user-circle"));
            VBox vbox = new VBox();
            vbox.setAlignment(Pos.CENTER);
            vbox.getChildren().addAll(drawerButtons);
            drawer.setSidePane(vbox);
        });
    }

    public JFXDrawer getRootDrawer() {
        return drawer;
    }

    public BorderPane getBorderPane() {
        return borderPane;
    }

    public void showDialogMessage(String message, boolean isError) {
        Utils.showDialogMessage(message, isError, drawer);
    }

    public void switchLoginButtonState() {
        if (getMainContainer() instanceof LoginPage) {
            ((LoginPage) getMainContainer()).switchLoginButtonState();
        }
        if (getMainContainer() instanceof MusicSystemPage) {
            ((MusicSystemPage) getMainContainer()).switchLoginButtonState();
        }
    }

    public Node getMainContainer() {
        return this.borderPane.getCenter();
    }

    public void setMainContainer(Node mainContainer, String id) {
        mainContainer.setId(id);
        this.borderPane.setCenter(mainContainer);
        drawerButtons.forEach(qmButton -> {
            if (qmButton.getId().equals(id)) {
                qmButton.setTextFill(Paint.valueOf("#1a8bcc"));
                ((FontIcon) qmButton.getGraphic()).setIconColor(Paint.valueOf("#1a8bcc"));
            } else {
                qmButton.setTextFill(Paint.valueOf("BLACK"));
                ((FontIcon) qmButton.getGraphic()).setIconColor(Paint.valueOf("BLACK"));
            }
        });
        this.refreshMenuButtons();
    }

    public void setMainContainer(Supplier<Pane> supplier, QMButton source, Pane parent) {
        if (source != null && !source.getId().isEmpty() && source.getId().equals(this.getMainContainer().getId())) {
            return;
        }
        this.setMainContainer(((MultiMenuProvider<?>) supplier.get()).withParentContainer(parent), source != null ? source.getId() : "");
    }

    public void setMainContainer(Supplier<Pane> supplier, QMButton source) {
        if (source != null && !source.getId().isEmpty() && source.getId().equals(this.getMainContainer().getId())) {
            return;
        }
        this.setMainContainer(supplier, source, (Pane) this.getMainContainer());
    }

    public QMButton createMainFunctionButton(String backgroundColor, String ripplerColor, String text, String iconCode, Supplier<Pane> supplier) {
        QMButton button = new QMButton(null, backgroundColor, false);
        button.setId(text);
        button.setPrefSize(200, 200);
        FontIcon fontIcon = new WhiteFontIcon(iconCode + ":100");
        Text text1 = new Text(text);
        text1.setFont(Settings.DEFAULT_FONT);
        text1.setFill(Paint.valueOf("WHITE"));
        StackPane stackPane = new StackPane(fontIcon, text1);
        StackPane.setAlignment(fontIcon, Pos.CENTER);
        StackPane.setAlignment(text1, Pos.BOTTOM_LEFT);
        StackPane.setMargin(text1, new Insets(0, 0, 2, 2));
        button.setGraphic(stackPane);
        button.setRipplerFill(Paint.valueOf(ripplerColor));
        button.setOnAction(actionEvent -> this.setMainContainer(supplier, button));
        QMButton button1 = new QMButton(text, null, false);
        button1.setPrefWidth(200);
        button1.setId(text);
        button1.setGraphic(new FontIcon(iconCode));
        button1.setOnAction(actionEvent -> {
            this.setMainContainer(supplier, button1, mainPage);
            this.getRootDrawer().close();
        });
        this.drawerButtons.add(button1);
        return button;
    }

    public void refreshMenuButtons() {
        this.menuItems.getChildren().clear();
        if (this.getMainContainer() instanceof ChildPage childPage) {
            Pane parent = childPage.getParentContainer();
            if (parent != null) {
                QMButton back = new QMButton("", null, false);
                back.setGraphic(new FontIcon("far-arrow-alt-circle-left"));
                back.setOnAction(event -> this.setMainContainer(parent, parent.getId()));
                this.menuItems.getChildren().add(back);
            }
        }
        if (this.getMainContainer() instanceof MultiMenuProvider<?> multiMenuProvider) {
            this.menuItems.getChildren().addAll(multiMenuProvider.getMenuButtons());
            int currentMenuIndex = multiMenuProvider.getCurrentMenuIndex();
            currentMenuIndex = this.getMainContainer() instanceof ChildPage ? currentMenuIndex + 1 : currentMenuIndex;
            if (currentMenuIndex != -1) {
                ((QMButton) this.menuItems.getChildren().get(currentMenuIndex)).setTextFill(Paint.valueOf("#1a8bcc"));
            }
        }
        if (this.getMainContainer() instanceof NamedPage namedPage) {
            bar.setText(namedPage.getName());
        } else {
            bar.setText("");
        }
    }

    public void updateMenuButtonTextFill() {
        this.menuItems.getChildren().forEach(node -> ((QMButton) node).setTextFill(Paint.valueOf("BLACK")));
        if (this.getMainContainer() instanceof MultiMenuProvider<?> multiMenuProvider) {
            int currentMenuIndex = multiMenuProvider.getCurrentMenuIndex();
            currentMenuIndex = this.getMainContainer() instanceof ChildPage ? currentMenuIndex + 1 : currentMenuIndex;
            if (currentMenuIndex != -1) {
                ((QMButton) this.menuItems.getChildren().get(currentMenuIndex)).setTextFill(Paint.valueOf("#1a8bcc"));
            }
        }
    }

    public String getUserName() {
        return isLogin() ? accountButton.getText() : null;
    }

    private void showSidebar() {
        drawer.open();
    }
}
