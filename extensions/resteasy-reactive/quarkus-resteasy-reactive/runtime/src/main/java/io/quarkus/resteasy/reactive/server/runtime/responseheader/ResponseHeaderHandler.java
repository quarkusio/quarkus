package io.quarkus.resteasy.reactive.server.runtime.responseheader;

import java.util.Map;

import javax.ws.rs.core.Response;

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
        Response response = requestContext.getResponse().get();
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                response.getHeaders().add(header.getKey(), header.getValue());
            }
        }
    }

}
