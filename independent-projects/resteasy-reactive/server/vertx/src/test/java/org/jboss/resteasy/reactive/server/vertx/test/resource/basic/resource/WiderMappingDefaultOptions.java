package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;

@Path("{x:.*}")
public class WiderMappingDefaultOptions {
    @OPTIONS
    public String options() {
        return "hello";
    }
}
