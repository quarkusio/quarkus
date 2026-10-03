package io.quarkus.dev.telemetry;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Attributes that are already immutable and JSON compatible, so that a {@link TelemetryEvent} can take them as they
 * are instead of copying and checking them again. Only this package can make one: the event's own copy, and the
 * builders, which only ever put immutable, JSON-compatible values in.
 */
final class CheckedAttributes extends AbstractMap<String, Object> {

    private final Map<String, Object> attributes;

    /**
     * @param attributes the attributes, handed over: nothing else may keep a reference to the map, and its values must
     *        already be immutable and JSON compatible
     */
    CheckedAttributes(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return attributes.entrySet();
    }

    @Override
    public Object get(Object key) {
        return attributes.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return attributes.containsKey(key);
    }

    @Override
    public int size() {
        return attributes.size();
    }

    /** An immutable copy of a map of strings; an entry without a key, which JSON cannot represent, is left out. */
    static Map<String, String> strings(Map<String, String> map) {
        Map<String, String> copy = new LinkedHashMap<>(map.size() * 2);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(copy);
    }
}
