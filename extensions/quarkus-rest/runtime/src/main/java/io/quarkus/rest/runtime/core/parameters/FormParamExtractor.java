package io.quarkus.rest.runtime.core.parameters;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class FormParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;
    private final boolean encoded;

    public FormParamExtractor(String name, boolean single, boolean encoded) {
        this.name = name;
        this.single = single;
        this.encoded = encoded;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        return context.getFormParameter(name, single, encoded);
    }
}
