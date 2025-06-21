package io.quarkus.vertx.http.runtime.cors;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CORSRecorder {
    private final RuntimeValue<VertxHttpConfig> httpConfig;

    public CORSRecorder(RuntimeValue<VertxHttpConfig> httpConfig) {
        this.httpConfig = httpConfig;
    }

    public Handler<RoutingContext> corsHandler() {
        if (httpConfig.getValue().corsEnabled()) {
            return new CORSFilter(httpConfig.getValue().cors());
        }
        return null;
    }

}
