package top.pigest.queuemanagerdemo.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.widget.QMButton;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class Utils {
    public static <T> T make(T object, Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }

    public static void showDialogMessage(String message, String color, StackPane rootStackPane) {
        JFXDialog dialog = new JFXDialog(rootStackPane, make(new Label(message), label -> {
            label.setFont(Settings.DEFAULT_FONT);
            label.setTextFill(Paint.valueOf(color));
            label.setTextAlignment(TextAlignment.CENTER);
            JFXDialog.setMargin(label, new Insets(30, 30, 30, 30));
        }), JFXDialog.DialogTransition.CENTER);
        dialog.show();
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
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:141.0) Gecko/20100101 Firefox/141.0");
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
}
