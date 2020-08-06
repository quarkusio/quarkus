package io.quarkus.qrs.runtime.core;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import io.quarkus.qrs.runtime.jaxrs.QrsUriInfo;
import io.vertx.core.http.HttpServerRequest;

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
        if (type.equals(RequestContext.class.getName())) {
            return context;
        }
        if (type.equals(UriInfo.class.getName())) {
            HttpServerRequest request = context.getContext().request();
            return new QrsUriInfo(request.absoluteURI() + (request.query() == null ? "" : "?" + request.query()), "/");
        }
        // FIXME: move to build time
        throw new IllegalStateException("Unsupported contextual type: " + type);
    }

}
