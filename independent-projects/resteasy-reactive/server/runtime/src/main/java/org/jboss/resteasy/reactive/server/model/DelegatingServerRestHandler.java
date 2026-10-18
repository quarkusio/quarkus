package org.jboss.resteasy.reactive.server.model;

import java.util.List;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class DelegatingServerRestHandler implements ServerRestHandler {

    private List<ServerRestHandler> delegates;

    public DelegatingServerRestHandler(List<ServerRestHandler> delegates) {
        this.delegates = delegates;
    }

    // for bytecode recording
    public DelegatingServerRestHandler() {
    }

    public List<ServerRestHandler> getDelegates() {
        return delegates;
    }

    public void setDelegates(List<ServerRestHandler> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        for (int i = 0; i < delegates.size(); i++) {
            delegates.get(i).handle(requestContext);
        }
    }
}
