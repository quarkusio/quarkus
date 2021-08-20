package io.quarkus.grpc.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.grpc.ServerInterceptor;

public abstract class InterceptorStorage {
    private final Map<String, Set<Class<? extends ServerInterceptor>>> perServiceInterceptors = new HashMap<>();
    private final Set<Class<? extends ServerInterceptor>> globalInterceptors = new HashSet<>();

    public Set<Class<? extends ServerInterceptor>> getInterceptors(String serviceClassName) {
        return perServiceInterceptors.get(serviceClassName);
    }

    public Set<Class<? extends ServerInterceptor>> getGlobalInterceptors() {
        return globalInterceptors;
    }

    @SuppressWarnings("unused") // used by generated code
    public void addGlobalInterceptor(Class<? extends ServerInterceptor> interceptor) {
        globalInterceptors.add(interceptor);
    }

    @SuppressWarnings("unused") // used by generated code
    public void addInterceptor(String serviceClassName, Class<? extends ServerInterceptor> interceptor) {
        Set<Class<? extends ServerInterceptor>> interceptors = perServiceInterceptors.computeIfAbsent(serviceClassName,
                c -> new HashSet<>());
        interceptors.add(interceptor);
    }
}
