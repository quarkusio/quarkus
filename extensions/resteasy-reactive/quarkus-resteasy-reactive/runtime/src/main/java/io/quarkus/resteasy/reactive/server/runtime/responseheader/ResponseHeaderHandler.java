package io.quarkus.resteasy.reactive.server.runtime.responseheader;

import java.util.Map;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ResponseHeaderHandler implements ServerRestHandler {
    private Map<String, String> headers;

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (headers != null) {
            requestContext.setResponseHeaders(headers);
        }
    }

}
