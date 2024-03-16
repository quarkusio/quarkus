package io.quarkus.resteasy.reactive.links.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that allows us to easily find a {@code GetterAccessor} based on a type and a field name.
 */
public class GetterAccessorsContainer {

    private final Map<String, Map<String, GetterAccessor>> getterAccessors = new HashMap<>();

    public GetterAccessor get(String className, String fieldName) {
        return getterAccessors.get(className).get(fieldName);
    }

    public void put(String className, String fieldName, GetterAccessor getterAccessor) {
        if (!getterAccessors.containsKey(className)) {
            getterAccessors.put(className, new HashMap<>());
        }
        Map<String, GetterAccessor> getterAccessorsByField = getterAccessors.get(className);
        if (!getterAccessorsByField.containsKey(fieldName)) {
            getterAccessorsByField.put(fieldName, getterAccessor);
        }
    }
}
