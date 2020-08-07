package io.quarkus.qrs.runtime.core.parameters;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.parameters.ParameterExtractor;

public class QueryParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;

    public QueryParamExtractor(String name, boolean single) {
        this.name = name;
        this.single = single;
    }

    @Override
    public Object extractParameter(QrsRequestContext context) {
        if (single) {
            return context.getContext().queryParams().get(name);
        } else {
            return context.getContext().queryParam(name);
        }
    }
}
