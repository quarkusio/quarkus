package io.quarkus.jfr.runtime.http.rest;

import io.quarkus.jfr.runtime.IdProducer;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;

@Dependent
public class RestRecorderProducer {

    @Context
    HttpServerRequest vertxRequest;

    @Context
    ResourceInfo resourceInfo;

    @Inject
    IdProducer idProducer;

    @Produces
    @RequestScoped
    public RestRecorder create() {
        String httpMethod = vertxRequest.method().name();
        String uri = vertxRequest.path();
        String resourceClass = resourceInfo.getResourceClass().getName();
        String resourceMethod = resourceInfo.getResourceMethod().toGenericString();
        String client = vertxRequest.remoteAddress().toString();

        return new RestReactiveRecorder(httpMethod, uri, resourceClass, resourceMethod, client, idProducer);
    }
}
