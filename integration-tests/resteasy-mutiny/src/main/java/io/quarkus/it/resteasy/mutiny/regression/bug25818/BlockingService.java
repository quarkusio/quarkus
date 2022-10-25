package io.quarkus.it.resteasy.mutiny.regression.bug25818;

import jakarta.enterprise.context.ApplicationScoped;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

@ApplicationScoped
public class BlockingService {

    public String getBlocking() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Context context = Vertx.currentContext();
        if (context == null) {
            return "~~ context is null ~~";
        } else {
            return "hello-" + context.getLocal("hello-target");
        }
    }
}
