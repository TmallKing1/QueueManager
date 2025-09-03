package top.pigest.queuemanagerdemo.window.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import top.pigest.queuemanagerdemo.QueueManager;
import top.pigest.queuemanagerdemo.util.Utils;
import top.pigest.queuemanagerdemo.control.QMButton;

import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public abstract class MultiMenuProvider<T extends Node> extends BorderPane implements ChildPage {
    public static final BorderStroke DEFAULT_BORDER_STROKE =
            new BorderStroke(Color.TRANSPARENT, Color.TRANSPARENT, Paint.valueOf("0x22222233"), Color.TRANSPARENT,
                    BorderStrokeStyle.SOLID, null, null, null,
                    CornerRadii.EMPTY, new BorderWidths(1), Insets.EMPTY);

    private T innerContainer;
    private Pane parentContainer;

    public MultiMenuProvider() {
        super();
    }

    public abstract LinkedHashMap<String, Supplier<T>> getMenus();

    public abstract int getMenuIndex(T innerContainer);

    public int getCurrentMenuIndex() {
        return this.getMenuIndex(innerContainer);
    }

    public List<QMButton> getMenuButtons() {
        Map<String, Supplier<T>> menus = getMenus();
        List<QMButton> menuButtons = new ArrayList<>();
        for (Map.Entry<String, Supplier<T>> menu : menus.entrySet()) {
            QMButton e = new QMButton(menu.getKey(), null, false);
            e.setRipplerFill(Paint.valueOf("BLACK"));
            e.setOnAction(event -> {
                this.setInnerContainer(menu.getValue().get());
                QueueManager.INSTANCE.getMainScene().updateMenuButtonTextFill();
            });
            menuButtons.add(e);
        }
        return menuButtons;
    }

    public void setInnerContainer(T innerContainer) {
        this.innerContainer = innerContainer;
        this.setCenter(innerContainer);
    }

    public T getInnerContainer() {
        return innerContainer;
    }

    public MultiMenuProvider<T> withParentContainer(Pane parentContainer) {
        this.parentContainer = parentContainer;
        return this;
    }

    @Override
    public Pane getParentContainer() {
        return parentContainer;
    }

    public static BorderPane createLRBorderPane(String title, int prefWidth, int prefHeight, Node innerContainer) {
        BorderPane borderPane = new BorderPane();
        if (prefWidth > 0) {
            borderPane.setPrefWidth(prefWidth);
        }
        if (prefHeight > 0) {
            borderPane.setPrefHeight(prefHeight);
        }
        Label label = Utils.createLabel(title, -1, Pos.CENTER_LEFT);
        borderPane.setLeft(label);
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        borderPane.setRight(innerContainer);
        BorderPane.setAlignment(innerContainer, Pos.CENTER_RIGHT);
        borderPane.setBorder(new Border(DEFAULT_BORDER_STROKE));
        borderPane.setPadding(new Insets(10, 10, 10, 10));
        return borderPane;
    }
}
