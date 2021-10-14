package io.quarkus.resteasy.reactive.server.test.headers;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
