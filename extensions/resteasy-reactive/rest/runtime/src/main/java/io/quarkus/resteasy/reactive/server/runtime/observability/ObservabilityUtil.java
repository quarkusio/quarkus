package io.quarkus.resteasy.reactive.server.runtime.observability;

import io.smallrye.common.vertx.ContextLocals;
import io.vertx.ext.web.RoutingContext;

final class ObservabilityUtil {

    private ObservabilityUtil() {
    }

    static void setUrlPathTemplate(RoutingContext routingContext, String templatePath) {
        ContextLocals.put("UrlPathTemplate", templatePath);
    }

    static String getUrlPathTemplate(RoutingContext routingContext) {
        return ContextLocals.<String> get("UrlPathTemplate").orElse(null);
    }
}
