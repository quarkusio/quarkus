package io.quarkus.devui.runtime;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * Alternative Json RPC communication using Streamable HTTP for MCP
 */
public class MCPStreamableHTTPHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(MCPStreamableHTTPHandler.class.getName());

    @Override
    public void handle(RoutingContext ctx) {
        LOG.info(ctx.request().method().name() + " : " + ctx.request().absoluteURI() + "  |  Accept header =  ["
                + ctx.request().headers().get("Accept") + "]");

        // TODO:
        // Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks
        // When running locally, servers SHOULD bind only to localhost (127.0.0.1) rather than all network interfaces (0.0.0.0)
        // Servers SHOULD implement proper authentication for all connections

        //The client MUST use HTTP POST to send JSON-RPC messages to the MCP endpoint.
        //The client MUST include an Accept header, listing both application/json and text/event-stream as supported content types.
        // The body of the POST request MUST be a single JSON-RPC request, notification, or response.
        if (ctx.request().method().equals(HttpMethod.GET)) { // TODO: Also check Accept header
            handleSSEInitRequest(ctx);
        } else if (ctx.request().method().equals(HttpMethod.POST)) { // TODO: Also check Accept header
            handleMCPJsonRPCRequest(ctx);
        }

    }

    private void handleSSEInitRequest(RoutingContext ctx) {
        // TODO: Add SSE Support
        // The client MAY issue an HTTP GET to the MCP endpoint.
        // This can be used to open an SSE stream, allowing the server to communicate to the client,
        // without the client first sending data via HTTP POST.
        // The client MUST include an Accept header, listing text/event-stream as a supported content type.

        //        ctx.response()
        //                .putHeader("Content-Type", "text/event-stream; charset=utf-8")
        //                .putHeader("Cache-Control", "no-cache")
        //                .putHeader("Connection", "keep-alive")
        //                .setChunked(true);
        //
        //        try {
        //            JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
        //            jsonRpcRouter.addSseSession(ctx);
        //        } catch (IllegalStateException e) {
        //            LOG.debug("Failed to connect to dev sse server", e);
        //            ctx.response().end();
        //        }

        // The server MUST either return Content-Type: text/event-stream in response to this HTTP GET,
        // or else return HTTP 405 Method Not Allowed, indicating that the server does not offer an SSE stream at this endpoint.

        ctx.response()
                .setStatusCode(405)
                .putHeader("Allow", "POST")
                .putHeader("Content-Type", "text/plain")
                .end("Method Not Allowed");

    }

    private void handleMCPJsonRPCRequest(RoutingContext ctx) {
        JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
        jsonRpcRouter.handlePost(ctx);

    }
}
