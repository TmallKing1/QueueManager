package top.pigest.queuemanagerdemo.window.login;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXTextField;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.TitledDialog;
import top.pigest.queuemanagerdemo.control.WhiteFontIcon;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SMSLogin extends VBox implements CaptchaLogin, LoginMethodLocker {
    private final boolean fromPassword;
    private final LoginMain loginMain;
    private final List<Country> countries = new ArrayList<>();
    private final QMButton countryButton = Utils.make(new QMButton("", "#1f1e33", false), button -> {
        button.setPrefWidth(100);
        button.setOnAction(actionEvent -> selectCountry());
    });
    private final QMButton smsCodeButton = Utils.make(new QMButton("获取验证码", "#1f1e33", false), button -> {
        button.setPrefWidth(140);
        button.setOnAction(actionEvent -> startCaptcha());
    });
    private final QMButton loginButton = Utils.make(new QMButton("登录", QMButton.DEFAULT_COLOR, false), button -> {
        button.disable(true);
        button.setPrefWidth(200);
        button.setOnAction(actionEvent -> login());
        button.setDefaultButton(true);
        button.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
    });
    private final JFXTextField accountField;
    private final JFXTextField passwordField = Utils.make(new JFXTextField(), textField -> {
        textField.setLabelFloat(true);
        textField.setFocusColor(Paint.valueOf("#1a8bcc"));
        textField.setPrefWidth(240);
        textField.setPromptText("验证码");
        textField.setFont(Settings.DEFAULT_FONT);
    });
    private int selectedCountry = 1;
    private String selectedCountryCid = "86";
    private int countdown = 0;

    private String captchaKey;

    private String tmpCode;
    private String requestId;

    private Stage captcha;

    private SMSLogin(boolean fromPassword, LoginMain loginMain) {
        super(30);
        this.fromPassword = fromPassword;
        this.loginMain = loginMain;
        this.setAlignment(Pos.CENTER);
        if (fromPassword) {
            this.accountField = Utils.make(new JFXTextField(), textField -> textField.setPrefWidth(400));
            this.accountField.setDisable(true);
        } else {
            this.accountField = Utils.make(new JFXTextField(), textField -> textField.setPrefWidth(280));
            new Thread(this::initCountries).start();
        }
        this.accountField.setLabelFloat(true);
        this.accountField.setFocusColor(Paint.valueOf("#1a8bcc"));
        this.accountField.setPromptText("手机号");
        this.accountField.setFont(Settings.DEFAULT_FONT);
        this.accountField.textProperty().addListener((observable, oldValue, newValue) -> loginButton.disable((accountField.getText().isEmpty() && !this.fromPassword) || passwordField.getText().isEmpty()));
        this.passwordField.textProperty().addListener((observable, oldValue, newValue) -> loginButton.disable((accountField.getText().isEmpty() && !this.fromPassword) || passwordField.getText().isEmpty()));
        this.accountField.setTextFormatter(new TextFormatter<>(change -> {
            if (this.fromPassword || change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        }));
        this.passwordField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null;
        }));
        this.getChildren().add(Utils.make(new HBox(20), hBox -> {
            hBox.setAlignment(Pos.CENTER);
            if (!this.fromPassword) {
                hBox.getChildren().add(this.countryButton);
            }
            hBox.getChildren().add(accountField);
        }));
        this.getChildren().add(Utils.make(new HBox(20), hBox -> {
            hBox.setAlignment(Pos.CENTER);
            hBox.getChildren().add(passwordField);
            hBox.getChildren().add(smsCodeButton);
        }));
        this.getChildren().add(loginButton);
    }

    private void login() {
        if (captchaKey == null) {
            this.loginMain.showDialogMessage("请先获取短信验证码");
            return;
        }

        this.accountField.setDisable(true);
        this.passwordField.setDisable(true);
        this.loginButton.disable(true);
        this.loginMain.lockLoginMethodButtons(true);

        this.loginButton.setText("登录中");
        this.loginButton.setGraphic(new WhiteFontIcon("fas-bullseye"));

        new Thread(() -> {
            try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
                if (this.fromPassword) {
                    HttpPost httpPost = new HttpPost("https://passport.bilibili.com/x/safecenter/login/tel/verify");
                    httpPost.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                    httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    List<NameValuePair> lp = new ArrayList<>();
                    lp.add(new BasicNameValuePair("tmp_code", tmpCode));
                    lp.add(new BasicNameValuePair("captcha_key", captchaKey));
                    lp.add(new BasicNameValuePair("type", "loginTelCheck"));
                    lp.add(new BasicNameValuePair("code", this.passwordField.getText()));
                    lp.add(new BasicNameValuePair("request_id", requestId));
                    lp.add(new BasicNameValuePair("source", "risk"));
                    httpPost.setEntity(new UrlEncodedFormEntity(lp));
                    CloseableHttpResponse response = client.execute(httpPost);
                    JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                    if (object.get("code").getAsInt() == 0) {
                        String code = object.getAsJsonObject("data").get("code").getAsString();
                        HttpPost httpPost1 = new HttpPost("https://passport.bilibili.com/x/passport-login/web/exchange_cookie");
                        httpPost1.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                        httpPost1.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        List<NameValuePair> lp1 = new ArrayList<>();
                        lp1.add(new BasicNameValuePair("source", "risk"));
                        lp1.add(new BasicNameValuePair("code", code));
                        httpPost1.setEntity(new UrlEncodedFormEntity(lp1));
                        CloseableHttpResponse response1 = client.execute(httpPost1);
                        JsonObject obj1 = JsonParser.parseString(EntityUtils.toString(response1.getEntity())).getAsJsonObject();
                        if (obj1.get("code").getAsInt() == 0) {
                            Settings.saveCookie(true);
                            Settings.setRefreshToken(obj1.getAsJsonObject("data").get("refresh_token").getAsString());
                            Platform.runLater(() -> ((LoginMain) this.getScene()).loginSuccess());
                        } else {
                            this.loginFail("交换 Cookie 失败", true);
                        }
                    } else {
                        this.loginFail("登录失败" + "(%s)".formatted(object.get("code").getAsInt()), true);
                    }
                } else {
                    HttpPost httpPost = new HttpPost("https://passport.bilibili.com/x/passport-login/web/login/sms");
                    httpPost.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                    httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                    List<NameValuePair> lp = new ArrayList<>();
                    lp.add(new BasicNameValuePair("cid", this.selectedCountryCid));
                    lp.add(new BasicNameValuePair("tel", this.accountField.getText()));
                    lp.add(new BasicNameValuePair("code", this.passwordField.getText()));
                    lp.add(new BasicNameValuePair("source", "main_web"));
                    lp.add(new BasicNameValuePair("captcha_key", captchaKey));
                    httpPost.setEntity(new UrlEncodedFormEntity(lp));
                    CloseableHttpResponse response = client.execute(httpPost);
                    JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                    int code = object.get("code").getAsInt();
                    if (code == 0) {
                        Settings.saveCookie(true);
                        Settings.setRefreshToken(object.getAsJsonObject("data").get("refresh_token").getAsString());
                        Platform.runLater(() -> ((LoginMain) this.getScene()).loginSuccess());
                    } else {
                        this.loginFail(object.get("message").getAsString() + "(%s)".formatted(code), true);
                    }
                }
            } catch (IOException e) {
                this.loginFail("登录请求失败", true);
            }
        }).start();
    }

    private void initCountries() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet("https://passport.bilibili.com/web/generic/country/list");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
            if (object.get("code").getAsInt() == 0) {
                object.getAsJsonObject("data").getAsJsonArray("common").forEach(country -> {
                    JsonObject countryObject = country.getAsJsonObject();
                    countries.add(new Country(countryObject.get("id").getAsInt(), countryObject.get("cname").getAsString(), countryObject.get("country_id").getAsString()));
                });
                object.getAsJsonObject("data").getAsJsonArray("others").forEach(country -> {
                    JsonObject countryObject = country.getAsJsonObject();
                    countries.add(new Country(countryObject.get("id").getAsInt(), countryObject.get("cname").getAsString(), countryObject.get("country_id").getAsString()));
                });
                this.selectedCountry = countries.getFirst().id;
                Platform.runLater(() -> this.countryButton.setText("+" + countries.getFirst().countryId));
            }
        } catch (IOException e) {
            Platform.runLater(() -> {
                JFXDialog dialog = new JFXDialog(this.loginMain.getRootStackPane(), Utils.make(Utils.createLabel("获取国家与地区列表失败\n" + e.getMessage(), "RED"), label -> {
                    JFXDialog.setMargin(label, new Insets(30, 30, 30, 30));
                }), JFXDialog.DialogTransition.CENTER);
                dialog.show();
            });
        }
    }

    public void selectCountry() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(360);
        VBox vBox = new VBox(2);
        vBox.setAlignment(Pos.CENTER);
        scrollPane.setContent(vBox);
        TitledDialog dialog = new TitledDialog("选择国家或地区", ((LoginMain) this.getScene()).getRootStackPane(), scrollPane, JFXDialog.DialogTransition.CENTER, false);
        this.countries.forEach(country -> {
            QMButton button = new QMButton("%s (+%s)".formatted(country.cname, country.countryId), null, false);
            if (country.id == selectedCountry) {
                button.setTextFill(Paint.valueOf("0x1a8bcc"));
            }
            button.setPrefWidth(400);
            button.setTextAlignment(TextAlignment.LEFT);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setOnAction(actionEvent -> {
                this.selectedCountry = country.id;
                this.selectedCountryCid = country.countryId;
                this.countryButton.setText("+" + country.countryId);
                dialog.close();
            });
            vBox.getChildren().add(button);
        });
        dialog.show();
    }

    @Override
    public void startCaptcha() {
        if (!this.fromPassword && this.accountField.getText().isEmpty()) {
            this.loginFail("请输入手机号", true);
            return;
        }
        this.smsCodeButton.disable(true);
        this.captcha = new Stage(StageStyle.UNDECORATED);
        this.captcha.initModality(Modality.WINDOW_MODAL);
        this.captcha.setResizable(false);
        this.captcha.initOwner(this.getScene().getWindow());
        this.captcha.show();
        if (this.fromPassword) {
            this.captcha.setScene(new Captcha(this, true));
        } else {
            this.captcha.setScene(new Captcha(this, false));
        }
        this.captcha.setOnCloseRequest(event -> {
            ((Captcha) captcha.getScene()).stop();
            captchaFail(true, null);
        });
    }

    @Override
    public void captchaSuccess(String token, String gt, String challenge, String validate, String seccode) {
        captcha.close();
        try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
            if (this.fromPassword) {
                HttpPost httpPost = new HttpPost("https://passport.bilibili.com/x/safecenter/common/sms/send");
                httpPost.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                List<NameValuePair> lp = new ArrayList<>();
                lp.add(new BasicNameValuePair("tmp_code", tmpCode));
                lp.add(new BasicNameValuePair("sms_type", "loginTelCheck"));
                lp.add(new BasicNameValuePair("recaptcha_token", token));
                lp.add(new BasicNameValuePair("gee_challenge", challenge));
                lp.add(new BasicNameValuePair("gee_validate", validate));
                lp.add(new BasicNameValuePair("gee_seccode", seccode));
                httpPost.setEntity(new UrlEncodedFormEntity(lp));
                CloseableHttpResponse response = client.execute(httpPost);
                JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                if (object.get("code").getAsInt() == 0) {
                    captchaKey = object.getAsJsonObject("data").get("captcha_key").getAsString();
                }
            } else {
                HttpPost httpPost = new HttpPost("https://passport.bilibili.com/x/passport-login/web/sms/send");
                httpPost.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setHeader("TE", "trailers");
                List<NameValuePair> lp = new ArrayList<>();
                lp.add(new BasicNameValuePair("cid", String.valueOf(this.selectedCountryCid)));
                lp.add(new BasicNameValuePair("tel", this.accountField.getText()));
                lp.add(new BasicNameValuePair("source", "main-fe-header"));
                lp.add(new BasicNameValuePair("token", token));
                lp.add(new BasicNameValuePair("challenge", challenge));
                lp.add(new BasicNameValuePair("validate", validate));
                lp.add(new BasicNameValuePair("seccode", seccode));
                httpPost.setEntity(new UrlEncodedFormEntity(lp));
                CloseableHttpResponse response = client.execute(httpPost);
                JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                int code = object.get("code").getAsInt();
                if (code == 0) {
                    captchaKey = object.getAsJsonObject("data").get("captcha_key").getAsString();
                } else {
                    Platform.runLater(() -> this.loginMain.showDialogMessage(object.get("message").getAsString() + "(%s)".formatted(code), true));
                    return;
                }
            }
        } catch (IOException e) {
            Platform.runLater(() -> this.loginMain.showDialogMessage("获取验证码失败", true));
            return;
        }
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            countdown--;
            this.smsCodeButton.setText(this.countdown + "s");
        }));
        timeline.setCycleCount(60);
        timeline.setOnFinished(event -> {
            this.smsCodeButton.disable(false);
            this.smsCodeButton.setText("获取验证码");
        });
        timeline.play();
        Platform.runLater(() -> {
            this.loginMain.showDialogMessage("验证码已发送");

            this.smsCodeButton.disable(true);
            this.countdown = 60;
            this.smsCodeButton.setText(this.countdown + "s");
        });
    }

    @Override
    public void captchaFail(boolean manualCancel, String failMessage) {
        Platform.runLater(() -> {
            captcha.close();
            this.smsCodeButton.disable(false);
        });
        if (manualCancel) {
            loginFail("取消验证", false);
        } else {
            loginFail(failMessage, true);
        }
    }

    public void loginFail(String failMessage, boolean isError) {
        Platform.runLater(() -> {
            this.loginMain.loginFail(failMessage, isError);
            if (!this.fromPassword) {
                this.accountField.setDisable(false);
            }
            this.passwordField.setDisable(false);
            this.loginButton.setText("登录");
            this.loginButton.setGraphic(new WhiteFontIcon("fas-sign-in-alt"));
        });
    }

    public static SMSLogin standalone(LoginMain loginMain) {
        return new SMSLogin(false, loginMain);
    }

    public static SMSLogin fromPassword(LoginMain loginMain, String tmpCode, String requestId) {
        SMSLogin smsLogin = new SMSLogin(true, loginMain);
        smsLogin.tmpCode = tmpCode;
        smsLogin.requestId = requestId;
        smsLogin.accountField.setText("获取中……");
        new Thread(() -> {
            try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(Settings.getCookieStore()).build()) {
                URI uri = new URIBuilder("https://passport.bilibili.com/x/safecenter/user/info")
                        .addParameter("tmp_code", tmpCode)
                        .build();
                HttpGet httpGet = new HttpGet(uri);
                CloseableHttpResponse response = client.execute(httpGet);
                JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                if (object.get("code").getAsInt() == 0) {
                    smsLogin.accountField.setText(object.getAsJsonObject("data").getAsJsonObject("account_info").get("hide_tel").getAsString());
                }
            } catch (Exception e) {
                smsLogin.accountField.setText("获取失败");
            }
        }).start();
        return smsLogin;
    }

    @Override
    public boolean lockLoginMethod() {
        return captchaKey != null || fromPassword;
    }

    private record Country(int id, String cname, String countryId) {

    }
}
