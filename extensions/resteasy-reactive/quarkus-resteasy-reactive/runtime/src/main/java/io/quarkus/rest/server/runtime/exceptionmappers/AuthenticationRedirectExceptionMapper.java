package io.quarkus.rest.server.runtime.exceptionmappers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

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
