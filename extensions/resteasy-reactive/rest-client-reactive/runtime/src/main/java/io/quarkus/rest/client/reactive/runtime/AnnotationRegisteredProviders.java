package io.quarkus.rest.client.reactive.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AnnotationRegisteredProviders {
    private final Map<String, Map<Class<?>, Integer>> providers = new HashMap<>();

    public Map<Class<?>, Integer> getProviders(Class<?> clientClass) {
        Map<Class<?>, Integer> providersForClass = providers.get(clientClass.getName());
        return providersForClass == null ? Collections.emptyMap() : providersForClass;
    }

    // used by generated code
    public void addProviders(String className, Map<Class<?>, Integer> providersForClass) {
        this.providers.put(className, providersForClass);
    }
}
