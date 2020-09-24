package io.quarkus.rest.runtime.core.parameters;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class HeaderParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;

    public HeaderParamExtractor(String name, boolean single) {
        this.name = name;
        this.single = single;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        return context.getHeader(name, single);
    }
}
