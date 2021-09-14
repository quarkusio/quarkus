package io.quarkus.grpc.runtime;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Prioritized;
import javax.inject.Inject;

import io.grpc.ClientInterceptor;

@ApplicationScoped
public class GrpcClientInterceptorContainer {

    @Inject
    @Any
    Instance<ClientInterceptor> interceptors;

    public List<ClientInterceptor> getSortedInterceptors() {
        return interceptors.stream().sorted(new Comparator<ClientInterceptor>() { // NOSONAR
            @Override
            public int compare(ClientInterceptor si1, ClientInterceptor si2) {
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
}
