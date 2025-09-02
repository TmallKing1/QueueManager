package top.pigest.queuemanagerdemo.util;

import java.util.ArrayList;

public class IndexedArrayList<T> extends ArrayList<T> {
    private int index = 0;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index){
        this.index = index;
    }

    public boolean hasPrev() {
        return index > 0;
    }

    public T getPrev() {
        if (this.hasPrev()) {
            return this.get(this.index - 1);
        }
        return null;
    }

    public T prev() {
        if (this.hasPrev()) {
            return this.get(--index);
        }
        return this.current();
    }

    public boolean hasNext() {
        return index < this.size() - 1;
    }

    public T getNext() {
        if (this.hasNext()) {
            return this.get(index + 1);
        }
        return this.current();
    }

    public T next() {
        if (this.hasNext()) {
            return this.get(++index);
        }
        return this.current();
    }

    public T current() {
        return this.get(index);
    }
}
