package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.Context;
import io.vertx.ext.web.RoutingContext;

public class RequestMetric {
    static final String METRICS_CONTEXT = "HTTP_REQUEST_METRICS_CONTEXT";
    static final String HTTP_REQUEST_PATH = "HTTP_REQUEST_PATH";
    static final String HTTP_REQUEST_PATH_MATCHED = "HTTP_REQUEST_MATCHED_PATH";
    static final Pattern VERTX_ROUTE_PARAM = Pattern.compile("^:(.*)$");

    /** Cache of vert.x resolved paths: /item/:id --> /item/{id} */
    final static ConcurrentHashMap<String, String> vertxRoutePath = new ConcurrentHashMap<>();

    volatile RoutingContext routingContext;

    /** Do not measure requests until/unless a uri path is set */
    boolean measure = false;

    /** URI path used as a tag value for non-error requests */
    String path;

    /** True IFF the path was revised by a matcher expression */
    boolean pathMatched = false;

    /** Store the sample used to measure the request */
    Timer.Sample sample;

    /**
     * Store the tags associated with the request (change 1.6.0).
     * Default is empty, value assigned @ requestBegin
     */
    Tags tags = Tags.empty();

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

    String getHttpRequestPath() {
        // Vertx binder configuration, see VertxMetricsTags
        if (pathMatched) {
            return path;
        }
        if (routingContext != null) {
            // JAX-RS or Servlet container filter
            String rcPath = routingContext.get(HTTP_REQUEST_PATH);
            if (rcPath != null) {
                return rcPath;
            }
            // vertx-web or reactive route
            String matchedPath = routingContext.currentRoute().getPath();
            if (matchedPath != null) {
                if (matchedPath.contains(":")) {
                    // Convert /item/:id to /item/{id} and save it for next time
                    matchedPath = vertxRoutePath.computeIfAbsent(matchedPath, k -> {
                        String segments[] = k.split("/");
                        for (int i = 0; i < segments.length; i++) {
                            segments[i] = VERTX_ROUTE_PARAM.matcher(segments[i]).replaceAll("{$1}");
                        }
                        return String.join("/", segments);
                    });
                }
                return matchedPath;
            }
        }
        return path;
    }
}
