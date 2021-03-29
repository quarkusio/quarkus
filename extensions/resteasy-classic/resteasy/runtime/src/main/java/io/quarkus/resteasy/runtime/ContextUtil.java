package io.quarkus.resteasy.runtime;

import java.util.Map;

import org.jboss.resteasy.core.ResteasyContext;

import io.quarkus.vertx.http.runtime.QuarkusHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class ContextUtil {
    /**
     * Used to push context objects from virtual http plugins like AWS Lambda and Azure Functions.
     * We keep this code here because its used in multiple places.
     *
     * @param routingContext
     */
    public static void pushContext(RoutingContext routingContext) {
        MultiMap qheaders = routingContext.request().headers();
        if (qheaders instanceof QuarkusHttpHeaders) {
            for (Map.Entry<Class<?>, Object> entry : ((QuarkusHttpHeaders) qheaders).getContextObjects().entrySet()) {
                ResteasyContext.pushContext((Class) entry.getKey(), entry.getValue());
            }
        }
    }
}
