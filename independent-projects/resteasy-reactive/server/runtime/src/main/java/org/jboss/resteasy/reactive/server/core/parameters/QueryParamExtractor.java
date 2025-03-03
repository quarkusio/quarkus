package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class QueryParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;
    private final boolean encoded;
    private final String separator;
    private final Type paramType;

    public enum Type {
        Map,
        MultiMap,
        Other
    }

    public QueryParamExtractor(String name, Type paramType, boolean single, boolean encoded, String separator) {
        this.name = name;
        this.paramType = paramType;
        this.single = single;
        this.encoded = encoded;
        this.separator = separator;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        if (paramType.equals(Type.Map)) {
            return context.getMapQueryParameter();
        }
        return context.getQueryParameter(name, single, encoded, separator);
    }
}
