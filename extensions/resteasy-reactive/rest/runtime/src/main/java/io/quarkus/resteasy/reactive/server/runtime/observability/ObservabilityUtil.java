package io.quarkus.resteasy.reactive.server.runtime.observability;

import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.ext.web.RoutingContext;

final class ObservabilityUtil {

    private ObservabilityUtil() {
    }

    static void setUrlPathTemplate(RoutingContext routingContext, String templatePath) {
        if (VertxContext.isOnDuplicatedContext()) {
            ContextLocals.put("UrlPathTemplate", templatePath);
        } else {
            routingContext.put("UrlPathTemplate", templatePath);
        }
    }

    static String getUrlPathTemplate(RoutingContext routingContext) {
        if (VertxContext.isOnDuplicatedContext()) {
            return ContextLocals.<String> get("UrlPathTemplate").orElse(null);
        }
        return routingContext.get("UrlPathTemplate");
    }
}
