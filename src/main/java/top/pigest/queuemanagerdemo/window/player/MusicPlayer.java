package top.pigest.queuemanagerdemo.window.player;

import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class MusicPlayer extends Scene {
    private final StackPane rootStackPane = new StackPane();
    private final BorderPane rootBorderPane = new BorderPane();
    private final BorderPane up = new BorderPane();
    private final BorderPane down = new BorderPane();
    private final BorderPane info = new BorderPane();
    public MusicPlayer() {
        super(new Pane(), -1, -1, false, SceneAntialiasing.BALANCED);
        this.setRoot(this.rootStackPane);
        this.rootStackPane.getChildren().add(rootBorderPane);
        this.rootBorderPane.setTop(up);
        this.rootBorderPane.setBottom(down);
        ImageView imageView = new ImageView();
        imageView.setFitHeight(250);
        imageView.setFitWidth(250);
    }
}
