package top.pigest.queuemanagerdemo.control;

import com.jfoenix.controls.JFXClippedPane;
import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import top.pigest.queuemanagerdemo.util.Utils;

public class ScrollablePane<T extends Node> extends JFXClippedPane {
    private SequentialTransition sequentialTransition;

    private double width;
    private double pauseDuration = 3.0;
    private double scrollSpeed = 50;
    private double backDuration = 0.8;
    private final T node;

    public ScrollablePane(T node, double width) {
        this.width = width;
        this.node = node;
        this.getChildren().add(this.node);
        if (node instanceof Region region) {
            region.setMaxWidth(USE_PREF_SIZE);
            region.widthProperty().addListener((observable, oldValue, newValue) -> resetAnimation());
        }
        resetAnimation();
    }

    public void resetAnimation() {
        this.setMinWidth(width);
        this.setMaxWidth(width);
        double width = node.getLayoutBounds().getWidth();
        double translation = (width - this.width) / 2;
        Utils.onPresent(this.sequentialTransition, Animation::stop);
        this.node.setTranslateX(translation);
        if (width > this.width) {
            PauseTransition pause1 = new PauseTransition(Duration.seconds(pauseDuration));
            TranslateTransition right = new TranslateTransition(Duration.seconds(Math.abs(translation * 2) / scrollSpeed), node);
            right.setByX(-Math.abs(translation * 2));
            right.setInterpolator(Interpolator.LINEAR);
            PauseTransition pause2 = new PauseTransition(Duration.seconds(pauseDuration));
            TranslateTransition left = new TranslateTransition(Duration.seconds(backDuration), node);
            left.setByX(Math.abs(translation * 2));
            left.setInterpolator(Interpolator.LINEAR);
            SequentialTransition sequential = new SequentialTransition(pause1, right, pause2, left);
            sequential.setCycleCount(SequentialTransition.INDEFINITE);
            sequential.play();
            this.sequentialTransition = sequential;
        }
    }

    public T getNode() {
        return node;
    }

    public void setDisplayWidth(double width) {
        this.width = width;
        this.resetAnimation();
    }

    public void setPauseDuration(double pauseDuration) {
        this.pauseDuration = pauseDuration;
        this.resetAnimation();
    }

    public void setScrollSpeed(double scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
        this.resetAnimation();
    }

    public void setBackDuration(double backDuration) {
        this.backDuration = backDuration;
        this.resetAnimation();
    }
}
