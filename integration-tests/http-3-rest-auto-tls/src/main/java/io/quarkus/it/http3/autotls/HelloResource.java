package io.quarkus.it.http3.autotls;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.vertx.core.http.HttpServerRequest;

@Path("/")
public class HelloResource {

    @Inject
    HttpServerRequest request;

    @GET
    @Path("/hello")
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/version")
    public String version() {
        return request.version().name();
    }
}
