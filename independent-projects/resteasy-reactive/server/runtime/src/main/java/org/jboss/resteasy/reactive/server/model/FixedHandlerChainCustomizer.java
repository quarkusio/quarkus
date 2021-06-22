package org.jboss.resteasy.reactive.server.model;

import java.util.Collections;
import java.util.List;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class FixedHandlerChainCustomizer implements HandlerChainCustomizer {

    private ServerRestHandler handler;
    private Phase phase;

    public FixedHandlerChainCustomizer(ServerRestHandler handler, Phase phase) {
        this.handler = handler;
        this.phase = phase;
    }

    public FixedHandlerChainCustomizer() {
    }

    @Override
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
            ServerResourceMethod serverResourceMethod) {
        if (this.phase == phase) {
            return Collections.singletonList(handler);
        }
        return Collections.emptyList();
    }

    public ServerRestHandler getHandler() {
        return handler;
    }

    public FixedHandlerChainCustomizer setHandler(ServerRestHandler handler) {
        this.handler = handler;
        return this;
    }

    public Phase getPhase() {
        return phase;
    }

    public FixedHandlerChainCustomizer setPhase(Phase phase) {
        this.phase = phase;
        return this;
    }
}
