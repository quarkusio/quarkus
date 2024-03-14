package io.quarkus.resteasy.reactive.server.test.headers;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.ResponseStatus;

public class Dummy {

    @Path("")
    public static class Endpoint {

        @ResponseStatus(201)
        @ResponseHeader(name = "X-FroMage", value = "Camembert")
        @GET
        public String hello() {
            return "Hello, World!";
        }
    }
}
