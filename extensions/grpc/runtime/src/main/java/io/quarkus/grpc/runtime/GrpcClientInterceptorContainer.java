package io.quarkus.grpc.runtime;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.grpc.ClientInterceptor;

@ApplicationScoped
public class GrpcClientInterceptorContainer {

    @Inject
    ClientInterceptorStorage interceptorStorage;

    public List<ClientInterceptor> getSortedPerServiceInterceptors(Set<String> interceptorClasses) {
        return Interceptors.getSortedPerServiceInterceptors(interceptorStorage.getPerClientInterceptors(interceptorClasses));
    }

    public List<ClientInterceptor> getSortedGlobalInterceptors() {
        return Interceptors.getSortedGlobalInterceptors(interceptorStorage.getGlobalInterceptors());
    }

}
