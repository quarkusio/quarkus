package io.quarkus.rest.runtime.core.parameters;

import java.util.List;

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
        if (single) {
            return context.getHeader(name);
        } else {
            List<String> all = context.getContext().request().headers().getAll(name);
            if (all.isEmpty()) {
                return null;
            }
            return all;
        }
    }
}
