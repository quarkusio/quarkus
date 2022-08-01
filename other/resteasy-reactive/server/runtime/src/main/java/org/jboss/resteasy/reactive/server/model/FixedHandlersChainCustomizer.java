package org.jboss.resteasy.reactive.server.model;

import java.util.Collections;
import java.util.List;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class FixedHandlersChainCustomizer implements HandlerChainCustomizer {

    private List<ServerRestHandler> handlers;
    private Phase phase;

    public FixedHandlersChainCustomizer(List<ServerRestHandler> handlers, Phase phase) {
        this.handlers = handlers;
        this.phase = phase;
    }

    public FixedHandlersChainCustomizer() {
    }

    @Override
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass,
            ServerResourceMethod serverResourceMethod) {
        if (this.phase == phase) {
            return handlers;
        }
        return Collections.emptyList();
    }

    public List<ServerRestHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<ServerRestHandler> handlers) {
        this.handlers = handlers;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }
}
