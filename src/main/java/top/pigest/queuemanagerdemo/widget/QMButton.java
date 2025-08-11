package top.pigest.queuemanagerdemo.widget;

import com.jfoenix.controls.JFXButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import top.pigest.queuemanagerdemo.Settings;

public class QMButton extends JFXButton {
    public static final String DEFAULT_COLOR = "#1a8bcc";
    private String backgroundColor;
    private boolean border = true;

    public QMButton(String text) {
        this(text, DEFAULT_COLOR);
    }

    public QMButton(String text, String backgroundColor) {
        super(text);
        this.backgroundColor = backgroundColor;
        init();
    }

    public QMButton(String text, String backgroundColor, boolean border) {
        super(text);
        this.border = border;
        this.backgroundColor = backgroundColor;
        init();
    }

    public void init() {
        this.setFont(Settings.DEFAULT_FONT);
        this.setDisableVisualFocus(true);
        if (!border) {
            this.setButtonType(ButtonType.RAISED);
        }
        if (this.backgroundColor != null) {
            this.setTextFill(Paint.valueOf("0xffffff"));
        }
        if (border) {
            this.setBorder(new Border(new BorderStroke(Paint.valueOf("0x222222"), BorderStrokeStyle.SOLID, new CornerRadii(2), new BorderWidths(2))));
        }
        updateColor();
    }

    public void disable(boolean value) {
        this.setDisable(value);
        updateColor();
    }

    public void updateColor() {
        if (this.backgroundColor != null) {
            if (this.isDisable()) {
                this.setStyle("-fx-background-color: #888888");
            } else {
                this.setStyle("-fx-background-color: " + this.backgroundColor);
            }
        }
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.updateColor();
    }
}
