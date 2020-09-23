package io.quarkus.rest.runtime.core.parameters;

import java.util.List;

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
            return context.getFormParameter(name);
        } else {
            List<String> values = context.getContext().request().formAttributes().getAll(name);
            if (values.isEmpty()) {
                return null;
            }
            return values;
        }
    }
}
