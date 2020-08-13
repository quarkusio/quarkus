package io.quarkus.qrs.runtime.core.parameters;

import io.quarkus.qrs.runtime.core.QrsRequestContext;

public class PathParamExtractor implements ParameterExtractor {

    private final int index;

    public PathParamExtractor(int index) {
        this.index = index;
    }

    @Override
    public Object extractParameter(QrsRequestContext context) {
        return context.getPathParam(index);
    }
}
