package top.pigest.queuemanagerdemo.util;

import javafx.scene.Node;

import java.util.List;
import java.util.function.Supplier;

public abstract class ListPagedContainer<T> extends PagedContainerFactory {
    private final List<T> items;
    private final int maxPerPage;
    private final boolean reversed;
    private final Supplier<Node> emptySupplier;
    public ListPagedContainer(String id, List<T> items, int maxPerPage, boolean reversed) {
        this(id,  items, maxPerPage, reversed, () -> null);
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

    public abstract Node getNode(T item);
}
