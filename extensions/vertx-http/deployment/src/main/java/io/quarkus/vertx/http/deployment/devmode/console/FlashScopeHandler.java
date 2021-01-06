package io.quarkus.vertx.http.deployment.devmode.console;

import io.quarkus.devconsole.runtime.spi.FlashScopeUtil;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FlashScopeHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        FlashScopeUtil.handleFlashCookie(event);
        event.next();
    }

}
