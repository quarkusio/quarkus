package io.quarkus.vertx.http.runtime;

import java.util.function.Supplier;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ThreadLocalHandler implements Handler<RoutingContext> {

    private final ThreadLocal<Handler<RoutingContext>> threadLocal;

    public ThreadLocalHandler(Supplier<Handler<RoutingContext>> supplier) {
        threadLocal = new ThreadLocal<Handler<RoutingContext>>() {
            @Override
            protected Handler<RoutingContext> initialValue() {
                return supplier.get();
            }
        };
    }

    @Override
    public void handle(RoutingContext event) {
        threadLocal.get().handle(event);
    }
}
