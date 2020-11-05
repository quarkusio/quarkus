package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class HeaderParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;

    public HeaderParamExtractor(String name, boolean single) {
        this.name = name;
        this.single = single;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getHeader(name, single);
    }
}
