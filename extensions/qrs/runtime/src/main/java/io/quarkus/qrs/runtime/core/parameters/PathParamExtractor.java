package io.quarkus.qrs.runtime.core.parameters;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.parameters.ParameterExtractor;

public class PathParamExtractor implements ParameterExtractor {

    private final String name;

    public PathParamExtractor(String name) {
        this.name = name;
    }

    @Override
    public Object extractParameter(QrsRequestContext context) {
        return context.getPathParamValues().get(name);
    }
}
