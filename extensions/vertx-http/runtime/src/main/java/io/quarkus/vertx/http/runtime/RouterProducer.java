package io.quarkus.vertx.http.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.ext.web.Router;

@Singleton
public class RouterProducer {

    private volatile Router router;
    private volatile io.vertx.mutiny.ext.web.Router mutinyRouter;

    void initialize(Router router, io.vertx.mutiny.ext.web.Router mutinyRouter) {
        this.router = router;
        this.mutinyRouter = mutinyRouter;
    }

    // Note that we need a client proxy because if a bean also @Observes Router a null value would be injected 
    @ApplicationScoped
    @Produces
    Router produceRouter() {
        return router;
    }

    @Produces
    io.vertx.mutiny.ext.web.Router produceMutinyRouter() {
        return mutinyRouter;
    }

}
