package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.HashMap;

import io.vertx.core.Context;
import io.vertx.ext.web.RoutingContext;

public class MetricsContext extends HashMap<String, Object> {
    static final String METRICS_CONTEXT = "METRICS_CONTEXT";

    static final String HTTP_REQUEST_PATH = "HTTP_REQUEST_PATH";
    static final String HTTP_REQUEST_SAMPLE = "HTTP_REQUEST_SAMPLE";

    final Context vertxContext;
    volatile RoutingContext routingContext;

    static MetricsContext addMetricsContext(Context vertxContext) {
        MetricsContext ctx = new MetricsContext(vertxContext);
        vertxContext.put(METRICS_CONTEXT, ctx);
        return ctx;
    }

    private MetricsContext(Context vertxContext) {
        this.vertxContext = vertxContext;
    }

    public void removeMetricsContext() {
        vertxContext.remove(METRICS_CONTEXT);
    }

    <T> T getFromRoutingContext(String key) {
        if (routingContext != null) {
            return routingContext.get(key);
        }
        return null;
    }

    <T> T getValue(String key) {
        Object o = this.get(key);
        return o == null ? null : (T) o;
    }

    public static void addRoutingContext(Context context, RoutingContext event) {
        MetricsContext metricsContext = context.get(METRICS_CONTEXT);
        if (metricsContext != null) {
            metricsContext.routingContext = event;
        }
    }

    public static void removeRoutingContext(Context context) {
        MetricsContext metricsContext = context.get(METRICS_CONTEXT);
        if (metricsContext != null) {
            metricsContext.routingContext = null;
        }
    }
}
