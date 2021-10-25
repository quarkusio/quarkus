package org.jboss.resteasy.reactive.server.core.parameters;

import java.util.List;
import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public class PathParamExtractor implements ParameterExtractor {

    private final int index;
    private final boolean encoded;
    private final boolean single;

    public PathParamExtractor(int index, boolean encoded, boolean single) {
        this.index = index;
        this.encoded = encoded;
        this.single = single;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        String pathParam = context.getPathParam(index);
        if (encoded) {
            pathParam = Encode.encodeQueryParam(pathParam);
        }
        if (single) {
            return pathParam;
        } else {
            return List.of(pathParam.split("/"));
        }
    }
}
