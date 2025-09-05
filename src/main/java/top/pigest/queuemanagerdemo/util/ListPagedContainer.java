package top.pigest.queuemanagerdemo.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import top.pigest.queuemanagerdemo.Settings;

import java.util.List;
import java.util.function.Supplier;

public abstract class ListPagedContainer<T> extends PagedContainerFactory {
    public static final Supplier<Node> DEFAULT_EMPTY_SUPPLIER = () -> {
        StackPane pane = new StackPane();
        Text text = new Text("- 这里什么都没有 -");
        text.setFont(Settings.DEFAULT_FONT);
        text.setFill(Color.GRAY);
        pane.getChildren().add(text);
        pane.setPadding(new Insets(30));
        return pane;
    };
    public static final Supplier<Node> LOADING_SUPPLIER = () -> {
        StackPane pane = new StackPane();
        Text text = new Text("--- 加载中 ---");
        text.setFont(Settings.DEFAULT_FONT);
        text.setFill(Color.GRAY);
        pane.getChildren().add(text);
        pane.setPadding(new Insets(30));
        return pane;
    };
    private final List<T> items;
    private final int maxPerPage;
    private final boolean reversed;
    private Supplier<Node> emptySupplier;
    public ListPagedContainer(String id, List<T> items, int maxPerPage, boolean reversed) {
        this(id,  items, maxPerPage, reversed, DEFAULT_EMPTY_SUPPLIER);
    }


    public ListPagedContainer(String id, List<T> items, int maxPerPage, boolean reversed, Supplier<Node> emptySupplier) {
        super(id);
        this.items = items;
        this.maxPerPage = Math.max(maxPerPage, 1);
        this.reversed = reversed;
        this.emptySupplier = emptySupplier;
        this.construct();
    }

    private void construct() {
        if (this.getItems().isEmpty()) {
            this.addNode(emptySupplier.get());
        }
        int f = 0;
        for (T item : reversed ? this.getItems().reversed() : this.getItems()) {
            if (f >= maxPerPage) {
                this.addPage();
                f = 0;
            }
            this.addNode(this.getNode(item));
            f++;
        }
    }

    public void refresh() {
        int index = this.pages.getIndex();
        this.pages.clear();
        this.pages.setIndex(0);
        this.addPage();
        this.construct();
        this.pages.setIndex(Math.min(index, this.pages.size() - 1));
        this.current.setCenter(this.pages.current());
        this.update();
    }

    protected List<T> getItems() {
        return items;
    }

    public void setEmptySupplier(Supplier<Node> emptySupplier) {
        this.emptySupplier = emptySupplier;
    }

    public abstract Node getNode(T item);
}
