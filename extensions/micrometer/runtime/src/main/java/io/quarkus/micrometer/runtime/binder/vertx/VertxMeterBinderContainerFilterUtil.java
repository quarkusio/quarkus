package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import io.vertx.ext.web.RoutingContext;

final class VertxMeterBinderContainerFilterUtil {

    private VertxMeterBinderContainerFilterUtil() {
    }

    static void doFilter(RoutingContext routingContext, UriInfo info) {

        String path = info.getPath();
        MultivaluedMap<String, String> pathParameters = info.getPathParameters();
        if (!pathParameters.isEmpty()) {
            // Replace parameter values in the URI with {key}: /items/123 -> /items/{id}
            for (Map.Entry<String, List<String>> entry : pathParameters.entrySet()) {
                for (String value : entry.getValue()) {
                    path = path.replace(value, "{" + entry.getKey() + "}");
                }
            }

            HttpRequestMetric.getRequestMetric(routingContext).setTemplatePath(path);
        }
    }
}
