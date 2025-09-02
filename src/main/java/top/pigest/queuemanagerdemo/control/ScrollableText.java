package top.pigest.queuemanagerdemo.control;

import javafx.animation.*;
import javafx.scene.text.Text;
import top.pigest.queuemanagerdemo.Settings;

public class ScrollableText extends ScrollablePane<Text> {
    private SequentialTransition sequentialTransition;

    public ScrollableText(String text, double width) {
        super(new Text(text), width);
        this.getText().setFont(Settings.DEFAULT_FONT);
        resetAnimation();
    }
    public Text getText() {
        return this.getNode();
    }

    public String getTextContent() {
        return this.getNode().getText();
    }

    public void setText(String text) {
        this.getNode().setText(text);
        this.resetAnimation();
    }
}
