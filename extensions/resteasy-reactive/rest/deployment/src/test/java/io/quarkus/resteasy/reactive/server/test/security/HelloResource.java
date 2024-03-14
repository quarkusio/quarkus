package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.security.UnauthorizedException;

@Path("/hello")
public class HelloResource {
    @GET
    public String hello() {
        return "hello";
    }

    @ServerExceptionMapper
    public Response unauthorizedExceptionMapper(UnauthorizedException unauthorizedException) {
        return Response.ok("unauthorizedExceptionMapper").build();
    }
}
