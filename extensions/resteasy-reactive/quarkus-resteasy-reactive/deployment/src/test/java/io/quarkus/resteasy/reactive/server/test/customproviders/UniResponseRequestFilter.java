package io.quarkus.resteasy.reactive.server.test.customproviders;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.smallrye.mutiny.Uni;

public class UniResponseRequestFilter {

    @ServerRequestFilter
    Uni<Response> uniResponse(UriInfo uriInfo, HttpHeaders httpHeaders, ContainerRequestContext requestContext) {
        String exceptionHeader = httpHeaders.getHeaderString("some-other-uni-exception-input");
        if ((exceptionHeader != null) && !exceptionHeader.isEmpty()) {
            return Uni.createFrom().item(Response.serverError().entity(exceptionHeader).build());
        }
        return Uni.createFrom().deferred(() -> {
            String inputHeader = httpHeaders.getHeaderString("some-other-uni-input");
            if (inputHeader != null) {
                requestContext.getHeaders().putSingle("custom-uni-header", uriInfo.getPath() + "-" + inputHeader);
            }
            return Uni.createFrom().nullItem();
        });
    }
}
