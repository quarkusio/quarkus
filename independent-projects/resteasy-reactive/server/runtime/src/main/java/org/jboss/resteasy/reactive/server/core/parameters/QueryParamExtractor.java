package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class QueryParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;
    private final boolean encoded;

    public QueryParamExtractor(String name, boolean single, boolean encoded) {
        this.name = name;
        this.single = single;
        this.encoded = encoded;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getQueryParameter(name, single, encoded);
    }
}
