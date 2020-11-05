package io.quarkus.rest.server.runtime.core.parameters;

import org.jboss.resteasy.reactive.common.runtime.util.Encode;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

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
