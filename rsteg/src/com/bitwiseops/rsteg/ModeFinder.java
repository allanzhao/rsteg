package com.bitwiseops.rsteg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the mode (most common element) of a succession of items.
 */
public class ModeFinder<T> {
    private final Map<T, Integer> indices = new HashMap<T, Integer>();
    private final Map<T, Integer> counts = new HashMap<T, Integer>();
    private final List<T> items = new ArrayList<T>();
    
    public void add(T item) {
        if(indices.containsKey(item)) {
            int index = indices.get(item);
            int count = counts.get(item);
            if(index > 0 && counts.get(items.get(index - 1)) == count) {
                T itemAhead = items.get(index - 1);
                indices.put(itemAhead, index);
                indices.put(item, index - 1);
                items.set(index, itemAhead);
                items.set(index - 1, item);
            }
            counts.put(item, count + 1);
        } else {
            indices.put(item, items.size());
            counts.put(item, 1);
            items.add(item);
        }
    }
    
    public T getMode() {
        return items.get(0);
    }
    
    public boolean hasMode() {
        return !items.isEmpty();
    }
    
    public void reset() {
        indices.clear();
        counts.clear();
        items.clear();
    }
}
