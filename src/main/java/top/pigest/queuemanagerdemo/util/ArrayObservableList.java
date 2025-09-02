package top.pigest.queuemanagerdemo.util;

import javafx.collections.ModifiableObservableListBase;

import java.util.ArrayList;
import java.util.List;

public class ArrayObservableList<E> extends ModifiableObservableListBase<E> {
    private final List<E> list = new ArrayList<>();

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    protected void doAdd(int index, E element) {
        list.add(index, element);
    }

    @Override
    protected E doSet(int index, E element) {
        return list.set(index, element);
    }

    @Override
    protected E doRemove(int index) {
        return list.remove(index);
    }
}
