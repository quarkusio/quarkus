package io.quarkus.qrs.runtime.core;

import javax.ws.rs.core.HttpHeaders;

public class ContextParamExtractor implements ParameterExtractor {

    private String type;

    public ContextParamExtractor(String type) {
        this.type = type;
    }

    @Override
    public Object extractParameter(RequestContext context) {
        if (type.equals(HttpHeaders.class.getName())) {
            return context.getHttpHeaders();
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
