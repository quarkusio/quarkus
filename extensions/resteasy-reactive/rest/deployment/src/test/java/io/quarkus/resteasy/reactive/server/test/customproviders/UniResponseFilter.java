package io.quarkus.resteasy.reactive.server.test.customproviders;

import jakarta.ws.rs.container.ContainerResponseContext;

import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.smallrye.mutiny.Uni;

public class UniResponseFilter {

    @ServerResponseFilter
    Uni<Void> filter(SimpleResourceInfo simplifiedResourceInfo,
            ContainerResponseContext responseContext) {
        if (simplifiedResourceInfo.getMethodName() != null) {
            return Uni.createFrom().deferred(() -> {
                responseContext.getHeaders().putSingle("java-method", simplifiedResourceInfo.getMethodName());
                return Uni.createFrom().nullItem();
            });
        }
        return Uni.createFrom().nullItem();
    }
}
