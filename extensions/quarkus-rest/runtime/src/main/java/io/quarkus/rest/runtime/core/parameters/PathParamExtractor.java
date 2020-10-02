package io.quarkus.rest.runtime.core.parameters;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.util.Encode;

public class PathParamExtractor implements ParameterExtractor {

    private final int index;
    private final boolean encoded;

    public PathParamExtractor(int index, boolean encoded) {
        this.index = index;
        this.encoded = encoded;
    }

    @Override
    public Object extractParameter(QuarkusRestRequestContext context) {
        String pathParam = context.getPathParam(index);
        if (encoded) {
            return Encode.encodeQueryParam(pathParam);
        }
        return pathParam;
    }
}
