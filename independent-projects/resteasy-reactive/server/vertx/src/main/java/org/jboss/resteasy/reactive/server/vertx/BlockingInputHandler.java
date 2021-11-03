package org.jboss.resteasy.reactive.server.vertx;

import io.vertx.ext.web.RoutingContext;
import java.time.Duration;
import javax.ws.rs.HttpMethod;
import org.jboss.resteasy.reactive.common.util.EmptyInputStream;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;

/**
 * Handler that reads data and sets up the input stream and blocks until the data has been read.
 * This is meant to be used only when the request processing has already been offloaded to a worker thread.
 */
public class BlockingInputHandler implements RuntimeConfigurableServerRestHandler {

    private volatile Duration timeout;

    @Override
    public void configure(RuntimeConfiguration configuration) {
        timeout = configuration.readTimeout();
    }

    @Override
    public void handle(ResteasyReactiveRequestContext context) throws Exception {
        // in some cases, with sub-resource locators or via request filters, 
        // it's possible we've already read the entity
        if (context.getInputStream() != EmptyInputStream.INSTANCE) {
            // let's not set it twice
            return;
        }
        if (context.serverRequest().getRequestMethod().equals(HttpMethod.GET) ||
                context.serverRequest().getRequestMethod().equals(HttpMethod.HEAD)) {
            return;
        }
        if (context instanceof VertxResteasyReactiveRequestContext) {
            //TODO: this should not be installed for servlet
            VertxResteasyReactiveRequestContext vertxContext = (VertxResteasyReactiveRequestContext) context;
            RoutingContext routingContext = vertxContext.getContext();
            vertxContext.setInputStream(
                    new VertxInputStream(routingContext, timeout.toMillis(), (VertxResteasyReactiveRequestContext) context));
        }
    }
}
