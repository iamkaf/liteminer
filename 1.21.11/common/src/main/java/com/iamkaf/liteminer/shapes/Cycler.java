package com.iamkaf.liteminer.shapes;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic class for cycling through a fixed list of items in both forward and backward directions.
 *
 * @param <T> the type of items managed by this class
 */
public class Cycler<T> {
    private final List<T> items;
    private int currentIndex;

    /**
     * Creates a new Cycler with a given list of items.
     *
     * @param initialItems the list of items to manage
     * @throws IllegalArgumentException if the provided list is null or empty
     */
    public Cycler(List<T> initialItems) {
        if (initialItems == null || initialItems.isEmpty()) {
            throw new IllegalArgumentException("Item list cannot be null or empty");
        }
        items = new ArrayList<>(initialItems);
        currentIndex = 0;
    }

    /**
     * Retrieves the current item in the list.
     *
     * @return the current item
     */
    public T getCurrentItem() {
        return items.get(currentIndex);
    }

    /**
     * Advances to the next item in the list and retrieves it. The cycling wraps
     * around to the first item after the last one.
     *
     * @return the next item in the list
     */
    public T nextItem() {
        currentIndex = (currentIndex + 1) % items.size();
        return getCurrentItem();
    }

    /**
     * Moves to the previous item in the list and retrieves it. The cycling wraps
     * around to the last item if the current item is the first one.
     *
     * @return the previous item in the list
     */
    public T previousItem() {
        currentIndex = (currentIndex - 1 + items.size()) % items.size();
        return getCurrentItem();
    }

    /**
     * Retrieves the current item index.
     *
     * @return the current item index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }
}
