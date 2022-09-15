package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

public interface ParameterSubResRoot {
    @Path("sub/{path}")
    ParameterSubResSub getSub(@PathParam("path") String path);

    @Path("subclass")
    Class<ParameterSubResClassSub> getSubClass();
}
