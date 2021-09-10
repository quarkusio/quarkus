package io.quarkus.vertx.web.runtime;

import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.ext.web.RoutingContext;

public class RoutingExchangeImpl implements RoutingExchange {

    private final RoutingContext context;

    public RoutingExchangeImpl(RoutingContext context) {
        this.context = context;
    }

    @Override
    public RoutingContext context() {
        return context;
    }
}
