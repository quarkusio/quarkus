package io.quarkus.devui.runtime;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * Alternative Dev UI Json RPC communication using Server-Sent Events (SSE)
 */
public class DevUIServerSentEvents implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(DevUIServerSentEvents.class.getName());

    @Override
    public void handle(RoutingContext ctx) {
        LOG.info(ctx.request().method().name() + " : " + ctx.request().absoluteURI());

        if (ctx.request().method().equals(HttpMethod.GET)) {

            ctx.response()
                    .putHeader("Content-Type", "text/event-stream; charset=utf-8")
                    .putHeader("Cache-Control", "no-cache")
                    .putHeader("Connection", "keep-alive")
                    .setChunked(true);

            try {
                JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
                jsonRpcRouter.addSseSession(ctx);
            } catch (IllegalStateException e) {
                LOG.debug("Failed to connect to dev sse server", e);
                ctx.response().end();
            }
        }

    }
}