package io.quarkus.csrf.reactive.runtime;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.getProgrammaticCsrfConfig;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;

@ApplicationScoped
public class RestCsrfConfigHolder {

    private final RestCsrfConfig config;

    RestCsrfConfigHolder(RestCsrfConfig config, VertxHttpConfig httpConfig, VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        if (getProgrammaticCsrfConfig(httpConfig, httpBuildTimeConfig) instanceof RestCsrfConfig programmaticConfig) {
            this.config = programmaticConfig;
        } else {
            this.config = config;
        }
    }

    RestCsrfConfig getConfig() {
        return config;
    }
}
