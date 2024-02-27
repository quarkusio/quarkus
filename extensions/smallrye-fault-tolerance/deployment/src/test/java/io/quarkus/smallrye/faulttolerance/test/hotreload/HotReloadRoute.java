package io.quarkus.smallrye.faulttolerance.test.hotreload;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.vertx.ext.web.Router;

@Singleton
public class HotReloadRoute {
    public void route(@Observes Router router, HotReloadBean bean) {
        router.get("/").handler(ctx -> {
            ctx.response().setStatusCode(200).end(bean.hello());
        });
    }
}
