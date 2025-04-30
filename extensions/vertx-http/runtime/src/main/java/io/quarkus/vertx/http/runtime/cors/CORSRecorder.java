package io.quarkus.vertx.http.runtime.cors;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CORSRecorder {
    final VertxHttpConfig httpConfig;

    public CORSRecorder(VertxHttpConfig httpConfig) {
        this.httpConfig = httpConfig;
    }

    public Handler<RoutingContext> corsHandler() {
        if (httpConfig.corsEnabled()) {
            return new CORSFilter(httpConfig.cors());
        }
        return null;
    }

}
