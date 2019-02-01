package org.jboss.shamrock.jwt.runtime.auth;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.shamrock.jwt.runtime.MPJWTProducer;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 8/13/18
 */
public class MpJwtPrincipalCleanupHandler implements HttpHandler {

    private final HttpHandler next;

    public MpJwtPrincipalCleanupHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            next.handleRequest(exchange);
        } finally {
            MPJWTProducer.setJWTPrincipal(null);
        }
    }
}
