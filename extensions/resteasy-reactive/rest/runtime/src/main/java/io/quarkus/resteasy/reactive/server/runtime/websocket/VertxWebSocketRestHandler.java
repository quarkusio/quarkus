package io.quarkus.resteasy.reactive.server.runtime.websocket;

import java.util.Collections;
import java.util.List;

import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class VertxWebSocketRestHandler implements HandlerChainCustomizer {

    private static final ServerRestHandler[] AWOL = new ServerRestHandler[] {
            new ServerRestHandler() {

                @Override
                public void handle(ResteasyReactiveRequestContext requestContext)
                        throws Exception {
                    throw new IllegalStateException("VertxWebSocketRestHandler was restarted unexpectedly. " +
                               "WebSocket REST handlers are not designed to be restarted once initialized. " +
                               "This indicates an invalid lifecycle state.");

                }
            }
    };

    @Override
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
        if (phase == Phase.AFTER_METHOD_INVOKE) {
            return Collections.singletonList(new ServerRestHandler() {
                @Override
                public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
                    //make sure that we are never restarted
                    requestContext.restart(AWOL, true);
                    requestContext.suspend(); //we never resume
                }
            });
        }
        return Collections.emptyList();

    }
}
