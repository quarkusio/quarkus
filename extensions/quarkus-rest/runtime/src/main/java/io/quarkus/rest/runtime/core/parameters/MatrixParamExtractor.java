package io.quarkus.rest.runtime.core.parameters;

import javax.ws.rs.core.PathSegment;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class MatrixParamExtractor implements ParameterExtractor {

    private final String name;
    private final boolean single;

    public MatrixParamExtractor(String name, boolean single) {
        this.name = name;
        this.single = single;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        for (PathSegment i : context.getPathSegments()) {
            String res = i.getMatrixParameters().getFirst(name);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

}
