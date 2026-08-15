package io.quarkus.it.resteasy.mutiny.regression.bug25818;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.common.vertx.ContextLocals;
import io.vertx.core.Vertx;

@ApplicationScoped
public class BlockingService {

    public String getBlocking() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (Vertx.currentContext() == null) {
            return "~~ context is null ~~";
        } else {
            return "hello-" + ContextLocals.<String> get("hello-target", null);
        }
    }
}
