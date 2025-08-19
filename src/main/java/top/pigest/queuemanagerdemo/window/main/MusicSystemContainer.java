package top.pigest.queuemanagerdemo.window.main;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.system.netease.Music163Login;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.widget.QMButton;
import top.pigest.queuemanagerdemo.widget.WhiteFontIcon;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Supplier;

public class MusicSystemContainer extends MultiMenuProvider<Pane> implements NamedContainer {
    private boolean preloaded = false;
    private boolean loggedIn = false;
    private Timeline preloadTimeline;
    private final QMButton loginButton = Utils.make(new QMButton("点击登录网易云账号", "#fc3c55"), button -> {
        button.setPrefSize(350, 40);
        button.setOnAction(actionEvent -> login());
        button.setDefaultButton(true);
        button.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
    });
    private Stage login;

    private void login() {
        openMusic163Login();
        switchLoginButtonState();
    }

    public void openMusic163Login() {
        this.login = new Stage();
        this.login.setResizable(false);
        this.login.setTitle("网易云账号登录");
        this.login.initOwner(QueueManager.INSTANCE.getMainScene().getWindow());
        this.login.initModality(Modality.WINDOW_MODAL);
        this.login.setScene(new Music163Login(this));
        this.login.setOnCloseRequest(event -> {
            switchLoginButtonState();
            ((Music163Login) this.login.getScene()).stopTimeline();
        });
        this.login.show();
    }

    public void closeLogin() {
        this.login.close();
    }

    public MusicSystemContainer() {
        if (!Settings.hasCookie("sDeviceId")) {
            this.preload();
        } else {
            this.preloaded = true;
            this.loggedIn = Settings.hasCookie("MUSIC_U");
        }
        this.setInnerContainer(this.getMenus().entrySet().iterator().next().getValue().get());
    }

    public Pane preload() {
        StackPane pane = new StackPane();
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        WebView webView = new WebView();
        //webView.setMaxSize(0, 0);
        webView.setContextMenuEnabled(false);
        WebEngine webEngine = webView.getEngine();
        webEngine.setUserAgent(Settings.USER_AGENT);
        webEngine.load("https://music.163.com/#/login");
        this.preloadTimeline = new Timeline(new KeyFrame(new Duration(500), event -> {
            Optional<HttpCookie> sDeviceId = cookieManager.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equalsIgnoreCase("sDeviceId")).findFirst();
            if (sDeviceId.isPresent()) {
                CookieStore cookieStore = Settings.getBiliCookieStore();
                HttpCookie httpCookie = sDeviceId.get();
                BasicClientCookie cookie = new BasicClientCookie(httpCookie.getName(), httpCookie.getValue());
                cookie.setPath(httpCookie.getPath());
                cookie.setDomain(httpCookie.getDomain());
                cookie.setExpiryDate(new Date(System.currentTimeMillis() + httpCookie.getMaxAge() * 1000));
                cookieStore.addCookie(cookie);
                Settings.saveCookie(false);
                this.preloaded = true;
                this.preloadTimeline.stop();
                Platform.runLater(() -> QueueManager.INSTANCE.getMainScene().setMainContainer(new MusicSystemContainer().withParentContainer(this.getParentContainer()), this.getId()));
            }
        }));
        this.preloadTimeline.setCycleCount(Timeline.INDEFINITE);
        this.preloadTimeline.play();
        Label label = new Label("网易云音乐服务正在初始化\n请稍等几秒钟");
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        label.setFont(Settings.DEFAULT_FONT);
        pane.getChildren().addAll(webView, label);
        pane.setId("c0");
        return pane;
    }

    public Pane loginC0() {
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        Label label = Utils.createLabel("登录你的网易云音乐账号以开始使用\n仅能够扫码登录");
        label.setTextAlignment(TextAlignment.CENTER);
        vBox.setId("c0");
        vBox.getChildren().addAll(label, loginButton);
        return vBox;
    }

    public Pane getC0() {

        return null;
    }

    private Pane getC1() {
        return null;
    }

    public void switchLoginButtonState() {
        if (!loginButton.isDisable()) {
            loginButton.disable(true);
            loginButton.setText("请在弹出窗口中完成登录");
            loginButton.setGraphic(new WhiteFontIcon("fas-bullseye"));
        }  else {
            loginButton.disable(false);
            loginButton.setText("点击登录网易云账号");
            loginButton.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
        }
    }

    @Override
    public LinkedHashMap<String, Supplier<Pane>> getMenus() {
        LinkedHashMap<String, Supplier<Pane>> map = new LinkedHashMap<>();
        if (!preloaded) {
            map.put("初始化", this::preload);
        } else if (!loggedIn) {
            map.put("账号登录", this::loginC0);
        } else {
            map.put("播放设置", this::getC0);
            map.put("点歌设置", this::getC1);
        }
        return map;
    }

    @Override
    public int getMenuIndex(Pane innerContainer) {
        String id = innerContainer.getId();
        if (id == null) {
            return -1;
        }
        return id.charAt(1) - '0';
    }

    @Override
    public String getName() {
        return "点歌系统";
    }
}
