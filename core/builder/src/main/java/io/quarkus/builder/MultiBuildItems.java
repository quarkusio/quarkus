package io.quarkus.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A facility for storing and retrieving {@link MultiBuildItem}s by {@link ItemId}.
 */
class MultiBuildItems {
    private final Map<ItemId, List<MultiBuildItem>[]> multis;

    /**
     * A map from {@link ItemId} to an array of ordinals of the {@link BuildStep}s producing the given
     * {@link ItemId}. The ordinals array is sorted in the ascending order.
     */
    private final Map<ItemId, int[]> producingOrdinals;

    @SuppressWarnings("unchecked")
    MultiBuildItems(Map<ItemId, int[]> producingOrdinals) {
        this.producingOrdinals = producingOrdinals;
        final Map<ItemId, List<MultiBuildItem>[]> ms = new HashMap<ItemId, List<MultiBuildItem>[]>(producingOrdinals.size());
        for (Entry<ItemId, int[]> en : producingOrdinals.entrySet()) {
            ms.put(en.getKey(), new List[en.getValue().length + 1]);
        }
        this.multis = ms;
    }

    /**
     * Store the given {@code value} produced by the {@link BuildStep} having the given {@code ordinal} under the given
     * {@link ItemId}.
     *
     * @param ordinal see {@link StepInfo#getOrdinal()}
     * @param id the {@link ItemId} to store the given {@code value} under
     * @param value the {@link MultiBuildItem} to store.
     */
    void put(int ordinal, ItemId id, MultiBuildItem value) {
        final List<MultiBuildItem>[] list = multis.get(id);
        int pos = Arrays.binarySearch(producingOrdinals.get(id), ordinal);
        synchronized (list) {
            List<MultiBuildItem> entry = list[pos + 1];
            if (entry == null) {
                entry = new ArrayList<MultiBuildItem>();
                list[pos + 1] = entry;
            }
            entry.add(value);
        }
    }

    /**
     * Store the given initial {@link MultiBuildItem} under the given {@link ItemId}. Initial values are the ones put
     * into an execution before it has started.
     *
     * @param id the {@link ItemId} to store the given {@code value} under
     * @param value the {@link MultiBuildItem} to store.
     */
    void putInitial(ItemId id, MultiBuildItem value) {
        @SuppressWarnings("unchecked")
        final List<MultiBuildItem>[] list = multis.computeIfAbsent(id, x -> new ArrayList[1]);
        synchronized (list) {
            List<MultiBuildItem> entry = list[0];
            if (entry == null) {
                entry = new ArrayList<MultiBuildItem>();
                list[0] = entry;
            }
            entry.add(value);
        }
    }

    /**
     * Returns a {@link List} of {@link MultiBuildItem}s for the given {@link ItemId}.
     * <p>
     * The ordering of the items in the returned {@link List} is guaranteed to be reproducible (same program inputs
     * should give the same ordering).
     *
     * @param <T> the type of the elements of the returned {@link List}
     * @param id the {@link ItemId} whose {@link MultiBuildItem}s should be retrieved
     * @return a {@link List} of {@link MultiBuildItem}s
     */
    @SuppressWarnings("unchecked")
    <T extends MultiBuildItem> List<T> get(ItemId id) {
        final List<MultiBuildItem>[] list = multis.get(id);
        if (list == null) {
            return List.of();
        }
        final ArrayList<MultiBuildItem> result = new ArrayList<MultiBuildItem>();
        synchronized (list) {
            for (List<MultiBuildItem> entry : list) {
                if (entry != null) {
                    result.addAll(entry);
                }
            }
        }
        return (List<T>) result;
    }

    /**
     * Calls {@link AutoCloseable#close()} on all {@link AutoCloseable} items stored in this {@link MultiBuildItems}.
     */
    void closeAll() {
        for (List<MultiBuildItem>[] list : multis.values()) {
            for (List<MultiBuildItem> entry : list) {
                synchronized (list) {
                    for (MultiBuildItem item : entry) {
                        if (item instanceof AutoCloseable) {
                            try {
                                ((AutoCloseable) item).close();
                            } catch (Exception e) {
                                Messages.msg.closeFailed(item, e);
                            }
                        }
                    }
                }
            }
        }
    }

}
