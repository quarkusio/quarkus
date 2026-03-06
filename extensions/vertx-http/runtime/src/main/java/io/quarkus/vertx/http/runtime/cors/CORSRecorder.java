package io.quarkus.vertx.http.runtime.cors;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CORSRecorder {
    private final RuntimeValue<VertxHttpConfig> httpConfig;

    public CORSRecorder(RuntimeValue<VertxHttpConfig> httpConfig) {
        this.httpConfig = httpConfig;
    }

    public Handler<RoutingContext> corsHandler(RuntimeValue<CORSConfig> programmaticCorsConfig, boolean securityPresent) {
        final CORSConfig corsConfig;
        if (programmaticCorsConfig != null && programmaticCorsConfig.getValue() != null) {
            corsConfig = programmaticCorsConfig.getValue();
        } else {
            corsConfig = httpConfig.getValue().cors();
        }
        if (corsConfig.enabled()) {
            if (!corsConfig.returnExactOrigins()) {
                if (!CORSFilter.isOriginConfiguredWithWildcard(corsConfig.origins())) {
                    throw new ConfigurationException(
                            "'quarkus.http.cors.return-exact-origins=false' can only be used when "
                                    + "'quarkus.http.cors.origins' is set to '*'.");
                }
                if (corsConfig.accessControlAllowCredentials().orElse(false)) {
                    throw new ConfigurationException(
                            "'quarkus.http.cors.return-exact-origins=false' is incompatible with "
                                    + "'quarkus.http.cors.access-control-allow-credentials=true' because the CORS "
                                    + "specification does not allow credentials when 'Access-Control-Allow-Origin' "
                                    + "is '*'.");
                }
                if (securityPresent) {
                    throw new ConfigurationException(
                            "'quarkus.http.cors.return-exact-origins=false' can't be used when "
                                    + "a Quarkus Security capability is detected.");
                }
            }
            return new CORSFilter(corsConfig);
        }
        return null;
    }

}
