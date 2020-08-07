package io.quarkus.qrs.runtime.core.parameters;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.parameters.ParameterExtractor;

public class ContextParamExtractor implements ParameterExtractor {

    private String type;

    public ContextParamExtractor(String type) {
        this.type = type;
    }

    @Override
    public Object extractParameter(QrsRequestContext context) {
        if (type.equals(HttpHeaders.class.getName())) {
            return context.getHttpHeaders();
        }
        if (type.equals(QrsRequestContext.class.getName())) {
            return context;
        }
        if (type.equals(UriInfo.class.getName())) {
            return context.getUriInfo();
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
