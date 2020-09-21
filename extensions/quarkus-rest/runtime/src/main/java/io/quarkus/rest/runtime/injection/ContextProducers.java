package io.quarkus.rest.runtime.injection;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.Sse;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSse;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * Provides CDI producers for objects that can be injected via @Context
 * In quarkus-rest this works because @Context is considered an alias for @Inject
 * through the use of {@code AutoInjectAnnotationBuildItem}
 */
@Singleton
public class ContextProducers {

    /**
     * TODO: Remove this, stops the warning about JSONB being removed
     */
    @Inject
    Jsonb jsonb;

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @RequestScoped
    @Produces
    UriInfo uriInfo() {
        return getContext().getUriInfo();
    }

    @RequestScoped
    @Produces
    HttpHeaders headers() {
        return getContext().getHttpHeaders();
    }

    @Singleton
    @Produces
    Sse sse() {
        return QuarkusRestSse.INSTANCE;
    }

    @RequestScoped
    @Produces
    Request request() {
        return getContext().getRequest();
    }

    // HttpServerRequest, HttpServerRequest are Vert.x types so it's not necessary to have it injectable via @Context,
    // however we do use it in the Quickstarts so let's make it work

    @RequestScoped
    @Produces
    HttpServerRequest httpServerRequest() {
        return currentVertxRequest.getCurrent().request();
    }

    @RequestScoped
    @Produces
    HttpServerResponse httpServerResponse() {
        return currentVertxRequest.getCurrent().response();
    }

    @Singleton
    @Produces
    Providers providers() {
        return getContext().getProviders();
    }

    @RequestScoped
    @Produces
    ResourceInfo resourceInfo() {
        return getContext().getTarget().getLazyMethod();
    }

    private QuarkusRestRequestContext getContext() {
        return (QuarkusRestRequestContext) currentVertxRequest.getOtherHttpContextObject();
    }
}
