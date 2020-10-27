package io.quarkus.rest.server.runtime.core.parameters;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

public class MatrixParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;
    private final boolean encoded;

    public MatrixParamExtractor(String name, boolean single, boolean encoded) {
        this.name = name;
        this.single = single;
        this.encoded = encoded;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        return context.getMatrixParameter(name, single, encoded);
    }

}
