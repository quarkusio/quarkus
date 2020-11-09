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

    static void doFilter(RoutingContext routingContext, UriInfo info) {
        // bail early if we have no routing context, or if path munging isn't necessary
        if (routingContext == null || routingContext.get(RequestMetric.HTTP_REQUEST_PATH_MATCHED) != null) {
            return;
        }

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
        }

        routingContext.put(RequestMetric.HTTP_REQUEST_PATH, path);
    }
}
