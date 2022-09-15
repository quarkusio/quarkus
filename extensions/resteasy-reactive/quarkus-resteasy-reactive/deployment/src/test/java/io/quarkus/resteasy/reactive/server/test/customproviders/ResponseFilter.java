package io.quarkus.resteasy.reactive.server.test.customproviders;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

public class ResponseFilter {

    @ServerRequestFilter
    public Response aborting(HttpHeaders httpHeaders) {
        String abortInputHeader = httpHeaders.getHeaderString("response-input");
        if ("abort".equals(abortInputHeader)) {
            return Response.accepted().entity(abortInputHeader).build();
        }
        return null;
    }
}
