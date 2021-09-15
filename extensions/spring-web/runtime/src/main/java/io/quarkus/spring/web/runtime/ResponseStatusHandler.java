package io.quarkus.spring.web.runtime;

import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

// TODO: consider whether or not this should be moved to the resteasy reactive server extension
public class ResponseStatusHandler implements ServerRestHandler {

    // make mutable to allow for bytecode serialization
    private int defaultResponseCode;
    private int newResponseCode;

    public int getDefaultResponseCode() {
        return defaultResponseCode;
    }

    public void setDefaultResponseCode(int defaultResponseCode) {
        this.defaultResponseCode = defaultResponseCode;
    }

    public int getNewResponseCode() {
        return newResponseCode;
    }

    public void setNewResponseCode(int newResponseCode) {
        this.newResponseCode = newResponseCode;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        ResponseImpl response = (ResponseImpl) requestContext.getResponse().get();
        if (response.getStatus() == defaultResponseCode) { // only set the status if it has not already been set
            response.setStatus(newResponseCode);
        }
    }
}
