package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

public class OptionalRequestFilter {

    @ServerRequestFilter
    public Optional<Response> filter(HttpHeaders httpHeaders) {
        String optionalHeader = httpHeaders.getHeaderString("optional-input");
        if ("abort".equals(optionalHeader)) {
            return Optional.of(Response.noContent().build());
        }
        return Optional.empty();
    }
}
