package top.pigest.queuemanagerdemo.util;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class DynamicListPagedContainer<T> extends ListPagedContainer<T> {
    private int currentItemPage = 0;
    private boolean isEnd;

    public DynamicListPagedContainer(String id, int maxPerPage) {
        this(id, new ArrayList<>(), maxPerPage, false);
    }

    public DynamicListPagedContainer(String id, List<T> items, int maxPerPage, boolean reversed) {
        this(id, items, maxPerPage, reversed, LOADING_SUPPLIER);
    }

    public DynamicListPagedContainer(String id, List<T> items, int maxPerPage, boolean reversed, Supplier<Node> emptySupplier) {
        super(id, items, maxPerPage, reversed, emptySupplier);
    }

    @Override
    public void update() {
        left.disable(!pages.hasPrev());
        right.disable(isEnd && !pages.hasNext());
        text.setText("第 %s 页".formatted(this.pages.getIndex() + 1));
    }

    public int getCurrentItemPage() {
        return currentItemPage;
    }

    public void prepareReadNextPage() {
        if (isEnd) {
            return;
        }
        if (currentItemPage >= pages.size() - 2) {
            readNextPage();
        }
    }

    public void readNextPage() {
        currentItemPage++;
        CompletableFuture.supplyAsync(() -> this.getNextItems(currentItemPage))
                .thenAccept(l -> {
                    if (l.isEmpty()) {
                        isEnd = true;
                        this.setEmptySupplier(DEFAULT_EMPTY_SUPPLIER);
                        this.refresh();
                        return;
                    }
                    Platform.runLater(() -> {
                        this.getItems().addAll(l);
                        this.refresh();
                    });
                });
    }

    public boolean isEnd() {
        return isEnd;
    }

    @Override
    public BorderPane build() {
        BorderPane borderPane = super.build();
        prepareReadNextPage();
        return borderPane;
    }

    @Override
    public void rightAction(ActionEvent event) {
        prepareReadNextPage();
        super.rightAction(event);
    }

    public abstract List<T> getNextItems(int page);
}
