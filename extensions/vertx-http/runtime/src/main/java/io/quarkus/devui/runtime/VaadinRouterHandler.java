package io.quarkus.devui.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler to enable Vaadin router.
 */
public class VaadinRouterHandler implements Handler<RoutingContext> {
    private String basePath;

    public VaadinRouterHandler() {

    }

    public VaadinRouterHandler(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void handle(RoutingContext event) {
        if (event.normalizedPath().startsWith(basePath)) {
            event.reroute(basePath + "/index.html");
            return;
        }
        event.next();
    }
}