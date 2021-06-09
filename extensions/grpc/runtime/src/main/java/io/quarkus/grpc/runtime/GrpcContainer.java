package io.quarkus.grpc.runtime;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Prioritized;
import javax.inject.Inject;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GrpcService;

@ApplicationScoped
public class GrpcContainer {

    @Inject
    @GrpcService
    Instance<BindableService> services;

    @Inject
    @Any
    Instance<ServerInterceptor> interceptors;

    List<ServerInterceptor> getSortedInterceptors() {
        if (interceptors.isUnsatisfied()) {
            return Collections.emptyList();
        }

        return interceptors.stream().sorted(new Comparator<ServerInterceptor>() { // NOSONAR
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
        }).collect(Collectors.toList());
    }

    public Instance<BindableService> getServices() {
        return services;
    }
}
