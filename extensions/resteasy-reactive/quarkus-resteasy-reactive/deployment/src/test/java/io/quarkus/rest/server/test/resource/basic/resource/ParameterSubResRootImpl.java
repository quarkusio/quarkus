package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.Path;

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
