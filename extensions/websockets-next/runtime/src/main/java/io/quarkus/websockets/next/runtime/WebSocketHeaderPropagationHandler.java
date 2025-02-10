package io.quarkus.websockets.next.runtime;

import static io.quarkus.websockets.next.HandshakeRequest.SEC_WEBSOCKET_PROTOCOL;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Filter used to propagate WebSocket subprotocols as the WebSocket opening handshake headers.
 * This class is not part of public API and can change at any time.
 */
public final class WebSocketHeaderPropagationHandler implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(WebSocketHeaderPropagationHandler.class);
    private static final String QUARKUS_HTTP_UPGRADE_PROTOCOL = "quarkus-http-upgrade";
    private static final String HEADER_SEPARATOR = "#";

    public WebSocketHeaderPropagationHandler() {
    }

    @Override
    public void handle(RoutingContext routingContext) {
        String webSocketProtocols = routingContext.request().headers().get(SEC_WEBSOCKET_PROTOCOL);
        if (webSocketProtocols != null && webSocketProtocols.contains(QUARKUS_HTTP_UPGRADE_PROTOCOL)) {
            // this implementation expects that there is exactly one header and protocols are separated by a comma
            // specs allows to also have multiple headers, but I couldn't reproduce it (hence test it) with
            // the JS client or Vert.x client and this is feature exists to support the JS client
            routingContext.request().headers().remove(SEC_WEBSOCKET_PROTOCOL);
            StringBuilder otherProtocols = null;
            for (String protocol : webSocketProtocols.split(",")) {
                protocol = protocol.trim();
                if (protocol.startsWith(QUARKUS_HTTP_UPGRADE_PROTOCOL)) {
                    protocol = URLDecoder.decode(protocol, StandardCharsets.UTF_8);
                    String[] headerNameToValue = protocol.split(HEADER_SEPARATOR);
                    if (headerNameToValue.length != 3) {
                        failRequest(routingContext,
                                "Quarkus header format is incorrect. Expected format is: quarkus-http-upgrade#header-name#header-value");
                        return;
                    }
                    routingContext.request().headers().add(headerNameToValue[1], headerNameToValue[2]);
                } else {
                    if (otherProtocols == null) {
                        otherProtocols = new StringBuilder(protocol);
                    } else {
                        otherProtocols.append(",").append(protocol);
                    }
                }
            }
            if (otherProtocols == null) {
                failRequest(routingContext,
                        """
                                WebSocket opening handshake header '%s' only contains '%s' subprotocol.
                                Client expects that the WebSocket server agreed to serve exactly one of offered subprotocols.
                                Please add one of protocols configured with the 'quarkus.websockets-next.server.supported-subprotocols' configuration property.
                                """
                                .formatted(SEC_WEBSOCKET_PROTOCOL, QUARKUS_HTTP_UPGRADE_PROTOCOL));
                return;
            } else {
                routingContext.request().headers().add(SEC_WEBSOCKET_PROTOCOL, otherProtocols);
            }
        }
        routingContext.next();
    }

    private static void failRequest(RoutingContext routingContext, String exceptionMessage) {
        // this is also logged as some clients may not show response body
        LOG.error(exceptionMessage);
        routingContext.fail(500, new IllegalArgumentException(exceptionMessage));
    }
}
