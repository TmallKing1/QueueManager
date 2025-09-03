package top.pigest.queuemanagerdemo.window.music;

import com.jfoenix.controls.JFXClippedPane;
import com.jfoenix.effects.JFXDepthManager;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.ScrollablePane;
import top.pigest.queuemanagerdemo.control.ScrollableText;
import top.pigest.queuemanagerdemo.music.MusicHandler;
import top.pigest.queuemanagerdemo.music.Song;
import top.pigest.queuemanagerdemo.util.MouseUtils;

import java.util.List;
import java.util.Objects;

public class MusicPlayerScene extends Scene {
    private final StackPane rootStackPane = new StackPane();
    private final BorderPane rootBorderPane = new BorderPane();
    private final StackPane up = new StackPane();
    private final HBox upMain = new HBox();
    private final HBox upControl = new HBox();
    private final ImageView cover = new ImageView();
    private final VBox info = new VBox(8);
    private final HBox infoBox = new HBox();
    private final ScrollablePane<HBox> titleInfo = new ScrollablePane<>(infoBox, 300);
    private final Text title = new Text();
    private final Text artist = new Text();
    private final ScrollableText lyric = new ScrollableText("", 300);

    private final JFXClippedPane down = new JFXClippedPane();
    private final VBox downMain = new VBox();
    private Animation downAnimation;

    private final MusicHandler musicHandler;

    private double xOffset = 0;
    private double yOffset = 0;
    private double mouseX = 0;
    private double mouseY = 0;

    public MusicPlayerScene(MusicHandler musicHandler) {
        super(new Pane(), -1, -1, false, SceneAntialiasing.BALANCED);
        this.setFill(Color.TRANSPARENT);
        this.musicHandler = musicHandler;
        this.setRoot(this.rootStackPane);
        this.rootStackPane.setOnMouseMoved(event -> {
            this.mouseX = event.getSceneX();
            this.mouseY = event.getSceneY();
        });
        this.rootStackPane.setBackground(Background.EMPTY);
        this.rootStackPane.getChildren().add(rootBorderPane);
        this.rootBorderPane.setTop(up);
        BorderPane.setMargin(down, new Insets(10, 0, 0, 0));
        this.setQueueShow();
        this.up.getChildren().add(upMain);
        this.upControl.setAlignment(Pos.CENTER);
        this.upControl.setOpacity(0);
        this.up.setOnMouseEntered(event -> {
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(200), upControl);
            fadeTransition.setFromValue(this.up.getChildren().contains(upControl) ? upControl.getOpacity() : 0);
            fadeTransition.setToValue(1);
            fadeTransition.setInterpolator(Interpolator.LINEAR);
            fadeTransition.play();
            if (!this.up.getChildren().contains(upControl)) {
                this.up.getChildren().add(upControl);
            }
        });
        this.up.setOnMouseExited(event -> {
            if (!event.isPrimaryButtonDown()) {
                hideControl(upControl, this.up);
            }
        });

        HBox.setMargin(cover, new Insets(15, 15, 15 , 15));
        HBox.setMargin(info, new Insets(15, 15, 15 , 0));
        this.upMain.setAlignment(Pos.CENTER_LEFT);
        this.upMain.getChildren().addAll(cover, info);
        this.info.setAlignment(Pos.CENTER_LEFT);
        this.info.getChildren().add(titleInfo);
        this.setLyricShow();
        this.infoBox.getChildren().addAll(title, artist);
        this.infoBox.setAlignment(Pos.BOTTOM_CENTER);
        this.cover.setFitHeight(60);
        this.cover.setFitWidth(60);
        JFXDepthManager.setDepth(this.cover, 2);
        this.refreshControlButtons(false);

        updateListHeight();
        this.down.setAlignment(Pos.TOP_LEFT);
        this.down.getChildren().add(downMain);

        updateColor();
        updateFont();
        stopPlaying();
    }

    public void updateListHeight() {
        this.down.setMinHeight(Settings.getMusicServiceSettings().listHeight);
        this.down.setMaxHeight(Settings.getMusicServiceSettings().listHeight);
    }

    private void hideControl(HBox upControl, StackPane up) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(200), upControl);
        fadeTransition.setFromValue(1);
        fadeTransition.setToValue(0);
        fadeTransition.setInterpolator(Interpolator.LINEAR);
        fadeTransition.play();
        fadeTransition.setOnFinished(event1 -> {
            if (!MouseUtils.isMouseInsideNode(up, mouseX, mouseY)) {
                up.getChildren().remove(upControl);
            }
        });
    }

    public void refreshControlButtons(boolean lock) {
        this.upControl.getChildren().clear();
        this.upControl.setOnMousePressed(event -> {
            this.xOffset = event.getSceneX();
            this.yOffset = event.getSceneY();
        });
        this.upControl.setOnMouseDragged(event -> {
            this.musicHandler.getPlayerStage().setX(event.getScreenX() - this.xOffset);
            this.musicHandler.getPlayerStage().setY(event.getScreenY() - this.yOffset);
        });
        Color text = Color.valueOf(Settings.getMusicServiceSettings().textColor);
        if (Settings.getMusicServiceSettings().playPrevious) {
            QMButton prev = new QMButton("", null, false);
            prev.setMinSize(50, 50);
            FontIcon prevIcon = new FontIcon("fas-step-backward");
            prevIcon.setIconColor(text);
            prev.setGraphic(prevIcon);
            prev.setOnAction(event -> {
                this.musicHandler.playPrevious();
                refreshControlButtons(true);
            });
            this.upControl.getChildren().add(prev);
        }

        QMButton curr = new QMButton("", null, false);
        curr.setMinSize(50, 50);
        if (this.musicHandler.getState() == MusicHandler.State.DOWNLOADING || lock) {
            FontIcon bullsEye = new FontIcon("fas-bullseye");
            bullsEye.setIconColor(text);
            curr.setGraphic(bullsEye);
            curr.setOnAction(event -> {});
        } else if (this.musicHandler.getState() == MusicHandler.State.PLAYING) {
            FontIcon pause = new FontIcon("fas-pause");
            pause.setIconColor(text);
            curr.setGraphic(pause);
            curr.setOnAction(event -> this.musicHandler.pause());
        } else if (this.musicHandler.getState() == MusicHandler.State.PAUSED) {
            FontIcon pause = new FontIcon("fas-play");
            pause.setIconColor(text);
            curr.setGraphic(pause);
            curr.setOnAction(event -> this.musicHandler.play(this.musicHandler.getCurrentSong()));
        } else {
            FontIcon pause = new FontIcon("fas-play");
            pause.setIconColor(text);
            curr.setGraphic(pause);
            curr.setOnAction(event -> {
                this.musicHandler.playNext();
                refreshControlButtons(true);
            });
        }
        this.upControl.getChildren().add(curr);

        QMButton next = new QMButton("", null, false);
        next.setMinSize(50, 50);
        FontIcon nextIcon = new FontIcon("fas-step-forward");
        nextIcon.setIconColor(text);
        next.setGraphic(nextIcon);
        next.setOnAction(event -> {
            this.musicHandler.playNext();
            refreshControlButtons(true);
        });
        this.upControl.getChildren().add(next);

        QMButton thumbTack = new QMButton("", null, false);
        thumbTack.setMinSize(50, 50);
        FontIcon thumbIcon = new FontIcon("fas-thumbtack");
        thumbIcon.setIconColor(text);
        if (Settings.getMusicServiceSettings().alwaysOnTop) {
            thumbIcon.setIconColor(Color.LIGHTGREEN);
        }
        thumbTack.setGraphic(thumbIcon);
        thumbTack.setOnAction(event -> {
            Settings.getMusicServiceSettings().setAlwaysOnTop(!Settings.getMusicServiceSettings().alwaysOnTop);
            this.musicHandler.getPlayerStage().setAlwaysOnTop(Settings.getMusicServiceSettings().alwaysOnTop);
            refreshControlButtons(false);
        });
        this.upControl.getChildren().add(thumbTack);

        QMButton close =  new QMButton("", null, false);
        close.setMinSize(50, 50);
        FontIcon closeIcon = new FontIcon("fas-times");
        closeIcon.setIconColor(text);
        close.setGraphic(closeIcon);
        close.setOnAction(event -> this.musicHandler.getPlayerStage().hide());
        this.upControl.getChildren().add(close);
    }

    public void setLyricShow() {
        this.info.getChildren().remove(lyric);
        if (Settings.getMusicServiceSettings().displayLyric) {
            this.info.getChildren().add(lyric);
        }
    }

    public void setQueueShow() {
        if (Settings.getMusicServiceSettings().displayList) {
            this.rootBorderPane.setBottom(down);
        } else {
            this.rootBorderPane.setBottom(null);
        }
        if (this.musicHandler.getPlayerStage() != null && this.musicHandler.getPlayerStage().isShowing()) {
            this.musicHandler.getPlayerStage().sizeToScene();
        }
        updateColor();
    }

    public void stopPlaying() {
        this.cover.setImage(this.musicHandler.getNoPlayingImage());
        this.title.setText("暂无播放");
        this.artist.setText("");
        this.lyric.setText("");
    }

    public void updateContent() {
        Song song = this.musicHandler.getCurrentSong();
        this.cover.setImage(song.getCover(this.cover::setImage));
        this.title.setText(song.getTitle());
        if (song.getArtist() != null) {
            this.artist.setText(" - " + song.getArtist());
        }
        updateQueue(this.musicHandler.getSongs());
    }

    public void updateQueue(List<Song> songs) {
        this.downMain.getChildren().clear();
        for (int index = 0; index < songs.size(); index++) {
            this.downMain.getChildren().add(this.getSongManager(songs.get(index), index));
        }
    }

    public void addQueueSong(List<Song> songs, int index) {
        if (downAnimation != null) {
            downAnimation.stop();
        }
        updateQueue(songs);
        if (songs.isEmpty()) {
            return;
        }
        if (index == -1) {
            index = songs.size() - 1;
        }
        TranslateTransition transition = new TranslateTransition(Duration.millis(200), this.downMain.getChildren().get(index));
        transition.setFromX(300);
        transition.setToX(0);
        transition.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition transition1 = new FadeTransition(Duration.millis(200), this.downMain.getChildren().get(index));
        transition1.setFromValue(0);
        transition1.setToValue(1);
        transition1.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().add(transition);
        parallelTransition.getChildren().add(transition1);
        while (index < songs.size() - 1) {
            index++;
            TranslateTransition transition2 = new TranslateTransition(Duration.millis(200), this.downMain.getChildren().get(index));
            transition2.setFromY(-30);
            transition2.setToY(0);
            parallelTransition.getChildren().add(transition2);
        }
        downAnimation = parallelTransition;
        parallelTransition.play();
        parallelTransition.setOnFinished(event -> this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.setControllable(true);
            }
        }));
        this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.setControllable(false);
            }
        });
    }

    public void removeQueueSong(List<Song> songs, int index) {
        if (downAnimation != null) {
            downAnimation.stop();
        }
        updateQueue(songs);
        TranslateTransition transition = new TranslateTransition(Duration.millis(200), this.downMain.getChildren().get(index));
        transition.setFromX(0);
        transition.setToX(-300);
        transition.setInterpolator(Interpolator.EASE_IN);
        FadeTransition transition1 = new FadeTransition(Duration.millis(200), this.downMain.getChildren().get(index));
        transition1.setFromValue(1);
        transition1.setToValue(0);
        transition1.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().add(transition);
        parallelTransition.getChildren().add(transition1);
        index++;
        while (index < songs.size()) {
            TranslateTransition transition2 = new TranslateTransition(Duration.millis(200), this.downMain.getChildren().get(index));
            transition2.setFromY(0);
            transition2.setToY(-30);
            parallelTransition.getChildren().add(transition2);
            index++;
        }
        downAnimation = parallelTransition;
        parallelTransition.play();
        parallelTransition.setOnFinished(event -> {
            updateQueue(this.musicHandler.getSongs());
            this.downMain.getChildren().forEach(node -> {
                if (node instanceof SongManager songManager) {
                    songManager.setControllable(true);
                }
            });
        });
        this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.setControllable(false);
            }
        });
    }

    public void moveQueueSong(List<Song> songs, int fromIndex, int toIndex) {
        if (downAnimation != null) {
            downAnimation.stop();
        }
        updateQueue(songs);
        TranslateTransition transition = new TranslateTransition(Duration.millis(200), this.downMain.getChildren().get(toIndex));
        transition.setFromY(30 * (fromIndex - toIndex));
        transition.setToY(0);
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.getChildren().add(transition);
        while (toIndex > fromIndex) {
            toIndex--;
            TranslateTransition transition2 = new TranslateTransition(Duration.millis(200), this.downMain.getChildren().get(toIndex));
            transition2.setFromY(30);
            transition2.setToY(0);
            parallelTransition.getChildren().add(transition2);
        }
        while (toIndex < fromIndex) {
            toIndex++;
            TranslateTransition transition2 = new TranslateTransition(Duration.millis(200), this.downMain.getChildren().get(toIndex));
            transition2.setFromY(-30);
            transition2.setToY(0);
            parallelTransition.getChildren().add(transition2);
        }
        parallelTransition.play();
        parallelTransition.setOnFinished(event -> this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.setControllable(true);
            }
        }));
        this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.setControllable(false);
            }
        });
    }

    public void clearQueueSong() {
        ParallelTransition parallelTransition = new ParallelTransition();
        this.downMain.getChildren().forEach(node -> {
            TranslateTransition transition = new TranslateTransition(Duration.millis(200), node);
            transition.setFromX(0);
            transition.setToX(-300);
            transition.setInterpolator(Interpolator.EASE_IN);
            FadeTransition  transition1 = new FadeTransition(Duration.millis(200), node);
            transition1.setFromValue(1);
            transition1.setToValue(0);
            transition1.setInterpolator(Interpolator.EASE_IN);
            parallelTransition.getChildren().add(transition);
            parallelTransition.getChildren().add(transition1);
        });
        parallelTransition.play();
        parallelTransition.setOnFinished(event -> updateQueue(this.musicHandler.getSongs()));
        this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.setControllable(false);
            }
        });
    }

    private SongManager getSongManager(Song song, int index) {
        return new SongManager(song, index);
    }

    public void updateLyric(int time) {
        Song song = this.musicHandler.getCurrentSong();
        String s = song.getLyrics().getLyric(time);
        if (!s.equals(this.lyric.getTextContent())) {
            this.lyric.setText(s);
        }
    }

    public void updateColor() {
        Color background = Color.valueOf(Settings.getMusicServiceSettings().backgroundColor);
        double backgroundOpacity = Settings.getMusicServiceSettings().backgroundOpacity;
        Color text = Color.valueOf(Settings.getMusicServiceSettings().textColor);
        Color finalBackground = new Color(background.getRed(), background.getGreen(), background.getBlue(), backgroundOpacity);
        CornerRadii qClosed = new CornerRadii(10);
        CornerRadii qOpenedUp = new CornerRadii(10, 10, 0, 0, false);
        CornerRadii qOpenedDown = new CornerRadii(0, 0, 10, 10, false);
        this.up.setBackground(new Background(new BackgroundFill(finalBackground, Settings.getMusicServiceSettings().displayList ? qOpenedUp : qClosed, Insets.EMPTY)));
        this.down.setBackground(new Background(new BackgroundFill(finalBackground, Settings.getMusicServiceSettings().displayList ? qOpenedDown : qClosed, Insets.EMPTY)));

        this.upControl.setBackground(new Background(new BackgroundFill(new Color(background.getRed(), background.getGreen(), background.getBlue(), 0.8 * backgroundOpacity), Settings.getMusicServiceSettings().displayList ? qOpenedUp : qClosed, Insets.EMPTY)));
        this.title.setFill(text);
        this.artist.setFill(text);
        this.lyric.getText().setFill(text);
        this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.updateColor();
            }
        });
        this.refreshControlButtons(false);
    }

    public void updateFont() {
        String font = Settings.getMusicServiceSettings().textFont;
        this.title.setFont(new Font(font.isEmpty() ? Settings.DEFAULT_FONT.getFamily() : font, 20));
        this.artist.setFont(new Font(font.isEmpty() ? Settings.DEFAULT_FONT.getFamily() : font, 15));
        this.lyric.getText().setFont(new Font(font.isEmpty() ? Settings.DEFAULT_FONT.getFamily() : font, 18));
        this.titleInfo.resetAnimation();
        this.lyric.resetAnimation();
        this.downMain.getChildren().forEach(node -> {
            if (node instanceof SongManager songManager) {
                songManager.updateFont();
            }
        });
    }

    public StackPane getRootStackPane() {
        return rootStackPane;
    }

    private class SongManager extends StackPane {
        private final BorderPane rootBorderPane = new BorderPane();
        private final HBox bodyRoot = new HBox(20);
        private final ScrollablePane<HBox> body = new ScrollablePane<>(bodyRoot, Region.USE_PREF_SIZE);
        private final String id;
        private Node head;
        private final Text title;
        private final Text user;

        private boolean controllable = true;
        private final HBox control = new HBox();
        public SongManager(Song song, int index) {
            this.id = song.getId();
            this.title = new Text(song.getTitle());
            this.user = new Text(song.getUser() != null ? "[%s 点歌]".formatted(song.getUser()) : "");
            bodyRoot.setAlignment(Pos.CENTER_LEFT);
            bodyRoot.getChildren().addAll(title, user);
            rootBorderPane.setMinHeight(40);
            rootBorderPane.setRight(body);
            this.getChildren().add(rootBorderPane);

            this.control.setAlignment(Pos.CENTER);
            this.control.setOpacity(0);
            this.setOnMouseEntered(event -> {
                if (this.controllable) {
                    FadeTransition fadeTransition = new FadeTransition(Duration.millis(200), control);
                    fadeTransition.setFromValue(this.getChildren().contains(control) ? control.getOpacity() : 0);
                    fadeTransition.setToValue(1);
                    fadeTransition.setInterpolator(Interpolator.LINEAR);
                    fadeTransition.play();
                    if (!this.getChildren().contains(control)) {
                        this.getChildren().add(control);
                    }
                }
            });
            this.setOnMouseExited(event -> {
                if (this.controllable) {
                    hideControl(control, this);
                }
            });
            if (index == 0) {
                QMButton skip = new QMButton("", null, false);
                skip.setGraphic(new FontIcon("fas-step-forward"));
                skip.setOnAction(event -> MusicPlayerScene.this.musicHandler.playNext());
                control.getChildren().add(skip);
            } else {
                if (index != 1) {
                    QMButton top = new QMButton("", null, false);
                    top.setGraphic(new FontIcon("fas-angle-double-up"));
                    top.setOnAction(event -> MusicPlayerScene.this.musicHandler.moveSong(song, 1));
                    control.getChildren().add(top);
                }
                QMButton play = new QMButton("", null, false);
                play.setGraphic(new FontIcon("fas-play"));
                play.setOnAction(event -> {
                    MusicPlayerScene.this.musicHandler.endFirst(true);
                    MusicPlayerScene.this.musicHandler.play(song);
                });
                control.getChildren().add(play);
                QMButton remove = new QMButton("", null, false);
                remove.setGraphic(new FontIcon("fas-trash"));
                remove.setOnAction(event -> MusicPlayerScene.this.musicHandler.removeSong(song));
                control.getChildren().add(remove);
            }
            updateFont();
            updateHead(index);
        }

        public void updateFont() {
            String font = Settings.getMusicServiceSettings().textFont;
            this.title.setFont(new Font(font.isEmpty() ? Settings.DEFAULT_FONT.getFamily() : font, 20));
            this.user.setFont(new Font(font.isEmpty() ? Settings.DEFAULT_FONT.getFamily() : font, 20));
            if (this.head instanceof Text text && !(this.head instanceof FontIcon)) {
                text.setFont(new Font(font.isEmpty() ? Settings.DEFAULT_FONT.getFamily() : font, 20));
            }
        }

        public void updateHead(int index) {
            if (index == 0) {
                head = new FontIcon("fas-play:20");
            } else {
                Text head1 = new Text(String.valueOf(index));
                head = head1;
                String font = Settings.getMusicServiceSettings().textFont;
                head1.setFont(new Font(font.isEmpty() ? Settings.DEFAULT_FONT.getFamily() : font, 20));
            }
            rootBorderPane.setLeft(head);
            BorderPane.setAlignment(head, Pos.CENTER);
            BorderPane.setMargin(head, new Insets(0, 0, 0, 10));
            updateColor();
            body.setDisplayWidth(MusicPlayerScene.this.getWidth() - head.getBoundsInLocal().getWidth() - 45);
            BorderPane.setMargin(body, new Insets(0, 10, 0, 0));
            BorderPane.setAlignment(body, Pos.CENTER_LEFT);
        }

        public void updateColor() {
            Color paint = Color.valueOf(Settings.getMusicServiceSettings().textColor);
            Color background = Color.valueOf(Settings.getMusicServiceSettings().backgroundColor);
            background = new Color(background.getRed(), background.getGreen(), background.getBlue(), 0.8 * background.getOpacity());
            if (this.head instanceof FontIcon fontIcon) {
                fontIcon.setIconColor(paint);
            } else if (this.head instanceof Text text) {
                text.setFill(paint);
            }
            title.setFill(paint);
            user.setFill(paint);

            for (Node node : control.getChildren()) {
                if (node instanceof QMButton button && button.getGraphic() instanceof FontIcon fontIcon) {
                    fontIcon.setIconColor(paint);
                }
            }
            control.setBackground(Background.fill(background));
        }

        public void setControllable(boolean controllable) {
            this.controllable = controllable;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SongManager)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            return Objects.equals(this.id, ((SongManager) obj).id);
        }
    }
}
