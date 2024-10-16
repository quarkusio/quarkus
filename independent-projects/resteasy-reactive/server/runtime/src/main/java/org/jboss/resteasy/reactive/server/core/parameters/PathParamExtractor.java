package org.jboss.resteasy.reactive.server.core.parameters;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        String pathParam = context.getPathParam(index, true);
        if (single) {
            return encoded ? pathParam : Encode.decodePath(pathParam);
        } else {
            return encoded
                    ? List.of(pathParam.split("/"))
                    : Arrays.stream(pathParam.split("/")).map(Encode::decodePath)
                            .collect(Collectors.toList());
        }
    }
}
