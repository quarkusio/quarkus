package io.quarkus.devshell.runtime;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * A wrapper around BuildTimeDataReader that uses reflection to call methods.
 * This is needed because the reader instance may be from a different classloader
 * than the runtime-dev classes.
 */
public class ReflectiveBuildTimeDataReader {

    private final Object delegate;
    private Method getBuildTimeDataMethod;
    private Method getBuildTimeDataFieldMethod;

    public ReflectiveBuildTimeDataReader(Object delegate) {
        this.delegate = delegate;
        cacheMethods();
    }

    private void cacheMethods() {
        try {
            Class<?> delegateClass = delegate.getClass();
            getBuildTimeDataMethod = delegateClass.getMethod("getBuildTimeData", String.class);
            getBuildTimeDataFieldMethod = delegateClass.getMethod("getBuildTimeDataField", String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find BuildTimeDataReader methods", e);
        }
    }

    /**
     * Get all build-time data for a given extension namespace.
     *
     * @param namespace the extension namespace (e.g., "quarkus-arc")
     * @return a map of field names to their JSON string values
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getBuildTimeData(String namespace) {
        try {
            return (Map<String, String>) getBuildTimeDataMethod.invoke(delegate, namespace);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Get a specific build-time data field for an extension.
     *
     * @param namespace the extension namespace (e.g., "quarkus-arc")
     * @param fieldName the field name (e.g., "beans")
     * @return the JSON string value, or null if not found
     */
    public String getBuildTimeDataField(String namespace, String fieldName) {
        try {
            return (String) getBuildTimeDataFieldMethod.invoke(delegate, namespace, fieldName);
        } catch (Exception e) {
            return null;
        }
    }
}
