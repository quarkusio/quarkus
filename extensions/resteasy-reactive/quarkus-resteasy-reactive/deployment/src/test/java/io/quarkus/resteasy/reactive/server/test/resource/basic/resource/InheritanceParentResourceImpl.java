package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.Path;

@Path(value = "/InheritanceTest")
public class InheritanceParentResourceImpl implements InheritanceParentResource {

    public String firstest() {
        return "First";
    }
}
