package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.HashMap;

import io.vertx.core.Context;
import io.vertx.ext.web.RoutingContext;

public class RequestMetric extends HashMap<String, Object> {
    static final String METRICS_CONTEXT = "HTTP_REQUEST_METRICS_CONTEXT";

    static final String HTTP_REQUEST_PATH = "HTTP_REQUEST_PATH";
    static final String HTTP_REQUEST_SAMPLE = "HTTP_REQUEST_SAMPLE";

    volatile RoutingContext routingContext;

    /**
     * Stash the RequestMetric in the Vertx Context
     * 
     * @param context Vertx context to store RequestMetric in
     * @param requestMetric
     * @see VertxMeterFilter
     */
    public static void setRequestMetric(Context context, RequestMetric requestMetric) {
        if (context != null) {
            context.put(METRICS_CONTEXT, requestMetric);
        }
    }

    /**
     * Retrieve and remove the RequestMetric from the Vertx Context
     * 
     * @param context
     * @return the RequestMetricContext stored in the Vertx Context, or null
     * @see VertxMeterFilter
     */
    public static RequestMetric retrieveRequestMetric(Context context) {
        if (context != null) {
            RequestMetric requestMetric = context.get(METRICS_CONTEXT);
            context.remove(METRICS_CONTEXT);
            return requestMetric;
        }
        return null;
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
}
