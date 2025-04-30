package io.quarkus.load.shedding.runtime;

import jakarta.inject.Singleton;

import io.quarkus.load.shedding.RequestClassifier;
import io.vertx.core.http.HttpServerRequest;

@Singleton
public class HttpRequestClassifier implements RequestClassifier<HttpServerRequest> {
    @Override
    public boolean appliesTo(Object request) {
        return request instanceof HttpServerRequest;
    }

    @Override
    public int cohort(HttpServerRequest request) {
        int hour = (int) (System.currentTimeMillis() >> 22); // roughly 1 hour
        String host = request.remoteAddress().hostAddress();
        if (host == null) {
            host = "";
        }
        return hour + host.hashCode();
    }
}
