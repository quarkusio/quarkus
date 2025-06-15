package io.quarkus.grpc.runtime;

import java.util.HashSet;
import java.util.Set;

public final class ClientInterceptorStorage {

    private final Set<Class<?>> perClientInterceptors;
    private final Set<Class<?>> globalInterceptors;

    public ClientInterceptorStorage(Set<Class<?>> perClientInterceptors, Set<Class<?>> globalInterceptors) {
        this.perClientInterceptors = Set.copyOf(perClientInterceptors);
        this.globalInterceptors = Set.copyOf(globalInterceptors);
    }

    public Set<Class<?>> getPerClientInterceptors(Set<String> interceptorClasses) {
        Set<Class<?>> ret = new HashSet<Class<?>>();
        for (Class<?> interceptor : perClientInterceptors) {
            if (interceptorClasses.contains(interceptor.getName())) {
                ret.add(interceptor);
            }
        }
        return ret;
    }

    public Class<?> getPerClientInterceptor(String interceptorClass) {
        for (Class<?> interceptor : perClientInterceptors) {
            if (interceptor.getName().equals(interceptorClass)) {
                return interceptor;
            }
        }
        return null;
    }

    public Set<Class<?>> getGlobalInterceptors() {
        return globalInterceptors;
    }

}
