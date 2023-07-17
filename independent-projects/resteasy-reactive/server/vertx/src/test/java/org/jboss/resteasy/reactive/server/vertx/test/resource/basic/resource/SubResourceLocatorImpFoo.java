package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Path;

@Path("blah")
public class SubResourceLocatorImpFoo implements SubResourceLocatorFoo {
    @Override
    public Object getFoo(String val) {
        return "hello";
    }
}
