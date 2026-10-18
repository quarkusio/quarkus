package io.quarkus.jfr.runtime.internal.http.rest.reactive;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.jfr.api.IdProducer;
import io.vertx.core.http.HttpServerRequest;

public class ReactiveServerEventRecorderProducer {

    @RequestScoped
    public ReactiveServerEventRecorder create(IdProducer idProducer, HttpServerRequest vertxRequest) {
        String httpMethod = vertxRequest.method().name();
        String uri = vertxRequest.path();
        String client = vertxRequest.remoteAddress().toString();

        return new ReactiveServerEventRecorder(new RequestInfo(httpMethod, uri, client), idProducer);
    }
}
