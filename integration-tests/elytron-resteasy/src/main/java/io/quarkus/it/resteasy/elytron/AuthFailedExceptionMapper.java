package io.quarkus.it.resteasy.elytron;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.quarkus.security.AuthenticationFailedException;

@Provider
public class AuthFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {

    static final String EXPECTED_RESPONSE = "expected response";

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        return Response.status(401).entity(EXPECTED_RESPONSE).build();
    }
}
