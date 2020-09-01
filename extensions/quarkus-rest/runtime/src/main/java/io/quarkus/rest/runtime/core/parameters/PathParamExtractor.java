package io.quarkus.rest.runtime.core.parameters;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class PathParamExtractor implements ParameterExtractor {

    private final int index;

    public PathParamExtractor(int index) {
        this.index = index;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        return context.getPathParam(index);
    }
}
