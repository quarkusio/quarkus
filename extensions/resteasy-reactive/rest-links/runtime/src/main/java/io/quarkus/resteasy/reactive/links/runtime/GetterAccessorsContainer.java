package io.quarkus.resteasy.reactive.links.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that allows us to easily find a {@code GetterAccessor} based on a type and a field name.
 */
public class GetterAccessorsContainer {

    private final Map<String, Map<String, GetterAccessor>> getterAccessors = new HashMap<>();

    /**
     * Creates a container from the accessor class names, mapped by entity type and then by field name.
     */
    public static GetterAccessorsContainer of(Map<String, Map<String, String>> accessorClassNames) {
        GetterAccessorsContainer container = new GetterAccessorsContainer();
        for (Map.Entry<String, Map<String, String>> byClass : accessorClassNames.entrySet()) {
            for (Map.Entry<String, String> byField : byClass.getValue().entrySet()) {
                String accessorName = byField.getValue();
                try {
                    GetterAccessor accessor = (GetterAccessor) Thread.currentThread()
                            .getContextClassLoader()
                            .loadClass(accessorName)
                            .getDeclaredConstructor()
                            .newInstance();
                    container.put(byClass.getKey(), byField.getKey(), accessor);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize " + accessorName + ": " + e.getMessage());
                }
            }
        }
        return container;
    }

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
