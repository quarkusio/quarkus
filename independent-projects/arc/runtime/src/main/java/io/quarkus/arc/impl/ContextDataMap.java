package io.quarkus.arc.impl;

import io.quarkus.arc.ArcInvocationContext;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This Map implementation is specifically optimised to implement
 * {@link InvocationContext#getContextData()}.
 * We try to leverage the fact that in most use cases, while there is a high
 * chance for integration points to need to read the content of this map,
 * write operations are less likely. So we only allocate an actual underlying
 * HashMap on a write operation.
 * In addition, an {@link ArcInvocationContext} always allows to return the set of
 * interceptor bindings via {@link ArcInvocationContext#KEY_INTERCEPTOR_BINDINGS};
 * this is implemented while avoiding a write operation on the underlying Map.
 * While one goal is efficiency, another goal is safety: while the CDI specification
 * allows any interceptor to make changes to this Map, we specifically disallow
 * 3rd party code to make changes to the interceptor bindings of the platform.
 */
final class ContextDataMap implements Map<String, Object> {

    private final Set<Annotation> interceptorBindings;
    private HashMap<String, Object> delegate; //important to lazily initialize this

    ContextDataMap(Set<Annotation> interceptorBindings) {
        this.interceptorBindings = Objects.requireNonNull(interceptorBindings);
    }

    private Map<String, Object> getDelegateForRead() {
        if (delegate == null) {
            return Collections.emptyMap();
        } else {
            return delegate;
        }
    }

    private Map<String, Object> getDelegateForWrite(final int sizeHint) {
        if (delegate == null) {
            this.delegate = new HashMap<>(sizeHint, 1.0f);
        }
        return delegate;
    }

    // ** Implement methods from the Map interface **

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not allowed to clear the context data map");
    }

    @Override
    public boolean containsKey(Object key) {
        if (ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS.equals(key)) {
            return true;
        }
        return getDelegateForRead().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (interceptorBindings.equals(value)) {
            return true;
        }
        return getDelegateForRead().containsValue(value);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        final AbstractMap.SimpleImmutableEntry<String, Object> firstEntry = new AbstractMap.SimpleImmutableEntry<>(
                ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS, interceptorBindings);
        if (delegate == null) {
            return Collections.singleton(firstEntry);
        } else {
            Set<Map.Entry<String, Object>> entries = new HashSet<>(delegate.size() + 1);
            entries.addAll(delegate.entrySet());
            entries.add(firstEntry);
            return entries;
        }
    }

    @Override
    public Object get(Object key) {
        if (ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS.equals(key)) {
            return interceptorBindings;
        }
        return getDelegateForRead().get(key);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<String> keySet() {
        if (delegate == null) {
            return Collections.singleton(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
        } else {
            Set<String> set = new HashSet<>(delegate.size() + 1);
            set.addAll(delegate.keySet());
            set.add(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
            return set;
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        if (m.containsKey(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS)) {
            throw new IllegalArgumentException(
                    "Not allowed to put key '" + ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS + "' in the context data map");
        }
        getDelegateForWrite(m.size()).putAll(m);
    }

    @Override
    public Object remove(Object key) {
        if (ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS.equals(key)) {
            throw new IllegalArgumentException("Not allowed to remove key '" + ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS
                    + "' from the context data map");
        } else if (delegate == null) {
            return null;
        } else {
            return delegate.remove(key);
        }
    }

    @Override
    public int size() {
        return getDelegateForRead().size() + 1;
    }

    @Override
    public Collection<Object> values() {
        if (delegate == null) {
            return Collections.singleton(interceptorBindings);
        } else {
            List<Object> list = new ArrayList<>(delegate.size() + 1);
            list.addAll(delegate.values());
            list.add(interceptorBindings);
            return list;
        }
    }

    @Override
    public Object put(String key, Object value) {
        if (ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS.equals(key)) {
            throw new IllegalArgumentException(
                    "Not allowed to put key '" + ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS + "' in the context data map");
        }
        return getDelegateForWrite(1).put(key, value);
    }

}
