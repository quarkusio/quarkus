package io.quarkus.jfr.runtime.http.rest;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;

import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import io.quarkus.jfr.runtime.IdProducer;
import io.vertx.core.http.HttpServerRequest;

@Dependent
public class ReactiveServerRecorderProducer {

    @Context
    HttpServerRequest vertxRequest;

    @Context
    SimpleResourceInfo resourceInfo;

    @Inject
    IdProducer idProducer;

    @Produces
    @RequestScoped
    public Recorder create() {
        String httpMethod = vertxRequest.method().name();
        String uri = vertxRequest.path();
        Class<?> resourceClass = resourceInfo.getResourceClass();
        String resourceClassName = (resourceClass == null) ? null : resourceClass.getName();
        String resourceMethodName = resourceInfo.getMethodName();
        String client = vertxRequest.remoteAddress().toString();

        return new ServerRecorder(httpMethod, uri, resourceClassName, resourceMethodName, client, idProducer);
    }
}
