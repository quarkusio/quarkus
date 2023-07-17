package io.quarkus.resteasy.reactive.server.runtime.observability;

import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.ext.web.RoutingContext;

final class ObservabilityUtil {

    private ObservabilityUtil() {
    }

    static void setUrlPathTemplate(RoutingContext routingContext, String templatePath) {
        ((HttpServerRequestInternal) (routingContext.request())).context()
                .putLocal("UrlPathTemplate", templatePath);
    }
}
