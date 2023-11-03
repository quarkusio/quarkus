package io.quarkus.vertx.http.runtime.cors;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CORSRecorder {
    final HttpConfiguration configuration;

    public CORSRecorder(HttpConfiguration configuration) {
        this.configuration = configuration;
    }

    public Handler<RoutingContext> corsHandler() {
        if (configuration.corsEnabled) {
            return new CORSFilter(configuration.cors);
        }
        return null;
    }

}
