package org.jboss.resteasy.reactive.common.util;

import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unmodifiable implementation of {@link jakarta.ws.rs.core.MultivaluedMap} interface
 *
 * @author <a href="mailto:mstefank@redhat.conm">Martin Stefanko</a>
 * @version $Revision: 1 $
 */
public class UnmodifiableMultivaluedMap<K, V> implements MultivaluedMap<K, V> {

    private final MultivaluedMap<K, V> delegate;

    private volatile Set<java.util.Map.Entry<K, List<V>>> entrySet;

    public UnmodifiableMultivaluedMap(final MultivaluedMap<K, V> delegate) {
        this(delegate, true);
    }

    public UnmodifiableMultivaluedMap(final MultivaluedMap<K, V> delegate, final boolean eagerlyInitializeEntrySet) {
        this.delegate = delegate;
        if (eagerlyInitializeEntrySet) {
            this.entrySet = buildEntrySet();
        }
    }

    private Set<java.util.Map.Entry<K, List<V>>> buildEntrySet() {
        Set<java.util.Map.Entry<K, List<V>>> entrySetDelegator = delegate.entrySet();
        Set<java.util.Map.Entry<K, List<V>>> entrySetToReturn = new HashSet<>();
        for (final java.util.Map.Entry<K, List<V>> entry : entrySetDelegator) {
            entrySetToReturn.add(new Entry<K, List<V>>() {

                @Override
                public K getKey() {
                    return entry.getKey();
                }

                @Override
                public List<V> getValue() {
                    List<V> value = entry.getValue();
                    return value == null ? null : Collections.unmodifiableList(value);
                }

                @Override
                public List<V> setValue(List<V> value) {
                    throw new UnsupportedOperationException();
                }
            });
        }
        return Collections.unmodifiableSet(entrySetToReturn);
    }

    @Override
    public void putSingle(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V getFirst(K key) {
        return delegate.getFirst(key);
    }

    @SuppressWarnings(value = "unchecked")
    @Override
    public void addAll(K k, V... vs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(K k, List<V> list) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFirst(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equalsIgnoreValueOrder(MultivaluedMap<K, V> omap) {
        return delegate.equalsIgnoreValueOrder(omap);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        if (this.entrySet == null) {
            this.entrySet = buildEntrySet();
        }
        return this.entrySet;
    }

    @Override
    public List<V> get(Object key) {
        List<V> list = delegate.get(key);
        return list == null ? null : Collections.unmodifiableList(list);
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    @Override
    public List<V> put(K key, List<V> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends List<V>> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<V> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<List<V>> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    @Override
    public boolean containsKey(Object o) {
        return delegate.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return delegate.containsValue(o);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
