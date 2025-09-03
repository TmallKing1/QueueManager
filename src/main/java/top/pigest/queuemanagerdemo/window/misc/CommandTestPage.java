package top.pigest.queuemanagerdemo.window.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.WhiteFontIcon;
import top.pigest.queuemanagerdemo.liveroom.LiveMessageService;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.window.main.ChildPage;
import top.pigest.queuemanagerdemo.window.main.NamedPage;

import java.util.concurrent.CompletableFuture;

public class CommandTestPage extends VBox implements NamedPage, ChildPage {
    private Pane parentPage;

    public CommandTestPage() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(30);
        Label label = Utils.createLabel("此处你可以测试本应用中各种功能的效果\n在下方输入框输入模拟弹幕并点击执行即可\n弹幕仅会被相关功能模块监听\n不会实际发送到你的直播间");
        label.setTextAlignment(TextAlignment.CENTER);
        JFXTextField textField = new JFXTextField();
        textField.setFont(Settings.DEFAULT_FONT);
        textField.setPromptText("输入模拟弹幕");
        textField.setMaxWidth(500);
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 40) {
                textField.setText(oldValue);
            }
        });

        WhiteFontIcon whiteFontIcon = new WhiteFontIcon("far-paper-plane");
        QMButton execute = getExecute(whiteFontIcon, textField);
        this.getChildren().addAll(label, textField, execute);
    }

    private static QMButton getExecute(WhiteFontIcon whiteFontIcon, JFXTextField textField) {
        QMButton execute = new QMButton("执行", "#1a8bcc", false);
        execute.setGraphic(whiteFontIcon);
        execute.setDefaultButton(true);
        execute.setPrefWidth(150);
        execute.setOnAction(event -> {
            if (!LiveMessageService.getInstance().isSessionAvailable()) {
                Utils.showDialogMessage("请先连接直播弹幕服务", true, QueueManager.INSTANCE.getMainScene().getRootDrawer());
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("cmd", "DANMU_MSG");
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(new JsonArray());
            jsonArray.add(textField.getText());
            JsonArray user = new JsonArray();
            user.add(Settings.MID);
            user.add(QueueManager.INSTANCE.getMainScene().getUserName());
            jsonArray.add(user);
            jsonObject.add("info", jsonArray);
            CompletableFuture.runAsync(() -> LiveMessageService.getInstance().getMessageHandlers().forEach(handler -> handler.handle(jsonObject)));
        });
        return execute;
    }

    @Override
    public Pane getParentPage() {
        return parentPage;
    }

    @Override
    public void setParentPage(Pane parentPage) {
        this.parentPage = parentPage;
    }

    @Override
    public String getName() {
        return "命令测试";
    }
}
