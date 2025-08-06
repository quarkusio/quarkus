package io.quarkus.swaggerui.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.swagger-ui")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SwaggerUiRuntimeConfig {

    /**
     * If Swagger UI is included, it should be enabled/disabled. By default, Swagger UI is enabled if it is included (see
     * {@code always-include}).
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * If Swagger UI is included, it should be enabled/disabled. By default, Swagger UI is enabled if it is included (see
     * {@code always-include}).
     *
     * @deprecated use {@code quarkus.swagger-ui.enabled} instead
     */
    @Deprecated(since = "3.26", forRemoval = true)
    Optional<Boolean> enable();

}
