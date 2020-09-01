package io.quarkus.rest.runtime.core.parameters;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class FormParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;

    public FormParamExtractor(String name, boolean single) {
        this.name = name;
        this.single = single;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        if (single) {
            return context.getContext().request().getFormAttribute(name);
        } else {
            return context.getContext().request().formAttributes().getAll(name);
        }
    }
}
