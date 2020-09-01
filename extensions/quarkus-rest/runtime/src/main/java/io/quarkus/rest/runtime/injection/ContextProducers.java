package io.quarkus.rest.runtime.injection;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.Sse;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSse;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

@Singleton
public class ContextProducers {

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

    private QuarkusRestRequestContext getContext() {
        return (QuarkusRestRequestContext) currentVertxRequest.getOtherHttpContextObject();
    }
}
