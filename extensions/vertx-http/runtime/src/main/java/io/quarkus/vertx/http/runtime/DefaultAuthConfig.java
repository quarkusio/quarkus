package io.quarkus.vertx.http.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

/**
 * Mapping for Quarkus Security fluent API defaults
 */
@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DefaultAuthConfig {
    @WithParentName
    @ConfigDocIgnore
    Defaults defaults();

    default AuthRuntimeConfig auth() {
        return defaults().auth().get("defaults");
    }

    default CORSConfig cors() {
        return defaults().cors().get("defaults");
    }

    interface Defaults {
        @WithUnnamedKey
        @WithDefaults
        Map<String, AuthRuntimeConfig> auth();

        @WithUnnamedKey
        @WithDefaults
        Map<String, CORSConfig> cors();
    }
}
