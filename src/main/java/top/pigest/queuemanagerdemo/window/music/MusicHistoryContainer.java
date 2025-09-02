package top.pigest.queuemanagerdemo.window.music;

import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.control.ScrollableText;
import top.pigest.queuemanagerdemo.music.MusicHandler;
import top.pigest.queuemanagerdemo.music.Song;
import top.pigest.queuemanagerdemo.util.ListPagedContainer;
import top.pigest.queuemanagerdemo.window.main.MultiMenuProvider;

public class MusicHistoryContainer extends ListPagedContainer<Song> {
    public MusicHistoryContainer(String id, ObservableList<Song> songs) {
        super(id, songs, 6, MusicHistoryContainer::createSongPreview, true, () -> {
            StackPane pane = new StackPane();
            Text text = new Text("- 这里什么都没有 -");
            text.setFont(Settings.DEFAULT_FONT);
            text.setFill(Color.GRAY);
            pane.getChildren().add(text);
            pane.setPadding(new Insets(30));
            return pane;
        });
        songs.addListener((ListChangeListener<Song>) c -> Platform.runLater(this::refresh));
    }

    private static BorderPane createSongPreview(Song song) {
        BorderPane borderPane = new BorderPane();
        ImageView imageView = new ImageView();
        imageView.setImage(song.getCover(imageView::setImage));
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        JFXDepthManager.setDepth(imageView, 2);
        borderPane.setLeft(imageView);
        BorderPane.setAlignment(imageView, Pos.CENTER);
        BorderPane.setMargin(imageView, new Insets(0, 15, 0, 0));

        VBox vBox = new VBox(5);
        ScrollableText name = new ScrollableText(song.getTitle(), 480);
        ScrollableText artist = new ScrollableText(song.getArtist(), 480);
        artist.getText().setFont(Font.font(Settings.DEFAULT_FONT.getFamily(), 16));
        artist.resetAnimation();
        vBox.getChildren().addAll(name, artist);
        borderPane.setCenter(vBox);
        BorderPane.setAlignment(vBox, Pos.CENTER_LEFT);

        HBox hBox = new HBox();
        QMButton play = getPlay(song);
        QMButton delete = new QMButton("", null, false);
        FontIcon trashIcon = new FontIcon("far-trash-alt:40");
        delete.setGraphic(trashIcon);
        delete.setOnAction(event -> MusicHandler.INSTANCE.getHistory().remove(song));
        hBox.getChildren().addAll(play, delete);
        hBox.setAlignment(Pos.CENTER_RIGHT);
        borderPane.setRight(hBox);
        BorderPane.setAlignment(hBox, Pos.CENTER_RIGHT);

        borderPane.setBorder(new Border(MultiMenuProvider.DEFAULT_BORDER_STROKE));
        borderPane.setPadding(new Insets(10, 10, 10, 10));
        return borderPane;
    }

    private static QMButton getPlay(Song song) {
        QMButton play = new QMButton("", null, false);
        FontIcon playIcon = new FontIcon("far-play-circle:40");
        FontIcon pauseIcon = new FontIcon("far-pause-circle:40");
        if (song.equals(MusicHandler.INSTANCE.getCurrentSong())) {
            playPauseButton(song, play, playIcon, pauseIcon);
        } else {
            play.setGraphic(playIcon);
            play.setOnAction(event -> {
                FontIcon requestingIcon = new FontIcon("fas-bullseye:40");
                play.setGraphic(requestingIcon);
                play.setOnAction(event1 -> {});
                song.setFormat(null);
                MusicHandler.INSTANCE.play(song);
            });
        }
        return play;
    }

    private static void playPauseButton(Song song, QMButton play, FontIcon playIcon, FontIcon pauseIcon) {
        if (MusicHandler.INSTANCE.isPaused()) {
            playButton(song, play, playIcon, pauseIcon);
        } else {
            pauseButton(song, play, playIcon, pauseIcon);
        }
    }

    private static void pauseButton(Song song, QMButton play, FontIcon playIcon, FontIcon pauseIcon) {
        play.setGraphic(pauseIcon);
        play.setOnAction(event -> {
            MusicHandler.INSTANCE.pause();
            Platform.runLater(() -> playButton(song, play, playIcon, pauseIcon));
        });
    }

    private static void playButton(Song song, QMButton play, FontIcon playIcon, FontIcon pauseIcon) {
        play.setGraphic(playIcon);
        play.setOnAction(event -> {
            MusicHandler.INSTANCE.play(song);
            Platform.runLater(() -> pauseButton(song, play, playIcon, pauseIcon));
        });
    }
}
