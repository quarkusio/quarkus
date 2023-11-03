package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import io.quarkus.security.AuthenticationRedirectException;

public class AuthenticationRedirectExceptionMapper implements ExceptionMapper<AuthenticationRedirectException> {

    @Override
    public Response toResponse(AuthenticationRedirectException ex) {
        return Response.status(ex.getCode())
                .header(HttpHeaders.LOCATION, ex.getRedirectUri())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

}
