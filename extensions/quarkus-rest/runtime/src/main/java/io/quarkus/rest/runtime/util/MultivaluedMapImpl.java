package io.quarkus.rest.runtime.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
@SuppressWarnings("serial")
public class MultivaluedMapImpl<K, V> extends HashMap<K, List<V>> implements MultivaluedMap<K, V> {
    public void putSingle(K key, V value) {
        List<V> list = new ArrayList<V>();
        list.add(value);
        put(key, list);
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

    public final void add(K key, V value) {
        getList(key).add(value);
    }

    public final void addMultiple(K key, Collection<V> values) {
        getList(key).addAll(values);
    }

    public V getFirst(K key) {
        List<V> list = get(key);
        return list == null ? null : list.get(0);
    }

    public final List<V> getList(K key) {
        List<V> list = get(key);
        if (list == null)
            put(key, list = new ArrayList<V>());
        return list;
    }

    public void addAll(MultivaluedMapImpl<K, V> other) {
        for (Map.Entry<K, List<V>> entry : other.entrySet()) {
            getList(entry.getKey()).addAll(entry.getValue());
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
}
