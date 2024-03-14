package io.quarkus.resteasy.reactive.server.test.customproviders;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.smallrye.mutiny.Uni;

public class UniVoidRequestFilter {

    @ServerRequestFilter
    Uni<Void> uniVoid(UriInfo uriInfo, HttpHeaders httpHeaders, ContainerRequestContext requestContext) {
        String exceptionHeader = httpHeaders.getHeaderString("some-uni-exception-input");
        if ((exceptionHeader != null) && !exceptionHeader.isEmpty()) {
            return Uni.createFrom().failure(new UniException(exceptionHeader));
        }
        return Uni.createFrom().deferred(() -> {
            String inputHeader = httpHeaders.getHeaderString("some-uni-input");
            if (inputHeader != null) {
                requestContext.getHeaders().putSingle("custom-uni-header", uriInfo.getPath() + "-" + inputHeader);
            }
            return Uni.createFrom().nullItem();
        });
    }
}
