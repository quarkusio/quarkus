package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

public class VertxMeterBinderContainerFilter implements ContainerRequestFilter {
    private static final Logger log = Logger.getLogger(VertxMeterBinderContainerFilter.class);

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        RoutingContext routingContext = CDI.current().select(CurrentVertxRequest.class).get().getCurrent();

        // bail early if we have no routing context, or if path munging isn't necessary
        if (routingContext == null || routingContext.get(RequestMetric.HTTP_REQUEST_PATH_MATCHED) != null) {
            return;
        }

        UriInfo info = requestContext.getUriInfo();
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
