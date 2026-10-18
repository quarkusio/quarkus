package io.quarkus.vertx.web.runtime;

import java.util.function.Consumer;

import io.vertx.ext.web.RoutingContext;

public final class UniFailureCallback implements Consumer<Throwable> {

    private final RoutingContext context;

    public UniFailureCallback(RoutingContext context) {
        this.context = context;
    }

    @Override
    public void accept(Throwable t) {
        context.fail(t);
    }

}
