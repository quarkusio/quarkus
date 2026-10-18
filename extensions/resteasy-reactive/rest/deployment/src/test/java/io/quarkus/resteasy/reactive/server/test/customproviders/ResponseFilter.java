package io.quarkus.resteasy.reactive.server.test.customproviders;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

@SuppressWarnings("FieldCanBeLocal") // we use the local fields to test the code path where the generated class is @ApplicationScoped
public class ResponseFilter {

    private final String headerName = "response-input";
    private final String abortValue = "abort";

    @ServerRequestFilter
    public Response aborting(HttpHeaders httpHeaders) {
        String abortInputHeader = httpHeaders.getHeaderString(headerName);
        if (abortValue.equals(abortInputHeader)) {
            return Response.accepted().entity(abortInputHeader).build();
        }
        return null;
    }
}
