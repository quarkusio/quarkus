package org.jboss.resteasy.reactive.common.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class MultivaluedTreeMap<K, V> implements QuarkusMultivaluedMap<K, V>, Serializable {
    private static final long serialVersionUID = -5819694356897323354L;

    private final Map<K, List<V>> map;

    public MultivaluedTreeMap() {
        map = new TreeMap<>();
    }

    /**
     * Used to create a CaseInsensitiveMap.
     *
     * @param keyComparator key comparator
     */

    public MultivaluedTreeMap(final Comparator<K> keyComparator) {
        map = new TreeMap<K, List<V>>(keyComparator);
    }

    public MultivaluedTreeMap(final Map<K, V> map) {
        this();
        for (K key : map.keySet()) {
            add(key, map.get(key));
        }
    }

    public void add(K key, V value) {
        List<V> list = getOrCreate(key);
        list.add(value);
    }

    public V getFirst(K key) {
        List<V> list = get(key);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    public void putSingle(K key, V value) {
        List<V> list = getOrCreate(key);
        list.clear();
        list.add(value);
    }

    private List<V> getOrCreate(K key) {
        List<V> list = this.get(key);
        if (list == null) {
            list = createValueList(key);
            // do not call the version that copies
            map.put(key, list);
        }
        return list;
    }

    private List<V> createValueList(K key) {
        return new ArrayList<V>(1);
    }

    private List<V> createValueList(List<V> values) {
        return new ArrayList<V>(values);
    }

    public MultivaluedTreeMap<K, V> clone() {
        return clone(this);
    }

    public static <K, V> MultivaluedTreeMap<K, V> clone(MultivaluedMap<K, V> src) {
        MultivaluedTreeMap<K, V> clone = new MultivaluedTreeMap<K, V>();
        copy(src, clone);
        return clone;
    }

    public static <K, V> void copy(MultivaluedMap<K, V> src, MultivaluedMap<K, V> dest) {
        for (K key : src.keySet()) {
            List<V> value = src.get(key);
            List<V> newValue = new ArrayList<V>(value);
            dest.put(key, newValue);
        }
    }

    public static <K, V> void addAll(MultivaluedMap<K, V> src, MultivaluedMap<K, V> dest) {
        for (K key : src.keySet()) {
            List<V> srcList = src.get(key);
            List<V> destList = dest.get(key);
            if (destList == null) {
                destList = new ArrayList<V>(srcList.size());
                dest.put(key, destList);
            }
            destList.addAll(srcList);
        }
    }

    @Override
    public String toString() {
        return "[" + toString(this, ",") + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public static String toString(MultivaluedMap<?, ?> map, String delimiter) {
        StringBuilder result = new StringBuilder();
        MultivaluedMap<?, ?> params = map;
        String delim = ""; //$NON-NLS-1$
        for (Object name : params.keySet()) {
            for (Object value : params.get(name)) {
                result.append(delim);
                if (name == null) {
                    result.append("null"); //$NON-NLS-1$
                } else {
                    result.append(name.toString());
                }
                if (value != null) {
                    result.append('=');
                    result.append(value.toString());
                }
                delim = delimiter;
            }
        }
        return result.toString();
    }

    public void clear() {
        map.clear();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Set<Entry<K, List<V>>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public List<V> get(Object key) {
        return map.get(key);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public List<V> put(K key, List<V> value) {
        if (value != null) {
            // do not use mutable external data storage internally
            value = createValueList(value);
        }
        return map.put(key, value);
    }

    public void putAll(Map<? extends K, ? extends List<V>> t) {
        for (Entry<? extends K, ? extends List<V>> entry : t.entrySet()) {
            // call the version that copies
            put(entry.getKey(), entry.getValue());
        }
    }

    public List<V> remove(Object key) {
        return map.remove(key);
    }

    public int size() {
        return map.size();
    }

    public Collection<List<V>> values() {
        return map.values();
    }

    @SuppressWarnings(value = "unchecked")
    @Override
    public void addAll(K key, V... newValues) {
        for (V value : newValues) {
            add(key, value);
        }
    }

    @Override
    public void addAll(K key, List<V> valueList) {
        for (V value : valueList) {
            add(key, value);
        }
    }

    @Override
    public void addFirst(K key, V value) {
        List<V> list = get(key);
        if (list == null) {
            add(key, value);
            return;
        } else {
            list.add(0, value);
        }
    }

    @Override
    public boolean equalsIgnoreValueOrder(MultivaluedMap<K, V> omap) {
        if (this == omap) {
            return true;
        }
        if (!keySet().equals(omap.keySet())) {
            return false;
        }
        for (Map.Entry<K, List<V>> e : entrySet()) {
            List<V> olist = omap.get(e.getKey());
            if (e.getValue().size() != olist.size()) {
                return false;
            }
            for (V v : e.getValue()) {
                if (!olist.contains(v)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super List<V>> action) {
        map.forEach(action);
    }
}
