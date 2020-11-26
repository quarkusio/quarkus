package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;

@Path("{x:.*}")
public class WiderMappingDefaultOptions {
    @OPTIONS
    public String options() {
        return "hello";
    }
}
