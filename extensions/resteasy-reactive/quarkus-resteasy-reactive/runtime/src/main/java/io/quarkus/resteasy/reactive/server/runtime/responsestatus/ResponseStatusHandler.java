package io.quarkus.resteasy.reactive.server.runtime.responsestatus;

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
        requestContext.serverResponse().setStatusCode(status);
    }
}
