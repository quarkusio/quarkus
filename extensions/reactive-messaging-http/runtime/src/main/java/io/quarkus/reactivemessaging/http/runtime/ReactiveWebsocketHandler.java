package io.quarkus.reactivemessaging.http.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 27/09/2019
 */
public class ReactiveWebsocketHandler implements Handler<RoutingContext> {
    private final ReactiveHttpHandlerBean handler;

    public ReactiveWebsocketHandler(ReactiveHttpHandlerBean handler) {
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext event) {
        handler.handleWebsocket(event);
    }
}
