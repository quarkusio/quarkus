package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class HeaderParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;
    private final boolean restHeaderMap;

    public HeaderParamExtractor(String name, boolean single) {
        this(name, single, false);
    }

    public HeaderParamExtractor(String name, boolean single, boolean restHeaderMap) {
        this.name = name;
        this.single = single;
        this.restHeaderMap = restHeaderMap;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getHeader(name, single, restHeaderMap);
    }
}
