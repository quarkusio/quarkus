package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;

@Path("{x:.*}")
public class WiderMappingDefaultOptions {
    @OPTIONS
    public String options() {
        return "hello";
    }
}
