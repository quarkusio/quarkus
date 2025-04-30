package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class LocatableResourcePathParamExtractor implements ParameterExtractor {

    private final String name;

    public LocatableResourcePathParamExtractor(String name) {
        this.name = name;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getResourceLocatorPathParam(name, false);
    }

}
