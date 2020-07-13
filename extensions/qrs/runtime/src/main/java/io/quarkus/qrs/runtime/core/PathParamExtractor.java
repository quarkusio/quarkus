package io.quarkus.qrs.runtime.core;

public class PathParamExtractor implements ParameterExtractor {

    private final String name;

    public PathParamExtractor(String name) {
        this.name = name;
    }

    @Override
    public Object extractParameter(RequestContext context) {
        return context.getPathParamValues().get(name);
    }
}
