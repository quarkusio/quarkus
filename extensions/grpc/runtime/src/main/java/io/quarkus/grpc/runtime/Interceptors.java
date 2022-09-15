package io.quarkus.grpc.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Prioritized;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

final class Interceptors {

    static <T> List<T> getSortedPerServiceInterceptors(String name, Set<Class<?>> interceptorClasses) {
        if (interceptorClasses.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> interceptors = new ArrayList<>();

        for (Class<?> interceptorClass : interceptorClasses) {
            // Note that additional qualifiers are ignored - @Any is required
            InstanceHandle<?> interceptorInstance = Arc.container().instance(interceptorClass, Any.Literal.INSTANCE);
            @SuppressWarnings("unchecked")
            T interceptor = (T) interceptorInstance.get();
            if (interceptor == null) {
                throw new IllegalArgumentException("Interceptor class " + interceptorClass + " is not a CDI bean. " +
                        "Only CDI beans can be used as gRPC server/client interceptors. Add one of the scope-defining annotations"
                        +
                        " (@Singleton, @ApplicationScoped, @RequestScoped) on the interceptor class.");
            }
            interceptors.add(interceptor);
        }
        interceptors.sort(Interceptors.INTERCEPTOR_COMPARATOR);
        return interceptors;
    }

    static <T> List<T> getSortedPerServiceInterceptors(Set<Class<?>> interceptorClasses) {
        if (interceptorClasses.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> interceptors = new ArrayList<>();

        for (Class<?> interceptorClass : interceptorClasses) {
            // Note that additional qualifiers are ignored - @Any is required
            InstanceHandle<?> interceptorInstance = Arc.container().instance(interceptorClass, Any.Literal.INSTANCE);
            @SuppressWarnings("unchecked")
            T interceptor = (T) interceptorInstance.get();
            if (interceptor == null) {
                throw new IllegalArgumentException("Interceptor class " + interceptorClass + " is not a CDI bean. " +
                        "Only CDI beans can be used as gRPC server/client interceptors. Add one of the scope-defining annotations"
                        +
                        " (@Singleton, @ApplicationScoped, @RequestScoped) on the interceptor class.");
            }
            interceptors.add(interceptor);
        }
        interceptors.sort(Interceptors.INTERCEPTOR_COMPARATOR);
        return interceptors;
    }

    static <T> List<T> getSortedGlobalInterceptors(Set<Class<?>> globalInterceptors) {
        if (globalInterceptors.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> interceptors = new ArrayList<>();
        for (Class<?> interceptorClass : globalInterceptors) {
            // Note that additional qualifiers are ignored - @Any is required
            InstanceHandle<?> interceptorInstance = Arc.container().instance(interceptorClass, Any.Literal.INSTANCE);
            @SuppressWarnings("unchecked")
            T serverInterceptor = (T) interceptorInstance.get();
            if (serverInterceptor == null) {
                throw new IllegalArgumentException("Interceptor class " + interceptorClass + " is not a CDI bean. " +
                        "Only CDI beans can be used as gRPC server/client interceptors. Add one of the scope-defining annotations"
                        +
                        " (@Singleton, @ApplicationScoped, @RequestScoped) on the interceptor class.");
            }
            interceptors.add(serverInterceptor);
        }
        interceptors.sort(Interceptors.INTERCEPTOR_COMPARATOR);
        return interceptors;
    }

    static final Comparator<Object> INTERCEPTOR_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(Object i1, Object i2) {
            int p1 = 0;
            int p2 = 0;
            if (i1 instanceof Prioritized) {
                p1 = ((Prioritized) i1).getPriority();
            }
            if (i2 instanceof Prioritized) {
                p2 = ((Prioritized) i2).getPriority();
            }
            if (i1.equals(i2)) {
                return 0;
            }
            return Integer.compare(p1, p2);
        }
    };

}
