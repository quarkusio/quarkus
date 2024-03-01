package io.quarkus.rest.client.reactive.runtime;

import java.util.HashMap;
import java.util.Map;

public abstract class AnnotationRegisteredProviders {
    private final Map<String, Map<Class<?>, Integer>> providers = new HashMap<>();
    private final Map<Class<?>, Integer> globalProviders = new HashMap<>();

    public Map<Class<?>, Integer> getProviders(Class<?> clientClass) {
        return providers.getOrDefault(clientClass.getName(), globalProviders);
    }

    // used by generated code
    // MUST be called after addGlobalProvider
    public void addProviders(String className, Map<Class<?>, Integer> providersForClass) {
        Map<Class<?>, Integer> providers = new HashMap<>(providersForClass);
        providers.putAll(globalProviders);
        this.providers.put(className, providers);
    }

    // used by generated code
    public void addGlobalProvider(Class<?> providerClass, int priority) {
        globalProviders.put(providerClass, priority);
    }
}
