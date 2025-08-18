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

    public Handler<RoutingContext> corsHandler(RuntimeValue<CORSConfig> programmaticCorsConfig) {
        final CORSConfig corsConfig;
        if (programmaticCorsConfig != null && programmaticCorsConfig.getValue() != null) {
            corsConfig = programmaticCorsConfig.getValue();
        } else {
            corsConfig = httpConfig.getValue().cors();
        }
        if (corsConfig.enabled()) {
            return new CORSFilter(corsConfig);
        }
        return null;
    }

}
