package io.quarkus.devui.runtime.observability.telemetry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the maps and lists of strings that telemetry events carry, e.g. a meter's tags or a span's attributes. An
 * event's attributes are immutable, so when they already are strings - always the case for an event from the
 * builders - they are used as they are; anything else is turned into strings, since that is all the dashboard shows.
 */
public final class StringValues {

    private StringValues() {
    }

    /**
     * @return the map of strings, or an empty map when the attribute is not a map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> map(Object raw) {
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        if (allStrings(map.values())) {
            // The keys of an event's maps are always strings.
            return (Map<String, String>) map;
        }
        Map<String, String> strings = new LinkedHashMap<>();
        map.forEach((key, value) -> strings.put(String.valueOf(key), String.valueOf(value)));
        return strings;
    }

    /**
     * @return the list of strings, or an empty list when the attribute is not a list
     */
    @SuppressWarnings("unchecked")
    public static List<String> list(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        if (allStrings(list)) {
            return (List<String>) list;
        }
        List<String> strings = new ArrayList<>(list.size());
        for (Object value : list) {
            strings.add(String.valueOf(value));
        }
        return strings;
    }

    private static boolean allStrings(Iterable<?> values) {
        for (Object value : values) {
            if (!(value instanceof String)) {
                return false;
            }
        }
        return true;
    }
}
