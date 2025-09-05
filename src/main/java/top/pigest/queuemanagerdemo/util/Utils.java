package top.pigest.queuemanagerdemo.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.effects.JFXDepthManager;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.control.QMButton;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Utils {
    public static <T> T make(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    public static <T> void onPresent(T object, Consumer<T> consumer) {
        if (object == null) {
            return;
        }
        consumer.accept(object);
    }

    public static void showDialogMessage(String message, boolean isError, StackPane rootStackPane) {
        showDialogMessage(message, isError, rootStackPane, 5, 400);
    }

    public static void showDialogMessage(String message, boolean isError, StackPane rootStackPane, double duration) {
        showDialogMessage(message, isError, rootStackPane, duration, 400);
    }

    public static void showDialogMessage(String message, boolean isError, StackPane rootStackPane, double duration, int width) {
        JFXSnackbar snackbar = new JFXSnackbar(rootStackPane);
        snackbar.setPrefWidth(width);
        snackbar.setCursor(Cursor.HAND);
        snackbar.setOnMouseClicked(event -> snackbar.close());
        Label toast = new Label();
        toast.setMinWidth(Control.USE_PREF_SIZE);
        toast.setWrapText(true);
        toast.setText(message);
        toast.setFont(Settings.DEFAULT_FONT);
        toast.setTextFill(Paint.valueOf("WHITE"));
        StackPane toastContainer = new StackPane(toast);
        toastContainer.setPadding(new Insets(10, 20, 10, 20));
        toastContainer.setBackground(new Background(new BackgroundFill(Paint.valueOf(isError ? "#ff6060" : "#1f1e33"), new CornerRadii(3), Insets.EMPTY)));
        JFXDepthManager.setDepth(toastContainer, 2);
        JFXSnackbar.SnackbarEvent snackbarEvent = new JFXSnackbar.SnackbarEvent(toastContainer, Duration.seconds(duration));
        snackbar.enqueue(snackbarEvent);
    }

    public static void showChoosingDialog(String message, String strA, String strB, Consumer<ActionEvent> actionA, Consumer<ActionEvent> actionB, StackPane rootStackPane) {
        VBox vBox = new VBox();
        JFXDialog dialog = new JFXDialog(rootStackPane, vBox, JFXDialog.DialogTransition.CENTER, false);
        dialog.setId("close-confirm");
        vBox.setPadding(new Insets(20, 20, 20, 20));
        Label label = Utils.createLabel(message);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setPadding(new Insets(0, 0, 20, 0));
        vBox.getChildren().add(label);
        HBox hBox = new HBox(40);
        hBox.setAlignment(Pos.CENTER);
        QMButton ok = new QMButton(strA, QMButton.DEFAULT_COLOR, false);
        ok.setPrefWidth(80);
        ok.setOnAction(event -> {
            actionA.accept(event);
            dialog.close();
        });
        QMButton cancel = new QMButton(strB, "#bb5555", false);
        cancel.setPrefWidth(80);
        cancel.setOnAction(event -> {
            actionB.accept(event);
            dialog.close();
        });
        hBox.getChildren().addAll(ok, cancel);
        vBox.getChildren().add(hBox);
        dialog.show();
    }

    public static Label createLabel(String text) {
        Label label = new Label(text);
        label.setFont(Settings.DEFAULT_FONT);
        return label;
    }

    public static Label createLabel(String text, String color) {
        Label label = createLabel(text);
        label.setTextFill(Paint.valueOf(color));
        return label;
    }

    public static Label createLabel(String text, int prefWidth) {
        Label label = createLabel(text);
        label.setPrefWidth(prefWidth);
        return label;
    }

    public static Label createLabel(String text, int prefWidth, Pos alignment) {
        Label label = createLabel(text);
        label.setPrefWidth(prefWidth);
        label.setAlignment(alignment);
        return label;
    }

    public static void fillCookies(CookieStore cookieStore) throws IOException {
        List<Cookie> cookies = cookieStore.getCookies();
        if (cookies.stream().noneMatch(cookie -> cookie.getName().equals("buvid3"))) {
            try (CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(cookieStore).build()) {
                HttpGet httpGet = new HttpGet("https://api.bilibili.com/x/frontend/finger/spi");
                httpGet.setConfig(Settings.DEFAULT_REQUEST_CONFIG);
                httpGet.setHeader("User-Agent", Settings.USER_AGENT);
                HttpResponse response = client.execute(httpGet);
                JsonObject object = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                String b3 = object.getAsJsonObject("data").get("b_3").getAsString();
                String b4 = object.getAsJsonObject("data").get("b_4").getAsString();
                BasicClientCookie buvid3 = new BasicClientCookie("buvid3", b3);
                buvid3.setDomain("bilibili.com");
                buvid3.setExpiryDate(new Date(System.currentTimeMillis() + 400L * 24 * 3600 * 1000));
                buvid3.setPath("/");
                Settings.getCookieStore().addCookie(buvid3);
                BasicClientCookie buvid4 = new BasicClientCookie("buvid4", b4);
                buvid4.setDomain("bilibili.com");
                buvid3.setExpiryDate(new Date(System.currentTimeMillis() + 400L * 24 * 3600 * 1000));
                buvid3.setPath("/");
                Settings.getCookieStore().addCookie(buvid4);
                Settings.saveCookie(false);
            }
        }
    }

    public static void stringToQRCode(String s, QRCodeWriter writer, Map<EncodeHintType, Object> hints, ImageView imageView) throws WriterException, IOException {
        BitMatrix bitMatrix = writer.encode(s, BarcodeFormat.QR_CODE, 270, 270, hints);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", Settings.DATA_DIRECTORY.toPath().resolve("qrcode.png"));
        Image image = new Image(new FileInputStream(Settings.DATA_DIRECTORY.toPath().resolve("qrcode.png").toFile()));
        imageView.setImage(image);
    }

    public static Color toColor(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return Color.rgb(red, green, blue);
    }

    public static String colorToString(Color color) {
        int r = Math.toIntExact(Math.round(color.getRed() * 255.0));
        int g = Math.toIntExact(Math.round(color.getGreen() * 255.0));
        int b = Math.toIntExact(Math.round(color.getBlue() * 255.0));
        return String.format("#%02x%02x%02x", r, g, b);
    }
}
