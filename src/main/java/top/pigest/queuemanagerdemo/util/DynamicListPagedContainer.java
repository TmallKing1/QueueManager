package top.pigest.queuemanagerdemo.util;

import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class DynamicListPagedContainer<T> extends ListPagedContainer<T> {
    private int currentItemPage = 0;
    private boolean isEnd;

    public DynamicListPagedContainer(String id, int maxPerPage) {
        this(id, new ArrayList<>(), maxPerPage, false);
    }

    public DynamicListPagedContainer(String id, List<T> items, int maxPerPage, boolean reversed) {
        this(id, items, maxPerPage, reversed, () -> null);
    }

    public DynamicListPagedContainer(String id, List<T> items, int maxPerPage, boolean reversed, Supplier<Node> emptySupplier) {
        super(id, items, maxPerPage, reversed, emptySupplier);
    }

    @Override
    public void update() {
        left.disable(!pages.hasPrev());
        right.disable(isEnd);
        text.setText("第 %s 页".formatted(this.pages.getIndex() + 1));
    }

    public int getCurrentItemPage() {
        return currentItemPage;
    }

    public void readNextPage() {
        currentItemPage++;
        this.getItems().addAll(this.getNextItems());
        this.refresh();
    }

    public abstract List<T> getNextItems();
}
