package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class PathParamExtractor implements ParameterExtractor {

    private final int index;
    private final boolean encoded;

    public PathParamExtractor(int index, boolean encoded) {
        this.index = index;
        this.encoded = encoded;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        String pathParam = context.getPathParam(index);
        if (encoded) {
            return Encode.encodeQueryParam(pathParam);
        }
        return pathParam;
    }
}
