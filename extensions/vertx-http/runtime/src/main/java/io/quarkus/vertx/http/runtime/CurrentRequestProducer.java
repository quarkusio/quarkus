package io.quarkus.vertx.http.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Note that we intentionally put the producer in a separate class so that it's possible to exclude the bean if a naming
 * conflict exists.
 */
@Singleton
public class CurrentRequestProducer {

    @Named("vertxRequest")
    @Produces
    @RequestScoped
    public HttpServerRequest getCurrentRequest(RoutingContext rc) {
        return rc.request();
    }

}
