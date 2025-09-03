package top.pigest.queuemanagerdemo.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import top.pigest.queuemanagerdemo.Settings;
import top.pigest.queuemanagerdemo.control.QMButton;
import top.pigest.queuemanagerdemo.window.main.MultiMenuProvider;

public class PagedContainerFactory {
    protected final BorderPane current;
    protected final IndexedArrayList<VBox> pages = new IndexedArrayList<>();
    protected final QMButton left = new QMButton("", null, false);
    protected final QMButton right = new QMButton("", null, false);
    protected final Label text = new Label();

    public PagedContainerFactory(String id) {
        this.current = new BorderPane();
        this.current.setId(id);
        this.addPage();
    }

    public void update() {
        left.disable(!pages.hasPrev());
        right.disable(!pages.hasNext());
        text.setText("第 %s / %s 页".formatted(this.pages.getIndex() + 1, this.pages.size()));
    }

    public PagedContainerFactory addPage() {
        return addPage(true);
    }

    public PagedContainerFactory addPage(boolean next) {
        VBox vBox = new VBox();
        BorderPane.setMargin(vBox, new Insets(0, 40, 0, 40));
        pages.add(vBox);
        if (next) {
            pages.next();
        }
        return this;
    }

    public PagedContainerFactory addControl(String left, Node right) {
        return this.addControl(left, right, -1, -1);
    }

    public PagedContainerFactory addControl(String left, Node right, int prefWidth, int prefHeight) {
        pages.current().getChildren().add(MultiMenuProvider.createLRBorderPane(left, prefWidth, prefHeight, right));
        return this;
    }

    public PagedContainerFactory addNode(Node content) {
        pages.current().getChildren().add(content);
        return this;
    }

    public PagedContainerFactory addNode(int pageIndex, Node content) {
        pages.get(pageIndex).getChildren().add(content);
        return this;
    }

    public PagedContainerFactory addNode(int pageIndex, int nodeIndex, Node content) {
        pages.get(pageIndex).getChildren().add(nodeIndex, content);
        return this;
    }

    public PagedContainerFactory removeNode(int pageIndex, int nodeIndex) {
        if (pageIndex < pages.size()) {
            pages.get(pageIndex).getChildren().remove(nodeIndex);
        }
        return this;
    }

    public BorderPane build() {
        if (this.pages.isEmpty()) {
            throw new RuntimeException("No pages found");
        }
        this.pages.setIndex(0);
        HBox lr = new HBox();
        lr.setAlignment(Pos.CENTER);
        text.setAlignment(Pos.CENTER);
        text.setFont(Settings.DEFAULT_FONT);
        left.setGraphic(Utils.make(new FontIcon("fas-arrow-left"), fontIcon -> fontIcon.setIconSize(25)));
        right.setGraphic(Utils.make(new FontIcon("fas-arrow-right"), fontIcon -> fontIcon.setIconSize(25)));
        text.setPrefWidth(150);
        left.setPrefHeight(30);
        right.setPrefHeight(30);
        text.setPrefHeight(30);
        left.setAlignment(Pos.CENTER);
        right.setAlignment(Pos.CENTER);
        update();
        left.setOnAction(event -> {
            current.setCenter(pages.prev());
            update();
        });
        right.setOnAction(event -> {
            current.setCenter(pages.next());
            update();
        });
        lr.getChildren().addAll(left, text, right);
        this.current.setCenter(this.pages.current());
        this.current.setBottom(lr);
        BorderPane.setMargin(this.current, new Insets(10, 0, 5, 0));
        return this.current;
    }
}
