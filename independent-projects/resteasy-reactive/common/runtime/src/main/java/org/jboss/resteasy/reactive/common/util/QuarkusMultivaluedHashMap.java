package org.jboss.resteasy.reactive.common.util;

import jakarta.ws.rs.core.MultivaluedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * QuarkusMultivaluedHashMap without the bug in put/putAll that leaks external mutable storage into our storage.
 */
public class QuarkusMultivaluedHashMap<Key, Value> extends MultivaluedHashMap<Key, Value>
        implements QuarkusMultivaluedMap<Key, Value> {

    private static final long serialVersionUID = 4136263572124588039L;

    @Override
    public List<Value> put(Key key, List<Value> value) {
        if (value != null) {
            // this is the storage the supertype uses
            value = new LinkedList<>(value);
        }
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends Key, ? extends List<Value>> m) {
        for (Entry<? extends Key, ? extends List<Value>> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
