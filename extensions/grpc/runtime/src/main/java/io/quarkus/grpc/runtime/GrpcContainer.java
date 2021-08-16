package io.quarkus.grpc.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Prioritized;
import javax.inject.Inject;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.grpc.GrpcService;

@ApplicationScoped
public class GrpcContainer {

    private static final Comparator<ServerInterceptor> INTERCEPTOR_COMPARATOR = new Comparator<>() {
        @Override
        public int compare(ServerInterceptor si1, ServerInterceptor si2) {
            int p1 = 0;
            int p2 = 0;
            if (si1 instanceof Prioritized) {
                p1 = ((Prioritized) si1).getPriority();
            }
            if (si2 instanceof Prioritized) {
                p2 = ((Prioritized) si2).getPriority();
            }
            if (si1.equals(si2)) {
                return 0;
            }
            return Integer.compare(p1, p2);
        }
    };

    @Inject
    @GrpcService
    Instance<BindableService> services;

    @Inject
    Instance<ServerInterceptor> interceptors;

    @Inject
    InterceptorStorage perServiceInterceptors;

    List<ServerInterceptor> getSortedPerServiceInterceptors(String serviceClassName) {
        Set<Class<? extends ServerInterceptor>> interceptorClasses = perServiceInterceptors.getInterceptors(serviceClassName);
        if (interceptorClasses == null || interceptorClasses.isEmpty()) {
            return Collections.emptyList();
        }

        List<ServerInterceptor> interceptors = new ArrayList<>();

        for (Class<? extends ServerInterceptor> interceptorClass : interceptorClasses) {
            InstanceHandle<? extends ServerInterceptor> interceptorInstance = Arc.container().instance(interceptorClass);
            ServerInterceptor serverInterceptor = interceptorInstance.get();
            if (serverInterceptor == null) {
                throw new IllegalArgumentException("Server interceptor class " + interceptorClass + " is not a CDI bean. " +
                        "Only CDI beans can be used as gRPC server interceptors. Add one of the scope-defining annotations" +
                        " (@Singleton, @ApplicationScoped, @RequestScoped) on the interceptor class.");
            }
            interceptors.add(serverInterceptor);
        }
        interceptors.sort(INTERCEPTOR_COMPARATOR);

        return interceptors;
    }

    List<ServerInterceptor> getSortedGlobalInterceptors() {
        if (interceptors.isUnsatisfied()) {
            return Collections.emptyList();
        }

        Set<Class<? extends ServerInterceptor>> globalInterceptors = perServiceInterceptors.getGlobalInterceptors();
        List<ServerInterceptor> interceptors = new ArrayList<>();
        for (Class<? extends ServerInterceptor> interceptorClass : globalInterceptors) {
            InstanceHandle<? extends ServerInterceptor> interceptorInstance = Arc.container().instance(interceptorClass);
            ServerInterceptor serverInterceptor = interceptorInstance.get();
            if (serverInterceptor == null) {
                throw new IllegalArgumentException("Server interceptor class " + interceptorClass + " is not a CDI bean. " +
                        "Only CDI beans can be used as gRPC server interceptors. Add one of the scope-defining annotations" +
                        " (@Singleton, @ApplicationScoped, @RequestScoped) on the interceptor class.");
            }
            interceptors.add(serverInterceptor);
        }
        interceptors.sort(INTERCEPTOR_COMPARATOR);
        return interceptors;
    }

    public Instance<BindableService> getServices() {
        return services;
    }
}
