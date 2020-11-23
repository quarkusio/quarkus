package io.quarkus.rest.server.runtime;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * Provides CDI producers for objects that can be injected via @Context
 * In quarkus-rest this works because @Context is considered an alias for @Inject
 * through the use of {@code AutoInjectAnnotationBuildItem}
 */
@Singleton
public class QuarkusContextProducers {

    // HttpServerRequest, HttpServerRequest are Vert.x types so it's not necessary to have it injectable via @Context,
    // however we do use it in the Quickstarts so let's make it work
    @RequestScoped
    @Produces
    HttpServerRequest httpServerRequest() {
        return CurrentRequestManager.get().serverRequest().unwrap(HttpServerRequest.class);
    }

    @RequestScoped
    @Produces
    HttpServerResponse httpServerResponse() {
        return CurrentRequestManager.get().serverRequest().unwrap(HttpServerResponse.class);
    }

    private ResteasyReactiveRequestContext getContext() {
        return CurrentRequestManager.get();
    }
}
