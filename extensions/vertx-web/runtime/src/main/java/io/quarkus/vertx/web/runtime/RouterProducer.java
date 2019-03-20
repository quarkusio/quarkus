package io.quarkus.vertx.web.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.ext.web.Router;

@Singleton
public class RouterProducer {

    private volatile Router router;

    void initialize(Router router) {
        this.router = router;
    }

    // Note that we need a client proxy because if a bean also @Observes Router a null value would be injected 
    @ApplicationScoped
    @Produces
    Router produceRouter() {
        return router;
    }

}
