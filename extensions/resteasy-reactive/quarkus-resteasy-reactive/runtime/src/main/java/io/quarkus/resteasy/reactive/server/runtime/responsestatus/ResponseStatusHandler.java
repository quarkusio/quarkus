package io.quarkus.resteasy.reactive.server.runtime.responsestatus;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.core.LazyResponse;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ResponseStatusHandler implements ServerRestHandler {

    private int status;

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        Response response = requestContext.getResponse().get();

        Response.ResponseBuilder responseBuilder = Response.fromResponse(response);
        responseBuilder.status(status);
        LazyResponse.Existing lazyResponse = new LazyResponse.Existing(responseBuilder.build());
        requestContext.setResponse(lazyResponse);
    }
}
