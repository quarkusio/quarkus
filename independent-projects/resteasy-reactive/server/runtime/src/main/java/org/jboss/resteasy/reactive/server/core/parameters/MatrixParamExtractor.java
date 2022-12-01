package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

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
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return context.getMatrixParameter(name, single, encoded);
    }

}
