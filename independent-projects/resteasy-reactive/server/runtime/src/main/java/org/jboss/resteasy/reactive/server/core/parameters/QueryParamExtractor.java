package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class QueryParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;
    private final boolean encoded;
    private final String separator;

    public QueryParamExtractor(String name, boolean single, boolean encoded, String separator) {
        this.name = name;
        this.single = single;
        this.encoded = encoded;
        this.separator = separator;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getQueryParameter(name, single, encoded, separator);
    }
}
