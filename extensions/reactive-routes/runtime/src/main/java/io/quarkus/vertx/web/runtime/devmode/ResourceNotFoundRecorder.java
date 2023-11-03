package io.quarkus.vertx.web.runtime.devmode;

import java.util.List;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;
import io.vertx.ext.web.Router;

@Recorder
public class ResourceNotFoundRecorder {

    public void registerNotFoundHandler(RuntimeValue<Router> router, String httpRoot, List<RouteDescription> reactiveRoutes,
            List<String> additionalEndpoints) {
        router.getValue().errorHandler(404, new ResourceNotFoundHandler(httpRoot, reactiveRoutes, additionalEndpoints));
    }

}
