package io.quarkus.resteasy.reactive.server.runtime.observability;

import io.vertx.core.Context;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.ext.web.RoutingContext;

final class ObservabilityUtil {

    private ObservabilityUtil() {
    }

    static void setUrlPathTemplate(RoutingContext routingContext, String templatePath) {
        getRequestContext(routingContext).putLocal("UrlPathTemplate", templatePath);
    }

    static String getUrlPathTemplate(RoutingContext routingContext) {
        return getRequestContext(routingContext).getLocal("UrlPathTemplate");
    }

    private static Context getRequestContext(RoutingContext routingContext) {
        return ((HttpServerRequestInternal) (routingContext.request())).context();
    }
}
