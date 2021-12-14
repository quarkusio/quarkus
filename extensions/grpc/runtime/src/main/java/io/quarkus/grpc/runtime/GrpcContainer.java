package io.quarkus.grpc.runtime;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
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
    ServerInterceptorStorage interceptorStorage;

    List<ServerInterceptor> getSortedPerServiceInterceptors(String serviceClassName) {
        return Interceptors.getSortedPerServiceInterceptors(serviceClassName,
                interceptorStorage.getInterceptors(serviceClassName));
    }

    List<ServerInterceptor> getSortedGlobalInterceptors() {
        return Interceptors.getSortedGlobalInterceptors(interceptorStorage.getGlobalInterceptors());
    }

    public Instance<BindableService> getServices() {
        return services;
    }
}
