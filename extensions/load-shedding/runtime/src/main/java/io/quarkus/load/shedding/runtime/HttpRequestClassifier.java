package io.quarkus.load.shedding.runtime;

import jakarta.inject.Singleton;

import io.quarkus.load.shedding.RequestClassifier;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class HttpRequestClassifier implements RequestClassifier<RoutingContext> {
    @Override
    public boolean appliesTo(Object request) {
        return request instanceof RoutingContext;
    }

    @Override
    public int cohort(RoutingContext request) {
        int hour = (int) (System.currentTimeMillis() >> 22); // roughly 1 hour
        String host = request.request().remoteAddress().hostAddress();
        if (host == null) {
            host = "";
        }
        return hour + host.hashCode();
    }
}
