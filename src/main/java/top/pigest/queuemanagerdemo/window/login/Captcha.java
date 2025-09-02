package top.pigest.queuemanagerdemo.window.login;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;

import java.io.IOException;

public class Captcha extends Scene {
    private final CaptchaLogin parent;
    private final BorderPane root = new BorderPane();
    private final WebView captcha = new WebView();
    private final boolean safeCheck;
    private String token;
    private String challenge;
    private String gt;
    private Timeline timeline;

    public Captcha(CaptchaLogin parent) {
        this(parent, false);
    }

    public Captcha(CaptchaLogin parent, boolean safeCheck) {
        super(new Pane(), 520, 520, false, SceneAntialiasing.BALANCED);
        this.setRoot(root);
        this.parent = parent;
        this.safeCheck = safeCheck;
        this.init();
    }

    private void init() {
        BorderPane borderPane = new BorderPane(Utils.make(Utils.createLabel("请完成人机验证"), label -> BorderPane.setAlignment(label, Pos.CENTER)), null, Utils.make(new QMButton("取消", "#bb5555"), button -> {
            button.setPrefSize(100, 40);
            button.setOnAction(actionEvent -> cancel());
        }), null, null);
        root.setTop(borderPane);

        new Thread(() -> {
            try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
                if (safeCheck) {
                    HttpPost httpPost = new HttpPost("https://passport.bilibili.com/x/safecenter/captcha/pre");
                    httpPost.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                    CloseableHttpResponse response = client.execute(httpPost);
                    String string = EntityUtils.toString(response.getEntity());
                    JsonObject element = ((JsonObject) JsonParser.parseString(string));
                    if (element.get("code").getAsInt() == 0) {
                        this.token = element.getAsJsonObject("data").get("recaptcha_token").getAsString();
                        this.challenge = element.getAsJsonObject("data").get("gee_challenge").getAsString();
                        this.gt = element.getAsJsonObject("data").get("gee_gt").getAsString();
                    } else {
                        this.parent.captchaFail(false, "获取验证码时出现错误");
                        return;
                    }
                } else {
                    HttpGet httpGet = getHttpGet();
                    CloseableHttpResponse response = client.execute(httpGet);
                    String string = EntityUtils.toString(response.getEntity());
                    JsonObject element = ((JsonObject) JsonParser.parseString(string));
                    if (element.get("code").getAsInt() == 0) {
                        this.token = element.getAsJsonObject("data").get("token").getAsString();
                        this.challenge = element.getAsJsonObject("data").getAsJsonObject("geetest").get("challenge").getAsString();
                        this.gt = element.getAsJsonObject("data").getAsJsonObject("geetest").get("gt").getAsString();
                    } else {
                        this.parent.captchaFail(false, "获取验证码时出现错误");
                        return;
                    }
                }
            } catch (IOException exception) {
                this.parent.captchaFail(false, "获取验证码时出现错误");
                return;
            }
            Platform.runLater(() -> {
                captcha.setContextMenuEnabled(false);
                WebEngine engine = captcha.getEngine();
                engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        engine.executeScript("man(\"" + gt + "\", \"" + challenge + "\")");
                    }
                });
                this.timeline = getTimeline(engine);
                engine.load(Settings.loadCaptcha());
                root.setCenter(captcha);
                root.setStyle("-fx-background-color: #cccccc");
            });
        }).start();
    }

    private static HttpGet getHttpGet() {
        HttpGet httpGet = new HttpGet("https://passport.bilibili.com/x/passport-login/captcha?source=main-fe-header");
        httpGet.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
        return httpGet;
    }

    private Timeline getTimeline(WebEngine engine) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String validate = ((String) engine.executeScript("getValidate(\"" + gt + "\", \"" + challenge + "\")"));
                    if (!validate.isEmpty()) {
                        String seccode = ((String) engine.executeScript("getSeccode(\"" + gt + "\", \"" + challenge + "\")"));
                        submit(validate, seccode);
                    }
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        return timeline;
    }

    private void cancel() {
        stop();
        this.parent.captchaFail(true, null);
    }

    private void submit(String validate, String seccode) {
        stop();
        this.parent.captchaSuccess(token, gt, challenge, validate, seccode);
    }

    public void stop() {
        Utils.onPresent(timeline, Timeline::stop);
    }
}
