package io.quarkus.smallrye.health.runtime;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class SmallRyeHealthRecorder {

    public void registerHealthCheckResponseProvider(Class<? extends HealthCheckResponseProvider> providerClass) {
        try {
            HealthCheckResponse.setResponseProvider(providerClass.getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to instantiate service " + providerClass + " using the no-arg constructor.");
        }
    }

    public Handler<RoutingContext> uiHandler(String healthUiFinalDestination, String healthUiPath) {

        StaticHandler staticHandler = StaticHandler.create().setAllowRootFileSystemAccess(true)
                .setWebRoot(healthUiFinalDestination)
                .setDefaultContentEncoding("UTF-8");

        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.normalisedPath().length() == healthUiPath.length()) {

                    event.response().setStatusCode(302);
                    event.response().headers().set(HttpHeaders.LOCATION, healthUiPath + "/");
                    event.response().end();
                    return;
                } else if (event.normalisedPath().length() == healthUiPath.length() + 1) {
                    event.reroute(healthUiPath + "/index.html");
                    return;
                }

                staticHandler.handle(event);
            }
        };
    }
}
