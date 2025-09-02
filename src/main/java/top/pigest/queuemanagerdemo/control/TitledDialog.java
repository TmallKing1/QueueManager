package top.pigest.queuemanagerdemo.control;

import com.jfoenix.controls.JFXDialog;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.util.Utils;

public class TitledDialog extends JFXDialog {
    public TitledDialog(String title, StackPane dialogContainer, Region content, DialogTransition transition, boolean overlayClose) {
        super(dialogContainer, null, transition, overlayClose);
        BorderPane borderPane = new BorderPane();
        this.setContent(borderPane);
        BorderPane.setMargin(content, new Insets(10, 20, 20, 20));
        Label label = Utils.make(new Label(), label1 -> {
            label1.setFont(Settings.DEFAULT_FONT);
            BorderPane.setMargin(label1, new Insets(20, 20, 10, 20));
        });
        borderPane.setTop(label);
        label.setText(title);
        borderPane.setCenter(content);
    }
}
