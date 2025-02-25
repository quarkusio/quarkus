package io.quarkus.jfr.runtime.http.rest.classic;

import java.lang.reflect.Method;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;

import io.quarkus.jfr.runtime.IdProducer;
import io.vertx.core.http.HttpServerRequest;

@Dependent
public class ClassicServerRecorderProducer {

    @Inject
    HttpServerRequest vertxRequest;

    @Inject
    ResourceInfo resourceInfo;

    @Inject
    IdProducer idProducer;

    @Produces
    @RequestScoped
    public ClassicServerRecorder create() {
        String httpMethod = vertxRequest.method().name();
        String uri = vertxRequest.path();
        Class<?> resourceClass = resourceInfo.getResourceClass();
        String resourceClassName = (resourceClass == null) ? null : resourceClass.getName();
        Method resourceMethod = resourceInfo.getResourceMethod();
        String resourceMethodName = (resourceMethod == null) ? null : resourceMethod.getName();
        String client = vertxRequest.remoteAddress().toString();

        return new ClassicServerRecorder(httpMethod, uri, resourceClassName, resourceMethodName, client, idProducer);
    }
}
