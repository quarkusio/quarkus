package io.quarkus.smallrye.health.runtime;

import java.util.function.Function;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

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

    public Function<Router, Route> route(String name) {
        return new Function<Router, Route>() {
            @Override
            public Route apply(Router router) {
                return router.route(name);
            }
        };
    }

}
