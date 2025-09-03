package top.pigest.queuemanagerdemo.music;

import com.google.gson.JsonObject;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.WhiteFontIcon;
import top.pigest.queuemanagerdemo.window.music.MusicSystemPage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Music163Login extends Scene {
    private final StackPane root = new StackPane();
    private final BorderPane borderPane = new BorderPane();

    private final StackPane qrCodeContainer = new StackPane();
    private final ImageView imageView = new ImageView();
    private final VBox qrCodeHintBox = Utils.make(new VBox(), vBox -> {
        vBox.setAlignment(Pos.CENTER);
        vBox.setPrefSize(270, 270);
        vBox.setMaxSize(270, 270);
        vBox.setStyle("-fx-background-color: #ffffffdd;");
    });
    private final Label qrCodeHint = Utils.make(Utils.createLabel(""), label -> {
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
    });
    private final Label hint = Utils.make(Utils.createLabel("请使用网易云音乐移动端扫描二维码"), label -> {
        BorderPane.setMargin(label, new Insets(0, 0, 30, 0));
        BorderPane.setAlignment(label, Pos.CENTER);
    });
    private final QMButton reset = Utils.make(new QMButton("重新生成", QMButton.DEFAULT_COLOR, false), button -> {
        button.setPrefWidth(200);
        button.setOnAction(event -> refreshQRCode());
        button.setGraphic(new WhiteFontIcon("fas-undo"));
        BorderPane.setMargin(button, new Insets(0, 0, 25, 0));
        BorderPane.setAlignment(button, Pos.CENTER);
    });
    private final MusicSystemPage parent;
    private String qrcodeKey;
    private Timeline timeline;
    private boolean scanned;

    public Music163Login(MusicSystemPage parent) {
        super(new Pane(), 640, 480, false, SceneAntialiasing.BALANCED);
        this.parent = parent;
        this.setRoot(root);
        this.root.getChildren().add(borderPane);
        this.qrCodeContainer.setPrefSize(270, 270);
        new Thread(this::refreshQRCode).start();
        this.borderPane.setCenter(qrCodeContainer);
        BorderPane.setMargin(qrCodeContainer, new Insets(15, 0, 0, 0));
    }

    private void refreshQRCode() {
        Platform.runLater(() -> this.qrCodeContainer.getChildren().clear());
        this.scanned = false;
        try {
            String s = getLoginURL();
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            Platform.runLater(() -> {
                try {
                    Utils.stringToQRCode(s, writer, hints, this.imageView);
                    this.qrCodeContainer.getChildren().add(imageView);
                this.borderPane.setBottom(hint);
                this.timeline = createTimeline();
                this.timeline.play();
                } catch (WriterException | IOException e) {
                    Utils.showDialogMessage("二维码生成失败", true, root);
                    this.borderPane.setBottom(reset);
                }
            });
        } catch (Exception e) {
            Utils.showDialogMessage("二维码生成失败", true, root);
            this.borderPane.setBottom(reset);
        }
    }

    private String getLoginURL() throws Exception {
        JsonObject object = Music163Api.qrLoginUniKey();
        this.qrcodeKey = object.get("unikey").getAsString();
        return "https://music.163.com/login?codekey=" + this.qrcodeKey;
    }

    private Timeline createTimeline() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(1000), event -> {
                    try {
                        JsonObject o = Music163Api.qrLoginCheck(this.qrcodeKey);
                        int code = o.get("code").getAsInt();
                        int status = updateCodeStatus(code);
                        switch (status) {
                            case 0 -> loginSuccess();
                            case 2 -> loginFail();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        return timeline;
    }

    public void loginSuccess() {
        stopTimeline();
        this.parent.closeLogin();
        Settings.saveCookie(true);
        Platform.runLater(() -> {
            QueueManager.INSTANCE.getMainScene().setMainContainer(new MusicSystemPage().withParentPage(this.parent.getParentPage()), this.parent.getId());
            Utils.showDialogMessage("登录成功", false, QueueManager.INSTANCE.getMainScene().getRootDrawer());
        });
    }

    public void stopTimeline() {
        Utils.onPresent(timeline, Timeline::stop);
    }

    public void loginFail() {
        stopTimeline();
    }

    private int updateCodeStatus(int statusCode) {
        switch (statusCode) {
            case 803 -> {
                return 0;
            }
            case 800 -> {
                Platform.runLater(() -> {
                    this.qrCodeHintBox.getChildren().clear();
                    this.qrCodeHint.setText("二维码已失效");
                    this.qrCodeHintBox.getChildren().add(qrCodeHint);
                    this.qrCodeContainer.getChildren().add(this.qrCodeHintBox);
                    this.borderPane.setBottom(this.reset);
                });
                return 2;
            }
            case 802 -> {
                Platform.runLater(() -> {
                    if (!this.scanned) {
                        this.qrCodeHintBox.getChildren().clear();
                        this.qrCodeHint.setText("扫码成功\n请在手机上点击【授权登录】");
                        FontIcon icon = new FontIcon("far-check-circle:50");
                        icon.setIconColor(Paint.valueOf("#379437"));
                        VBox.setMargin(icon, new Insets(0, 0, 30, 0));
                        this.qrCodeHintBox.getChildren().add(icon);
                        this.qrCodeHintBox.getChildren().add(this.qrCodeHint);
                        this.qrCodeContainer.getChildren().add(this.qrCodeHintBox);
                        this.borderPane.setBottom(null);
                        this.scanned = true;
                    }
                });
                return 1;
            }
            case 801 -> {
                return 1;
            }
        }
        return 1;
    }
}
