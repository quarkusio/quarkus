package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Path;

@Path(value = "/InheritanceTest")
public class InheritenceParentResourceImpl implements InheritenceParentResource {

    public String firstest() {
        return "First";
    }
}
