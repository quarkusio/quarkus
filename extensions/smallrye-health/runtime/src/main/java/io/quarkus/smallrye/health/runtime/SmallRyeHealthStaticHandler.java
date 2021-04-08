package io.quarkus.smallrye.health.runtime;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Handling static Health UI content
 */
public class SmallRyeHealthStaticHandler implements Handler<RoutingContext> {

    private String healthUiFinalDestination;
    private String healthUiPath;

    public SmallRyeHealthStaticHandler() {
    }

    public SmallRyeHealthStaticHandler(String healthUiFinalDestination, String healthUiPath) {
        this.healthUiFinalDestination = healthUiFinalDestination;
        this.healthUiPath = healthUiPath;
    }

    public String getHealthUiFinalDestination() {
        return healthUiFinalDestination;
    }

    public void setHealthUiFinalDestination(String healthUiFinalDestination) {
        this.healthUiFinalDestination = healthUiFinalDestination;
    }

    public String getHealthUiPath() {
        return healthUiPath;
    }

    public void setHealthUiPath(String healthUiPath) {
        this.healthUiPath = healthUiPath;
    }

    @Override
    public void handle(RoutingContext event) {
        StaticHandler staticHandler = StaticHandler.create().setAllowRootFileSystemAccess(true)
                .setWebRoot(healthUiFinalDestination)
                .setDefaultContentEncoding("UTF-8");

        if (event.normalizedPath().length() == healthUiPath.length()) {
            event.response().setStatusCode(302);
            event.response().headers().set(HttpHeaders.LOCATION, healthUiPath + "/");
            event.response().end();
            return;
        } else if (event.normalizedPath().length() == healthUiPath.length() + 1) {
            event.reroute(healthUiPath + "/index.html");
            return;
        }

        staticHandler.handle(event);
    }

}
