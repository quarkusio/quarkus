package io.quarkus.gcp.functions;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.google-cloud-functions")
public interface GoogleCloudFunctionsConfig {
    /**
     * The function name. Function names are specified on function classes using the {@link @jakarta.inject.Named} annotation.
     *
     * If this name is unspecified and there is exactly one unnamed function then this unnamed function will be used.
     * If there is only a single named function and the name is unspecified then the named function will be used.
     * These rules apply for each function implementation (HttpFunction, BackgroundFunction, RawBackgroundFunction).
     */
    Optional<String> function();
}
