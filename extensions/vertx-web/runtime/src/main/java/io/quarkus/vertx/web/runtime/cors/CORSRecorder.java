package io.quarkus.vertx.web.runtime.cors;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.web.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CORSRecorder {

    public Handler<RoutingContext> corsHandler(HttpConfiguration configuration) {
        if (configuration.corsEnabled) {
            return new CORSFilter(configuration.cors);
        }
        return null;
    }

}
