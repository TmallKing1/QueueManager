package top.pigest.queuemanagerdemo.control;

import javafx.scene.paint.Paint;
import org.kordamp.ikonli.javafx.FontIcon;

public class WhiteFontIcon extends FontIcon {
    public WhiteFontIcon(String iconCode) {
        super(iconCode);
        this.setIconColor(Paint.valueOf("WHITE"));
    }
}
