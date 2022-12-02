package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;

@Path("{x:.*}")
public class WiderMappingDefaultOptions {
    @OPTIONS
    public String options() {
        return "hello";
    }
}
