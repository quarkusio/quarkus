package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;

import io.vertx.ext.web.RoutingContext;

final class VertxMeterBinderContainerFilterUtil {

    private VertxMeterBinderContainerFilterUtil() {
    }

    private static final Logger log = Logger.getLogger(VertxMeterBinderRestEasyContainerFilter.class);

    static HttpRequestMetric doFilter(RoutingContext routingContext, UriInfo info) {
        HttpRequestMetric metric = HttpRequestMetric.getRequestMetric(routingContext);
        String path = info.getPath();

        MultivaluedMap<String, String> pathParameters = info.getPathParameters();
        if (!pathParameters.isEmpty()) {
            // Replace parameter values in the URI with {key}: /items/123 -> /items/{id}
            for (Map.Entry<String, List<String>> entry : pathParameters.entrySet()) {
                for (String value : entry.getValue()) {
                    path = path.replace(value, "{" + entry.getKey() + "}");
                }
            }
            log.debugf("Saving parameterized path %s in %s", path, routingContext);
            metric.setTemplatePath(path);
        }
        return metric;
    }
}
