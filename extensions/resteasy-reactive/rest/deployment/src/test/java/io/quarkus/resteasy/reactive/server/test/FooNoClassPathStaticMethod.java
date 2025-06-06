package io.quarkus.resteasy.reactive.server.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class FooNoClassPathStaticMethod {

    @Path("noclass")
    @GET
    public static String hello() {
        return "noclass";
    }
}
