package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Path;

@Path("/path")
public class ParameterSubResRootImpl implements ParameterSubResRoot {
    @Override
    public ParameterSubResSubImpl<Integer> getSub(String path) {
        return new ParameterSubResSubImpl<Integer>(path) {
        };
    }

    @Override
    public Class<ParameterSubResClassSub> getSubClass() {
        return ParameterSubResClassSub.class;
    }

}
