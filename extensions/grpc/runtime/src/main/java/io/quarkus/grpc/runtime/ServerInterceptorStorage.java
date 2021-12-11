package io.quarkus.grpc.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class ServerInterceptorStorage {

    private final Map<String, Set<Class<?>>> perServiceInterceptors;
    private final Set<Class<?>> globalInterceptors;

    public ServerInterceptorStorage(Map<String, Set<Class<?>>> perServiceInterceptors,
            Set<Class<?>> globalInterceptors) {
        this.perServiceInterceptors = Map.copyOf(perServiceInterceptors);
        this.globalInterceptors = Set.copyOf(globalInterceptors);
    }

    public Set<Class<?>> getInterceptors(String serviceClassName) {
        return perServiceInterceptors.getOrDefault(serviceClassName, Collections.emptySet());
    }

    public Set<Class<?>> getGlobalInterceptors() {
        return globalInterceptors;
    }

}
