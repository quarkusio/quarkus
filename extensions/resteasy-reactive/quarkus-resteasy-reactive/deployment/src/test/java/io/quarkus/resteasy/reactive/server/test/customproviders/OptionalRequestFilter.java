package io.quarkus.resteasy.reactive.server.test.customproviders;

import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

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
