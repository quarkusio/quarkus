package io.quarkus.jfr.runtime.http.rest.reactive;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.jfr.runtime.IdProducer;
import io.vertx.core.http.HttpServerRequest;

public class ReactiveServerRecorderProducer {

    @RequestScoped
    public ReactiveServerRecorder create(IdProducer idProducer, HttpServerRequest vertxRequest) {
        String httpMethod = vertxRequest.method().name();
        String uri = vertxRequest.path();
        String client = vertxRequest.remoteAddress().toString();

        return new ReactiveServerRecorder(new RequestInfo(httpMethod, uri, client), idProducer);
    }
}
